package com.etl.engine.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.etl.engine.entity.EtlDagNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 节点执行调度服务
 * 为33种节点类型提供执行逻辑骨架
 */
@Slf4j
@Service
public class NodeExecutorService {

    private Map<String, Object> parseConfig(EtlDagNode node) {
        try {
            if (node.getConfig() != null && !node.getConfig().isEmpty()) {
                return JSON.parseObject(node.getConfig());
            }
        } catch (Exception e) { /* ignore */ }
        return new JSONObject();
    }

    /**
     * 执行单个DAG节点
     */
    public String executeNode(EtlDagNode node, Map<String, Object> context) {
        String type = node.getNodeType();
        log.info("[NodeExec] 开始执行节点: id={}, type={}, name={}", node.getNodeId(), type, node.getNodeName());

        try {
            return switch (type) {
                // 数据同步
                case "DB_SYNC" -> executeDbSync(node, context);
                case "API_SYNC" -> executeApiSync(node, context);
                case "SERVER_DS" -> executeServerDs(node, context);
                case "FILE_READ" -> executeFileRead(node, context);
                case "JIANADAOYUN" -> executeJiandaoyun(node, context);

                // 数据转换
                case "VISUAL_TRANSFORM" -> executeVisualTransform(node, context);
                case "FIELD_SELECT" -> executeFieldSelect(node, context);
                case "FIELD_RENAME" -> executeFieldRename(node, context);
                case "FIELD_CALC" -> executeFieldCalc(node, context);
                case "DATA_FILTER" -> executeDataFilter(node, context);
                case "DATA_AGG" -> executeDataAgg(node, context);
                case "DATA_JOIN" -> executeDataJoin(node, context);
                case "DATA_SORT" -> executeDataSort(node, context);
                case "DATA_DEDUP" -> executeDataDedup(node, context);
                case "FIELD_SPLIT" -> executeFieldSplit(node, context);
                case "NULL_HANDLE" -> executeNullHandle(node, context);
                case "JSON_PARSE" -> executeJsonParse(node, context);
                case "XML_PARSE" -> executeXmlParse(node, context);
                case "DATA_COMPARE" -> executeDataCompare(node, context);

                // 脚本
                case "SQL_SCRIPT" -> executeSqlScript(node, context);
                case "PYTHON_SCRIPT" -> executePythonScript(node, context);
                case "SHELL_SCRIPT" -> executeShellScript(node, context);
                case "BAT_SCRIPT" -> executeBatScript(node, context);

                // 流程控制
                case "CONDITION" -> executeCondition(node, context);
                case "PARAM_ASSIGN" -> executeParamAssign(node, context);
                case "CALL_TASK" -> executeCallTask(node, context);
                case "LOOP_CONTAINER" -> executeLoopContainer(node, context);
                case "MESSAGE_NOTIFY" -> executeMessageNotify(node, context);
                case "VIRTUAL_NODE" -> executeVirtualNode(node, context);
                case "NOTE" -> executeNote(node, context);

                // 文件传输/输出
                case "FTP_UPLOAD" -> executeFtpUpload(node, context);
                case "SFTP" -> executeSftp(node, context);
                case "LOCAL_FILE" -> executeLocalFile(node, context);
                case "DB_OUTPUT" -> executeDbOutput(node, context);
                case "API_OUTPUT" -> executeApiOutput(node, context);

                default -> {
                    log.warn("[NodeExec] 未知节点类型: {}", type);
                    yield "SKIPPED";
                }
            };
        } catch (Exception e) {
            log.error("[NodeExec] 节点执行失败: type={}, id={}", type, node.getNodeId(), e);
            return "FAILED";
        }
    }

    // ========== 数据同步节点 ==========

    private String executeDbSync(EtlDagNode node, Map<String, Object> ctx) {
        Map<String, Object> cfg = parseConfig(node);
        String sourceDsId = String.valueOf(cfg.getOrDefault("datasourceId", ""));
        String tableName = String.valueOf(cfg.getOrDefault("tableName", ""));
        String syncMode = String.valueOf(cfg.getOrDefault("syncMode", "FULL"));
        log.info("[DB_SYNC] 数据源={}, 表={}, 模式={}", sourceDsId, tableName, syncMode);
        ctx.put("syncMode", syncMode);
        ctx.put("sourceTable", tableName);
        return "SUCCESS";
    }

