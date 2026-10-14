package com.btcassistant.research;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExperimentRunRepository extends JpaRepository<ExperimentRunEntity, UUID> {

    List<ExperimentRunEntity> findByExperimentCodeOrderByCreatedAtDesc(String experimentCode);
}
