package com.btcassistant.operation.web;

import com.btcassistant.operation.OperationEntity;
import com.btcassistant.operation.OperationScreenshotEntity;
import com.btcassistant.operation.OperationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/operations")
public class OperationController {

    private final OperationService service;

    public OperationController(OperationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OperationResponse> register(@Valid @RequestBody RegisterOperationRequest request) {
        OperationEntity operation = service.registerOperation(request.decisionId(), request.side(), request.quantity(),
                request.price(), request.fees(), request.estimatedSlippage(), request.occurredAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(operation));
    }

    @PostMapping("/{operationId}/screenshot")
    public ResponseEntity<ScreenshotResponse> uploadScreenshot(@PathVariable UUID operationId,
                                                               @RequestParam("file") MultipartFile file) {
        OperationScreenshotEntity screenshot = service.attachScreenshot(operationId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ScreenshotResponse(screenshot.getScreenshotId(), screenshot.getOperationId(), screenshot.getCreatedAt()));
    }

    private static OperationResponse toResponse(OperationEntity e) {
        return new OperationResponse(e.getOperationId(), e.getDecisionId(), e.getTradeId(), e.getSide(), e.getQuantity(),
                e.getPrice(), e.getNotional(), e.getFees(), e.getEstimatedSlippage(), e.getOccurredAt(), e.getSource(), e.getCreatedAt());
    }

    public record ScreenshotResponse(UUID screenshotId, UUID operationId, java.time.Instant createdAt) {
    }
}
