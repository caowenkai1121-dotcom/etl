<template>
  <div class="node-property-panel">
    <div v-if="!node && !edge" class="empty-state">
      <el-icon :size="40" class="empty-icon"><InfoFilled /></el-icon>
      <p class="empty-text">选择节点或连线查看属性</p>
    </div>

    <!-- 节点属性 -->
    <div v-if="node" class="properties-wrap">
      <!-- 顶部区域：节点名称 + 关闭 + 生成数据转换按钮 -->
      <div class="panel-header">
        <div class="header-title-row">
          <el-input
            v-model="nodeData.name"
            class="node-name-input"
            size="default"
            @input="emitUpdate"
            placeholder="节点名称"
          />
          <el-button link class="close-btn" @click="handleCloseNode">
            <el-icon :size="18"><Close /></el-icon>
          </el-button>
        </div>
        <el-button
          v-if="isSyncNode"
          type="primary"
          size="small"
          class="generate-btn"
          @click="handleGenerateTransform"
        >
          Generate Data Transformation
        </el-button>
      </div>

      <!-- 标签页 -->
      <el-tabs v-model="activeTab" class="config-tabs">
        <!-- ==================== Tab 1: Node Information ==================== -->
        <el-tab-pane label="Node Information" name="nodeInfo">
          <div class="tab-content">
            <el-form label-width="100px" size="small" label-position="left">
              <el-form-item label="Node Name">
                <el-input v-model="nodeData.name" @input="emitUpdate" placeholder="节点名称" />
              </el-form-item>
              <el-form-item label="Node ID">
                <el-input :model-value="nodeData.id" disabled />
              </el-form-item>
              <el-form-item label="Node Type">
                <el-tag size="small" effect="plain" :color="nodeColor">{{ typeLabel }}</el-tag>
              </el-form-item>
              <el-form-item label="Description">
                <el-input
                  v-model="nodeData.description"
                  type="textarea"
                  :rows="3"
                  @input="emitUpdate"
                  placeholder="节点描述（可选）"
                />
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- ==================== Tab 2: Data Source ==================== -->
        <el-tab-pane label="Data Source" name="dataSource">
          <div class="tab-content">
            <!-- 数据同步节点：数据源配置 -->
            <template v-if="isSyncNode">
              <el-form label-width="110px" size="small" label-position="left">
                <el-form-item label="Source Type">
                  <el-select v-model="nodeData.config.sourceType" @change="emitUpdate" style="width:100%" placeholder="选择数据源类型">
                    <el-option v-for="t in datasourceTypes" :key="t" :label="t" :value="t" />
                  </el-select>
                </el-form-item>
                <el-form-item label="Data Connection">
                  <el-select v-model="nodeData.config.datasourceId" @change="handleDatasourceChange" style="width:100%" placeholder="选择数据连接">
                    <el-option v-for="ds in datasourceList" :key="ds.id" :label="ds.name" :value="ds.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label=" ">
                  <div class="action-btn-row">
                    <el-button size="small" @click="handleTestConnection">Test Connection</el-button>
                    <el-button size="small" @click="handleDataPreview">Data Preview</el-button>
                  </div>
                </el-form-item>
                <el-form-item label="Data Table">
                  <el-select v-model="nodeData.config.tableName" @change="emitUpdate" style="width:100%" placeholder="选择数据表" filterable>
                    <el-option v-for="t in tableList" :key="t.name" :label="t.name" :value="t.name" />
                  </el-select>
                </el-form-item>
                <el-form-item label="SQL Mode">
                  <el-radio-group v-model="nodeData.config.sqlMode" @change="emitUpdate" size="small">
                    <el-radio-button value="TABLE">Table</el-radio-button>
                    <el-radio-button value="SQL">SQL</el-radio-button>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="SQL Statement" v-if="nodeData.config.sqlMode === 'SQL'">
                  <div class="sql-editor-wrap">
                    <div class="sql-toolbar">
                      <span class="sql-lang-label">SQL</span>
                      <el-button link size="small" @click="formatScript">格式化</el-button>
                    </div>
                    <el-input
                      v-model="nodeData.config.sqlStatement"
                      type="textarea"
                      :rows="6"
                      @input="emitUpdate"
                      placeholder="SELECT * FROM table_name WHERE ..."
                      class="sql-textarea"
                    />
                  </div>
                </el-form-item>
                <el-form-item label="Partition Field">
                  <el-select v-model="nodeData.config.partitionField" @change="emitUpdate" style="width:100%" placeholder="选择分区字段（可选）" clearable filterable>
                    <el-option v-for="f in inputFields" :key="f.name" :label="f.name + ' (' + f.type + ')'" :value="f.name" />
                  </el-select>
                </el-form-item>
              </el-form>
            </template>

            <!-- 脚本节点：脚本配置 -->
            <template v-if="isScriptNode">
              <el-form label-width="100px" size="small" label-position="left">
                <el-form-item label="Script Type">
                  <el-select v-model="nodeData.config.scriptType" @change="emitUpdate" style="width:100%">
                    <el-option label="SQL" value="SQL" />
                    <el-option label="Python" value="PYTHON" />
                    <el-option label="Shell" value="SHELL" />
                    <el-option label="Bat" value="BAT" />
                  </el-select>
                </el-form-item>
                <el-form-item label="Data Source" v-if="nodeData.config.scriptType === 'SQL'">
                  <el-select v-model="nodeData.config.datasourceId" @change="emitUpdate" style="width:100%" placeholder="选择数据源">
                    <el-option v-for="ds in datasourceList" :key="ds.id" :label="ds.name" :value="ds.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="Script Content">
                  <div class="sql-editor-wrap">
                    <div class="sql-toolbar">
                      <span class="sql-lang-label">{{ nodeData.config.scriptType || 'SQL' }}</span>
                      <el-button link size="small" @click="formatScript">格式化</el-button>
                    </div>
                    <el-input
                      v-model="nodeData.config.scriptContent"
                      type="textarea"
                      :rows="10"
                      @input="emitUpdate"
                      placeholder="输入脚本内容..."
                      class="sql-textarea"
                    />
                  </div>
                </el-form-item>
              </el-form>
            </template>

            <!-- 文件传输节点 -->
            <template v-if="isFileNode">
              <el-form label-width="100px" size="small" label-position="left">
                <el-form-item label="Protocol">
                  <el-select v-model="nodeData.config.transferProtocol" @change="emitUpdate" style="width:100%" placeholder="选择传输协议">
                    <el-option label="FTP" value="FTP" />
                    <el-option label="SFTP" value="SFTP" />
                    <el-option label="Local File" value="LOCAL" />
                    <el-option label="HDFS" value="HDFS" />
                    <el-option label="OSS" value="OSS" />
                  </el-select>
                </el-form-item>
                <el-form-item label="Server Host" v-if="nodeData.config.transferProtocol !== 'LOCAL'">
                  <el-input v-model="nodeData.config.serverHost" @input="emitUpdate" placeholder="服务器地址" />
                </el-form-item>
                <el-form-item label="Port" v-if="nodeData.config.transferProtocol !== 'LOCAL'">
                  <el-input-number v-model="nodeData.config.serverPort" :min="1" :max="65535" @change="emitUpdate" style="width:100%" />
                </el-form-item>
                <el-form-item label="Source Path">
                  <el-input v-model="nodeData.config.sourcePath" @input="emitUpdate" placeholder="/data/input/source.csv" />
                </el-form-item>
                <el-form-item label="Target Path">
                  <el-input v-model="nodeData.config.targetPath" @input="emitUpdate" placeholder="/data/output/" />
                </el-form-item>
                <el-form-item label="File Name">
                  <el-input v-model="nodeData.config.fileName" @input="emitUpdate" placeholder="output_${date}.csv" />
                </el-form-item>
              </el-form>
            </template>

            <!-- 转换节点：无独立数据源配置 -->
            <div v-if="isTransformNode" class="info-tip">
              Data Transformation节点无需配置独立数据源，上游节点输出将作为输入。
            </div>

            <!-- 控制节点：无独立数据源配置 -->
            <div v-if="isControlNode" class="info-tip">
              控制节点无需配置数据源。
            </div>
          </div>
        </el-tab-pane>

        <!-- ==================== Tab 3: Data Destination and Mapping ==================== -->
        <el-tab-pane label="Data Destination and Mapping" name="destMapping">
          <div class="tab-content">
            <!-- 数据同步节点：目标端和字段映射 -->
            <template v-if="isSyncNode">
              <el-form label-width="110px" size="small" label-position="left">
                <el-form-item label="Target Type">
                  <el-select v-model="nodeData.config.targetType" @change="emitUpdate" style="width:100%" placeholder="选择目标数据源类型">
                    <el-option v-for="t in datasourceTypes" :key="t" :label="t" :value="t" />
                  </el-select>
                </el-form-item>
                <el-form-item label="Target Connection">
                  <el-select v-model="nodeData.config.targetDatasourceId" @change="emitUpdate" style="width:100%" placeholder="选择目标数据连接">
                    <el-option v-for="ds in datasourceList" :key="ds.id" :label="ds.name" :value="ds.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="Target Table">
                  <el-input v-model="nodeData.config.targetTableName" @input="emitUpdate" placeholder="目标表名" />
                </el-form-item>
                <el-form-item label=" ">
                  <el-checkbox v-model="nodeData.config.clearBeforeWrite" @change="emitUpdate" size="small">
                    Clear target table before writing
                  </el-checkbox>
                </el-form-item>
              </el-form>

              <!-- 字段映射 -->
              <div class="mapping-section">
                <div class="mapping-toolbar">
                  <span class="mapping-label">Field Mapping</span>
                  <div class="mapping-actions">
                    <el-button size="small" type="primary" plain @click="handleAutoMapping">Auto Match</el-button>
                    <el-button size="small" plain @click="handleClearMapping">Clear Mapping</el-button>
                    <el-button size="small" plain @click="handleAddMappingRow">Add Row</el-button>
                  </div>
                </div>
                <el-table
                  :data="fieldMappings"
                  size="small"
                  border
                  class="mapping-table"
                  max-height="260"
                >
                  <el-table-column label="#" type="index" width="40" align="center" />
                  <el-table-column label="Source Field" width="150">
                    <template #default="{ row }">
                      <el-select v-model="row.sourceField" size="small" style="width:100%" placeholder="源字段" filterable @change="emitUpdate">
                        <el-option v-for="f in inputFields" :key="f.name" :label="f.name" :value="f.name" />
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column label="Target Field" width="150">
                    <template #default="{ row }">
                      <el-input v-model="row.targetField" size="small" placeholder="目标字段" @input="emitUpdate" />
                    </template>
                  </el-table-column>
                  <el-table-column label="Type" width="100">
                    <template #default="{ row }">
                      <el-input :model-value="row.fieldType" size="small" disabled placeholder="自动匹配" />
                    </template>
                  </el-table-column>
                  <el-table-column label="Operation" width="80" align="center">
                    <template #default="{ $index }">
                      <el-button link type="danger" size="small" @click="handleRemoveMappingRow($index)">
                        <el-icon><Delete /></el-icon>
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </template>

            <!-- 转换节点：转换算子配置 -->
            <template v-if="isTransformNode">
              <el-form label-width="100px" size="small" label-position="left">
                <el-form-item label="Operator Type">
                  <el-select v-model="nodeData.config.operatorType" @change="handleOperatorTypeChange" style="width:100%" placeholder="选择转换算子">
                    <el-option v-for="op in transformTypes" :key="op.value" :label="op.label" :value="op.value" />
                  </el-select>
                </el-form-item>
              </el-form>
              <component
                :is="operatorComponent"
                v-if="operatorComponent"
                v-model:config="nodeData.config"
                :input-fields="inputFields"
                :available-tables="availableTables"
                @update:config="handleOperatorUpdate"
              />
              <div v-else-if="!nodeData.config.operatorType" class="info-tip">请选择转换算子类型以配置具体参数</div>
            </template>

            <!-- 控制节点 -->
            <template v-if="isControlNode">
              <el-form label-width="100px" size="small" label-position="left">
                <!-- 条件分支 -->
                <template v-if="nodeData.type === 'CONDITION'">
                  <el-form-item label="Condition">
                    <el-input v-model="nodeData.config.conditionExpr" type="textarea" :rows="3" @input="emitUpdate"
                      placeholder="如: ${row_count} >= 10000 ? 'branch_a' : 'branch_b'" />
                  </el-form-item>
                </template>
                <!-- 参数赋值 -->
                <template v-if="nodeData.type === 'PARAM_ASSIGN'">
                  <el-form-item label="Param Name">
                    <el-input v-model="nodeData.config.paramName" @input="emitUpdate" placeholder="参数名称" />
                  </el-form-item>
                  <el-form-item label="Source">
                    <el-select v-model="nodeData.config.paramSource" @change="emitUpdate" style="width:100%">
                      <el-option label="Upstream Output" value="UPSTREAM" />
                      <el-option label="System Variable" value="SYSTEM" />
                      <el-option label="Constant" value="CONSTANT" />
                      <el-option label="SQL Query" value="SQL" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="Expression" v-if="nodeData.config.paramSource !== 'CONSTANT'">
                    <el-input v-model="nodeData.config.paramExpression" @input="emitUpdate" placeholder="取值表达式" />
                  </el-form-item>
                  <el-form-item label="Value" v-if="nodeData.config.paramSource === 'CONSTANT'">
                    <el-input v-model="nodeData.config.paramValue" @input="emitUpdate" placeholder="常量值" />
                  </el-form-item>
                  <el-form-item label="Default">
                    <el-input v-model="nodeData.config.defaultValue" @input="emitUpdate" />
                  </el-form-item>
                </template>
                <!-- 调用任务 -->
                <template v-if="nodeData.type === 'CALL_TASK'">
                  <el-form-item label="Target Task">
                    <el-select v-model="nodeData.config.targetTaskId" @change="emitUpdate" style="width:100%" filterable>
                      <el-option v-for="t in taskList" :key="t.id" :label="t.name" :value="t.id" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="Pass Params">
                    <el-input v-model="nodeData.config.passParams" @input="emitUpdate" placeholder="key1=val1&key2=${param}" />
                  </el-form-item>
                  <el-form-item label="Wait Complete">
                    <el-switch v-model="nodeData.config.waitComplete" @change="emitUpdate" size="small" />
                  </el-form-item>
                </template>
                <!-- 循环容器 -->
                <template v-if="nodeData.type === 'LOOP_CONTAINER'">
                  <el-form-item label="Loop Type">
                    <el-radio-group v-model="nodeData.config.loopType" @change="emitUpdate" size="small">
                      <el-radio-button value="FOR">For</el-radio-button>
                      <el-radio-button value="WHILE">While</el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                  <el-form-item label="Condition" v-if="nodeData.config.loopType === 'WHILE'">
                    <el-input v-model="nodeData.config.loopCondition" @input="emitUpdate" placeholder="${index} < ${total}" />
                  </el-form-item>
                  <el-form-item label="Source" v-if="nodeData.config.loopType === 'FOR'">
                    <el-input v-model="nodeData.config.loopSource" @input="emitUpdate" placeholder="${param_list}" />
                  </el-form-item>
                  <el-form-item label="Max Loops">
                    <el-input-number v-model="nodeData.config.maxLoops" :min="1" :max="10000" @change="emitUpdate" style="width:100%" />
                  </el-form-item>
                </template>
                <!-- 消息通知 -->
                <template v-if="nodeData.type === 'MESSAGE_NOTIFY'">
                  <el-form-item label="Channels">
                    <el-checkbox-group v-model="nodeData.config.notifyChannels" @change="emitUpdate" size="small">
                      <el-checkbox label="EMAIL">Email</el-checkbox>
                      <el-checkbox label="SMS">SMS</el-checkbox>
                      <el-checkbox label="WECHAT">WeChat</el-checkbox>
                      <el-checkbox label="DINGTALK">DingTalk</el-checkbox>
                    </el-checkbox-group>
                  </el-form-item>
                  <el-form-item label="Receivers">
                    <el-input v-model="nodeData.config.receivers" @input="emitUpdate" placeholder="逗号分隔" />
                  </el-form-item>
                  <el-form-item label="Title">
                    <el-input v-model="nodeData.config.notifyTitle" @input="emitUpdate" />
                  </el-form-item>
                  <el-form-item label="Content">
                    <el-input v-model="nodeData.config.notifyContent" type="textarea" :rows="3" @input="emitUpdate" />
                  </el-form-item>
                </template>
                <!-- 虚拟节点 -->
                <template v-if="nodeData.type === 'VIRTUAL_NODE'">
                  <div class="info-tip">虚拟节点仅用于流程汇聚/分支编排，不执行实际操作</div>
                </template>
                <!-- 备注 -->
                <template v-if="nodeData.type === 'NOTE'">
                  <el-form-item label="Note">
                    <el-input v-model="nodeData.config.noteContent" type="textarea" :rows="4" @input="emitUpdate" />
                  </el-form-item>
                </template>
              </el-form>
            </template>

            <!-- 脚本/文件节点：无映射配置 -->
            <div v-if="isScriptNode || isFileNode" class="info-tip">
              该节点类型无需配置目标端和字段映射。
            </div>
          </div>
        </el-tab-pane>

        <!-- ==================== Tab 4: Write Method ==================== -->
        <el-tab-pane label="Write Method" name="writeMethod">
          <div class="tab-content">
            <!-- 数据同步节点：写入方式详细配置 -->
            <template v-if="isSyncNode">
              <el-form label-width="120px" size="small" label-position="left">
                <el-form-item label="Write Mode">
                  <el-select v-model="nodeData.config.writeMode" @change="emitUpdate" style="width:100%" placeholder="选择写入模式">
                    <el-option label="INSERT" value="INSERT" />
                    <el-option label="UPDATE" value="UPDATE" />
                    <el-option label="UPSERT" value="UPSERT" />
                    <el-option label="DELETE" value="DELETE" />
                  </el-select>
                </el-form-item>
                <el-form-item label="Batch Size">
                  <el-input-number v-model="nodeData.config.batchSize" :min="100" :max="50000" :step="100" @change="emitUpdate" style="width:100%" />
                </el-form-item>
                <el-form-item label="Transaction Mode">
                  <el-select v-model="nodeData.config.transactionMode" @change="emitUpdate" style="width:100%" placeholder="选择事务模式">
                    <el-option label="Auto Commit" value="AUTO" />
                    <el-option label="Per Batch" value="PER_BATCH" />
                    <el-option label="Manual" value="MANUAL" />
                  </el-select>
                </el-form-item>
                <el-form-item label="Concurrency">
                  <el-input-number v-model="nodeData.config.concurrency" :min="1" :max="100" @change="emitUpdate" style="width:100%" />
                </el-form-item>
                <el-form-item label="Sync Mode">
                  <el-radio-group v-model="nodeData.config.syncMode" @change="emitUpdate" size="small">
                    <el-radio-button value="FULL">Full</el-radio-button>
                    <el-radio-button value="INCREMENTAL">Incremental</el-radio-button>
                    <el-radio-button value="CDC">CDC Real-time</el-radio-button>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="Incremental Field" v-if="nodeData.config.syncMode === 'INCREMENTAL'">
                  <el-select v-model="nodeData.config.incrField" @change="emitUpdate" style="width:100%" placeholder="选择增量字段">
                    <el-option v-for="f in inputFields" :key="f.name" :label="f.name" :value="f.name" />
                  </el-select>
                </el-form-item>
              </el-form>

              <!-- 高级设置 -->
              <div class="advanced-section">
                <div class="advanced-header" @click="showAdvanced = !showAdvanced">
                  <span class="advanced-label">Advanced Settings</span>
                  <el-icon :size="14">
                    <ArrowDown v-if="!showAdvanced" />
                    <ArrowUp v-else />
                  </el-icon>
                </div>
                <el-collapse-transition>
                  <el-form v-show="showAdvanced" label-width="120px" size="small" label-position="left" class="advanced-form">
                    <el-form-item label="Timeout (s)">
                      <el-input-number v-model="nodeData.config.timeout" :min="60" :max="3600" @change="emitUpdate" style="width:100%" />
                    </el-form-item>
                    <el-form-item label="Retry Times">
                      <el-input-number v-model="nodeData.config.retryTimes" :min="0" :max="10" @change="emitUpdate" style="width:100%" />
                    </el-form-item>
                    <el-form-item label="Retry Interval (s)">
                      <el-input-number v-model="nodeData.config.retryInterval" :min="1" :max="300" @change="emitUpdate" style="width:100%" />
                    </el-form-item>
                    <el-form-item label="Fail Strategy">
                      <el-select v-model="nodeData.config.failStrategy" @change="emitUpdate" style="width:100%">
                        <el-option label="Suspend" value="SUSPEND" />
                        <el-option label="Skip" value="SKIP" />
                        <el-option label="Alert Only" value="ALERT" />
                      </el-select>
                    </el-form-item>
                  </el-form>
                </el-collapse-transition>
              </div>
            </template>

            <!-- 非同步节点：高级设置 -->
            <template v-if="!isSyncNode">
              <el-form label-width="120px" size="small" label-position="left">
                <el-form-item label="Timeout (s)">
                  <el-input-number v-model="nodeData.config.timeout" :min="60" :max="3600" @change="emitUpdate" style="width:100%" />
                </el-form-item>
                <el-form-item label="Retry Times">
                  <el-input-number v-model="nodeData.config.retryTimes" :min="0" :max="10" @change="emitUpdate" style="width:100%" />
                </el-form-item>
                <el-form-item label="Retry Interval (s)">
                  <el-input-number v-model="nodeData.config.retryInterval" :min="1" :max="300" @change="emitUpdate" style="width:100%" />
                </el-form-item>
                <el-form-item label="Fail Strategy">
                  <el-select v-model="nodeData.config.failStrategy" @change="emitUpdate" style="width:100%">
                    <el-option label="Suspend" value="SUSPEND" />
                    <el-option label="Skip" value="SKIP" />
                    <el-option label="Alert Only" value="ALERT" />
                  </el-select>
                </el-form-item>
                <el-form-item label="Concurrency">
                  <el-input-number v-model="nodeData.config.concurrency" :min="1" :max="100" @change="emitUpdate" style="width:100%" />
                </el-form-item>
              </el-form>
            </template>
          </div>
        </el-tab-pane>
      </el-tabs>

      <!-- 底部删除按钮 -->
      <div class="prop-actions">
        <el-button type="danger" size="small" plain @click="handleDelete" :disabled="!editable" style="width:100%">
          <el-icon><Delete /></el-icon> Delete Node
        </el-button>
      </div>
    </div>

    <!-- 连线属性 -->
    <div v-if="edge" class="properties-wrap">
      <div class="panel-header">
        <div class="header-title-row">
          <span class="edge-title">Edge Properties</span>
          <el-button link class="close-btn" @click="handleCloseEdge">
            <el-icon :size="18"><Close /></el-icon>
          </el-button>
        </div>
      </div>
      <div class="prop-section">
        <el-form label-width="100px" size="small" label-position="left">
          <el-form-item label="Source Node">
            <el-input :model-value="edge.source" disabled />
          </el-form-item>
          <el-form-item label="Target Node">
            <el-input :model-value="edge.target" disabled />
          </el-form-item>
          <el-form-item label="Condition">
            <el-radio-group v-model="edgeData.condition" @change="emitUpdate" size="small">
              <el-radio-button value="SUCCESS">Success</el-radio-button>
              <el-radio-button value="FAILED">Failed</el-radio-button>
              <el-radio-button value="ANY">Any</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </div>
      <div class="prop-actions">
        <el-button type="danger" size="small" plain @click="handleDeleteEdge" :disabled="!editable" style="width:100%">
          <el-icon><Delete /></el-icon> Delete Edge
        </el-button>
      </div>
    </div>
    <!-- Data Preview Dialog -->
    <el-dialog v-model="previewVisible" title="Data Preview" width="900px" :close-on-click-modal="false" destroy-on-close>
      <div v-loading="previewLoading" class="preview-container">
        <div v-if="previewError" class="preview-error">
          <el-icon :size="32"><WarningFilled /></el-icon>
          <p>{{ previewError }}</p>
        </div>
        <template v-else-if="previewColumns.length > 0">
          <div class="preview-info">
            <span>Table: <strong>{{ previewTableName }}</strong></span>
            <el-tag size="small" type="info">{{ previewRows.length }} rows</el-tag>
          </div>
          <el-table :data="previewRows" border stripe size="small" max-height="400" class="preview-table">
            <el-table-column
              v-for="col in previewColumns"
              :key="col"
              :prop="col"
              :label="col"
              min-width="120"
              show-overflow-tooltip
            />
          </el-table>
        </template>
        <el-empty v-else-if="!previewLoading" description="No data available" />
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">Close</el-button>
        <el-button type="primary" @click="handleDataPreview" :loading="previewLoading">Refresh</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, shallowRef } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled, Close, Delete, ArrowDown, ArrowUp, WarningFilled } from '@element-plus/icons-vue'
