package com.btcassistant.operation;

import com.btcassistant.decision.UserDecisionEntity;
import com.btcassistant.decision.UserDecisionRepository;
import com.btcassistant.web.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entrada e saída de um mesmo {@code decisionId} são agrupadas automaticamente num único
 * {@link TradeEntity} (SDD §53) — a primeira operação registrada vira a entrada, a segunda vira a
 * saída (e fecha o trade). Nesta fase, cada decisão só pode ter um trade com uma entrada e uma
 * saída (ver docs/phases/FASE-6-decision-memory.md §11).
 */
@Service
public class OperationService {

    private static final Logger log = LoggerFactory.getLogger(OperationService.class);

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/webp", ".webp");

    private final UserDecisionRepository decisionRepository;
    private final TradeRepository tradeRepository;
    private final OperationRepository operationRepository;
    private final OperationScreenshotRepository screenshotRepository;
    private final OperationProperties properties;

    public OperationService(UserDecisionRepository decisionRepository, TradeRepository tradeRepository,
                            OperationRepository operationRepository, OperationScreenshotRepository screenshotRepository,
                            OperationProperties properties) {
        this.decisionRepository = decisionRepository;
        this.tradeRepository = tradeRepository;
        this.operationRepository = operationRepository;
        this.screenshotRepository = screenshotRepository;
        this.properties = properties;
    }

    public OperationEntity registerOperation(UUID decisionId, Side side, BigDecimal quantity, BigDecimal price,
                                             BigDecimal fees, BigDecimal estimatedSlippage, Instant occurredAt) {
        UserDecisionEntity decision = decisionRepository.findById(decisionId)
                .orElseThrow(() -> new NotFoundException("Decisão não encontrada: " + decisionId));

        TradeEntity trade = tradeRepository.findByDecisionId(decisionId).orElse(null);
        boolean isEntry = trade == null || trade.getEntryOperationId() == null;

        if (trade != null && !isEntry) {
            if (trade.getStatus() == TradeStatus.CLOSED) {
                throw new IllegalArgumentException("Decisão " + decisionId + " já possui um trade fechado (entrada+saída) nesta fase");
            }
            UUID entryOperationId = trade.getEntryOperationId();
            OperationEntity entryOperation = operationRepository.findById(entryOperationId)
                    .orElseThrow(() -> new NotFoundException("Operação de entrada não encontrada: " + entryOperationId));
            if (entryOperation.getSide() == side) {
                throw new IllegalArgumentException("Operação de saída deve ter lado oposto ao da entrada (" + entryOperation.getSide() + ")");
            }
            if (entryOperation.getQuantity().compareTo(quantity) != 0) {
                throw new IllegalArgumentException("Quantidade de saída deve ser igual à quantidade de entrada nesta fase");
            }
        }

        if (trade == null) {
            trade = TradeEntity.open(decisionId);
            tradeRepository.save(trade);
        }

        BigDecimal resolvedFees = fees != null ? fees : quantity.multiply(price).multiply(properties.getDefaultFeeRate(), MathContext.DECIMAL64);
        BigDecimal resolvedSlippage = estimatedSlippage != null ? estimatedSlippage
                : quantity.multiply(price).multiply(properties.getDefaultSlippageEstimate(), MathContext.DECIMAL64);

        OperationEntity operation = OperationEntity.create(decision.getDecisionId(), trade.getTradeId(), side, quantity, price,
                resolvedFees, resolvedSlippage, occurredAt);
        operationRepository.save(operation);

        if (isEntry) {
            trade.attachEntry(operation.getOperationId());
        } else {
            trade.attachExitAndClose(operation.getOperationId());
        }
        tradeRepository.save(trade);

        log.info("operation_created decisionId={} tradeId={} operationId={} side={} role={}",
                decisionId, trade.getTradeId(), operation.getOperationId(), side, isEntry ? "ENTRY" : "EXIT");
        return operation;
    }

    public OperationScreenshotEntity attachScreenshot(UUID operationId, MultipartFile file) {
        operationRepository.findById(operationId)
                .orElseThrow(() -> new NotFoundException("Operação não encontrada: " + operationId));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo de screenshot vazio");
        }
        if (file.getSize() > properties.getScreenshots().getMaxSizeBytes()) {
            throw new IllegalArgumentException("Arquivo excede o tamanho máximo permitido");
        }
        String contentType = file.getContentType();
        if (contentType == null || !properties.getScreenshots().getAllowedContentTypes().contains(contentType)) {
            throw new IllegalArgumentException("Tipo de arquivo não permitido: " + contentType);
        }

        String extension = EXTENSION_BY_CONTENT_TYPE.getOrDefault(contentType, "");
        String fileName = operationId + "-" + UUID.randomUUID() + extension;
        Path storageDir = Path.of(properties.getScreenshots().getStoragePath());
        Path destination = storageDir.resolve(fileName);

        try {
            Files.createDirectories(storageDir);
            Files.write(destination, file.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao salvar screenshot da operação " + operationId, e);
        }

        OperationScreenshotEntity screenshot = OperationScreenshotEntity.create(operationId, destination.toString());
        screenshotRepository.save(screenshot);
        log.info("operation_screenshot_attached operationId={} screenshotId={} path={}",
                operationId, screenshot.getScreenshotId(), destination);
        return screenshot;
    }
}
