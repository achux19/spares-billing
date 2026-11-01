# Deployment kit

Server layout:
  /opt/spares/repo      git clone (read-only deploy key)
  /opt/spares/edge      Caddy: copy of deploy/edge + its .env
  /opt/spares/staging   .env (and previous_version)
  /opt/spares/prod      .env (later)
  /opt/spares/backups   local backup copies
  /opt/spares/secrets   backup.pass (chmod 600)

Use:
  deploy/scripts/deploy.sh   staging 1.0.0-rc1
  deploy/scripts/rollback.sh staging
  deploy/scripts/backup.sh   prod daily
  deploy/scripts/restore.sh  staging <file.dump.gpg>

Cron (crontab -e):
  CRON_TZ=Asia/Kolkata
  0 14 * * *   /opt/spares/repo/deploy/scripts/backup.sh prod daily >> /opt/spares/backups/cron.log 2>&1
  30 23 * * *  /opt/spares/repo/deploy/scripts/backup.sh prod daily >> /opt/spares/backups/cron.log 2>&1