import { getDatasourceList, getTables, getTableInfo, testConnection } from '@/api'
import FieldSelect from './operators/FieldSelect.vue'
import FieldRename from './operators/FieldRename.vue'
import DataFilter from './operators/DataFilter.vue'
import DataAggregate from './operators/DataAggregate.vue'
import FieldCalculate from './operators/FieldCalculate.vue'
import DataJoin from './operators/DataJoin.vue'
import DataDeduplicate from './operators/DataDeduplicate.vue'
import FieldSplit from './operators/FieldSplit.vue'
import DataSort from './operators/DataSort.vue'
import NullHandler from './operators/NullHandler.vue'
import JsonParse from './operators/JsonParse.vue'
import XmlParse from './operators/XmlParse.vue'
import DataCompare from './operators/DataCompare.vue'

const props = defineProps({
  node: { type: Object, default: null },
  edge: { type: Object, default: null },
  editable: { type: Boolean, default: true }
})

const emit = defineEmits(['update', 'delete-node', 'delete-edge', 'generate-transform', 'close-node', 'close-edge'])

const nodeData = ref({})
const edgeData = ref({})
const datasourceList = ref([])
const tableList = ref([])
const taskList = ref([])
const inputFields = ref([])
const availableTables = ref([])
const showAdvanced = ref(false)
const activeTab = ref('nodeInfo')
const fieldMappings = ref([])

