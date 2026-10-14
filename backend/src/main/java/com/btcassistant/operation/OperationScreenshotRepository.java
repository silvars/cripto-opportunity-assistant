package com.btcassistant.operation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OperationScreenshotRepository extends JpaRepository<OperationScreenshotEntity, UUID> {

    List<OperationScreenshotEntity> findByOperationIdOrderByCreatedAtDesc(UUID operationId);
}
