package com.btcassistant.research;

import com.btcassistant.alert.AlertEntity;
import com.btcassistant.alert.AlertRepository;
import com.btcassistant.historical.HistoricalOutcomeCalculator;
import com.btcassistant.historical.HistoricalOutcomeResult;
import com.btcassistant.market.Instrument;
import com.btcassistant.market.MarketProperties;
import com.btcassistant.opportunity.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Varre alertas em busca de horizontes ainda não avaliados mas que já decorreram de verdade (SDD
 * §58). Mesma postura de {@link com.btcassistant.historical.HistoricalOutcomeBackfillService}:
 * scan completo a cada execução, aceitável nesta fase. Reaproveita
 * {@link HistoricalOutcomeCalculator} (Fase 4) — o cálculo de futureReturn/MFE/MAE/
 * targetBeforeStop a partir de candles futuros reais é o mesmo, só muda o id usado pra rotular o
 * resultado (alertId em vez de eventId).
 */
@Service
public class AlertEvaluator {

    private static final Logger log = LoggerFactory.getLogger(AlertEvaluator.class);

    private final AlertRepository alertRepository;
    private final AlertEvaluationRepository evaluationRepository;
    private final HistoricalOutcomeCalculator outcomeCalculator;
    private final MarketProperties marketProperties;
    private final ResearchProperties properties;

    public AlertEvaluator(AlertRepository alertRepository, AlertEvaluationRepository evaluationRepository,
                          HistoricalOutcomeCalculator outcomeCalculator, MarketProperties marketProperties,
                          ResearchProperties properties) {
        this.alertRepository = alertRepository;
        this.evaluationRepository = evaluationRepository;
        this.outcomeCalculator = outcomeCalculator;
        this.marketProperties = marketProperties;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${research.evaluation.fixed-delay-ms:300000}")
    public void evaluatePendingAlerts() {
        evaluatePendingAlerts(Instant.now());
    }

    public void evaluatePendingAlerts(Instant asOf) {
        int updated = 0;
        for (AlertEntity alert : alertRepository.findAll()) {
            if (alert.getExecutionMode() != ExecutionMode.LIVE || !alert.getCreatedAt().isBefore(asOf)) {
                continue;
            }
            Instrument instrument = marketProperties.findEnabledBySymbol(alert.getSymbol());
            if (instrument == null) {
                continue;
            }

            AlertEvaluationEntity evaluation = evaluationRepository.findById(alert.getAlertId())
                    .orElseGet(() -> AlertEvaluationEntity.create(alert.getAlertId(), alert.getExecutionMode()));
            if (evaluation.isFullyEvaluated(properties.getEvaluation().getHorizons())) {
                continue;
            }

            boolean changed = fillMissingHorizons(alert, instrument, evaluation, asOf);
            if (changed) {
                evaluationRepository.save(evaluation);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("alert_evaluated count={}", updated);
        }
    }

    private boolean fillMissingHorizons(AlertEntity alert, Instrument instrument, AlertEvaluationEntity evaluation, Instant asOf) {
        boolean changed = false;
        String canonicalHorizon = properties.getEvaluation().getCanonicalHorizon();
        for (String horizon : properties.getEvaluation().getHorizons()) {
            if (evaluation.getHorizonReturn(horizon) != null) {
                continue;
            }
            Optional<HistoricalOutcomeResult> result = outcomeCalculator.calculate(alert.getAlertId(), alert.getSymbol(),
                    instrument.interval(), alert.getCreatedAt(), alert.getPriceAtAlert(), horizon);
            if (result.isEmpty()) {
                continue;
            }
            evaluation.setHorizonReturn(horizon, result.get().futureReturn());
            if (horizon.equals(canonicalHorizon)) {
                evaluation.setOutcomeSummary(result.get().mfe(), result.get().mae(), result.get().targetBeforeStop());
            }
            changed = true;
        }
        return changed;
    }
}
