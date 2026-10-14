package com.btcassistant.decision;

import com.btcassistant.opportunity.ExecutionMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidade de persistência — insert-only, nunca atualizada (SDD §57: decisão do usuário é um fato
 * independente do que o sistema calculou e do que o mercado fez depois).
 */
@Entity
@Table(name = "user_decisions")
public class UserDecisionEntity {

    @Id
    private UUID decisionId;

    @Column(name = "alert_id", nullable = false)
    private UUID alertId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Decision decision;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_mode", nullable = false, length = 10)
    private ExecutionMode executionMode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserDecisionEntity() {
        // JPA
    }

    public static UserDecisionEntity create(UUID alertId, Decision decision, ExecutionMode executionMode) {
        UserDecisionEntity entity = new UserDecisionEntity();
        entity.decisionId = UUID.randomUUID();
        entity.alertId = alertId;
        entity.decision = decision;
        entity.executionMode = executionMode;
        entity.createdAt = Instant.now();
        return entity;
    }

    public UUID getDecisionId() {
        return decisionId;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public Decision getDecision() {
        return decision;
    }

    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
