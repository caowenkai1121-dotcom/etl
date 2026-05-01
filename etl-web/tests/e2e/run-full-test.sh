#!/bin/bash
BASE="http://localhost:3000"
PASS=0; FAIL=0; TOTAL=0

check() {
  TOTAL=$((TOTAL+1))
  local name="$1" url="$2" key="$3"
  local body=$(curl -s "$url")
  local code=$(echo "$body" | grep -o '"code":[0-9]*' | head -1 | cut -d: -f2)
  local ok=0
  if [ "$code" = "200" ]; then ok=1; fi
  if [ -n "$key" ]; then
    local has=$(echo "$body" | grep -c "\"$key\"")
    if [ "$has" -eq 0 ]; then ok=0; fi
  fi
  if [ "$ok" = "1" ]; then
    PASS=$((PASS+1)); echo "  PASS $name"
  else
    FAIL=$((FAIL+1)); echo "  FAIL $name (code=$code)"
  fi
}

echo "=========================================================="
echo "       ETL Sync System - Full Integration Test Report"
echo "=========================================================="

echo ""
echo "--- Part 1: Backend API Endpoints (30) ---"
check "[Monitor] Overview" "$BASE/api/monitor/overview" "todaySuccess"
check "[Monitor] Trend" "$BASE/api/monitor/trend?days=7" ""
check "[Monitor] ExecPage" "$BASE/api/monitor/execution/page?pageNum=1&pageSize=10" "list"
check "[Monitor] ThreadPool" "$BASE/api/monitor/thread-pool-status" "corePoolSize"
check "[Monitor] Cache" "$BASE/api/monitor/cache-status" "tableInfoCache"
check "[Monitor] SysInfo" "$BASE/api/monitor/system-info" "jvmMaxMemory"
check "[DS] Page" "$BASE/api/datasource/page?pageNum=1&pageSize=10" "list"
check "[DS] Types" "$BASE/api/datasource/types" "MYSQL"
check "[DS] List" "$BASE/api/datasource/list" ""
check "[Task] Page" "$BASE/api/task/page?pageNum=1&pageSize=10" "list"
check "[Task] SyncModes" "$BASE/api/task/sync-modes" ""
check "[CDC] List" "$BASE/api/cdc-config/list" ""
check "[CDC] Page" "$BASE/api/cdc-config/page?pageNum=1&pageSize=10" "list"
check "[Transform] Pipeline" "$BASE/api/transform/pipeline?pageNum=1&pageSize=10" "list"
check "[Transform] RuleTypes" "$BASE/api/transform/pipeline/rules" ""
check "[Transform] Rules" "$BASE/api/transform/rules" ""
check "[Scheduler] Tasks" "$BASE/api/scheduler/tasks" ""
check "[Scheduler] DAGs" "$BASE/api/scheduler/dag" ""
check "[Scheduler] DAGNodes" "$BASE/api/scheduler/dag/1/nodes" ""
check "[Alert] RulePage" "$BASE/api/alert/rule/page?pageNum=1&pageSize=10" "list"
check "[Alert] Channels" "$BASE/api/alert/channels" ""
check "[Alert] ChannelList" "$BASE/api/alert/channel/list" ""
check "[Alert] Records" "$BASE/api/alert/record/page?pageNum=1&pageSize=10" "list"
check "[Quality] RulePage" "$BASE/api/quality/rule/page?pageNum=1&pageSize=10" "list"
check "[Log] Page" "$BASE/api/log/page?pageNum=1&pageSize=10" "list"
check "[Log] Stats" "$BASE/api/log/stats/overview" "totalLogs"
check "[Log] ByStage" "$BASE/api/log/stats/by-stage" ""
check "[Log] ErrorTrend" "$BASE/api/log/stats/error-trend?days=7" ""
check "[Health] Detail" "$BASE/api/health/detail" "database"
check "[Config] List" "$BASE/api/config/list" "list"
P1=$PASS; F1=$FAIL; PASS=0; FAIL=0

