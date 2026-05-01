package com.etl.common.pipeline;

import com.etl.common.context.SyncContext;

public interface Transformer {
    String getName();
    void transform(SyncContext context);
}
