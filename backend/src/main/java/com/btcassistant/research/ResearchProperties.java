package com.btcassistant.research;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Config-driven — caminho dos experimentos e horizontes de avaliação nunca hardcoded (SDD §5). */
@ConfigurationProperties(prefix = "research")
public class ResearchProperties {

    private String experimentsPath = "research/experiments";
    private Evaluation evaluation = new Evaluation();

    public String getExperimentsPath() {
        return experimentsPath;
    }

    public void setExperimentsPath(String experimentsPath) {
        this.experimentsPath = experimentsPath;
    }

    public Evaluation getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;
    }

    public static class Evaluation {

        /** Horizontes de {@code alert_evaluations} (SDD §58) — nota: não inclui "2h" (ver FASE-7 §11). */
        private List<String> horizons = List.of("5m", "15m", "30m", "1h", "4h", "12h", "24h");
        private String canonicalHorizon = "24h";
        private long fixedDelayMs = 300_000;

        public List<String> getHorizons() {
            return horizons;
        }

        public void setHorizons(List<String> horizons) {
            this.horizons = horizons;
        }

        public String getCanonicalHorizon() {
            return canonicalHorizon;
        }

        public void setCanonicalHorizon(String canonicalHorizon) {
            this.canonicalHorizon = canonicalHorizon;
        }

        public long getFixedDelayMs() {
            return fixedDelayMs;
        }

        public void setFixedDelayMs(long fixedDelayMs) {
            this.fixedDelayMs = fixedDelayMs;
        }
    }
}
