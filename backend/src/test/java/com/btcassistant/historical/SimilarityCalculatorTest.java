package com.btcassistant.historical;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SimilarityCalculatorTest {

    private final SimilarityCalculator calculator = new SimilarityCalculator();
    private final HistoricalProperties.Weights weights = new HistoricalProperties.Weights();

    @Test
    void identicalVectorsHaveZeroDistanceAndMaxSimilarity() {
        Map<String, BigDecimal> vector = Map.of(
                HistoricalFeatureNames.MAGNITUDE, new BigDecimal("0.05"),
                HistoricalFeatureNames.DURATION, new BigDecimal("15"),
                HistoricalFeatureNames.VOLUME_RATIO, new BigDecimal("1.2"),
                HistoricalFeatureNames.VOLATILITY, new BigDecimal("0.02"),
                HistoricalFeatureNames.MOMENTUM, new BigDecimal("0.01"));

        BigDecimal distance = calculator.distance(vector, vector, weights);

        assertThat(distance).isEqualByComparingTo("0");
        assertThat(calculator.similarityScore(distance)).isEqualByComparingTo("1");
    }

    @Test
    void largerDifferenceYieldsLowerSimilarity() {
        Map<String, BigDecimal> a = Map.of(HistoricalFeatureNames.MAGNITUDE, new BigDecimal("0.05"));
        Map<String, BigDecimal> bClose = Map.of(HistoricalFeatureNames.MAGNITUDE, new BigDecimal("0.06"));
        Map<String, BigDecimal> bFar = Map.of(HistoricalFeatureNames.MAGNITUDE, new BigDecimal("0.50"));

        BigDecimal distanceClose = calculator.distance(a, bClose, weights);
        BigDecimal distanceFar = calculator.distance(a, bFar, weights);

        assertThat(calculator.similarityScore(distanceClose)).isGreaterThan(calculator.similarityScore(distanceFar));
    }

    @Test
    void missingDimensionInEitherSideIsExcludedAndWeightRenormalized() {
        Map<String, BigDecimal> a = Map.of(
                HistoricalFeatureNames.MAGNITUDE, new BigDecimal("0.05"),
                HistoricalFeatureNames.DURATION, new BigDecimal("15"));
        Map<String, BigDecimal> b = Map.of(HistoricalFeatureNames.MAGNITUDE, new BigDecimal("0.05"));

        // só magnitude está em comum e é idêntica -> distância deveria ser zero, não penalizada
        // pela ausência de duration do outro lado
        BigDecimal distance = calculator.distance(a, b, weights);

        assertThat(distance).isEqualByComparingTo("0");
    }

    @Test
    void noCommonDimensionsYieldsMaximalDistance() {
        Map<String, BigDecimal> a = Map.of(HistoricalFeatureNames.MAGNITUDE, new BigDecimal("0.05"));
        Map<String, BigDecimal> b = Map.of(HistoricalFeatureNames.DURATION, new BigDecimal("15"));

        BigDecimal distance = calculator.distance(a, b, weights);

        assertThat(distance).isEqualByComparingTo("10");
    }
}
