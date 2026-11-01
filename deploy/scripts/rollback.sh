#!/usr/bin/env bash
# usage: rollback.sh <prod|staging>   (app only, Level A)
# If the new version ran a database migration, read playbook section 7 first (Level B/C).
set -euo pipefail
. "$(dirname "$0")/lib.sh"
stack_check "${1:-}"
PREV_FILE=$BASE/$STACK/previous_version
[ -f "$PREV_FILE" ] || { echo "no previous_version recorded" >&2; exit 2; }
PREV=$(cat "$PREV_FILE"); CUR=$(getenv VERSION)
echo "Rolling $STACK back: v$CUR -> v$PREV"
cd "$REPO"
git checkout -q "v$PREV"
setenv VERSION "$PREV"
dc up -d
wait_healthy "spares-$STACK-backend" 180 && wait_healthy "spares-$STACK-web" 60 \
  && echo "ROLLED BACK to v$PREV. Run the smoke test, then fill the rollback record."
echo "$CUR" > "$BASE/$STACK/previous_version"
