#!/usr/bin/env bash
# usage: deploy.sh <prod|staging> <version>      e.g. deploy.sh staging 1.0.0-rc1
set -euo pipefail
. "$(dirname "$0")/lib.sh"
stack_check "${1:-}"
NEW=${2:?give the version, e.g. 1.0.0}

cd "$REPO"
git fetch --tags --quiet
git rev-parse -q --verify "refs/tags/v$NEW" >/dev/null || { echo "tag v$NEW not found in git" >&2; exit 2; }

# Switch to the new version's files first and run THIS script again from them,
# so a release always uses its own (newest) deploy script.
if [ -z "${DEPLOY_REEXEC:-}" ]; then
  git checkout -q "v$NEW"
  DEPLOY_REEXEC=1 exec "$REPO/deploy/scripts/deploy.sh" "$@"
fi

OLD=$(getenv VERSION)
echo "$OLD" > "$BASE/$STACK/previous_version"

if [ "$STACK" = prod ] && docker ps -q -f "name=^spares-prod-db$" | grep -q .; then
  echo "== pre-release backup =="
  "$(dirname "$0")/backup.sh" prod prerelease
fi

echo "== building v$NEW =="
setenv VERSION "$NEW"
dc up -d --build

echo "== waiting for health =="
if wait_healthy "spares-$STACK-db" 60 && wait_healthy "spares-$STACK-backend" 180 && wait_healthy "spares-$STACK-web" 60; then
  echo "DEPLOYED $STACK v$NEW (previous: v$OLD)"
  echo "Next: run the smoke test. If it fails: deploy/scripts/rollback.sh $STACK"
else
  echo "DEPLOY FAILED. Logs: docker logs spares-$STACK-backend --tail 100" >&2
  echo "Roll back with: deploy/scripts/rollback.sh $STACK" >&2
  exit 1
fi
