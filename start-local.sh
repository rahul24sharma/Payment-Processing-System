#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT_DIR/.local-run"
PID_DIR="$RUN_DIR/pids"
LOG_DIR="$RUN_DIR/logs"

mkdir -p "$PID_DIR" "$LOG_DIR"

SERVICES=(
  "eureka-server|8761|env SPRING_DEVTOOLS_RESTART_ENABLED=false ./mvnw spring-boot:run"
  "merchant-service|8086|env SPRING_DEVTOOLS_RESTART_ENABLED=false ./mvnw spring-boot:run"
  "fraud-service|8082|env SPRING_DEVTOOLS_RESTART_ENABLED=false ./mvnw spring-boot:run"
  "payment-service|8081|env SPRING_DEVTOOLS_RESTART_ENABLED=false PAYMENT_KAFKA_ENABLED=true KAFKA_BOOTSTRAP_SERVERS=localhost:29092 ./mvnw spring-boot:run"
  "ledger-service|8083|env SPRING_DEVTOOLS_RESTART_ENABLED=false KAFKA_BOOTSTRAP_SERVERS=localhost:29092 ./mvnw spring-boot:run"
  "settlement-service|8084|env SPRING_DEVTOOLS_RESTART_ENABLED=false KAFKA_BOOTSTRAP_SERVERS=localhost:29092 ./mvnw spring-boot:run"
  "notification-service|8085|env SPRING_DEVTOOLS_RESTART_ENABLED=false KAFKA_BOOTSTRAP_SERVERS=localhost:29092 ./mvnw spring-boot:run"
  "api-gateway|8080|env SPRING_DEVTOOLS_RESTART_ENABLED=false ./mvnw spring-boot:run"
)

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

start_process() {
  local name="$1"
  local port="$2"
  local workdir="$3"
  local cmd="$4"
  local pid_file="$PID_DIR/$name.pid"
  local log_file="$LOG_DIR/$name.log"
  local wrapper_pid=""

  if [[ -f "$pid_file" ]]; then
    local existing_pid
    existing_pid="$(cat "$pid_file" 2>/dev/null || true)"
    if [[ -n "$existing_pid" ]] && is_pid_running "$existing_pid"; then
      echo "Skipping $name (already running, pid $existing_pid)"
      return 0
    fi
    rm -f "$pid_file"
  fi

  if is_port_busy "$port"; then
    echo "Skipping $name (port $port already in use)"
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

main() {
  require_cmd bash
  require_cmd lsof
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

  local failed_services=()

  echo "Starting backend services..."
  for entry in "${SERVICES[@]}"; do
    IFS="|" read -r name port cmd <<< "$entry"
    if ! start_process "$name" "$port" "$name" "$cmd"; then
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
