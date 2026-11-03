#!/bin/sh
# Dumps the whole database (accounts, grunts and images) out of the bundled mongo service.
#   deploy/backup.sh [directory]      (default: ./backups)
# Run it from the folder that holds compose.yaml. If you removed the mongo service and use
# your own MongoDB, back that up the way you back up the rest of it instead.
set -eu

dest="${1:-./backups}"
stamp="$(date +%Y%m%d-%H%M%S)"

mkdir -p "$dest"
# mongodump reads a consistent copy while the app keeps running.
docker compose exec -T mongo mongodump --quiet --db openwalrus --archive --gzip > "$dest/openwalrus-$stamp.archive.gz"
echo "Saved $dest/openwalrus-$stamp.archive.gz"
