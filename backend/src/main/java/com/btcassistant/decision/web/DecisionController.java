package com.btcassistant.decision.web;

import com.btcassistant.decision.UserDecisionEntity;
import com.btcassistant.decision.UserDecisionRepository;
import com.btcassistant.decision.UserDecisionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/decisions")
public class DecisionController {

    private final UserDecisionService service;
    private final UserDecisionRepository repository;

    public DecisionController(UserDecisionService service, UserDecisionRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<UserDecisionResponse> register(@Valid @RequestBody RegisterDecisionRequest request) {
        UserDecisionEntity entity = service.register(request.alertId(), request.decision());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(entity));
    }

    @GetMapping("/{alertId}")
    public List<UserDecisionResponse> forAlert(@PathVariable UUID alertId) {
        return repository.findByAlertIdOrderByCreatedAtDesc(alertId).stream()
                .map(DecisionController::toResponse)
                .toList();
    }

    private static UserDecisionResponse toResponse(UserDecisionEntity e) {
        return new UserDecisionResponse(e.getDecisionId(), e.getAlertId(), e.getDecision(), e.getExecutionMode(), e.getCreatedAt());
    }
}
