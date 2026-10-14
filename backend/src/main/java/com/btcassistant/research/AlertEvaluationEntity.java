package com.btcassistant.research;

import com.btcassistant.opportunity.ExecutionMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Preenchida incrementalmente (SDD §58): cada coluna {@code future_Xh} só recebe valor quando o
 * horizonte correspondente já decorreu de verdade. Uma coluna já preenchida nunca é sobrescrita —
 * diferente de {@code market_events}/{@code opportunities}/{@code alerts}, esta linha permite
 * UPDATE, mas só para preencher o que ainda está {@code NULL} (ver docs/phases/FASE-7-research.md §11).
 */
@Entity
@Table(name = "alert_evaluations")
public class AlertEvaluationEntity {

    @Id
    @Column(name = "alert_id")
    private UUID alertId;

    @Column(name = "future_5m", precision = 10, scale = 6)
    private BigDecimal future5m;
    @Column(name = "future_15m", precision = 10, scale = 6)
    private BigDecimal future15m;
    @Column(name = "future_30m", precision = 10, scale = 6)
    private BigDecimal future30m;
    @Column(name = "future_1h", precision = 10, scale = 6)
    private BigDecimal future1h;
    @Column(name = "future_4h", precision = 10, scale = 6)
    private BigDecimal future4h;
    @Column(name = "future_12h", precision = 10, scale = 6)
    private BigDecimal future12h;
    @Column(name = "future_24h", precision = 10, scale = 6)
    private BigDecimal future24h;

    @Column(precision = 10, scale = 6)
    private BigDecimal mfe;
    @Column(precision = 10, scale = 6)
    private BigDecimal mae;
    @Column(name = "target_before_stop")
    private Boolean targetBeforeStop;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_mode", nullable = false, length = 10)
    private ExecutionMode executionMode;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    protected AlertEvaluationEntity() {
        // JPA
    }

    public static AlertEvaluationEntity create(UUID alertId, ExecutionMode executionMode) {
        AlertEvaluationEntity entity = new AlertEvaluationEntity();
        entity.alertId = alertId;
        entity.executionMode = executionMode;
        entity.evaluatedAt = Instant.now();
        return entity;
    }

    public BigDecimal getHorizonReturn(String horizon) {
        return switch (horizon) {
            case "5m" -> future5m;
            case "15m" -> future15m;
            case "30m" -> future30m;
            case "1h" -> future1h;
            case "4h" -> future4h;
            case "12h" -> future12h;
            case "24h" -> future24h;
            default -> throw new IllegalArgumentException("Horizonte não suportado em alert_evaluations: " + horizon);
        };
    }

    /** Nunca sobrescreve um valor já preenchido (fato de mercado, fixado assim que calculado). */
    public void setHorizonReturn(String horizon, BigDecimal value) {
        if (getHorizonReturn(horizon) != null) {
            return;
        }
        switch (horizon) {
            case "5m" -> future5m = value;
            case "15m" -> future15m = value;
            case "30m" -> future30m = value;
            case "1h" -> future1h = value;
            case "4h" -> future4h = value;
            case "12h" -> future12h = value;
            case "24h" -> future24h = value;
            default -> throw new IllegalArgumentException("Horizonte não suportado em alert_evaluations: " + horizon);
        }
        evaluatedAt = Instant.now();
    }

    public void setOutcomeSummary(BigDecimal mfe, BigDecimal mae, Boolean targetBeforeStop) {
        if (this.mfe != null) {
            return;
        }
        this.mfe = mfe;
        this.mae = mae;
        this.targetBeforeStop = targetBeforeStop;
        this.evaluatedAt = Instant.now();
    }

    public boolean isFullyEvaluated(java.util.List<String> horizons) {
        return horizons.stream().allMatch(h -> getHorizonReturn(h) != null) && mfe != null;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public BigDecimal getMfe() {
        return mfe;
    }

    public BigDecimal getMae() {
        return mae;
    }

    public Boolean getTargetBeforeStop() {
        return targetBeforeStop;
    }

    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }
}
