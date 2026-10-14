package com.btcassistant.research;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/** Dispara um {@link BacktestRunner} e persiste o resultado agregado (nunca as linhas intermediárias). */
@Service
public class ExperimentRunService {

    private static final Logger log = LoggerFactory.getLogger(ExperimentRunService.class);

    private final BacktestRunner backtestRunner;
    private final ExperimentRunRepository repository;
    private final ObjectMapper objectMapper;

    public ExperimentRunService(BacktestRunner backtestRunner, ExperimentRunRepository repository, ObjectMapper objectMapper) {
        this.backtestRunner = backtestRunner;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public ExperimentRunEntity run(String experimentCode, BacktestConfig config) {
        log.info("backtest_started experimentCode={} symbol={} from={} to={}", experimentCode, config.symbol(), config.from(), config.to());
        BacktestResult result = backtestRunner.run(config);

        @SuppressWarnings("unchecked")
        Map<String, Object> resultsJson = objectMapper.convertValue(result, Map.class);
        ExperimentRunEntity entity = repository.save(ExperimentRunEntity.create(experimentCode, config, resultsJson));

        log.info("backtest_completed experimentCode={} experimentRunId={} eventsDetected={} opportunitiesGenerated={}",
                experimentCode, entity.getExperimentRunId(), result.eventsDetected(), result.opportunitiesGenerated());
        return entity;
    }
}
