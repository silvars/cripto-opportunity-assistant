package com.btcassistant.research;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Insert-only — reexecutar um experimento cria uma nova linha, nunca sobrescreve a anterior (SDD §68). */
@Entity
@Table(name = "experiment_runs")
public class ExperimentRunEntity {

    @Id
    private UUID experimentRunId;

    @Column(name = "experiment_code", nullable = false, length = 20)
    private String experimentCode;

    @Column(name = "engine_version", nullable = false, length = 10)
    private String engineVersion;
    @Column(name = "feature_version", nullable = false, length = 10)
    private String featureVersion;
    @Column(name = "config_version", nullable = false, length = 10)
    private String configVersion;
    @Column(name = "historical_version", nullable = false, length = 10)
    private String historicalVersion;
    @Column(name = "target_definition_version", nullable = false, length = 10)
    private String targetDefinitionVersion;
    @Column(name = "stop_definition_version", nullable = false, length = 10)
    private String stopDefinitionVersion;

    @Column(name = "execution_mode", nullable = false, length = 10)
    private String executionMode;

    @Column(name = "dataset_from", nullable = false)
    private Instant datasetFrom;
    @Column(name = "dataset_to", nullable = false)
    private Instant datasetTo;

    @Column(name = "is_out_of_sample", nullable = false)
    private boolean outOfSample;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> results;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ExperimentRunEntity() {
        // JPA
    }

    public static ExperimentRunEntity create(String experimentCode, BacktestConfig config, Map<String, Object> results) {
        ExperimentRunEntity entity = new ExperimentRunEntity();
        entity.experimentRunId = UUID.randomUUID();
        entity.experimentCode = experimentCode;
        entity.engineVersion = config.engineVersion();
        entity.featureVersion = config.featureVersion();
        entity.configVersion = config.configVersion();
        entity.historicalVersion = config.historicalVersion();
        entity.targetDefinitionVersion = config.targetDefinitionVersion();
        entity.stopDefinitionVersion = config.stopDefinitionVersion();
        entity.executionMode = "BACKTEST";
        entity.datasetFrom = config.from();
        entity.datasetTo = config.to();
        entity.outOfSample = config.outOfSample();
        entity.results = results;
        entity.createdAt = Instant.now();
        return entity;
    }

    public UUID getExperimentRunId() {
        return experimentRunId;
    }

    public String getExperimentCode() {
        return experimentCode;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public String getFeatureVersion() {
        return featureVersion;
    }

    public String getConfigVersion() {
        return configVersion;
    }

    public String getHistoricalVersion() {
        return historicalVersion;
    }

    public String getTargetDefinitionVersion() {
        return targetDefinitionVersion;
    }

    public String getStopDefinitionVersion() {
        return stopDefinitionVersion;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public Instant getDatasetFrom() {
        return datasetFrom;
    }

    public Instant getDatasetTo() {
        return datasetTo;
    }

    public boolean isOutOfSample() {
        return outOfSample;
    }

    public Map<String, Object> getResults() {
        return results;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
