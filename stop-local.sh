#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_DIR="$ROOT_DIR/.local-run/pids"

if [[ ! -d "$PID_DIR" ]]; then
  echo "No PID directory found ($PID_DIR). Nothing to stop."
  exit 0
fi

stopped_any=0

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

if [[ "$stopped_any" -eq 0 ]]; then
  echo "No running processes found from local launcher."
else
  echo "Stop signal sent."
fi
