package com.btcassistant.operation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Execução isolada — entrada ou saída de um {@link TradeEntity} (SDD §53). Insert-only. */
@Entity
@Table(name = "operations")
public class OperationEntity {

    public static final String SOURCE_MANUAL = "MANUAL";

    @Id
    private UUID operationId;

    @Column(name = "decision_id", nullable = false)
    private UUID decisionId;

    @Column(name = "trade_id", nullable = false)
    private UUID tradeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Side side;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal notional;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal fees;

    @Column(name = "estimated_slippage", nullable = false, precision = 20, scale = 8)
    private BigDecimal estimatedSlippage;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 20)
    private String source;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OperationEntity() {
        // JPA
    }

    public static OperationEntity create(UUID decisionId, UUID tradeId, Side side, BigDecimal quantity, BigDecimal price,
                                         BigDecimal fees, BigDecimal estimatedSlippage, Instant occurredAt) {
        OperationEntity entity = new OperationEntity();
        entity.operationId = UUID.randomUUID();
        entity.decisionId = decisionId;
        entity.tradeId = tradeId;
        entity.side = side;
        entity.quantity = quantity;
        entity.price = price;
        entity.notional = quantity.multiply(price);
        entity.fees = fees;
        entity.estimatedSlippage = estimatedSlippage;
        entity.occurredAt = occurredAt;
        entity.source = SOURCE_MANUAL;
        entity.createdAt = Instant.now();
        return entity;
    }

    public UUID getOperationId() {
        return operationId;
    }

    public UUID getDecisionId() {
        return decisionId;
    }

    public UUID getTradeId() {
        return tradeId;
    }

    public Side getSide() {
        return side;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getNotional() {
        return notional;
    }

    public BigDecimal getFees() {
        return fees;
    }

    public BigDecimal getEstimatedSlippage() {
        return estimatedSlippage;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