const datasourceTypes = [
  'MySQL', 'PostgreSQL', 'Oracle', 'SQL Server', 'Kafka',
  'MongoDB', 'Redis', 'Hive', 'HBase', 'Elasticsearch',
  'ClickHouse', 'TiDB', 'DB2', 'Sybase', 'Greenplum'
]

const operatorComponents = {
  FIELD_SELECT: FieldSelect, FIELD_RENAME: FieldRename, DATA_FILTER: DataFilter,
  DATA_AGG: DataAggregate, FIELD_CALC: FieldCalculate, DATA_JOIN: DataJoin,
  DATA_DEDUP: DataDeduplicate, FIELD_SPLIT: FieldSplit, DATA_SORT: DataSort,
  NULL_HANDLE: NullHandler, JSON_PARSE: JsonParse, XML_PARSE: XmlParse,
  DATA_COMPARE: DataCompare
}

const operatorComponent = shallowRef(null)

// FineDataLink 14节点类型标签映射
const nodeTypeLabels = {
  DATA_SYNC: '数据同步', DATA_TRANSFORM: '数据转换', FILE_TRANSFER: '文件传输',
  SQL_SCRIPT: 'SQL脚本', SHELL_SCRIPT: 'Shell脚本', BAT_SCRIPT: 'Bat脚本', PYTHON_SCRIPT: 'Python脚本',
  PARAM_ASSIGN: '参数赋值', CONDITION: '条件分支', CALL_TASK: '调用任务',
  LOOP_CONTAINER: '循环容器', MESSAGE_NOTIFY: '消息通知', VIRTUAL_NODE: '虚拟节点',
  NOTE: '备注'
}

