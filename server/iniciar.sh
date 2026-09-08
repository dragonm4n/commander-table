#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
if ! command -v java >/dev/null; then
  echo 'Instale Java 17 ou superior: https://adoptium.net/temurin/releases/?version=17'
  exit 1
fi
printf '%s\n' 'Commander Table: quando o servidor estiver pronto, abra http://localhost:8787'
exec java -Xmx3G -jar commander-table.jar --assets "$PWD/forge" --port 8787
