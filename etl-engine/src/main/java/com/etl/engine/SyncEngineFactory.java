package com.etl.engine;

import com.etl.common.enums.SyncMode;
import com.etl.engine.cdc.CdcSyncEngine;
import com.etl.engine.full.FullSyncEngine;
import com.etl.engine.incremental.IncrementalSyncEngine;
import com.etl.engine.incremental.IncrementalStrategyFactory;
import com.etl.engine.schema.TypeMappingService;
import com.etl.engine.mapper.CdcPositionMapper;
import com.etl.engine.service.SyncTaskService;
import com.etl.engine.service.TaskExecutionService;
import com.etl.engine.service.CdcConfigService;
import com.etl.datasource.service.DatasourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 同步引擎工厂
 * 通过 Spring ApplicationContext 对工厂创建的引擎执行自动注入，
 * 确保引擎内部 @Autowired、@Value 等注解生效
 */
@Component
@RequiredArgsConstructor
public class SyncEngineFactory {

    private final SyncTaskService syncTaskService;
    private final TaskExecutionService taskExecutionService;
    private final DatasourceService datasourceService;
    private final CdcPositionMapper cdcPositionMapper;
    private final IncrementalStrategyFactory incrementalStrategyFactory;
    private final TypeMappingService typeMappingService;
    private final CdcConfigService cdcConfigService;
    private final ApplicationContext applicationContext;

    @Value("${cdc.kafka.bootstrap-servers:kafka:9092}")
    private String kafkaBootstrapServers;

    @Value("${cdc.kafka.topic:canal-binlog}")
    private String kafkaTopic;

    /**
     * 获取同步引擎（已自动注入Spring依赖）
     */
    public SyncEngine getEngine(SyncMode syncMode) {
        SyncEngine engine = switch (syncMode) {
            case FULL -> new FullSyncEngine(syncTaskService, taskExecutionService, datasourceService, typeMappingService);
            case INCREMENTAL -> new IncrementalSyncEngine(syncTaskService, taskExecutionService, datasourceService,
                incrementalStrategyFactory, typeMappingService);
            case CDC -> new CdcSyncEngine(syncTaskService, taskExecutionService, datasourceService,
                cdcPositionMapper, kafkaBootstrapServers, kafkaTopic, typeMappingService, cdcConfigService);
        };
        // 对new创建的引擎执行Spring自动注入，确保@Autowired等注解生效
        applicationContext.getAutowireCapableBeanFactory().autowireBean(engine);
        return engine;
    }
}
