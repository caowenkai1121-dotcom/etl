package com.etl.engine.transform;

import lombok.extern.slf4j.Slf4j;

import javax.script.*;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 脚本执行引擎
 * 支持 Groovy 和 JavaScript 脚本转换
 */
@Slf4j
public class ScriptEngine {

    private final ScriptEngineManager manager = new ScriptEngineManager();
    private final long scriptTimeoutMs;

    public ScriptEngine(long scriptTimeoutMs) {
        this.scriptTimeoutMs = scriptTimeoutMs > 0 ? scriptTimeoutMs : 5000;
    }

    public ScriptEngine() {
        this(5000);
    }

    /**
     * 执行脚本
     *
     * @param script     脚本内容
     * @param language   脚本语言: GROOVY / JAVASCRIPT
     * @param context    脚本上下文变量
     * @return 脚本执行结果
     */
    public Object execute(String script, String language, Map<String, Object> context) throws Exception {
        String engineName = getEngineName(language);
        javax.script.ScriptEngine engine = manager.getEngineByName(engineName);

        if (engine == null) {
            throw new UnsupportedOperationException("不支持的脚本语言: " + language + " (引擎名: " + engineName + ")");
        }

        // 设置上下文变量
        SimpleBindings bindings = new SimpleBindings();
        if (context != null) {
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                bindings.put(entry.getKey(), entry.getValue());
            }
        }
        // 内置工具函数
        bindings.put("log", log);

        // 超时控制
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Object> future = executor.submit(() -> engine.eval(script, bindings));
            return future.get(scriptTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("脚本执行超时(" + scriptTimeoutMs + "ms)", e);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 执行 Groovy 脚本（常用场景的快捷方法）
     */
    public Object executeGroovy(String script, Map<String, Object> context) throws Exception {
        return execute(script, "GROOVY", context);
    }

    /**
     * 执行 JavaScript 脚本
     */
    public Object executeJavaScript(String script, Map<String, Object> context) throws Exception {
        return execute(script, "JAVASCRIPT", context);
    }

    private String getEngineName(String language) {
        if ("GROOVY".equalsIgnoreCase(language)) {
            return "groovy";
        } else if ("JAVASCRIPT".equalsIgnoreCase(language) || "JS".equalsIgnoreCase(language)) {
            return "nashorn";
        } else if ("PYTHON".equalsIgnoreCase(language)) {
            return "python";
        }
        return language.toLowerCase();
    }

    /**
     * 内置函数库：MD5加密
     */
    public static String md5(String input) {
        if (input == null) return null;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5计算失败", e);
        }
    }

    /**
     * 内置函数库：Base64编码
     */
    public static String base64Encode(String input) {
        if (input == null) return null;
        return java.util.Base64.getEncoder().encodeToString(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * 内置函数库：Base64解码
     */
    public static String base64Decode(String input) {
        if (input == null) return null;
        return new String(java.util.Base64.getDecoder().decode(input), java.nio.charset.StandardCharsets.UTF_8);
    }
}
