package com.btcassistant.historical;

import com.btcassistant.event.MarketEventRepository;
import com.btcassistant.market.MarketProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Varre eventos em busca de outcomes ainda não computados mas cujo horizonte já decorreu de
 * verdade (SDD §30). Varredura simples (scan completo de {@code market_events} a cada execução) —
 * aceitável nesta fase; se o volume crescer, considerar rastrear o progresso incrementalmente
 * antes de otimizações maiores (mesma postura do SDD §10 para a busca de similaridade).
 */
@Service
public class HistoricalOutcomeBackfillService {

    private static final Logger log = LoggerFactory.getLogger(HistoricalOutcomeBackfillService.class);

    private final MarketEventRepository eventRepository;
    private final HistoricalOutcomeRepository outcomeRepository;
    private final HistoricalOutcomeCalculator calculator;
    private final MarketProperties marketProperties;

    public HistoricalOutcomeBackfillService(MarketEventRepository eventRepository,
                                            HistoricalOutcomeRepository outcomeRepository,
                                            HistoricalOutcomeCalculator calculator,
                                            MarketProperties marketProperties) {
        this.eventRepository = eventRepository;
        this.outcomeRepository = outcomeRepository;
        this.calculator = calculator;
        this.marketProperties = marketProperties;
    }

    @Scheduled(fixedDelayString = "${historical-analyzer.backfill.fixed-delay-ms:300000}")
    public void computeMissingOutcomes() {
        int computed = 0;
        for (var event : eventRepository.findAll()) {
            var instrument = marketProperties.findEnabledBySymbol(event.getSymbol());
            if (instrument == null) {
                continue;
            }
            for (String horizon : HistoricalHorizons.LABELS) {
                if (outcomeRepository.findByEventIdAndHorizon(event.getEventId(), horizon).isPresent()) {
                    continue;
                }
                Optional<HistoricalOutcomeResult> result = calculator.calculate(event.getEventId(), event.getSymbol(),
                        instrument.interval(), event.getEventTime(), event.getPrice(), horizon);
                if (result.isPresent()) {
                    outcomeRepository.save(HistoricalOutcomeEntity.from(result.get()));
                    computed++;
                }
            }
        }
        if (computed > 0) {
            log.info("historical_outcomes_backfilled count={}", computed);
        }
    }
}
