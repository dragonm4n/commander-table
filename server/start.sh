#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
if ! command -v java >/dev/null; then
  echo 'Install Java 17 or later: https://adoptium.net/temurin/releases/?version=17'
  exit 1
fi
printf '%s\n' 'Commander Table: when the server is ready, open http://localhost:8787'
exec java -Xmx3G -jar commander-table.jar --assets "$PWD/forge" --port 8787
