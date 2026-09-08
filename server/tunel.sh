#!/usr/bin/env bash
set -euo pipefail
if ! command -v cloudflared >/dev/null; then
  printf '%s\n' 'Instale cloudflared e execute este arquivo novamente:' 'https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/downloads/'
  exit 1
fi
printf '%s\n' 'Abra a URL HTTPS exibida abaixo. Mantenha o servidor e esta janela abertos.'
exec cloudflared tunnel --url http://127.0.0.1:8787 --protocol http2
