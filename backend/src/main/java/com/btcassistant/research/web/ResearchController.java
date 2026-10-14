package com.btcassistant.research.web;

import com.btcassistant.research.AlertEvaluationEntity;
import com.btcassistant.research.AlertEvaluationRepository;
import com.btcassistant.research.MissedOpportunity;
import com.btcassistant.research.MissedOpportunityService;
import com.btcassistant.research.ResearchProperties;
import com.btcassistant.web.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/research")
public class ResearchController {

    private final AlertEvaluationRepository evaluationRepository;
    private final MissedOpportunityService missedOpportunityService;
    private final ResearchProperties properties;

    public ResearchController(AlertEvaluationRepository evaluationRepository, MissedOpportunityService missedOpportunityService,
                              ResearchProperties properties) {
        this.evaluationRepository = evaluationRepository;
        this.missedOpportunityService = missedOpportunityService;
        this.properties = properties;
    }

    @GetMapping("/alert-evaluations/{alertId}")
    public AlertEvaluationResponse evaluation(@PathVariable UUID alertId) {
        AlertEvaluationEntity entity = evaluationRepository.findById(alertId)
                .orElseThrow(() -> new NotFoundException("Avaliação ainda não disponível para o alerta: " + alertId));

        Map<String, BigDecimal> byHorizon = new LinkedHashMap<>();
        for (String horizon : properties.getEvaluation().getHorizons()) {
            BigDecimal value = entity.getHorizonReturn(horizon);
            if (value != null) {
                byHorizon.put(horizon, value);
            }
        }

        return new AlertEvaluationResponse(entity.getAlertId(), byHorizon, entity.getMfe(), entity.getMae(),
                entity.getTargetBeforeStop(), entity.getExecutionMode(), entity.getEvaluatedAt());
    }

    @GetMapping("/missed-opportunities")
    public List<MissedOpportunityResponse> missedOpportunities() {
        return missedOpportunityService.find().stream().map(ResearchController::toResponse).toList();
    }

    private static MissedOpportunityResponse toResponse(MissedOpportunity m) {
        return new MissedOpportunityResponse(m.alertId(), m.symbol(), m.direction(), m.classification(), m.score(),
                m.canonicalHorizonReturn(), m.favorableMove(), m.createdAt());
    }
}