const nodeTypeColors = {
  DATA_SYNC: '#1890ff', DATA_TRANSFORM: '#13c2c2', FILE_TRANSFER: '#52c41a',
  SQL_SCRIPT: '#1890ff', SHELL_SCRIPT: '#722ed1', BAT_SCRIPT: '#faad14', PYTHON_SCRIPT: '#52c41a',
  PARAM_ASSIGN: '#722ed1', CONDITION: '#faad14', CALL_TASK: '#13c2c2',
  LOOP_CONTAINER: '#1890ff', MESSAGE_NOTIFY: '#eb2f96', VIRTUAL_NODE: '#8c8c8c',
  NOTE: '#faad14'
}

// 转换算子子类型
const transformTypes = [
  { value: 'FIELD_SELECT', label: '字段选择' },
  { value: 'FIELD_RENAME', label: '字段重命名' },
  { value: 'DATA_FILTER', label: '数据过滤' },
  { value: 'DATA_AGG', label: '数据聚合' },
  { value: 'FIELD_CALC', label: '字段计算' },
  { value: 'DATA_JOIN', label: '数据关联' },
  { value: 'DATA_DEDUP', label: '数据去重' },
  { value: 'FIELD_SPLIT', label: '字段拆分' },
  { value: 'DATA_SORT', label: '数据排序' },
  { value: 'NULL_HANDLE', label: '空值处理' },
  { value: 'JSON_PARSE', label: 'JSON解析' },
  { value: 'XML_PARSE', label: 'XML解析' },
  { value: 'DATA_COMPARE', label: '数据比对' }
]

