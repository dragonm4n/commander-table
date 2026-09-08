#!/usr/bin/env bash
set -euo pipefail
if ! command -v cloudflared >/dev/null; then
  printf '%s\n' 'Install cloudflared and run this file again:' 'https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/downloads/'
  exit 1
fi
printf '%s\n' 'Open the HTTPS URL shown below. Keep the server and this window open.'
exec cloudflared tunnel --url http://127.0.0.1:8787 --protocol http2
