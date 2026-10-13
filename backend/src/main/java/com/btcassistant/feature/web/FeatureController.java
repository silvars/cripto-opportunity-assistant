package com.btcassistant.feature.web;

import com.btcassistant.feature.FeatureProperties;
import com.btcassistant.feature.MarketFeatureEntity;
import com.btcassistant.feature.MarketFeatureRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/features")
public class FeatureController {

    private final MarketFeatureRepository featureRepository;
    private final FeatureProperties featureProperties;

    public FeatureController(MarketFeatureRepository featureRepository, FeatureProperties featureProperties) {
        this.featureRepository = featureRepository;
        this.featureProperties = featureProperties;
    }

    @GetMapping("/{symbol}/latest")
    public FeatureResponse latest(@PathVariable String symbol, @RequestParam(defaultValue = "1m") String interval) {
        return featureRepository
                .findTopBySymbolAndIntervalAndFeatureVersionOrderByCandleOpenTimeDesc(symbol, interval, featureProperties.getVersion())
                .map(FeatureController::toResponse)
                .orElse(null);
    }

    private static FeatureResponse toResponse(MarketFeatureEntity e) {
        return new FeatureResponse(
                e.getSymbol(), e.getInterval(), e.getCandleOpenTime(), e.getFeatureVersion(),
                e.getReturn1m(), e.getReturn5m(), e.getReturn15m(), e.getReturn30m(),
                e.getReturn1h(), e.getReturn4h(), e.getReturn12h(), e.getReturn24h(),
                e.getVolumeSma(), e.getVolumeRatio(), e.getVolumeZscore(),
                e.getVolatility15m(), e.getVolatility1h(), e.getVolatility4h(), e.getVolatility24h(),
                e.getDistanceRecentHigh1h(), e.getDistanceRecentHigh4h(), e.getDistanceRecentHigh24h(),
                e.getDistanceRecentLow1h(), e.getDistanceRecentLow4h(), e.getDistanceRecentLow24h(),
                e.getMomentum5m(), e.getMomentum15m(), e.getMomentum1h(), e.getMomentum4h(),
                e.getDrawdownFromRecentHigh(), e.getRecoveryFromRecentLow()
        );
    }
}
