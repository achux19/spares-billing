#!/usr/bin/env bash
# Shared helpers. Sourced by the other scripts.
BASE=/opt/spares
REPO=$BASE/repo

stack_check() {
  case "${1:-}" in prod|staging) ;; *) echo "stack must be prod or staging" >&2; exit 2;; esac
  STACK=$1
  ENVF=$BASE/$STACK/.env
  [ -f "$ENVF" ] || { echo "missing $ENVF" >&2; exit 2; }
}

# getenv KEY  -> value from the stack .env (no shell sourcing, safe with $ signs)
getenv() { grep -E "^$1=" "$ENVF" | head -1 | cut -d= -f2- | sed -e "s/^'//" -e "s/'$//"; }

setenv() { # setenv KEY VALUE
  if grep -qE "^$1=" "$ENVF"; then sed -i "s|^$1=.*|$1=$2|" "$ENVF"; else echo "$1=$2" >> "$ENVF"; fi
}

dc() { docker compose -p "spares-$STACK" --env-file "$ENVF" -f "$REPO/deploy/docker-compose.app.yml" "$@"; }

wait_healthy() { # wait_healthy container seconds
  local c=$1 t=$2 s
  for _ in $(seq 1 $((t/3))); do
    s=$(docker inspect --format '{{.State.Health.Status}}' "$c" 2>/dev/null || echo none)
    [ "$s" = healthy ] && return 0
    sleep 3
  done
  echo "$c not healthy after ${t}s (status: $s)" >&2
  return 1
}
