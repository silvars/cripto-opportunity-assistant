package com.btcassistant.alert;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.opportunity.Classification;
import com.btcassistant.opportunity.Direction;
import com.btcassistant.opportunity.ExecutionMode;
import com.btcassistant.opportunity.MarketOpportunityEntity;
import com.btcassistant.risk.RiskLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entidade de persistência (SDD §42). Apenas {@code status} muda ao longo do lifecycle — todos os
 * demais campos são fixados na criação e nunca alterados (ver docs/phases/FASE-3-opportunity-risk.md §3/§11).
 */
@Entity
@Table(name = "alerts")
public class AlertEntity {

    @Id
    private UUID alertId;

    @Column(name = "opportunity_id", nullable = false)
    private UUID opportunityId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Direction direction;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Classification classification;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 10)
    private RiskLevel riskLevel;

    @Column(name = "price_at_alert", nullable = false, precision = 20, scale = 8)
    private BigDecimal priceAtAlert;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "market_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, BigDecimal> marketSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "feature_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, BigDecimal> featureSnapshot;

    @Column(name = "engine_version", nullable = false, length = 10)
    private String engineVersion;

    @Column(name = "config_version", nullable = false, length = 10)
    private String configVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_mode", nullable = false, length = 10)
    private ExecutionMode executionMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected AlertEntity() {
        // JPA
    }

    public static AlertEntity from(MarketOpportunityEntity opportunity, DetectedEvent event, Instant expiresAt) {
        AlertEntity entity = new AlertEntity();
        entity.alertId = UUID.randomUUID();
        entity.opportunityId = opportunity.getOpportunityId();
        entity.symbol = opportunity.getSymbol();
        entity.direction = opportunity.getDirection();
        entity.score = opportunity.getScore();
        entity.classification = opportunity.getClassification();
        entity.riskLevel = opportunity.getRiskLevel();
        entity.priceAtAlert = event.price();
        entity.marketSnapshot = Map.of("price", event.price());
        entity.featureSnapshot = event.featuresSnapshot();
        entity.engineVersion = opportunity.getEngineVersion();
        entity.configVersion = opportunity.getConfigVersion();
        entity.executionMode = opportunity.getExecutionMode();
        entity.status = AlertStatus.DETECTED;
        entity.createdAt = Instant.now();
        entity.expiresAt = expiresAt;
        return entity;
    }

    /** Única mutação permitida ao longo do lifecycle (SDD §42 — "apenas status muda"). */
    public void transitionTo(AlertStatus newStatus) {
        this.status = newStatus;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public UUID getOpportunityId() {
        return opportunityId;
    }

    public String getSymbol() {
        return symbol;
    }

    public Direction getDirection() {
        return direction;
    }

    public BigDecimal getScore() {
        return score;
    }

    public Classification getClassification() {
        return classification;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public BigDecimal getPriceAtAlert() {
        return priceAtAlert;
    }

    public Map<String, BigDecimal> getMarketSnapshot() {
        return marketSnapshot;
    }

    public Map<String, BigDecimal> getFeatureSnapshot() {
        return featureSnapshot;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public String getConfigVersion() {
        return configVersion;
    }

    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
