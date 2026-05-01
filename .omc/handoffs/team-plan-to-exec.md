## Handoff: team-plan → team-exec
- **Decided**: 5个主要升级任务，3个worker并行执行；worker-1负责前端核心（导航+数据开发+DAG），worker-2负责后端API和引擎，worker-3负责新增页面
- **Rejected**: 单体全部重建方案（过于复杂），采用增量升级策略，在现有代码基础上增强匹配FineDataLink
- **Risks**: FineDataLink浏览器交互困难（iframe超时），无法完整捕获所有细节；需要通过API和截图补充分析
- **Files**: 所有修改的文件集中在 etl-web/src/views/、etl-api/、etl-engine/、etl-scheduler/、docker/mysql/init/
- **Remaining**: 完成后端API升级后需要重新构建Docker镜像；前端改动后用vite build验证
