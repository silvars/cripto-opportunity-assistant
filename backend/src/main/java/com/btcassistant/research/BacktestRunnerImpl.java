package com.btcassistant.research;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.event.EventProperties;
import com.btcassistant.event.EventRule;
import com.btcassistant.event.FeatureHistory;
import com.btcassistant.feature.FeatureEngine;
import com.btcassistant.feature.FeatureProperties;
import com.btcassistant.feature.FeatureSnapshot;
import com.btcassistant.historical.HistoricalAnalyzer;
import com.btcassistant.historical.HistoricalContext;
import com.btcassistant.historical.HistoricalHorizons;
import com.btcassistant.historical.HistoricalOutcomeCalculator;
import com.btcassistant.historical.HistoricalOutcomeResult;
import com.btcassistant.historical.HistoricalProperties;
import com.btcassistant.historical.ResearchOutcomeProperties;
import com.btcassistant.market.Candle;
import com.btcassistant.market.CandleEntity;
import com.btcassistant.market.CandleRepository;
import com.btcassistant.market.Instrument;
import com.btcassistant.market.MarketProperties;
import com.btcassistant.opportunity.Classification;
import com.btcassistant.opportunity.OpportunityEngine;
import com.btcassistant.opportunity.OpportunityProperties;
import com.btcassistant.opportunity.OpportunityResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Reroda a MESMA lógica determinística do pipeline em tempo real (feature → event → opportunity →
 * historical context), mas inteiramente em memória — nunca escreve em
 * {@code market_features}/{@code market_events}/{@code opportunities}/{@code alerts} (ver
 * docs/phases/FASE-7-research.md §11: motivo é evitar qualquer risco de contaminar o dashboard
 * operacional ou o {@code AgentOrchestrator}, sem precisar filtrar {@code executionMode} em cada
 * consulta existente). Apenas o {@link BacktestResult} agregado é persistido (em
 * {@code experiment_runs}, fora desta classe).
 */
@Component
public class BacktestRunnerImpl implements BacktestRunner {

    private final MarketProperties marketProperties;
    private final CandleRepository candleRepository;
    private final FeatureEngine featureEngine;
    private final FeatureProperties featureProperties;
    private final EventProperties eventProperties;
    private final List<EventRule> eventRules;
    private final OpportunityEngine opportunityEngine;
    private final OpportunityProperties opportunityProperties;
    private final HistoricalAnalyzer historicalAnalyzer;
    private final HistoricalProperties historicalProperties;
    private final ResearchOutcomeProperties outcomeProperties;
    private final HistoricalOutcomeCalculator outcomeCalculator;

    private static final int HISTORY_SIZE = 5;

    public BacktestRunnerImpl(MarketProperties marketProperties, CandleRepository candleRepository,
                              FeatureEngine featureEngine, FeatureProperties featureProperties,
                              EventProperties eventProperties, List<EventRule> eventRules,
                              OpportunityEngine opportunityEngine, OpportunityProperties opportunityProperties,
                              HistoricalAnalyzer historicalAnalyzer,
                              HistoricalProperties historicalProperties, ResearchOutcomeProperties outcomeProperties,
                              HistoricalOutcomeCalculator outcomeCalculator) {
        this.marketProperties = marketProperties;
        this.candleRepository = candleRepository;
        this.featureEngine = featureEngine;
        this.featureProperties = featureProperties;
        this.eventProperties = eventProperties;
        this.eventRules = eventRules;
        this.opportunityEngine = opportunityEngine;
        this.opportunityProperties = opportunityProperties;
        this.historicalAnalyzer = historicalAnalyzer;
        this.historicalProperties = historicalProperties;
        this.outcomeProperties = outcomeProperties;
        this.outcomeCalculator = outcomeCalculator;
    }

    @Override
    public BacktestResult run(BacktestConfig config) {
        validateVersions(config);

        Instrument instrument = marketProperties.findEnabledBySymbol(config.symbol());
        if (instrument == null) {
            throw new IllegalArgumentException("Instrumento não configurado/habilitado: " + config.symbol());
        }

        Instant startedAt = Instant.now();
        List<CandleEntity> candles = candleRepository.findBySymbolAndIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                config.symbol(), instrument.interval(), config.from(), config.to());

        List<FeatureSnapshot> recentHistory = new ArrayList<>();
        int eventsDetected = 0;
        int opportunitiesGenerated = 0;
        Map<String, List<DetectedEvent>> eventsByType = new LinkedHashMap<>();

        for (CandleEntity candleEntity : candles) {
            Candle candle = candleEntity.toCandle();
            FeatureSnapshot features = featureEngine.calculate(instrument, candle.openTime());
            if (features == null) {
                continue;
            }

            FeatureHistory history = new FeatureHistory(List.copyOf(recentHistory));
            for (EventRule rule : eventRules) {
                Optional<DetectedEvent> detected = rule.evaluate(candle.close(), features, history);
                if (detected.isEmpty()) {
                    continue;
                }
                DetectedEvent event = detected.get();
                eventsDetected++;
                eventsByType.computeIfAbsent(event.eventType(), k -> new ArrayList<>()).add(event);

                OpportunityResult opportunity = evaluateOpportunity(event, features);
                if (opportunity.classification() != Classification.NONE) {
                    opportunitiesGenerated++;
                }
            }

            recentHistory.add(features);
            if (recentHistory.size() > HISTORY_SIZE) {
                recentHistory.remove(0);
            }
        }

