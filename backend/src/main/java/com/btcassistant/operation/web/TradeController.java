package com.btcassistant.operation.web;

import com.btcassistant.operation.OperationEntity;
import com.btcassistant.operation.OperationRepository;
import com.btcassistant.operation.PnlCalculator;
import com.btcassistant.operation.PnlResult;
import com.btcassistant.operation.TradeEntity;
import com.btcassistant.operation.TradeRepository;
import com.btcassistant.operation.TradeStatus;
import com.btcassistant.web.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trades")
public class TradeController {

    private final TradeRepository tradeRepository;
    private final OperationRepository operationRepository;
    private final PnlCalculator pnlCalculator;

    public TradeController(TradeRepository tradeRepository, OperationRepository operationRepository, PnlCalculator pnlCalculator) {
        this.tradeRepository = tradeRepository;
        this.operationRepository = operationRepository;
        this.pnlCalculator = pnlCalculator;
    }

    @GetMapping("/{tradeId}")
    public TradeResponse get(@PathVariable UUID tradeId) {
        TradeEntity trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new NotFoundException("Trade não encontrado: " + tradeId));

        PnlResult pnl = null;
        if (trade.getStatus() == TradeStatus.CLOSED) {
            OperationEntity entry = operationRepository.findById(trade.getEntryOperationId())
                    .orElseThrow(() -> new NotFoundException("Operação de entrada não encontrada: " + trade.getEntryOperationId()));
            OperationEntity exit = operationRepository.findById(trade.getExitOperationId())
                    .orElseThrow(() -> new NotFoundException("Operação de saída não encontrada: " + trade.getExitOperationId()));
            pnl = pnlCalculator.calculate(trade, entry, exit);
        }

        return new TradeResponse(trade.getTradeId(), trade.getDecisionId(), trade.getStatus(), trade.getEntryOperationId(),
                trade.getExitOperationId(), trade.getCreatedAt(), pnl);
    }
}
