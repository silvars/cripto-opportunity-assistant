package com.btcassistant.operation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

/** Config-driven — fee/slippage padrão nunca hardcoded (SDD §5). */
@ConfigurationProperties(prefix = "operations")
public class OperationProperties {

    private BigDecimal defaultFeeRate = new BigDecimal("0.001");
    private BigDecimal defaultSlippageEstimate = new BigDecimal("0.0005");
    private Screenshots screenshots = new Screenshots();

    public BigDecimal getDefaultFeeRate() {
        return defaultFeeRate;
    }

    public void setDefaultFeeRate(BigDecimal defaultFeeRate) {
        this.defaultFeeRate = defaultFeeRate;
    }

    public BigDecimal getDefaultSlippageEstimate() {
        return defaultSlippageEstimate;
    }

    public void setDefaultSlippageEstimate(BigDecimal defaultSlippageEstimate) {
        this.defaultSlippageEstimate = defaultSlippageEstimate;
    }

    public Screenshots getScreenshots() {
        return screenshots;
    }

    public void setScreenshots(Screenshots screenshots) {
        this.screenshots = screenshots;
    }

    public static class Screenshots {

        private String storagePath = "./data/screenshots";
        private long maxSizeBytes = 5_242_880; // 5MB
        private List<String> allowedContentTypes = List.of("image/png", "image/jpeg", "image/webp");

        public String getStoragePath() {
            return storagePath;
        }

        public void setStoragePath(String storagePath) {
            this.storagePath = storagePath;
        }

        public long getMaxSizeBytes() {
            return maxSizeBytes;
        }

        public void setMaxSizeBytes(long maxSizeBytes) {
            this.maxSizeBytes = maxSizeBytes;
        }

        public List<String> getAllowedContentTypes() {
            return allowedContentTypes;
        }

        public void setAllowedContentTypes(List<String> allowedContentTypes) {
            this.allowedContentTypes = allowedContentTypes;
        }
    }
}