const typeLabel = computed(() => nodeTypeLabels[nodeData.value.type] || nodeData.value.type || '未知')
const nodeColor = computed(() => nodeTypeColors[nodeData.value.type] || '#999')

const isSyncNode = computed(() => nodeData.value.type === 'DATA_SYNC')
const isTransformNode = computed(() => nodeData.value.type === 'DATA_TRANSFORM')
const isScriptNode = computed(() => ['SQL_SCRIPT', 'PYTHON_SCRIPT', 'SHELL_SCRIPT', 'BAT_SCRIPT'].includes(nodeData.value.type))
const isControlNode = computed(() => ['PARAM_ASSIGN', 'CONDITION', 'CALL_TASK', 'LOOP_CONTAINER', 'MESSAGE_NOTIFY', 'VIRTUAL_NODE', 'NOTE'].includes(nodeData.value.type))
const isFileNode = computed(() => nodeData.value.type === 'FILE_TRANSFER')

watch(() => props.node, (n) => {
  if (n) {
    nodeData.value = {
      id: n.id, name: n.name || '', type: n.type,
      description: n.description || '',
      config: {
        timeout: 300, retryTimes: 0, retryInterval: 10, failStrategy: 'SUSPEND', concurrency: 1,
        sourceType: 'MySQL', targetType: 'MySQL', sqlMode: 'TABLE', writeMode: 'INSERT',
        batchSize: 1000, transactionMode: 'AUTO', syncMode: 'FULL',
        clearBeforeWrite: false,
        ...n.config
      }
    }
    if (n.config?.datasourceId) {
      loadTables(n.config.datasourceId)
      loadFields(n.config.datasourceId, n.config.tableName)
    }
    operatorComponent.value = operatorComponents[n.config?.operatorType] || null
    activeTab.value = 'nodeInfo'
    showAdvanced.value = false

    // 初始化字段映射
    if (n.config?.fieldMappings && Array.isArray(n.config.fieldMappings)) {
      fieldMappings.value = [...n.config.fieldMappings]
    } else {
      fieldMappings.value = []
    }
  }
}, { deep: true, immediate: true })

