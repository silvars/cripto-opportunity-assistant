package com.btcassistant.agent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentAnalysisRepository extends JpaRepository<AgentAnalysisEntity, UUID> {

    List<AgentAnalysisEntity> findByAlertIdOrderByAnalysisVersionDesc(UUID alertId);

    Optional<AgentAnalysisEntity> findTopByAlertIdOrderByAnalysisVersionDesc(UUID alertId);
}
