package com.btcassistant.operation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TradeRepository extends JpaRepository<TradeEntity, UUID> {

    Optional<TradeEntity> findByDecisionId(UUID decisionId);
}
