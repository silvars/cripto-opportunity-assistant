package com.btcassistant.decision;

import com.btcassistant.alert.AlertEntity;
import com.btcassistant.alert.AlertRepository;
import com.btcassistant.web.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Registra o fato "usuário decidiu X para o alerta Y" — nunca muta o alerta em si (SDD §57). */
@Service
public class UserDecisionService {

    private static final Logger log = LoggerFactory.getLogger(UserDecisionService.class);

    private final UserDecisionRepository repository;
    private final AlertRepository alertRepository;

    public UserDecisionService(UserDecisionRepository repository, AlertRepository alertRepository) {
        this.repository = repository;
        this.alertRepository = alertRepository;
    }

    public UserDecisionEntity register(UUID alertId, Decision decision) {
        AlertEntity alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new NotFoundException("Alerta não encontrado: " + alertId));

        UserDecisionEntity entity = UserDecisionEntity.create(alertId, decision, alert.getExecutionMode());
        repository.save(entity);
        log.info("decision_registered alertId={} decisionId={} decision={}", alertId, entity.getDecisionId(), decision);
        return entity;
    }
}
