<!-- code-review-graph MCP tools -->
## MCP Tools: code-review-graph

**IMPORTANT: This project has a knowledge graph. ALWAYS use the
code-review-graph MCP tools BEFORE using Grep/Glob/Read to explore
the codebase.** The graph is faster, cheaper (fewer tokens), and gives
you structural context (callers, dependents, test coverage) that file
scanning cannot.

### When to use graph tools FIRST

- **Exploring code**: `semantic_search_nodes` or `query_graph` instead of Grep
- **Understanding impact**: `get_impact_radius` instead of manually tracing imports
- **Code review**: `detect_changes` + `get_review_context` instead of reading entire files
- **Finding relationships**: `query_graph` with callers_of/callees_of/imports_of/tests_for
- **Architecture questions**: `get_architecture_overview` + `list_communities`

Fall back to Grep/Glob/Read **only** when the graph doesn't cover what you need.

### Key Tools

| Tool | Use when |
|------|----------|
| `detect_changes` | Reviewing code changes — gives risk-scored analysis |
| `get_review_context` | Need source snippets for review — token-efficient |
| `get_impact_radius` | Understanding blast radius of a change |
| `get_affected_flows` | Finding which execution paths are impacted |
| `query_graph` | Tracing callers, callees, imports, tests, dependencies |
| `semantic_search_nodes` | Finding functions/classes by name or keyword |
| `get_architecture_overview` | Understanding high-level codebase structure |
| `refactor_tool` | Planning renames, finding dead code |

### Workflow

1. The graph auto-updates on file changes (via hooks).
2. Use `detect_changes` for code review.
3. Use `get_affected_flows` to understand impact.
4. Use `query_graph` pattern="tests_for" to check coverage.

## Self-Improvement 持续改进

项目启用了 self-improvement skill，自动记录错误、纠正和学习经验。

### .learnings/ 目录

| 文件 | 用途 |
|------|------|
| `.learnings/LEARNINGS.md` | 纠正、知识空白、最佳实践 |
| `.learnings/ERRORS.md` | 命令失败、异常错误 |
| `.learnings/FEATURE_REQUESTS.md` | 用户要求的功能 |

### 何时记录

- 命令/操作失败 → ERRORS.md
- 用户纠正（"不对，应该是..."） → LEARNINGS.md（correction）
- 学到新模式/最佳实践 → LEARNINGS.md（best_practice）
- 用户要求缺失功能 → FEATURE_REQUESTS.md
- 发现知识空白 → LEARNINGS.md（knowledge_gap）

### 升级规则

- 广泛适用的经验 → 升级到本 CLAUDE.md
- 用户偏好/项目背景 → 升级到 `~/.claude/projects/D--data-zg-etl-etl-sync-system/memory/`
- 重复出现3次以上 → 自动提醒升级

### 与 memory 系统的关系

self-improvement 的 `.learnings/` 记录**具体事件**（错误、纠正、需求），
claude-mem 的 `memory/` 记录**人物和上下文**（用户画像、偏好、项目背景）。
两者互补，各有分工。
