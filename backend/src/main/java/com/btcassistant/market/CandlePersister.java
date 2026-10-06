package com.btcassistant.market;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validação + dedupe + persistência compartilhada entre ingestão live, gap recovery e backfill
 * inicial (SDD Fase 0 §8 — evitar duplicação de lógica entre os mecanismos).
 */
/**
 * Validação + dedupe + persistência compartilhada entre ingestão live, gap recovery e backfill
 * inicial (SDD Fase 0 §8 — evitar duplicação de lógica entre os mecanismos).
 *
 * <p>Não atualiza {@link MarketStatusRegistry} diretamente: em backfill em massa isso geraria uma
 * query de contagem e uma publicação WebSocket por candle. Quem usa este componente em lote
 * (backfill/gap recovery) deve chamar {@code MarketStatusRegistry.refreshCounts} uma vez por
 * lote; a ingestão live (1 candle/minuto) atualiza por candle sem problema de performance.</p>
 */
@Component
public class CandlePersister {

    private static final Logger log = LoggerFactory.getLogger(CandlePersister.class);

    private final CandleRepository candleRepository;

    public CandlePersister(CandleRepository candleRepository) {
        this.candleRepository = candleRepository;
    }

    /** @return true se o candle foi persistido agora (novo); false se inválido ou já existente. */
    public boolean persist(Candle candle) {
        CandleValidator.ValidationResult result = CandleValidator.validate(candle);
        if (!result.valid()) {
            log.warn("candle_rejected symbol={} openTime={} reason={}", candle.symbol(), candle.openTime(), result.reason());
            return false;
        }

        boolean alreadyExists = candleRepository
                .findBySymbolAndIntervalAndOpenTime(candle.symbol(), candle.interval(), candle.openTime())
                .isPresent();
        if (alreadyExists) {
            return false;
        }

        candleRepository.save(CandleEntity.from(candle));
        log.info("candle_persisted symbol={} interval={} openTime={}", candle.symbol(), candle.interval(), candle.openTime());
        return true;
    }
}