echo ""
echo "--- Part 2: Frontend Page Routes (17) ---"
TOTAL2=0; PASS2=0
for r in /dashboard /datasource /task /cdc-config /scheduler /scheduler/dag /execution /etl/pipeline /etl/rules /etl/debug /monitor /log /alert /quality /config /health /log/transform; do
  TOTAL2=$((TOTAL2+1))
  c=$(curl -s -o /dev/null -w "%{http_code}" "$BASE$r")
  if [ "$c" = "200" ]; then PASS2=$((PASS2+1)); echo "  PASS $r"; else echo "  FAIL $r (HTTP $c)"; fi
done

echo ""
echo "--- Part 3: Nginx API Proxy (17) ---"
PASS=0; FAIL=0
check "Dashboard->Overview" "$BASE/api/monitor/overview" "todaySuccess"
check "Dashboard->Trend" "$BASE/api/monitor/trend?days=7" ""
check "Dashboard->Exec" "$BASE/api/monitor/execution/page?pageNum=1&pageSize=5" "list"
check "DS->Page" "$BASE/api/datasource/page?pageNum=1&pageSize=10" "list"
check "DS->Types" "$BASE/api/datasource/types" "MYSQL"
check "Task->Page" "$BASE/api/task/page?pageNum=1&pageSize=10" "list"
check "Task->Modes" "$BASE/api/task/sync-modes" ""
check "CDC->List" "$BASE/api/cdc-config/list" ""
check "Transform->Pipeline" "$BASE/api/transform/pipeline?pageNum=1&pageSize=10" "list"
check "Transform->Rules" "$BASE/api/transform/pipeline/rules" ""
check "Scheduler->Tasks" "$BASE/api/scheduler/tasks" ""
check "Scheduler->DAGs" "$BASE/api/scheduler/dag" ""
check "Alert->Rules" "$BASE/api/alert/rule/page?pageNum=1&pageSize=10" "list"
check "Alert->Channels" "$BASE/api/alert/channels" ""
check "Quality->Rules" "$BASE/api/quality/rule/page?pageNum=1&pageSize=10" "list"
check "Log->Page" "$BASE/api/log/page?pageNum=1&pageSize=10" "list"
check "Health->Detail" "$BASE/api/health/detail" "database"
P3=$PASS; F3=$FAIL; PASS=0; FAIL=0

echo ""
echo "--- Part 4: Data Integrity (5) ---"
PASS=0; FAIL=0
# DS types complete (use grep -o to count each match)
tc=$(curl -s "$BASE/api/datasource/types" | grep -o "MYSQL\|POSTGRESQL\|DORIS\|MONGODB\|REDIS" | wc -l)
TOTAL=$((TOTAL+1))
if [ "$tc" -ge 5 ]; then PASS=$((PASS+1)); echo "  PASS DS Types ($tc types)"; else FAIL=$((FAIL+1)); echo "  FAIL DS Types ($tc)"; fi

# Transform rules complete
rc=$(curl -s "$BASE/api/transform/pipeline/rules" | grep -o "ENCRYPT\|DESENSITIZE\|JSON_PARSE" | wc -l)
TOTAL=$((TOTAL+1))
if [ "$rc" -ge 3 ]; then PASS=$((PASS+1)); echo "  PASS Transform Rules ($rc types)"; else FAIL=$((FAIL+1)); echo "  FAIL Transform Rules ($rc)"; fi

# Alert channels complete
ac=$(curl -s "$BASE/api/alert/channels" | grep -o "DINGTALK\|EMAIL\|WEBHOOK" | wc -l)
TOTAL=$((TOTAL+1))
if [ "$ac" -ge 3 ]; then PASS=$((PASS+1)); echo "  PASS Alert Channels ($ac channels)"; else FAIL=$((FAIL+1)); echo "  FAIL Alert Channels ($ac)"; fi

