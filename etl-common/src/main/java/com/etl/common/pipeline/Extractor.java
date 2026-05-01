package com.etl.common.pipeline;

import com.etl.common.context.SyncContext;

public interface Extractor {
    String getName();
    void extract(SyncContext context);
}
