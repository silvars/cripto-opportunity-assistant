package com.btcassistant.agent.web;

import com.btcassistant.agent.AgentAnalysisEntity;
import com.btcassistant.agent.AgentAnalysisRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agent-analyses")
public class AgentAnalysisController {

    private final AgentAnalysisRepository repository;

    public AgentAnalysisController(AgentAnalysisRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{alertId}")
    public List<AgentAnalysisResponse> forAlert(@PathVariable UUID alertId) {
        return repository.findByAlertIdOrderByAnalysisVersionDesc(alertId).stream()
                .map(AgentAnalysisController::toResponse)
                .toList();
    }

    private static AgentAnalysisResponse toResponse(AgentAnalysisEntity e) {
        return new AgentAnalysisResponse(e.getAnalysisId(), e.getAlertId(), e.getAnalysisVersion(), e.getClassification(),
                e.getDirection(), e.getScore(), e.getConfidence(), e.getSummary(), e.getEvidence(), e.getHistoricalContext(),
                e.getRisk(), e.getInvalidations(), e.getAgentVersion(), e.getPromptVersion(), e.getStatus(), e.getCreatedAt());
    }
}
