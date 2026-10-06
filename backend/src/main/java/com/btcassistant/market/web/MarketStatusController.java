package com.btcassistant.market.web;

import com.btcassistant.market.MarketProperties;
import com.btcassistant.market.MarketStatus;
import com.btcassistant.market.MarketStatusRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Expõe o estado de ingestão (backfill/live/websocket) para o dashboard (SDD Fase 0 §11). */
@RestController
@RequestMapping("/api/v1/market")
public class MarketStatusController {

    private final MarketStatusRegistry statusRegistry;
    private final MarketProperties marketProperties;

    public MarketStatusController(MarketStatusRegistry statusRegistry, MarketProperties marketProperties) {
        this.statusRegistry = statusRegistry;
        this.marketProperties = marketProperties;
    }

    @GetMapping("/status")
    public List<MarketStatus> status() {
        return marketProperties.enabledInstruments().stream()
                .map(instrument -> statusRegistry.snapshot(instrument.symbol()))
                .toList();
    }

    @GetMapping("/{symbol}/status")
    public MarketStatus status(@PathVariable String symbol) {
        return statusRegistry.snapshot(symbol);
    }
}