    private String executeApiSync(EtlDagNode node, Map<String, Object> ctx) {
        String url = String.valueOf(parseConfig(node).getOrDefault("url", ""));
        String method = String.valueOf(parseConfig(node).getOrDefault("method", "GET"));
        log.info("[API_SYNC] URL={}, method={}", url, method);
        return "SUCCESS";
    }

    private String executeServerDs(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[SERVER_DS] 执行服务器数据集读取");
        return "SUCCESS";
    }

    private String executeFileRead(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[FILE_READ] 执行文件读取");
        return "SUCCESS";
    }

    private String executeJiandaoyun(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[JIANADAOYUN] 执行简道云数据集成");
        return "SUCCESS";
    }

    // ========== 数据转换节点 ==========

    private String executeVisualTransform(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[VISUAL_TRANSFORM] 执行可视化转换: config={}", parseConfig(node));
        ctx.put("transformExecuted", true);
        return "SUCCESS";
    }

    private String executeFieldSelect(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[FIELD_SELECT] 字段选择: fields={}", parseConfig(node).get("selectedFields"));
        return "SUCCESS";
    }

    private String executeFieldRename(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[FIELD_RENAME] 字段重命名: mappings={}", parseConfig(node).get("mappings"));
        return "SUCCESS";
    }

    private String executeFieldCalc(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[FIELD_CALC] 表达式={}", parseConfig(node).get("expression"));
        return "SUCCESS";
    }

    private String executeDataFilter(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[DATA_FILTER] 条件={}", parseConfig(node).get("condition"));
        return "SUCCESS";
    }

    private String executeDataAgg(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[DATA_AGG] 分组={}, 聚合={}", parseConfig(node).get("groupFields"), parseConfig(node).get("aggregateFields"));
        return "SUCCESS";
    }

    private String executeDataJoin(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[DATA_JOIN] 类型={}, 条件={}", parseConfig(node).get("joinType"), parseConfig(node).get("joinCondition"));
        return "SUCCESS";
    }

    private String executeDataSort(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[DATA_SORT] 字段={}, 顺序={}", parseConfig(node).get("fields"), parseConfig(node).get("order"));
        return "SUCCESS";
    }

    private String executeDataDedup(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[DATA_DEDUP] 去重字段={}", parseConfig(node).get("fields"));
        return "SUCCESS";
    }

    private String executeFieldSplit(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[FIELD_SPLIT] 规则={}", parseConfig(node).get("splitPattern"));
        return "SUCCESS";
    }

    private String executeNullHandle(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[NULL_HANDLE] 字段={}, 策略={}", parseConfig(node).get("fields"), parseConfig(node).get("strategy"));
        return "SUCCESS";
    }

    private String executeJsonParse(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[JSON_PARSE] 源字段={}", parseConfig(node).get("sourceField"));
        return "SUCCESS";
    }

    private String executeXmlParse(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[XML_PARSE] 根路径={}", parseConfig(node).get("rootPath"));
        return "SUCCESS";
    }

    private String executeDataCompare(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[DATA_COMPARE] 比对表={}", parseConfig(node).get("compareTable"));
        return "SUCCESS";
    }

    // ========== 脚本节点 ==========

    private String executeSqlScript(EtlDagNode node, Map<String, Object> ctx) {
        String sql = String.valueOf(parseConfig(node).getOrDefault("scriptContent", ""));
        log.info("[SQL_SCRIPT] SQL长度={}", sql.length());
        ctx.put("sqlExecuted", true);
        return "SUCCESS";
    }

    private String executePythonScript(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[PYTHON_SCRIPT] 脚本长度={}", String.valueOf(parseConfig(node).getOrDefault("scriptContent","")).length());
        return "SUCCESS";
    }

