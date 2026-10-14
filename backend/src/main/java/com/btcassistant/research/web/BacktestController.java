package com.btcassistant.research.web;

import com.btcassistant.research.BacktestConfig;
import com.btcassistant.research.ExperimentRunEntity;
import com.btcassistant.research.ExperimentRunRepository;
import com.btcassistant.research.ExperimentRunService;
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

@RestController
@RequestMapping("/api/v1/research/backtests")
public class BacktestController {

    private final ExperimentRunService experimentRunService;
    private final ExperimentRunRepository repository;

    public BacktestController(ExperimentRunService experimentRunService, ExperimentRunRepository repository) {
        this.experimentRunService = experimentRunService;
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<ExperimentRunResponse> run(@Valid @RequestBody RunBacktestRequest request) {
        BacktestConfig config = new BacktestConfig(request.symbol(), request.from(), request.to(), request.engineVersion(),
                request.featureVersion(), request.configVersion(), request.historicalVersion(),
                request.targetDefinitionVersion(), request.stopDefinitionVersion(), request.outOfSample());
        ExperimentRunEntity entity = experimentRunService.run(request.experimentCode(), config);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(entity));
    }

    @GetMapping("/{experimentCode}")
    public List<ExperimentRunResponse> runsFor(@PathVariable String experimentCode) {
        return repository.findByExperimentCodeOrderByCreatedAtDesc(experimentCode).stream()
                .map(BacktestController::toResponse)
                .toList();
    }

    private static ExperimentRunResponse toResponse(ExperimentRunEntity e) {
        return new ExperimentRunResponse(e.getExperimentRunId(), e.getExperimentCode(), e.getEngineVersion(), e.getFeatureVersion(),
                e.getConfigVersion(), e.getHistoricalVersion(), e.getTargetDefinitionVersion(), e.getStopDefinitionVersion(),
                e.getExecutionMode(), e.getDatasetFrom(), e.getDatasetTo(), e.isOutOfSample(), e.getResults(), e.getCreatedAt());
    }
}
