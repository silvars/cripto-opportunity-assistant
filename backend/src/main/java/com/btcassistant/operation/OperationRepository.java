package com.btcassistant.operation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OperationRepository extends JpaRepository<OperationEntity, UUID> {

    List<OperationEntity> findByTradeIdOrderByOccurredAtAsc(UUID tradeId);
}
