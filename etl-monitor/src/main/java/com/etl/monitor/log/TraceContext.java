package com.etl.monitor.log;

import java.util.concurrent.ThreadLocalRandom;

public class TraceContext {
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    public static void setTraceId(String traceId) { TRACE_ID.set(traceId); }
    public static String getTraceId() { return TRACE_ID.get(); }
    public static void clear() { TRACE_ID.remove(); }

    public static String generateTraceId() {
        return "ETL-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