watch(() => props.edge, (e) => {
  if (e) edgeData.value = { id: e.id, source: e.source, target: e.target, condition: e.config?.condition || 'SUCCESS' }
}, { deep: true, immediate: true })

onMounted(async () => {
  try { const r = await getDatasourceList(); datasourceList.value = r.data || [] } catch (_) { /* ignore */ }
  try {
    const m = await import('@/api/dev')
    const r = await m.devAPI.getTaskList({ pageNum: 1, pageSize: 100 })
    taskList.value = r?.list || r?.data?.list || []
  } catch (_) { /* ignore */ }
})

const loadTables = async (dsId) => {
  try { const r = await getTables(dsId); tableList.value = r.data || [] } catch (_) { /* ignore */ }
}

const loadFields = async () => {
  inputFields.value = [
    { name: 'id', type: 'INT' },
    { name: 'name', type: 'VARCHAR' },
    { name: 'created_at', type: 'DATETIME' },
    { name: 'updated_at', type: 'DATETIME' },
    { name: 'status', type: 'TINYINT' },
    { name: 'amount', type: 'DECIMAL' }
  ]
}

const handleDatasourceChange = (dsId) => {
  nodeData.value.config.tableName = ''
  loadTables(dsId)
  emitUpdate()
}

const emitUpdate = () => {
  if (props.node) {
    const data = { ...nodeData.value }
    data.config = { ...data.config, fieldMappings: [...fieldMappings.value] }
    emit('update', data)
  } else if (props.edge) {
    emit('update', edgeData.value)
  }
}

