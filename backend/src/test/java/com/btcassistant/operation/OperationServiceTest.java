package com.btcassistant.operation;

import com.btcassistant.decision.Decision;
import com.btcassistant.decision.UserDecisionEntity;
import com.btcassistant.decision.UserDecisionRepository;
import com.btcassistant.web.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cobre o DoD da Fase 6: operação não pode ser criada sem decisão válida, entrada/saída agrupadas
 * automaticamente num Trade, screenshot validado por tipo/tamanho antes de persistir (SDD §76). */
@ExtendWith(MockitoExtension.class)
class OperationServiceTest {

    private static final Instant ENTRY_TIME = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private UserDecisionRepository decisionRepository;
    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private OperationRepository operationRepository;
    @Mock
    private OperationScreenshotRepository screenshotRepository;

    private OperationProperties properties;
    private OperationService service;
    private Path tempStorageDir;

    @BeforeEach
    void setUp() throws Exception {
        properties = new OperationProperties();
        tempStorageDir = Files.createTempDirectory("screenshots-test");
        properties.getScreenshots().setStoragePath(tempStorageDir.toString());
        service = new OperationService(decisionRepository, tradeRepository, operationRepository, screenshotRepository, properties);
        org.mockito.Mockito.lenient().when(tradeRepository.save(any(TradeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(operationRepository.save(any(OperationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private UserDecisionEntity decision() {
        return UserDecisionEntity.create(UUID.randomUUID(), Decision.BUY, com.btcassistant.opportunity.ExecutionMode.LIVE);
    }

    @Test
    void throwsNotFoundWhenDecisionDoesNotExist() {
        UUID decisionId = UUID.randomUUID();
        when(decisionRepository.findById(decisionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerOperation(decisionId, Side.BUY, BigDecimal.ONE, new BigDecimal("50000"),
                null, null, ENTRY_TIME))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void firstOperationCreatesTradeAsEntry() {
        UserDecisionEntity decision = decision();
        when(decisionRepository.findById(decision.getDecisionId())).thenReturn(Optional.of(decision));
        when(tradeRepository.findByDecisionId(decision.getDecisionId())).thenReturn(Optional.empty());

        OperationEntity entry = service.registerOperation(decision.getDecisionId(), Side.BUY, new BigDecimal("1"),
                new BigDecimal("50000"), null, null, ENTRY_TIME);

        assertThat(entry.getSide()).isEqualTo(Side.BUY);
        assertThat(entry.getNotional()).isEqualByComparingTo("50000");
        // save() é chamado duas vezes para um trade novo: uma vez pra satisfazer a FK de
        // operations.trade_id antes do insert da operação, outra pra gravar o ponteiro de entrada.
        ArgumentCaptor<TradeEntity> tradeCaptor = ArgumentCaptor.forClass(TradeEntity.class);
        verify(tradeRepository, org.mockito.Mockito.times(2)).save(tradeCaptor.capture());
        assertThat(tradeCaptor.getValue().getEntryOperationId()).isEqualTo(entry.getOperationId());
        assertThat(tradeCaptor.getValue().getStatus()).isEqualTo(TradeStatus.OPEN);
    }

    @Test
    void appliesDefaultFeeAndSlippageWhenNotProvided() {
        UserDecisionEntity decision = decision();
        when(decisionRepository.findById(decision.getDecisionId())).thenReturn(Optional.of(decision));
        when(tradeRepository.findByDecisionId(decision.getDecisionId())).thenReturn(Optional.empty());

        OperationEntity entry = service.registerOperation(decision.getDecisionId(), Side.BUY, new BigDecimal("1"),
                new BigDecimal("50000"), null, null, ENTRY_TIME);

        assertThat(entry.getFees()).isEqualByComparingTo(new BigDecimal("50000").multiply(properties.getDefaultFeeRate()));
        assertThat(entry.getEstimatedSlippage()).isEqualByComparingTo(new BigDecimal("50000").multiply(properties.getDefaultSlippageEstimate()));
    }

    @Test
    void secondOperationClosesTheTradeAsExit() {
        UserDecisionEntity decision = decision();
        when(decisionRepository.findById(decision.getDecisionId())).thenReturn(Optional.of(decision));

        TradeEntity trade = TradeEntity.open(decision.getDecisionId());
        OperationEntity entryOperation = OperationEntity.create(decision.getDecisionId(), trade.getTradeId(), Side.BUY,
                new BigDecimal("1"), new BigDecimal("50000"), BigDecimal.ZERO, BigDecimal.ZERO, ENTRY_TIME);
        trade.attachEntry(entryOperation.getOperationId());

        when(tradeRepository.findByDecisionId(decision.getDecisionId())).thenReturn(Optional.of(trade));
        when(operationRepository.findById(entryOperation.getOperationId())).thenReturn(Optional.of(entryOperation));

        OperationEntity exit = service.registerOperation(decision.getDecisionId(), Side.SELL, new BigDecimal("1"),
                new BigDecimal("51000"), null, null, ENTRY_TIME.plus(Duration.ofHours(1)));

        assertThat(trade.getStatus()).isEqualTo(TradeStatus.CLOSED);
        assertThat(trade.getExitOperationId()).isEqualTo(exit.getOperationId());
    }

    @Test
    void rejectsExitWithSameSideAsEntry() {
        UserDecisionEntity decision = decision();
        when(decisionRepository.findById(decision.getDecisionId())).thenReturn(Optional.of(decision));

        TradeEntity trade = TradeEntity.open(decision.getDecisionId());
        OperationEntity entryOperation = OperationEntity.create(decision.getDecisionId(), trade.getTradeId(), Side.BUY,
                new BigDecimal("1"), new BigDecimal("50000"), BigDecimal.ZERO, BigDecimal.ZERO, ENTRY_TIME);
        trade.attachEntry(entryOperation.getOperationId());

        when(tradeRepository.findByDecisionId(decision.getDecisionId())).thenReturn(Optional.of(trade));
        when(operationRepository.findById(entryOperation.getOperationId())).thenReturn(Optional.of(entryOperation));

        assertThatThrownBy(() -> service.registerOperation(decision.getDecisionId(), Side.BUY, new BigDecimal("1"),
                new BigDecimal("51000"), null, null, ENTRY_TIME.plus(Duration.ofHours(1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lado oposto");
    }

    @Test
    void rejectsExitWithDifferentQuantityThanEntry() {
        UserDecisionEntity decision = decision();
        when(decisionRepository.findById(decision.getDecisionId())).thenReturn(Optional.of(decision));

        TradeEntity trade = TradeEntity.open(decision.getDecisionId());
        OperationEntity entryOperation = OperationEntity.create(decision.getDecisionId(), trade.getTradeId(), Side.BUY,
                new BigDecimal("1"), new BigDecimal("50000"), BigDecimal.ZERO, BigDecimal.ZERO, ENTRY_TIME);
        trade.attachEntry(entryOperation.getOperationId());

        when(tradeRepository.findByDecisionId(decision.getDecisionId())).thenReturn(Optional.of(trade));
        when(operationRepository.findById(entryOperation.getOperationId())).thenReturn(Optional.of(entryOperation));

        assertThatThrownBy(() -> service.registerOperation(decision.getDecisionId(), Side.SELL, new BigDecimal("2"),
                new BigDecimal("51000"), null, null, ENTRY_TIME.plus(Duration.ofHours(1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantidade");
    }

    @Test
    void rejectsThirdOperationWhenTradeAlreadyClosed() {
        UserDecisionEntity decision = decision();
        when(decisionRepository.findById(decision.getDecisionId())).thenReturn(Optional.of(decision));

        TradeEntity trade = TradeEntity.open(decision.getDecisionId());
        OperationEntity entryOperation = OperationEntity.create(decision.getDecisionId(), trade.getTradeId(), Side.BUY,
                new BigDecimal("1"), new BigDecimal("50000"), BigDecimal.ZERO, BigDecimal.ZERO, ENTRY_TIME);
        trade.attachEntry(entryOperation.getOperationId());
        OperationEntity exitOperation = OperationEntity.create(decision.getDecisionId(), trade.getTradeId(), Side.SELL,
                new BigDecimal("1"), new BigDecimal("51000"), BigDecimal.ZERO, BigDecimal.ZERO, ENTRY_TIME.plus(Duration.ofHours(1)));
        trade.attachExitAndClose(exitOperation.getOperationId());

        when(tradeRepository.findByDecisionId(decision.getDecisionId())).thenReturn(Optional.of(trade));

        assertThatThrownBy(() -> service.registerOperation(decision.getDecisionId(), Side.BUY, new BigDecimal("1"),
                new BigDecimal("52000"), null, null, ENTRY_TIME.plus(Duration.ofHours(2))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já possui um trade fechado");
    }

    @Test
    void attachesScreenshotWhenOperationExistsAndFileIsValid() {
        UUID operationId = UUID.randomUUID();
        when(operationRepository.findById(operationId)).thenReturn(Optional.of(
                OperationEntity.create(UUID.randomUUID(), UUID.randomUUID(), Side.BUY, BigDecimal.ONE, BigDecimal.TEN,
                        BigDecimal.ZERO, BigDecimal.ZERO, ENTRY_TIME)));
        when(screenshotRepository.save(any(OperationScreenshotEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MultipartFile file = new MockMultipartFile("file", "chart.png", "image/png", new byte[]{1, 2, 3});

        OperationScreenshotEntity screenshot = service.attachScreenshot(operationId, file);

        assertThat(screenshot.getOperationId()).isEqualTo(operationId);
        assertThat(Files.exists(Path.of(screenshot.getFilePath()))).isTrue();
    }

    @Test
    void rejectsScreenshotWithDisallowedContentType() {
        UUID operationId = UUID.randomUUID();
        when(operationRepository.findById(operationId)).thenReturn(Optional.of(
                OperationEntity.create(UUID.randomUUID(), UUID.randomUUID(), Side.BUY, BigDecimal.ONE, BigDecimal.TEN,
                        BigDecimal.ZERO, BigDecimal.ZERO, ENTRY_TIME)));
        MultipartFile file = new MockMultipartFile("file", "chart.exe", "application/octet-stream", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.attachScreenshot(operationId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo de arquivo não permitido");
    }

    @Test
    void rejectsScreenshotExceedingMaxSize() {
        UUID operationId = UUID.randomUUID();
        when(operationRepository.findById(operationId)).thenReturn(Optional.of(
                OperationEntity.create(UUID.randomUUID(), UUID.randomUUID(), Side.BUY, BigDecimal.ONE, BigDecimal.TEN,
                        BigDecimal.ZERO, BigDecimal.ZERO, ENTRY_TIME)));
        properties.getScreenshots().setMaxSizeBytes(2);
        MultipartFile file = new MockMultipartFile("file", "chart.png", "image/png", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.attachScreenshot(operationId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tamanho máximo");
    }

    @Test
    void throwsNotFoundWhenAttachingScreenshotToMissingOperation() {
        UUID operationId = UUID.randomUUID();
        when(operationRepository.findById(operationId)).thenReturn(Optional.empty());
        MultipartFile file = new MockMultipartFile("file", "chart.png", "image/png", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.attachScreenshot(operationId, file))
                .isInstanceOf(NotFoundException.class);
    }
}
