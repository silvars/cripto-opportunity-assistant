package com.btcassistant.alert.web;

import com.btcassistant.alert.AlertEntity;
import com.btcassistant.alert.AlertRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertRepository repository;

    public AlertController(AlertRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{symbol}")
    public List<AlertResponse> recent(@PathVariable String symbol) {
        return repository.findTop50BySymbolOrderByCreatedAtDesc(symbol).stream()
                .map(AlertController::toResponse)
                .toList();
    }

    private static AlertResponse toResponse(AlertEntity e) {
        return new AlertResponse(e.getAlertId(), e.getOpportunityId(), e.getSymbol(), e.getDirection(), e.getScore(),
                e.getClassification(), e.getRiskLevel(), e.getPriceAtAlert(), e.getStatus(), e.getCreatedAt(), e.getExpiresAt());
    }
}