const handleOperatorTypeChange = (operatorType) => {
  operatorComponent.value = operatorComponents[operatorType] || null
  nodeData.value.config = { ...nodeData.value.config, operatorType }
  emitUpdate()
}

const handleOperatorUpdate = (config) => {
  nodeData.value.config = { ...nodeData.value.config, ...config }
  emitUpdate()
}

const formatScript = () => {
  const script = nodeData.value.config.sqlStatement || nodeData.value.config.scriptContent
  if (!script || !script.trim()) {
    ElMessage.warning('没有可格式化的内容')
    return
  }
  const formatted = formatSQL(script)
  if (nodeData.value.config.sqlStatement) {
    nodeData.value.config.sqlStatement = formatted
  } else {
    nodeData.value.config.scriptContent = formatted
  }
  emitUpdate()
  ElMessage.success('格式化完成')
}

const formatSQL = (sql) => {
  const keywords = ['SELECT', 'FROM', 'WHERE', 'AND', 'OR', 'JOIN', 'LEFT JOIN', 'RIGHT JOIN',
    'INNER JOIN', 'OUTER JOIN', 'FULL JOIN', 'CROSS JOIN', 'ON', 'GROUP BY', 'ORDER BY',
    'HAVING', 'LIMIT', 'OFFSET', 'UNION', 'UNION ALL', 'INSERT INTO', 'VALUES', 'UPDATE',
    'SET', 'DELETE FROM', 'CREATE TABLE', 'ALTER TABLE', 'DROP TABLE', 'AS', 'IN', 'NOT IN',
    'BETWEEN', 'LIKE', 'IS NULL', 'IS NOT NULL', 'CASE', 'WHEN', 'THEN', 'ELSE', 'END',
    'DISTINCT', 'COUNT', 'SUM', 'AVG', 'MAX', 'MIN', 'COALESCE', 'CAST']
  const breakBefore = ['FROM', 'WHERE', 'JOIN', 'LEFT JOIN', 'RIGHT JOIN', 'INNER JOIN',
    'OUTER JOIN', 'FULL JOIN', 'CROSS JOIN', 'GROUP BY', 'ORDER BY', 'HAVING', 'LIMIT',
    'UNION', 'UNION ALL']

  let result = sql.trim().replace(/\s+/g, ' ')
  // Capitalize keywords
  keywords.forEach(kw => {
    const regex = new RegExp('\\b' + kw.replace(/ /g, '\\s+') + '\\b', 'gi')
    result = result.replace(regex, kw)
  })
  // Add line breaks before major clauses
  breakBefore.forEach(kw => {
    result = result.replace(new RegExp('\\s*\\b' + kw.replace(/ /g, '\\s+') + '\\b', 'gi'), '\n' + kw + ' ')
  })
  // Indent after first line
  const lines = result.split('\n')
  if (lines.length > 1) {
    result = lines[0].trim() + '\n' + lines.slice(1).map(l => '  ' + l.trim()).join('\n')
  }
  return result.trim()
}

// 字段映射操作
const handleAutoMapping = () => {
  fieldMappings.value = inputFields.value.map(f => ({
    sourceField: f.name,
    targetField: f.name,
    fieldType: f.type
  }))
  emitUpdate()
  ElMessage.success('Auto mapping completed')
}

const handleClearMapping = () => {
  fieldMappings.value = []
  emitUpdate()
  ElMessage.success('Mapping cleared')
}

const handleAddMappingRow = () => {
  fieldMappings.value.push({ sourceField: '', targetField: '', fieldType: '' })
  emitUpdate()
}

const handleRemoveMappingRow = (index) => {
  fieldMappings.value.splice(index, 1)
  emitUpdate()
}

// 数据预览
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewColumns = ref([])
const previewRows = ref([])
const previewTableName = ref('')
const previewError = ref('')

// 数据源操作
const handleTestConnection = async () => {
  if (!nodeData.value.config.datasourceId) {
    ElMessage.warning('请先选择数据连接')
    return
  }
  try {
    await testConnection(nodeData.value.config.datasourceId)
    ElMessage.success('Connection test successful')
  } catch (e) {
    ElMessage.error('Connection test failed')
  }
}

const handleDataPreview = async () => {
  if (!nodeData.value.config.datasourceId || !nodeData.value.config.tableName) {
    ElMessage.warning('请先选择数据连接和数据表')
    return
  }
  previewVisible.value = true
  previewLoading.value = true
  previewError.value = ''
  previewColumns.value = []
  previewRows.value = []
  previewTableName.value = nodeData.value.config.tableName
  try {
    const res = await getTableInfo(nodeData.value.config.datasourceId, nodeData.value.config.tableName)
    const data = res.data || {}
    if (data.columns && data.columns.length > 0) {
      previewColumns.value = data.columns.map(c => c.name || c.field || c)
    }
    if (data.rows && data.rows.length > 0) {
      previewRows.value = data.rows
    } else if (data.sampleData && data.sampleData.length > 0) {
      previewRows.value = data.sampleData
    }
    if (previewColumns.value.length === 0 && previewRows.value.length > 0) {
      previewColumns.value = Object.keys(previewRows.value[0])
    }
  } catch (e) {
    previewError.value = 'Failed to load preview data: ' + (e.message || 'Unknown error')
  } finally {
    previewLoading.value = false
  }
}

