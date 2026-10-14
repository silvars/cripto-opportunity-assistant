package com.btcassistant.operation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Caminho do arquivo persistido (fora do Git — SDD §54/§76). Sem OCR nesta fase. */
@Entity
@Table(name = "operation_screenshots")
public class OperationScreenshotEntity {

    @Id
    private UUID screenshotId;

    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(length = 10)
    private String confidence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OperationScreenshotEntity() {
        // JPA
    }

    public static OperationScreenshotEntity create(UUID operationId, String filePath) {
        OperationScreenshotEntity entity = new OperationScreenshotEntity();
        entity.screenshotId = UUID.randomUUID();
        entity.operationId = operationId;
        entity.filePath = filePath;
        entity.confidence = null;
        entity.createdAt = Instant.now();
        return entity;
    }

    public UUID getScreenshotId() {
        return screenshotId;
    }

    public UUID getOperationId() {
        return operationId;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getConfidence() {
        return confidence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
