package com.btcassistant.historical;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidade de persistência — insert-only (SDD §30/§68: mudar a metodologia de target/stop gera
 * nova versão, nunca sobrescreve). Nunca exposta diretamente na API (SDD §70).
 */
@Entity
@Table(name = "historical_outcomes", uniqueConstraints = @UniqueConstraint(name = "uq_outcome", columnNames = {"event_id", "horizon"}))
public class HistoricalOutcomeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(nullable = false, length = 10)
    private String horizon;

    @Column(name = "future_return", precision = 10, scale = 6)
    private BigDecimal futureReturn;

    @Column(precision = 10, scale = 6)
    private BigDecimal mfe;

    @Column(precision = 10, scale = 6)
    private BigDecimal mae;

    @Column(name = "time_to_target_seconds")
    private Long timeToTargetSeconds;

    @Column(name = "time_to_stop_seconds")
    private Long timeToStopSeconds;

    @Column(name = "target_before_stop")
    private Boolean targetBeforeStop;

    @Column(name = "evaluation_end", nullable = false)
    private Instant evaluationEnd;

    @Column(name = "target_definition_version", nullable = false, length = 10)
    private String targetDefinitionVersion;

    @Column(name = "stop_definition_version", nullable = false, length = 10)
    private String stopDefinitionVersion;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    protected HistoricalOutcomeEntity() {
        // JPA
    }

    public static HistoricalOutcomeEntity from(HistoricalOutcomeResult result) {
        HistoricalOutcomeEntity entity = new HistoricalOutcomeEntity();
        entity.eventId = result.eventId();
        entity.horizon = result.horizon();
        entity.futureReturn = result.futureReturn();
        entity.mfe = result.mfe();
        entity.mae = result.mae();
        entity.timeToTargetSeconds = result.timeToTarget() == null ? null : result.timeToTarget().getSeconds();
        entity.timeToStopSeconds = result.timeToStop() == null ? null : result.timeToStop().getSeconds();
        entity.targetBeforeStop = result.targetBeforeStop();
        entity.evaluationEnd = result.evaluationEnd();
        entity.targetDefinitionVersion = result.targetDefinitionVersion();
        entity.stopDefinitionVersion = result.stopDefinitionVersion();
        entity.computedAt = Instant.now();
        return entity;
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getHorizon() {
        return horizon;
    }

    public BigDecimal getFutureReturn() {
        return futureReturn;
    }

    public BigDecimal getMfe() {
        return mfe;
    }

    public BigDecimal getMae() {
        return mae;
    }

    public Long getTimeToTargetSeconds() {
        return timeToTargetSeconds;
    }

    public Long getTimeToStopSeconds() {
        return timeToStopSeconds;
    }

    public Boolean getTargetBeforeStop() {
        return targetBeforeStop;
    }

    public Instant getEvaluationEnd() {
        return evaluationEnd;
    }

    public String getTargetDefinitionVersion() {
        return targetDefinitionVersion;
    }

    public String getStopDefinitionVersion() {
        return stopDefinitionVersion;
    }

    public Instant getComputedAt() {
        return computedAt;
    }
}
