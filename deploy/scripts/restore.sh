#!/usr/bin/env bash
# usage: restore.sh <prod|staging> <file.dump.gpg | file.dump>
# Replaces ALL data in that stack's database. Takes a safety backup first.
set -euo pipefail
. "$(dirname "$0")/lib.sh"
stack_check "${1:-}"
FILE=${2:?give the backup file}
[ -f "$FILE" ] || { echo "no such file: $FILE" >&2; exit 2; }
DB_USER=$(getenv DB_USER); DB_NAME=$(getenv DB_NAME)

echo "This will REPLACE all data in the $STACK database with: $FILE"
read -r -p "Type the word $STACK to continue: " ans
[ "$ans" = "$STACK" ] || { echo "cancelled"; exit 1; }

"$(dirname "$0")/backup.sh" "$STACK" before_restore

WORK=$(mktemp -d); trap 'rm -rf "$WORK"' EXIT
case "$FILE" in
  *.gpg) gpg --batch --yes --pinentry-mode loopback --passphrase-file "$BASE/secrets/backup.pass" \
           -o "$WORK/r.dump" --decrypt "$FILE" ;;
  *)     cp "$FILE" "$WORK/r.dump" ;;
esac

docker stop "spares-$STACK-backend" >/dev/null 2>&1 || true
docker exec -i "spares-$STACK-db" pg_restore -U "$DB_USER" -d "$DB_NAME" \
  --clean --if-exists --no-owner < "$WORK/r.dump" || echo "(pg_restore printed warnings; check the output above)"
docker start "spares-$STACK-backend"
wait_healthy "spares-$STACK-backend" 120
echo "restore finished. Now open the app and run the quick smoke test."
