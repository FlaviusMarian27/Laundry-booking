#!/usr/bin/env bash
# Stops everything started by scripts/start.sh.
#   ./scripts/stop.sh            stop web-client, core-service and the Docker services
#   ./scripts/stop.sh --keep-docker   stop only web-client and core-service
# PostgreSQL data is kept (Docker volume). Kafka messages are lost on 'docker compose down'.
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="$ROOT/.run"
cd "$ROOT"

info() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
ok()   { printf '\033[1;32m OK\033[0m %s\n' "$*"; }

stop_bg() {
    local name="$1" pid_file="$RUN_DIR/$1.pid"
    if [[ ! -f "$pid_file" ]]; then
        ok "$name was not started by start.sh"
        return
    fi
    local pid
    pid="$(cat "$pid_file")"
    if kill -0 "$pid" 2>/dev/null; then
        info "Stopping $name (pid $pid)"
        # Stop the whole process group (Maven + forked JVM, npm + Vite); fall back to the single pid
        kill -TERM -- "-$pid" 2>/dev/null || kill -TERM "$pid" 2>/dev/null
        for _ in {1..15}; do
            kill -0 "$pid" 2>/dev/null || break
            sleep 1
        done
        if kill -0 "$pid" 2>/dev/null; then
            kill -KILL -- "-$pid" 2>/dev/null || kill -KILL "$pid" 2>/dev/null
        fi
    fi
    rm -f "$pid_file"
    ok "$name stopped"
}

stop_bg web-client
stop_bg core-service

if [[ "${1:-}" != "--keep-docker" ]]; then
    info "Stopping Docker services"
    docker compose down
    ok "Docker services stopped"
fi
