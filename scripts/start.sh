#!/usr/bin/env bash
# Starts the whole Laundry Booking stack for local development (Linux / macOS):
#   1. Docker: Kafka, Kafka UI, PostgreSQL, notification-service
#   2. core-service      (./mvnw spring-boot:run, in the background)
#   3. web-client        (npm run dev, in the background)
# Stop everything with: ./scripts/stop.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="$ROOT/.run"          # PIDs and logs of the processes started here (ignored by Git)
mkdir -p "$RUN_DIR"
cd "$ROOT"

info() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
ok()   { printf '\033[1;32m OK\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33mWARN\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31mERROR\033[0m %s\n' "$*" >&2; exit 1; }

# Waits until an HTTP endpoint answers, or fails after a timeout (seconds).
wait_for() {
    local name="$1" url="$2" timeout="$3" log="${4:-}"
    local waited=0
    printf '    waiting for %s' "$name"
    until curl -fs -o /dev/null "$url"; do
        if (( waited >= timeout )); then
            echo
            [[ -n "$log" ]] && fail "$name did not start in ${timeout}s. Check the log: $log"
            fail "$name did not start in ${timeout}s."
        fi
        printf '.'
        sleep 2
        waited=$((waited + 2))
    done
    echo
    ok "$name is up ($url)"
}

is_running() {
    local pid_file="$RUN_DIR/$1.pid"
    [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" 2>/dev/null
}

# Starts a command in the background, in its own process group, so stop.sh can stop it
# together with the processes it spawns (Maven forks a JVM, npm starts Vite).
start_bg() {
    local name="$1" dir="$2"; shift 2
    if command -v setsid >/dev/null; then
        (cd "$dir" && setsid nohup "$@" >"$RUN_DIR/$name.log" 2>&1 & echo $! >"$RUN_DIR/$name.pid")
    else
        (cd "$dir" && nohup "$@" >"$RUN_DIR/$name.log" 2>&1 & echo $! >"$RUN_DIR/$name.pid")
    fi
}

# ---- 0. Prerequisites ----------------------------------------------------------------
info "Checking prerequisites"
for cmd in docker java npm curl; do
    command -v "$cmd" >/dev/null || fail "'$cmd' is not installed"
done
docker info >/dev/null 2>&1 || fail "Docker is not running"

if [[ ! -f .env ]]; then
    cp .env.example .env
    fail "Created .env from .env.example. Set POSTGRES_PASSWORD in .env, then run this script again."
fi
grep -q '^POSTGRES_PASSWORD=change-me' .env && warn "POSTGRES_PASSWORD is still 'change-me' in .env"

# core-service reads .env from its own folder: link it to the root one (single source of truth)
[[ -e core-service/.env ]] || ln -s ../.env core-service/.env
chmod +x core-service/mvnw
ok "Prerequisites"

# ---- 1. Docker: Kafka, Kafka UI, PostgreSQL, notification-service --------------------
info "Starting Docker services (Kafka, Kafka UI, PostgreSQL, notification-service)"
docker compose up -d --build
wait_for "notification-service" "http://localhost:8082/bookings" 240

# ---- 2. core-service -------------------------------------------------------------------
if is_running core-service; then
    ok "core-service is already running"
else
    info "Starting core-service (first run downloads dependencies and can take a few minutes)"
    start_bg core-service core-service ./mvnw -q spring-boot:run
    wait_for "core-service" "http://localhost:8081/v3/api-docs" 300 "$RUN_DIR/core-service.log"
fi

# ---- 3. web-client --------------------------------------------------------------------
if is_running web-client; then
    ok "web-client is already running"
else
    if [[ ! -d web-client/node_modules ]]; then
        info "Installing web-client dependencies (npm install)"
        (cd web-client && npm install)
    fi
    info "Starting web-client"
    start_bg web-client web-client npm run dev -- --port 5173 --strictPort
    wait_for "web-client" "http://localhost:5173" 60 "$RUN_DIR/web-client.log"
fi

# ---- Summary ---------------------------------------------------------------------------
cat <<SUMMARY

Laundry Booking is running:
  Web client            http://localhost:5173
  core-service (API)    http://localhost:8081/swagger-ui.html
  notification-service  http://localhost:8082/bookings  /machines  /proposals
  Kafka UI              http://localhost:8080

Logs:
  core-service          tail -f .run/core-service.log
  web-client            tail -f .run/web-client.log
  notification-service  docker compose logs -f notification-service

Stop everything:        ./scripts/stop.sh
SUMMARY
