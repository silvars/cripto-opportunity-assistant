package com.btcassistant.opportunity.web;

import com.btcassistant.opportunity.MarketOpportunityEntity;
import com.btcassistant.opportunity.MarketOpportunityRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/opportunities")
public class OpportunityController {

    private final MarketOpportunityRepository repository;

    public OpportunityController(MarketOpportunityRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{symbol}")
    public List<OpportunityResponse> recent(@PathVariable String symbol) {
        return repository.findTop50BySymbolOrderByCreatedAtDesc(symbol).stream()
                .map(OpportunityController::toResponse)
                .toList();
    }

    private static OpportunityResponse toResponse(MarketOpportunityEntity e) {
        return new OpportunityResponse(e.getOpportunityId(), e.getEventId(), e.getSymbol(), e.getClassification(),
                e.getDirection(), e.getScore(), e.getScoreComponents(), e.getAvailableEvidenceWeight(),
                e.getEvidenceQuality(), e.getRiskLevel(), e.getRiskReasons(), e.getConfigVersion(),
                e.getEngineVersion(), e.getCreatedAt());
    }
}