    private String executeShellScript(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[SHELL_SCRIPT] 脚本长度={}", String.valueOf(parseConfig(node).getOrDefault("scriptContent","")).length());
        return "SUCCESS";
    }

    private String executeBatScript(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[BAT_SCRIPT] 脚本长度={}", String.valueOf(parseConfig(node).getOrDefault("scriptContent","")).length());
        return "SUCCESS";
    }

    // ========== 流程控制节点 ==========

    private String executeCondition(EtlDagNode node, Map<String, Object> ctx) {
        String expr = String.valueOf(parseConfig(node).getOrDefault("conditionExpr", "true"));
        log.info("[CONDITION] 表达式={}", expr);
        ctx.put("conditionResult", "branch_true"); // 简化：始终走true分支
        return "SUCCESS";
    }

    private String executeParamAssign(EtlDagNode node, Map<String, Object> ctx) {
        String paramName = String.valueOf(parseConfig(node).getOrDefault("paramName", "param"));
        String paramValue = String.valueOf(parseConfig(node).getOrDefault("paramValue", ""));
        log.info("[PARAM_ASSIGN] {}={}", paramName, paramValue);
        ctx.put(paramName, paramValue);
        return "SUCCESS";
    }

    private String executeCallTask(EtlDagNode node, Map<String, Object> ctx) {
        String targetTaskId = String.valueOf(parseConfig(node).getOrDefault("targetTaskId", ""));
        log.info("[CALL_TASK] 调用任务ID={}", targetTaskId);
        return "SUCCESS";
    }

    private String executeLoopContainer(EtlDagNode node, Map<String, Object> ctx) {
        String loopType = String.valueOf(parseConfig(node).getOrDefault("loopType", "FOR"));
        int maxLoops = Integer.parseInt(String.valueOf(parseConfig(node).getOrDefault("maxLoops", "10")));
        log.info("[LOOP_CONTAINER] 类型={}, 最大循环={}", loopType, maxLoops);
        return "SUCCESS";
    }

    private String executeMessageNotify(EtlDagNode node, Map<String, Object> ctx) {
        String channels = String.valueOf(parseConfig(node).getOrDefault("notifyChannels", ""));
        String title = String.valueOf(parseConfig(node).getOrDefault("notifyTitle", ""));
        log.info("[MESSAGE_NOTIFY] 渠道={}, 标题={}", channels, title);
        return "SUCCESS";
    }

    private String executeVirtualNode(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[VIRTUAL_NODE] 虚拟节点，无实际操作");
        return "SUCCESS";
    }

    private String executeNote(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[NOTE] 备注节点，无实际操作");
        return "SUCCESS";
    }

    // ========== 文件传输/输出节点 ==========

    private String executeFtpUpload(EtlDagNode node, Map<String, Object> ctx) {
        String host = String.valueOf(parseConfig(node).getOrDefault("serverHost", ""));
        int port = Integer.parseInt(String.valueOf(parseConfig(node).getOrDefault("serverPort", "21")));
        log.info("[FTP_UPLOAD] {}:{}", host, port);
        return "SUCCESS";
    }

    private String executeSftp(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[SFTP] 执行SFTP传输");
        return "SUCCESS";
    }

    private String executeLocalFile(EtlDagNode node, Map<String, Object> ctx) {
        log.info("[LOCAL_FILE] 执行本地文件操作");
        return "SUCCESS";
    }

    private String executeDbOutput(EtlDagNode node, Map<String, Object> ctx) {
        String writeMode = String.valueOf(parseConfig(node).getOrDefault("writeMode", "INSERT"));
        String targetTable = String.valueOf(parseConfig(node).getOrDefault("targetTable", ""));
        log.info("[DB_OUTPUT] 表={}, 模式={}", targetTable, writeMode);
        return "SUCCESS";
    }

    private String executeApiOutput(EtlDagNode node, Map<String, Object> ctx) {
        String url = String.valueOf(parseConfig(node).getOrDefault("targetUrl", ""));
        String method = String.valueOf(parseConfig(node).getOrDefault("method", "POST"));
        log.info("[API_OUTPUT] URL={}, method={}", url, method);
        return "SUCCESS";
    }
}
