package com.etl.common.pipeline;

import com.etl.common.context.SyncContext;

public interface Loader {
    String getName();
    void load(SyncContext context);
}
