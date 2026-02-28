#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT_DIR/.local-run"
PID_DIR="$RUN_DIR/pids"
LOG_DIR="$RUN_DIR/logs"

mkdir -p "$PID_DIR" "$LOG_DIR"

# Load local environment variables (e.g. JWT_SECRET, DB passwords) if present.
if [[ -f "$ROOT_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT_DIR/.env"
  set +a
fi

SERVICES=(
  "eureka-server|8761|/actuator/health|env SPRING_DEVTOOLS_RESTART_ENABLED=false ./mvnw spring-boot:run"
  "merchant-service|8086|/actuator/health|env SPRING_DEVTOOLS_RESTART_ENABLED=false ./mvnw spring-boot:run -Dspring-boot.run.mainClass=com.payment.merchant.MerchantServiceApplication"
  "fraud-service|8082|/actuator/health|env SPRING_DEVTOOLS_RESTART_ENABLED=false ./mvnw spring-boot:run -Dspring-boot.run.mainClass=com.payment.fraud.FraudServiceApplication"
  "payment-service|8081|/actuator/health|env SPRING_DEVTOOLS_RESTART_ENABLED=false PAYMENT_KAFKA_ENABLED=${PAYMENT_KAFKA_ENABLED:-true} KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092} ./mvnw spring-boot:run"
  "ledger-service|8083|/actuator/health|env SPRING_DEVTOOLS_RESTART_ENABLED=false KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092} ./mvnw spring-boot:run -Dspring-boot.run.mainClass=com.payment.ledger.LedgerServiceApplication"
  "settlement-service|8084|/actuator/health|env SPRING_DEVTOOLS_RESTART_ENABLED=false KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092} ./mvnw spring-boot:run -Dspring-boot.run.mainClass=com.payment.settlement.SettlementServiceApplication"
  "notification-service|8085|/actuator/health|env SPRING_DEVTOOLS_RESTART_ENABLED=false KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092} ./mvnw spring-boot:run -Dspring-boot.run.mainClass=com.payment.notification.NotificationServiceApplication"
  "api-gateway|8080|/actuator/health|env SPRING_DEVTOOLS_RESTART_ENABLED=false ./mvnw spring-boot:run"
)

KAFKA_DEPENDENT_SERVICES=("ledger-service" "settlement-service" "notification-service")

FRONTEND_NAME="merchant-dashboard"
FRONTEND_PORT="5173"
FRONTEND_CMD="npm run dev"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1"
    exit 1
  fi
}

get_port_pid() {
  local port="$1"
  lsof -nP -t -iTCP:"$port" -sTCP:LISTEN 2>/dev/null | head -n 1
}

is_pid_running() {
  local pid="$1"
  kill -0 "$pid" >/dev/null 2>&1
}

is_port_busy() {
  local port="$1"
  lsof -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1
}

wait_for_port() {
  local port="$1"
  local timeout="${2:-120}"
  local waited=0

  while (( waited < timeout )); do
    if is_port_busy "$port"; then
      return 0
    fi
    sleep 2
    waited=$(( waited + 2 ))
  done

  return 1
}

wait_for_dependency_port() {
  local port="$1"
  local name="$2"
  local timeout="${3:-40}"
  local waited=0

  while (( waited < timeout )); do
    if is_port_busy "$port"; then
      return 0
    fi
    sleep 2
    waited=$(( waited + 2 ))
  done

  echo "Dependency not ready: $name on port $port"
  return 1
}

wait_for_kafka_dependency() {
  local timeout="${1:-40}"
  local waited=0

  while (( waited < timeout )); do
    if is_port_busy 29092 || is_port_busy 9092; then
      return 0
    fi
    sleep 2
    waited=$(( waited + 2 ))
  done

  echo "Dependency not ready: Kafka on port 9092 or 29092"
  return 1
}

wait_for_http_health() {
  local port="$1"
  local health_path="$2"
  local timeout="${3:-90}"
  local waited=0
  local url="http://localhost:${port}${health_path}"

  while (( waited < timeout )); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
    waited=$(( waited + 2 ))
  done

  return 1
}

cleanup_stale_compiled_classes() {
  local service_dir="$1"
  local stale_dir="$ROOT_DIR/$service_dir/target/classes/com/payment 2"

  if [[ -d "$stale_dir" ]]; then
    echo "Cleaning stale compiled classes for $service_dir (target/classes/com/payment 2)"
    rm -rf "$stale_dir"
  fi
}

start_process() {
  local name="$1"
  local port="$2"
  local workdir="$3"
  local cmd="$4"
  local health_path="${5:-}"
  local pid_file="$PID_DIR/$name.pid"
  local log_file="$LOG_DIR/$name.log"
  local wrapper_pid=""

  if [[ -f "$pid_file" ]]; then
    local existing_pid
    existing_pid="$(cat "$pid_file" 2>/dev/null || true)"
    if [[ -n "$existing_pid" ]] && is_pid_running "$existing_pid"; then
      if is_port_busy "$port"; then
        echo "Skipping $name (already running, pid $existing_pid)"
        return 0
      fi
      echo "Cleaning stale $name process record (pid $existing_pid not serving port $port)"
      kill "$existing_pid" >/dev/null 2>&1 || true
    fi
    rm -f "$pid_file"
  fi

  if is_port_busy "$port"; then
    local listener_pid
    listener_pid="$(get_port_pid "$port" || true)"
    if [[ -n "$listener_pid" ]]; then
      echo "$listener_pid" >"$pid_file"
      echo "Port $port already in use for $name (pid $listener_pid)"
      if [[ -n "$health_path" ]] && ! wait_for_http_health "$port" "$health_path" 8; then
        echo "  error: $name health endpoint is not ready while port is occupied"
        return 1
      fi
      echo "  using existing healthy process"
    else
      echo "Port $port already in use for $name"
      return 1
    fi
    return 0
  fi

  echo "Starting $name on port $port..."
  (
    cd "$ROOT_DIR/$workdir"
    nohup bash -lc "exec $cmd" >"$log_file" 2>&1 &
    echo $! >"$pid_file"
  )

  wrapper_pid="$(cat "$pid_file")"
  if ! is_pid_running "$wrapper_pid"; then
    echo "Failed to start $name (wrapper exited immediately). Check $log_file"
    return 1
  fi

  if wait_for_port "$port" 180; then
    local listener_pid
    listener_pid="$(get_port_pid "$port" || true)"
    if [[ -n "$listener_pid" ]]; then
      echo "$listener_pid" >"$pid_file"
      echo "  pid=$listener_pid (listener) log=$log_file"
    else
      echo "  pid=$wrapper_pid (wrapper) log=$log_file"
    fi

    if [[ -n "$health_path" ]]; then
      if wait_for_http_health "$port" "$health_path" 120; then
        echo "  health=ok ($health_path)"
      else
        echo "  warning: health endpoint did not become ready for $name"
      fi
    fi
  else
    # Some failures happen after a long Spring startup; preserve log path for debugging.
    echo "  warning: $name did not open port $port within timeout. Check $log_file"
    echo "  pid=$wrapper_pid log=$log_file"
  fi
  return 0
}

print_preflight() {
  echo "Local app launcher (no Docker for services/frontend)"
  echo
  echo "Expected local infrastructure before starting:"
  echo "  - PostgreSQL on 5432"
  echo "  - Redis on 6379"
  echo "  - Kafka on 9092 or 29092 (depends on your local setup)"
  echo

  if ! is_port_busy 5432; then
    echo "Warning: PostgreSQL (5432) not detected"
  fi
  if ! is_port_busy 6379; then
    echo "Warning: Redis (6379) not detected"
  fi
  if ! is_port_busy 9092 && ! is_port_busy 29092; then
    echo "Warning: Kafka (9092/29092) not detected"
  fi
  echo
}

verify_dependencies_or_exit() {
  local dep_failures=0
  local skip_kafka_check="${SKIP_KAFKA_CHECK:-false}"

  echo "Verifying infrastructure dependencies..."
  wait_for_dependency_port 5432 "PostgreSQL" 30 || dep_failures=$((dep_failures + 1))
  wait_for_dependency_port 6379 "Redis" 30 || dep_failures=$((dep_failures + 1))
  if [[ "$skip_kafka_check" == "true" ]]; then
    echo "Skipping Kafka dependency check (SKIP_KAFKA_CHECK=true)"
  else
    wait_for_kafka_dependency 30 || dep_failures=$((dep_failures + 1))
  fi

  if (( dep_failures > 0 )); then
    echo
    echo "Dependency checks failed. Start infra first, then rerun ./start-local.sh"
    exit 1
  fi
  echo "All infrastructure dependencies are reachable."
  echo
}

should_skip_service() {
  local service_name="$1"
  local skip_kafka_check="${SKIP_KAFKA_CHECK:-false}"
  local start_kafka_services_without_broker="${START_KAFKA_SERVICES_WITHOUT_BROKER:-false}"
  if [[ "$skip_kafka_check" != "true" ]]; then
    return 1
  fi
  if [[ "$start_kafka_services_without_broker" == "true" ]]; then
    return 1
  fi

  for kafka_service in "${KAFKA_DEPENDENT_SERVICES[@]}"; do
    if [[ "$service_name" == "$kafka_service" ]]; then
      return 0
    fi
  done
  return 1
}

verify_security_env_or_exit() {
  local jwt_secret="${JWT_SECRET:-}"

  if [[ -z "$jwt_secret" ]]; then
    echo "Missing JWT_SECRET in environment/.env"
    echo "Generate one with: openssl rand -base64 32"
    exit 1
  fi

  if [[ "${#jwt_secret}" -lt 32 ]]; then
    echo "JWT_SECRET is too short (${#jwt_secret} chars). Use at least 32 characters."
    echo "Generate one with: openssl rand -base64 32"
    exit 1
  fi
}

main() {
  require_cmd bash
  require_cmd lsof
  require_cmd curl
  require_cmd node
  require_cmd npm
  require_cmd java

  # Vite 7 in merchant-dashboard requires Node 20.19+ or 22.12+.
  local node_major
  node_major="$(node -v | sed -E 's/^v([0-9]+).*/\1/')"
  if [[ "$node_major" -lt 20 ]]; then
    echo "Node.js $(node -v) detected."
    echo "merchant-dashboard uses Vite and needs Node 20.19+ (or 22.12+)."
    echo "Upgrade Node, then rerun ./start-local.sh"
    exit 1
  fi

  print_preflight
  verify_dependencies_or_exit
  verify_security_env_or_exit

  # Guard against stale malformed package outputs from previous local runs.
  cleanup_stale_compiled_classes "notification-service"
  cleanup_stale_compiled_classes "settlement-service"

  if [[ "${SKIP_KAFKA_CHECK:-false}" == "true" ]] && [[ -z "${PAYMENT_KAFKA_ENABLED:-}" ]]; then
    export PAYMENT_KAFKA_ENABLED=false
    export LEDGER_KAFKA_ENABLED=false
    export NOTIFICATION_KAFKA_ENABLED=false
    export START_KAFKA_SERVICES_WITHOUT_BROKER=true
    echo "Kafka-light mode enabled: PAYMENT_KAFKA_ENABLED=false"
    echo "Kafka consumers disabled for local mode: LEDGER_KAFKA_ENABLED=false, NOTIFICATION_KAFKA_ENABLED=false"
    echo "All services will still start (Kafka-dependent consumers are disabled)."
  fi

  local failed_services=()

  echo "Starting backend services..."
  for entry in "${SERVICES[@]}"; do
    IFS="|" read -r name port health_path cmd <<< "$entry"
    if should_skip_service "$name"; then
      echo "Skipping $name (requires Kafka; SKIP_KAFKA_CHECK=true)"
      continue
    fi
    if ! start_process "$name" "$port" "$name" "$cmd" "$health_path"; then
      failed_services+=("$name")
    fi
    sleep 2
  done

  echo
  echo "Starting frontend ($FRONTEND_NAME)..."
  if ! start_process "$FRONTEND_NAME" "$FRONTEND_PORT" "$FRONTEND_NAME" "$FRONTEND_CMD"; then
    failed_services+=("$FRONTEND_NAME")
  fi

  echo
  echo "All start commands issued."
  echo "Logs: $LOG_DIR"
  echo "Stop everything: ./stop-local.sh"
  if (( ${#failed_services[@]} > 0 )); then
    echo
    echo "Startup warnings (check logs): ${failed_services[*]}"
  fi
  echo
  echo "Common URLs:"
  echo "  API Gateway:      http://localhost:8080"
  echo "  Eureka Dashboard: http://localhost:8761"
  echo "  Frontend (Vite):  http://localhost:5173"
}

main "$@"
