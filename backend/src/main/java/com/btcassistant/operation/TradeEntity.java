package com.btcassistant.operation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Agrupa a operação de entrada e a de saída de uma mesma decisão (SDD §53) — status e os
 * ponteiros de entrada/saída são as únicas mutações permitidas (mesmo padrão de
 * {@code alert.AlertEntity}: demais campos fixados na criação).
 */
@Entity
@Table(name = "trades")
public class TradeEntity {

    @Id
    private UUID tradeId;

    @Column(name = "decision_id", nullable = false)
    private UUID decisionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TradeStatus status;

    @Column(name = "entry_operation_id")
    private UUID entryOperationId;

    @Column(name = "exit_operation_id")
    private UUID exitOperationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TradeEntity() {
        // JPA
    }

    public static TradeEntity open(UUID decisionId) {
        TradeEntity entity = new TradeEntity();
        entity.tradeId = UUID.randomUUID();
        entity.decisionId = decisionId;
        entity.status = TradeStatus.OPEN;
        entity.createdAt = Instant.now();
        return entity;
    }

    public void attachEntry(UUID operationId) {
        this.entryOperationId = operationId;
    }

    public void attachExitAndClose(UUID operationId) {
        this.exitOperationId = operationId;
        this.status = TradeStatus.CLOSED;
    }

    public UUID getTradeId() {
        return tradeId;
    }

    public UUID getDecisionId() {
        return decisionId;
    }

    public TradeStatus getStatus() {
        return status;
    }

    public UUID getEntryOperationId() {
        return entryOperationId;
    }

    public UUID getExitOperationId() {
        return exitOperationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
