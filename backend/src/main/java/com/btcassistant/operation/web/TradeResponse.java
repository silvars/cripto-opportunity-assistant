package com.btcassistant.operation.web;

import com.btcassistant.operation.PnlResult;
import com.btcassistant.operation.TradeStatus;

import java.time.Instant;
import java.util.UUID;

/** {@code pnl} só é preenchido quando o trade está CLOSED (SDD §55). */
public record TradeResponse(
    UUID tradeId,
    UUID decisionId,
    TradeStatus status,
    UUID entryOperationId,
    UUID exitOperationId,
    Instant createdAt,
    PnlResult pnl
) {
}
