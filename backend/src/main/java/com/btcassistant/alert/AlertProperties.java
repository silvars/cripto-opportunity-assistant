package com.btcassistant.alert;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Cooldown e janela de deduplicação — config-driven (SDD §41/§43, Fase 3 §5). */
@ConfigurationProperties(prefix = "alerts")
public class AlertProperties {

    private Duration cooldown = Duration.ofMinutes(15);
    private Duration dedupWindow = Duration.ofMinutes(15);

    public Duration getCooldown() {
        return cooldown;
    }

    public void setCooldown(Duration cooldown) {
        this.cooldown = cooldown;
    }

    public Duration getDedupWindow() {
        return dedupWindow;
    }

    public void setDedupWindow(Duration dedupWindow) {
        this.dedupWindow = dedupWindow;
    }
}
