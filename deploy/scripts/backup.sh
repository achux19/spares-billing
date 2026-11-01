#!/usr/bin/env bash
# usage: backup.sh <prod|staging> [daily|prerelease|before_restore]
set -euo pipefail
. "$(dirname "$0")/lib.sh"
stack_check "${1:-}"
KIND=${2:-daily}
DB_USER=$(getenv DB_USER); DB_NAME=$(getenv DB_NAME)
REMOTE=$(getenv RCLONE_REMOTE); HC=$(getenv HC_PING_URL)
DIR=$BASE/backups/$STACK/$KIND
PASSF=$BASE/secrets/backup.pass
mkdir -p "$DIR"
TS=$(TZ=Asia/Kolkata date +%Y-%m-%d_%H%M)
OUT=$DIR/${STACK}_${KIND}_${TS}.dump

fail() { echo "BACKUP FAILED: $1" >&2; [ -n "$HC" ] && curl -fsS -m 10 "$HC/fail" >/dev/null 2>&1 || true; exit 1; }

PREV=$(ls -1t "$DIR"/*.gpg 2>/dev/null | head -1 || true)

docker exec "spares-$STACK-db" pg_dump -U "$DB_USER" -Fc "$DB_NAME" > "$OUT.tmp" || fail "pg_dump"
mv "$OUT.tmp" "$OUT"
docker exec -i "spares-$STACK-db" pg_restore --list < "$OUT" > /dev/null || fail "dump unreadable"
SIZE=$(stat -c %s "$OUT")
[ "$SIZE" -gt 10000 ] || fail "dump suspiciously small ($SIZE bytes)"
if [ -n "$PREV" ] && [ "$KIND" = daily ]; then
  PSIZE=$(stat -c %s "$PREV")   # encrypted size is close to dump size
  [ $((SIZE * 2)) -ge "$PSIZE" ] || fail "dump is less than half of previous ($SIZE vs $PSIZE)"
fi

[ -f "$PASSF" ] || fail "missing $PASSF"
gpg --batch --yes --pinentry-mode loopback --passphrase-file "$PASSF" \
    --symmetric --cipher-algo AES256 -o "$OUT.gpg" "$OUT" || fail "gpg"
rm -f "$OUT"
echo "ok: $OUT.gpg ($(stat -c %s "$OUT.gpg") bytes)"

if [ -n "$REMOTE" ]; then
  rclone copy "$OUT.gpg" "$REMOTE/$STACK/$KIND/" || fail "rclone upload"
  if [ "$KIND" = daily ]; then
    [ "$(date +%u)" = 7 ]  && rclone copy "$OUT.gpg" "$REMOTE/$STACK/weekly/"  || true
    [ "$(date +%d)" = 01 ] && rclone copy "$OUT.gpg" "$REMOTE/$STACK/monthly/" || true
  fi
  rclone delete "$REMOTE/$STACK/daily"      --min-age 14d  || true
  rclone delete "$REMOTE/$STACK/weekly"     --min-age 56d  || true
  rclone delete "$REMOTE/$STACK/monthly"    --min-age 365d || true
  rclone delete "$REMOTE/$STACK/prerelease" --min-age 60d  || true
fi

find "$DIR" -name '*.gpg' -mtime +14 -delete
[ -n "$HC" ] && curl -fsS -m 10 "$HC" >/dev/null 2>&1 || true
