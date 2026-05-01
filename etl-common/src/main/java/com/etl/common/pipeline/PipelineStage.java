package com.etl.common.pipeline;

import com.etl.common.context.SyncContext;

public interface PipelineStage {
    String getName();
    void execute(SyncContext context);
}
