package com.btcassistant.historical;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Entidade de persistência — insert-only, uma linha por evento (SDD Fase 4 §3). Nunca exposta diretamente na API (SDD §70). */
@Entity
@Table(name = "historical_feature_store", uniqueConstraints = @UniqueConstraint(name = "uq_historical_feature_event", columnNames = "event_id"))
public class HistoricalFeatureStoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "normalized_features", nullable = false, columnDefinition = "jsonb")
    private Map<String, BigDecimal> normalizedFeatures;

    @Column(name = "historical_version", nullable = false, length = 10)
    private String historicalVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected HistoricalFeatureStoreEntity() {
        // JPA
    }

    public static HistoricalFeatureStoreEntity create(String symbol, UUID eventId, String eventType, Instant eventTime,
                                                       Map<String, BigDecimal> normalizedFeatures, String historicalVersion) {
        HistoricalFeatureStoreEntity entity = new HistoricalFeatureStoreEntity();
        entity.symbol = symbol;
        entity.eventId = eventId;
        entity.eventType = eventType;
        entity.eventTime = eventTime;
        entity.normalizedFeatures = normalizedFeatures;
        entity.historicalVersion = historicalVersion;
        entity.createdAt = Instant.now();
        return entity;
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public Map<String, BigDecimal> getNormalizedFeatures() {
        return normalizedFeatures;
    }

    public String getHistoricalVersion() {
        return historicalVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
