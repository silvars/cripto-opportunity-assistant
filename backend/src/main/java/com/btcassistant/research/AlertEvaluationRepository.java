package com.btcassistant.research;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlertEvaluationRepository extends JpaRepository<AlertEvaluationEntity, UUID> {
}
