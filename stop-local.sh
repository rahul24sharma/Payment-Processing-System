#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_DIR="$ROOT_DIR/.local-run/pids"
SERVICE_PORTS=(8761 8086 8082 8081 8083 8084 8085 8080 5173)

if [[ ! -d "$PID_DIR" ]]; then
  echo "No PID directory found ($PID_DIR). Nothing to stop."
  mkdir -p "$PID_DIR"
fi

stopped_any=0

get_port_pid() {
  local port="$1"
  lsof -nP -t -iTCP:"$port" -sTCP:LISTEN 2>/dev/null | head -n 1
}

for pid_file in "$PID_DIR"/*.pid; do
  [[ -e "$pid_file" ]] || continue

  name="$(basename "$pid_file" .pid)"
  pid="$(cat "$pid_file" 2>/dev/null || true)"

  if [[ -z "${pid:-}" ]]; then
    rm -f "$pid_file"
    continue
  fi

  if kill -0 "$pid" >/dev/null 2>&1; then
    echo "Stopping $name (pid $pid)..."
    kill "$pid" >/dev/null 2>&1 || true
    stopped_any=1
  else
    echo "Removing stale PID for $name (pid $pid)"
  fi

  rm -f "$pid_file"
done

for port in "${SERVICE_PORTS[@]}"; do
  pid="$(get_port_pid "$port" || true)"
  if [[ -n "${pid:-}" ]]; then
    echo "Stopping process on port $port (pid $pid)..."
    kill "$pid" >/dev/null 2>&1 || true
    stopped_any=1
  fi
done

if [[ "$stopped_any" -eq 0 ]]; then
  echo "No running processes found from local launcher."
else
  echo "Stop signal sent."
fi
