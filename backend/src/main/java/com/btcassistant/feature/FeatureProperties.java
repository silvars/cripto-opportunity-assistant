package com.btcassistant.feature;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuração do Feature Engine — versão e janelas (SDD Fase 1 §5). */
@ConfigurationProperties(prefix = "feature-engine")
public class FeatureProperties {

    private String version = "1.0";
    private Windows windows = new Windows();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Windows getWindows() {
        return windows;
    }

    public void setWindows(Windows windows) {
        this.windows = windows;
    }

    public static class Windows {

        /** Número de candles mais recentes usado em volume_sma/ratio/zscore. */
        private int volumeSma = 20;

        public int getVolumeSma() {
            return volumeSma;
        }

        public void setVolumeSma(int volumeSma) {
            this.volumeSma = volumeSma;
        }
    }
}
