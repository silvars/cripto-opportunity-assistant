package com.btcassistant.opportunity;

import com.btcassistant.risk.RiskAssessment;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Entidade de persistência — imutável, uma linha por avaliação (SDD Fase 3 §3). Nunca exposta diretamente na API (SDD §70). */
@Entity
@Table(name = "opportunities")
public class MarketOpportunityEntity {

    @Id
    private UUID opportunityId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Classification classification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Direction direction;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "score_components", nullable = false, columnDefinition = "jsonb")
    private Map<String, BigDecimal> scoreComponents;

    @Column(name = "available_evidence_weight", nullable = false, precision = 4, scale = 3)
    private BigDecimal availableEvidenceWeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_quality", nullable = false, length = 10)
    private EvidenceQuality evidenceQuality;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 10)
    private RiskLevel riskLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risk_reasons", nullable = false, columnDefinition = "jsonb")
    private List<String> riskReasons;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_mode", nullable = false, length = 10)
    private ExecutionMode executionMode;

    @Column(name = "config_version", nullable = false, length = 10)
    private String configVersion;

    @Column(name = "engine_version", nullable = false, length = 10)
    private String engineVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MarketOpportunityEntity() {
        // JPA
    }

    public static MarketOpportunityEntity from(UUID eventId, String symbol, OpportunityResult result, RiskAssessment risk) {
        MarketOpportunityEntity entity = new MarketOpportunityEntity();
        entity.opportunityId = UUID.randomUUID();
        entity.eventId = eventId;
        entity.symbol = symbol;
        entity.classification = result.classification();
        entity.direction = result.direction();
        entity.score = result.score();
        entity.scoreComponents = result.scoreComponents();
        entity.availableEvidenceWeight = result.availableEvidenceWeight();
        entity.evidenceQuality = result.evidenceQuality();
        entity.riskLevel = risk.level();
        entity.riskReasons = risk.reasons();
        entity.executionMode = ExecutionMode.LIVE;
        entity.configVersion = result.configVersion();
        entity.engineVersion = result.engineVersion();
        entity.createdAt = Instant.now();
        return entity;
    }

    public UUID getOpportunityId() {
        return opportunityId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getSymbol() {
        return symbol;
    }

    public Classification getClassification() {
        return classification;
    }

    public Direction getDirection() {
        return direction;
    }

    public BigDecimal getScore() {
        return score;
    }

    public Map<String, BigDecimal> getScoreComponents() {
        return scoreComponents;
    }

    public BigDecimal getAvailableEvidenceWeight() {
        return availableEvidenceWeight;
    }

    public EvidenceQuality getEvidenceQuality() {
        return evidenceQuality;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public List<String> getRiskReasons() {
        return riskReasons;
    }

    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public String getConfigVersion() {
        return configVersion;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
