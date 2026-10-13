package com.btcassistant.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entidade de persistência — somente INSERT, nenhum método de update é exposto (SDD §21: eventos
 * são imutáveis). Nunca exposta diretamente na API (SDD §70).
 */
@Entity
@Table(name = "market_events")
public class MarketEventEntity {

    @Id
    private UUID eventId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, BigDecimal> featuresSnapshot;

    @Column(precision = 10, scale = 6)
    private BigDecimal magnitude;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "engine_version", nullable = false, length = 10)
    private String engineVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MarketEventEntity() {
        // JPA
    }

    public static MarketEventEntity from(DetectedEvent event) {
        MarketEventEntity entity = new MarketEventEntity();
        entity.eventId = UUID.randomUUID();
        entity.symbol = event.symbol();
        entity.eventType = event.eventType();
        entity.eventTime = event.eventTime();
        entity.price = event.price();
        entity.featuresSnapshot = event.featuresSnapshot();
        entity.magnitude = event.magnitude();
        entity.durationSeconds = event.duration() == null ? null : event.duration().getSeconds();
        entity.engineVersion = event.engineVersion();
        entity.createdAt = Instant.now();
        return entity;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Map<String, BigDecimal> getFeaturesSnapshot() {
        return featuresSnapshot;
    }

    public BigDecimal getMagnitude() {
        return magnitude;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
