package com.btcassistant.historical;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Configuração do Historical Analyzer — versão, pesos de similaridade e tamanho mínimo de
 * amostra (SDD §5/§29). {@code distanceToExtreme} é computado e armazenado, mas não participa do
 * cálculo de distância — o SDD não define um peso para ele (ver docs/phases/FASE-4-historical-analyzer.md §11).
 */
@ConfigurationProperties(prefix = "historical-analyzer")
public class HistoricalProperties {

    private String version = "1.0";
    private String canonicalHorizon = "1h";
    private Similarity similarity = new Similarity();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /** Horizonte usado por padrão para alimentar o componente {@code historicalSupport} do score (SDD Fase 4 §11). */
    public String getCanonicalHorizon() {
        return canonicalHorizon;
    }

    public void setCanonicalHorizon(String canonicalHorizon) {
        this.canonicalHorizon = canonicalHorizon;
    }

    public Similarity getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Similarity similarity) {
        this.similarity = similarity;
    }

    public static class Similarity {

        private Weights weights = new Weights();
        private int minSampleSize = 30;
        private int maxSimilarEvents = 50;

        public Weights getWeights() {
            return weights;
        }

        public void setWeights(Weights weights) {
            this.weights = weights;
        }

        public int getMinSampleSize() {
            return minSampleSize;
        }

        public void setMinSampleSize(int minSampleSize) {
            this.minSampleSize = minSampleSize;
        }

        public int getMaxSimilarEvents() {
            return maxSimilarEvents;
        }

        public void setMaxSimilarEvents(int maxSimilarEvents) {
            this.maxSimilarEvents = maxSimilarEvents;
        }
    }

    public static class Weights {

        private BigDecimal magnitude = new BigDecimal("0.3");
        private BigDecimal duration = new BigDecimal("0.15");
        private BigDecimal volumeRatio = new BigDecimal("0.2");
        private BigDecimal volatility = new BigDecimal("0.2");
        private BigDecimal momentum = new BigDecimal("0.15");

        public BigDecimal getMagnitude() {
            return magnitude;
        }

        public void setMagnitude(BigDecimal magnitude) {
            this.magnitude = magnitude;
        }

        public BigDecimal getDuration() {
            return duration;
        }

        public void setDuration(BigDecimal duration) {
            this.duration = duration;
        }

        public BigDecimal getVolumeRatio() {
            return volumeRatio;
        }

        public void setVolumeRatio(BigDecimal volumeRatio) {
            this.volumeRatio = volumeRatio;
        }

        public BigDecimal getVolatility() {
            return volatility;
        }

        public void setVolatility(BigDecimal volatility) {
            this.volatility = volatility;
        }

        public BigDecimal getMomentum() {
            return momentum;
        }

        public void setMomentum(BigDecimal momentum) {
            this.momentum = momentum;
        }
    }
}