// 生成数据转换节点
const handleGenerateTransform = () => {
  emit('generate-transform', nodeData.value)
  ElMessage.success('Data Transformation node generated')
}

// 关闭面板
const handleCloseNode = () => {
  emit('close-node', nodeData.value.id)
}

const handleCloseEdge = () => {
  emit('close-edge', edgeData.value.id)
}

// 删除操作
const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定删除该节点？相关连线将一并删除。', '删除节点', { type: 'warning' })
    emit('delete-node', nodeData.value.id)
  } catch (_) { /* cancelled */ }
}

const handleDeleteEdge = async () => {
  try {
    await ElMessageBox.confirm('确定删除该连线？', '删除连线', { type: 'warning' })
    emit('delete-edge', edgeData.value.id)
  } catch (_) { /* cancelled */ }
}
</script>

<style lang="scss" scoped>
.node-property-panel {
  height: 100%;
  overflow-y: auto;

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 48px 0;
    .empty-icon { color: #ddd; margin-bottom: 12px; }
    .empty-text { color: #999; font-size: 13px; }
  }
}

.properties-wrap {
  display: flex;
  flex-direction: column;
  height: 100%;
}

// ====== 顶部面板头 ======
.panel-header {
  padding: 12px 16px;
  border-bottom: 1px solid #e8e8e8;
  background: #fff;

  .header-title-row {
    display: flex;
    align-items: center;
    gap: 8px;

    .node-name-input {
      flex: 1;
      :deep(.el-input__inner) {
        font-size: 15px;
        font-weight: 600;
        border: 1px solid transparent;
        background: transparent;
        padding-left: 0;
        &:focus {
          border-color: #1890ff;
          background: #fff;
        }
        &:hover {
          border-color: #d9d9d9;
        }
      }
    }

    .close-btn {
      flex-shrink: 0;
      color: #999;
      &:hover { color: #333; }
    }

    .edge-title {
      font-size: 15px;
      font-weight: 600;
      color: #333;
      flex: 1;
    }
  }

  .generate-btn {
    width: 100%;
    margin-top: 10px;
  }
}

// ====== 标签页 ======
.config-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  :deep(.el-tabs__header) {
    margin: 0;
    padding: 0 12px;
    background: #fafafa;
    border-bottom: 1px solid #e8e8e8;
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }

  :deep(.el-tabs__item) {
    font-size: 12px;
    padding: 0 12px;
    height: 36px;
    line-height: 36px;
  }

  :deep(.el-tabs__content) {
    flex: 1;
    overflow-y: auto;
    padding: 0;
  }
}

.tab-content {
  padding: 16px;
}

// ====== 操作按钮行 ======
.action-btn-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

// ====== SQL编辑器 ======
.sql-editor-wrap {
  width: 100%;
  .sql-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 4px 8px;
    background: #f0f0f0;
    border-radius: 4px 4px 0 0;
    border: 1px solid #e0e0e0;
    border-bottom: none;
    .sql-lang-label { font-size: 11px; color: #999; font-weight: 600; }
  }
  .sql-textarea {
    :deep(.el-textarea__inner) {
      border-radius: 0 0 4px 4px;
      font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
      font-size: 12px;
      line-height: 1.6;
    }
  }
}

// ====== 字段映射 ======
.mapping-section {
  margin-top: 16px;

  .mapping-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;

    .mapping-label {
      font-size: 13px;
      font-weight: 600;
      color: #333;
    }

    .mapping-actions {
      display: flex;
      gap: 4px;
    }
  }

  .mapping-table {
    width: 100%;

    :deep(.el-table__body-wrapper) {
      min-height: 60px;
    }

    :deep(.el-table th) {
      background: #fafafa;
      font-size: 12px;
      padding: 6px 0;
    }

    :deep(.el-table td) {
      padding: 4px 0;
    }
  }
}

// ====== 高级设置 ======
.advanced-section {
  margin-top: 16px;
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;

  .advanced-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    cursor: pointer;
    user-select: none;
    padding: 4px 0;

    .advanced-label {
      font-size: 13px;
      font-weight: 600;
      color: #333;
    }

    &:hover .advanced-label {
      color: #1890ff;
    }
  }

  .advanced-form {
    margin-top: 8px;
  }
}

// ====== 信息提示 ======
.info-tip {
  padding: 10px 14px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 6px;
  font-size: 12px;
  color: #ad8b00;
  line-height: 1.5;
}

// ====== 表单样式覆盖 ======
.prop-section {
  background: #fafafa;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid #f0f0f0;
  margin: 0 16px;
}

:deep(.el-form-item) {
  margin-bottom: 14px;
}
:deep(.el-form-item:last-child) {
  margin-bottom: 0;
}
:deep(.el-form-item__label) {
  font-size: 12px;
  color: #888;
  font-weight: 500;
}

// ====== 底部操作 ======
.prop-actions {
  padding: 12px 16px;
  border-top: 1px solid #e8e8e8;
  background: #fff;
}

:deep(.el-radio-button__inner) {
  padding: 5px 12px;
}

// ====== 数据预览对话框 ======
.preview-container {
  min-height: 200px;
  .preview-error {
    text-align: center;
    padding: 40px 0;
    color: #f56c6c;
    p { margin-top: 8px; font-size: 13px; }
  }
  .preview-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    font-size: 13px;
    color: #606266;
  }
  .preview-table {
    border-radius: 8px;
    :deep(.el-table__header th) {
      background: #f5f7fa;
      font-weight: 600;
    }
  }
}
</style>
