package com.btcassistant.research;

import com.btcassistant.alert.AlertEntity;
import com.btcassistant.alert.AlertRepository;
import com.btcassistant.decision.Decision;
import com.btcassistant.decision.UserDecisionRepository;
import com.btcassistant.opportunity.Direction;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * "Alert ignorado + mercado se moveu a favor" (SDD §56 — "Alert → IGNORE → Future market
 * outcome"). Precisa de uma {@link AlertEvaluationEntity} já preenchida no horizonte canônico —
 * alertas ainda sem avaliação disponível são omitidos (nunca antecipados).
 */
@Service
public class MissedOpportunityService {

    private final AlertRepository alertRepository;
    private final UserDecisionRepository decisionRepository;
    private final AlertEvaluationRepository evaluationRepository;
    private final ResearchProperties properties;

    public MissedOpportunityService(AlertRepository alertRepository, UserDecisionRepository decisionRepository,
                                    AlertEvaluationRepository evaluationRepository, ResearchProperties properties) {
        this.alertRepository = alertRepository;
        this.decisionRepository = decisionRepository;
        this.evaluationRepository = evaluationRepository;
        this.properties = properties;
    }

    public List<MissedOpportunity> find() {
        return alertRepository.findAll().stream()
                .filter(this::wasIgnored)
                .map(this::toMissedOpportunity)
                .flatMap(Optional::stream)
                .filter(m -> m.favorableMove().signum() > 0)
                .toList();
    }

    private boolean wasIgnored(AlertEntity alert) {
        return decisionRepository.findByAlertIdOrderByCreatedAtDesc(alert.getAlertId()).stream()
                .findFirst()
                .map(d -> d.getDecision() == Decision.IGNORE || d.getDecision() == Decision.WAIT)
                .orElse(true); // nenhuma decisão registrada é tratada como "ignorado" pra fins de avaliação
    }

    private Optional<MissedOpportunity> toMissedOpportunity(AlertEntity alert) {
        return evaluationRepository.findById(alert.getAlertId())
                .map(evaluation -> evaluation.getHorizonReturn(properties.getEvaluation().getCanonicalHorizon()))
                .filter(canonicalReturn -> canonicalReturn != null)
                .map(canonicalReturn -> {
                    BigDecimal favorableMove = alert.getDirection() == Direction.SELL ? canonicalReturn.negate() : canonicalReturn;
                    return new MissedOpportunity(alert.getAlertId(), alert.getSymbol(), alert.getDirection(),
                            alert.getClassification(), alert.getScore(), canonicalReturn, favorableMove, alert.getCreatedAt());
                });
    }
}