# Health data complete
hc=$(curl -s "$BASE/api/health/detail" | grep -o '"database"\|"jvm"\|"connectionPools"' | wc -l)
TOTAL=$((TOTAL+1))
if [ "$hc" -ge 3 ]; then PASS=$((PASS+1)); echo "  PASS Health Data ($hc fields)"; else FAIL=$((FAIL+1)); echo "  FAIL Health Data ($hc)"; fi

# DS entity fields
dc=$(curl -s "$BASE/api/datasource/page?pageNum=1&pageSize=10" | grep -o '"name"\|"type"\|"host"\|"port"' | wc -l)
TOTAL=$((TOTAL+1))
if [ "$dc" -ge 4 ]; then PASS=$((PASS+1)); echo "  PASS DS Entity Fields ($dc fields)"; else FAIL=$((FAIL+1)); echo "  FAIL DS Entity Fields ($dc)"; fi
P4=$PASS; F4=$FAIL; PASS=0; FAIL=0

echo ""
echo "--- Part 5: CRUD Operations (3) ---"
# Create DS
cb=$(curl -s -X POST "$BASE/api/datasource" -H "Content-Type: application/json" -d "{\"name\":\"E2E-Auto-Test\",\"type\":\"MYSQL\",\"host\":\"10.0.0.1\",\"port\":3306,\"databaseName\":\"e2e_test\",\"username\":\"root\",\"password\":\"test123\"}")
cc=$(echo "$cb" | grep -o '"code":[0-9]*' | head -1 | cut -d: -f2)
TOTAL=$((TOTAL+1))
if [ "$cc" = "200" ]; then
  PASS=$((PASS+1)); echo "  PASS Create Datasource"
  nid=$(echo "$cb" | grep -o '"data":[0-9]*' | head -1 | cut -d: -f2)
  # Read DS
  TOTAL=$((TOTAL+1))
  gb=$(curl -s "$BASE/api/datasource/$nid")
  gc=$(echo "$gb" | grep -o '"code":[0-9]*' | head -1 | cut -d: -f2)
  if [ "$gc" = "200" ]; then PASS=$((PASS+1)); echo "  PASS Read Datasource"; else FAIL=$((FAIL+1)); echo "  FAIL Read Datasource"; fi
  # Delete DS
  TOTAL=$((TOTAL+1))
  db=$(curl -s -X DELETE "$BASE/api/datasource/$nid")
  dc2=$(echo "$db" | grep -o '"code":[0-9]*' | head -1 | cut -d: -f2)
  if [ "$dc2" = "200" ]; then PASS=$((PASS+1)); echo "  PASS Delete Datasource"; else FAIL=$((FAIL+1)); echo "  FAIL Delete Datasource"; fi
else
  FAIL=$((FAIL+1)); echo "  FAIL Create Datasource (code=$cc)"
  FAIL=$((FAIL+1)); echo "  SKIP Read (no ID)"
  FAIL=$((FAIL+1)); echo "  SKIP Delete (no ID)"
fi
P5=$PASS; F5=$FAIL

echo ""
echo "=========================================================="
echo "                    FINAL TEST REPORT"
echo "=========================================================="
echo ""
echo "  Backend API Endpoints:  $P1 / 30"
echo "  Frontend Page Routes:   $PASS2 / 17"
echo "  Nginx API Proxy:        $P3 / 17"
echo "  Data Integrity:          $P4 / 5"
echo "  CRUD Operations:         $P5 / 3"
echo "  ---------------------------------------------------"
TP=$((P1 + PASS2 + P3 + P4 + P5))
TA=$((30 + 17 + 17 + 5 + 3))
echo "  TOTAL:                  $TP / $TA"
echo ""
if [ "$TP" -eq "$TA" ]; then
  echo "  *** ALL TESTS PASSED ***"
else
  echo "  Warning: $((TA - TP)) test(s) failed"
fi
echo "=========================================================="