        Map<String, EventTypeSummary> summaryByEventType = new LinkedHashMap<>();
        for (var entry : eventsByType.entrySet()) {
            summaryByEventType.put(entry.getKey(), summarize(entry.getValue(), instrument, config));
        }

        Instant finishedAt = Instant.now();
        return new BacktestResult(config, candles.size(), eventsDetected, opportunitiesGenerated,
                summaryByEventType, startedAt, finishedAt);
    }

    private OpportunityResult evaluateOpportunity(DetectedEvent event, FeatureSnapshot features) {
        HistoricalContext historicalContext = historicalAnalyzer.findSimilar(event, event.eventTime());
        Optional<HistoricalContext> historicalContextOptional = historicalContext.outcomesByHorizon().isEmpty()
                ? Optional.empty() : Optional.of(historicalContext);
        return opportunityEngine.evaluate(event, features, historicalContextOptional);
    }

    private EventTypeSummary summarize(List<DetectedEvent> events, Instrument instrument, BacktestConfig config) {
        Map<String, List<BigDecimal>> futureReturnsByHorizon = new LinkedHashMap<>();
        Map<String, List<BigDecimal>> mfeByHorizon = new LinkedHashMap<>();
        Map<String, List<BigDecimal>> maeByHorizon = new LinkedHashMap<>();
        Map<String, Integer> targetBeforeStopCountByHorizon = new LinkedHashMap<>();
        Map<String, Integer> resolvedCountByHorizon = new LinkedHashMap<>();

        for (String horizon : HistoricalHorizons.LABELS) {
            futureReturnsByHorizon.put(horizon, new ArrayList<>());
            mfeByHorizon.put(horizon, new ArrayList<>());
            maeByHorizon.put(horizon, new ArrayList<>());
            targetBeforeStopCountByHorizon.put(horizon, 0);
            resolvedCountByHorizon.put(horizon, 0);
        }

        for (DetectedEvent event : events) {
            for (String horizon : HistoricalHorizons.LABELS) {
                Duration duration = HistoricalHorizons.ALL.get(horizon);
                if (event.eventTime().plus(duration).isAfter(config.to())) {
                    continue; // horizonte ultrapassaria o limite declarado do backtest — nunca extrapola
                }
                Optional<HistoricalOutcomeResult> outcome = computeOutcome(
                        event, instrument, horizon);
                outcome.ifPresent(result -> {
                    futureReturnsByHorizon.get(horizon).add(result.futureReturn());
                    mfeByHorizon.get(horizon).add(result.mfe());
                    maeByHorizon.get(horizon).add(result.mae());
                    if (result.targetBeforeStop() != null) {
                        resolvedCountByHorizon.merge(horizon, 1, Integer::sum);
                        if (Boolean.TRUE.equals(result.targetBeforeStop())) {
                            targetBeforeStopCountByHorizon.merge(horizon, 1, Integer::sum);
                        }
                    }
                });
            }
        }

        Map<String, HorizonOutcomeStats> statsByHorizon = new LinkedHashMap<>();
        for (String horizon : HistoricalHorizons.LABELS) {
            List<BigDecimal> returns = futureReturnsByHorizon.get(horizon);
            if (returns.isEmpty()) {
                continue;
            }
            int resolved = resolvedCountByHorizon.get(horizon);
            BigDecimal targetBeforeStopRate = resolved == 0 ? null
                    : BigDecimal.valueOf(targetBeforeStopCountByHorizon.get(horizon))
                            .divide(BigDecimal.valueOf(resolved), MathContext.DECIMAL64);
            statsByHorizon.put(horizon, new HorizonOutcomeStats(returns.size(), mean(returns), mean(mfeByHorizon.get(horizon)),
                    mean(maeByHorizon.get(horizon)), targetBeforeStopRate));
        }

        return new EventTypeSummary(events.size(), statsByHorizon);
    }

    private Optional<HistoricalOutcomeResult> computeOutcome(DetectedEvent event, Instrument instrument, String horizon) {
        UUID syntheticId = UUID.nameUUIDFromBytes((event.symbol() + event.eventType() + event.eventTime()).getBytes(StandardCharsets.UTF_8));
        return outcomeCalculator.calculate(syntheticId, event.symbol(), instrument.interval(), event.eventTime(), event.price(), horizon);
    }

    private static BigDecimal mean(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            sum = sum.add(value);
        }
        return sum.divide(BigDecimal.valueOf(values.size()), MathContext.DECIMAL64);
    }

    private void validateVersions(BacktestConfig config) {
        requireMatch("engineVersion", config.engineVersion(), eventProperties.getEngineVersion());
        requireMatch("featureVersion", config.featureVersion(), featureProperties.getVersion());
        requireMatch("configVersion", config.configVersion(), opportunityProperties.getConfigVersion());
        requireMatch("historicalVersion", config.historicalVersion(), historicalProperties.getVersion());
        requireMatch("targetDefinitionVersion", config.targetDefinitionVersion(), outcomeProperties.getTargetDefinitionVersion());
        requireMatch("stopDefinitionVersion", config.stopDefinitionVersion(), outcomeProperties.getStopDefinitionVersion());
    }

    private static void requireMatch(String field, String requested, String actual) {
        if (requested != null && !requested.equals(actual)) {
            throw new IllegalArgumentException(field + " solicitado (" + requested + ") não bate com o configurado (" + actual
                    + ") — BacktestRunner não troca configuração em tempo de execução (ver FASE-7 §11)");
        }
    }
}
