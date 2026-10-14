package com.btcassistant.decision;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserDecisionRepository extends JpaRepository<UserDecisionEntity, UUID> {

    List<UserDecisionEntity> findByAlertIdOrderByCreatedAtDesc(UUID alertId);
}
