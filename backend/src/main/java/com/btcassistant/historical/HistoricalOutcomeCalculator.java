package com.btcassistant.historical;

import com.btcassistant.market.Candle;
import com.btcassistant.market.CandleRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Calcula um único outcome (evento, horizonte) a partir de candles futuros reais — nunca estima
 * (SDD §30). Só é computável quando o candle exatamente em {@code evaluationEnd} já existe; caso
 * contrário retorna {@link Optional#empty()} (o horizonte ainda não decorreu de verdade).
 */
@Component
public class HistoricalOutcomeCalculator {

    private final CandleRepository candleRepository;
    private final ResearchOutcomeProperties properties;

    public HistoricalOutcomeCalculator(CandleRepository candleRepository, ResearchOutcomeProperties properties) {
        this.candleRepository = candleRepository;
        this.properties = properties;
    }

    public Optional<HistoricalOutcomeResult> calculate(UUID eventId, String symbol, String interval,
                                                        Instant eventTime, BigDecimal priceAtEvent, String horizonLabel) {
        Duration horizon = HistoricalHorizons.ALL.get(horizonLabel);
        Instant evaluationEnd = eventTime.plus(horizon);

        boolean endCandleExists = candleRepository.findBySymbolAndIntervalAndOpenTime(symbol, interval, evaluationEnd).isPresent();
        if (!endCandleExists) {
            return Optional.empty();
        }

        List<Candle> window = candleRepository
                .findBySymbolAndIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(symbol, interval, eventTime.plusSeconds(1), evaluationEnd)
                .stream()
                .map(com.btcassistant.market.CandleEntity::toCandle)
                .toList();
        if (window.isEmpty()) {
            return Optional.empty();
        }

        Candle lastCandle = window.get(window.size() - 1);
        BigDecimal futureReturn = lastCandle.close().subtract(priceAtEvent).divide(priceAtEvent, MathContext.DECIMAL64);

        BigDecimal mfe = window.stream()
                .map(c -> c.high().subtract(priceAtEvent).divide(priceAtEvent, MathContext.DECIMAL64))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal mae = window.stream()
                .map(c -> c.low().subtract(priceAtEvent).divide(priceAtEvent, MathContext.DECIMAL64))
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal targetPrice = priceAtEvent.multiply(BigDecimal.ONE.add(properties.getTargetPercentage()));
        BigDecimal stopPrice = priceAtEvent.multiply(BigDecimal.ONE.subtract(properties.getStopPercentage()));

        Duration timeToTarget = null;
        Duration timeToStop = null;
        for (Candle candle : window) {
            if (timeToTarget == null && candle.high().compareTo(targetPrice) >= 0) {
                timeToTarget = Duration.between(eventTime, candle.closeTime());
            }
            if (timeToStop == null && candle.low().compareTo(stopPrice) <= 0) {
                timeToStop = Duration.between(eventTime, candle.closeTime());
            }
            if (timeToTarget != null && timeToStop != null) {
                break;
            }
        }

        Boolean targetBeforeStop;
        if (timeToTarget != null && timeToStop != null) {
            // empate no mesmo candle (granularidade de 1m): assume o stop primeiro — decisão conservadora
            targetBeforeStop = timeToTarget.compareTo(timeToStop) < 0;
        } else if (timeToTarget != null) {
            targetBeforeStop = true;
        } else if (timeToStop != null) {
            targetBeforeStop = false;
        } else {
            targetBeforeStop = null;
        }

        return Optional.of(new HistoricalOutcomeResult(eventId, horizonLabel, futureReturn, mfe, mae,
                timeToTarget, timeToStop, targetBeforeStop, evaluationEnd,
                properties.getTargetDefinitionVersion(), properties.getStopDefinitionVersion()));
    }
}
