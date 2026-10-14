package com.btcassistant.agent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entidade de persistência — insert-only (SDD §3). Reanálise cria {@code analysis_version}
 * incremental; a análise original nunca é sobrescrita. Nunca exposta diretamente na API (SDD §70).
 */
@Entity
@Table(name = "agent_analyses", uniqueConstraints = @UniqueConstraint(name = "uq_alert_analysis_version", columnNames = {"alert_id", "analysis_version"}))
public class AgentAnalysisEntity {

    @Id
    private UUID analysisId;

    @Column(name = "alert_id", nullable = false)
    private UUID alertId;

    @Column(name = "analysis_version", nullable = false)
    private int analysisVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private com.btcassistant.opportunity.Classification classification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private com.btcassistant.opportunity.Direction direction;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Confidence confidence;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> evidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "historical_context", columnDefinition = "jsonb")
    private Map<String, Object> historicalContext;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> risk;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> invalidations;

    @Column(name = "agent_version", nullable = false, length = 10)
    private String agentVersion;

    @Column(name = "prompt_version", nullable = false, length = 10)
    private String promptVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalysisStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AgentAnalysisEntity() {
        // JPA
    }

    public static AgentAnalysisEntity create(UUID alertId, int analysisVersion, AgentAnalysis analysis) {
        AgentAnalysisEntity entity = new AgentAnalysisEntity();
        entity.analysisId = UUID.randomUUID();
        entity.alertId = alertId;
        entity.analysisVersion = analysisVersion;
        entity.classification = analysis.classification();
        entity.direction = analysis.direction();
        entity.score = analysis.score();
        entity.confidence = analysis.confidence();
        entity.summary = analysis.summary();
        entity.evidence = analysis.evidence();
        entity.historicalContext = analysis.historicalContext() == null ? null
                : Map.of("sampleSize", analysis.historicalContext().sampleSize(),
                        "targetBeforeStopProbability", analysis.historicalContext().targetBeforeStopProbability());
        entity.risk = Map.of("level", analysis.risk().level().name(), "reasons", analysis.risk().reasons());
        entity.invalidations = analysis.invalidations();
        entity.agentVersion = analysis.agentVersion();
        entity.promptVersion = analysis.promptVersion();
        entity.status = analysis.status();
        entity.createdAt = Instant.now();
        return entity;
    }

    public UUID getAnalysisId() {
        return analysisId;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public int getAnalysisVersion() {
        return analysisVersion;
    }

    public com.btcassistant.opportunity.Classification getClassification() {
        return classification;
    }

    public com.btcassistant.opportunity.Direction getDirection() {
        return direction;
    }

    public BigDecimal getScore() {
        return score;
    }

    public Confidence getConfidence() {
        return confidence;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getEvidence() {
        return evidence;
    }

    public Map<String, Object> getHistoricalContext() {
        return historicalContext;
    }

    public Map<String, Object> getRisk() {
        return risk;
    }

    public List<String> getInvalidations() {
        return invalidations;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public AnalysisStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
