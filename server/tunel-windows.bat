@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist "tools" mkdir "tools"
if not exist "tools\cloudflared.exe" (
  echo Baixando cloudflared do repositorio oficial da Cloudflare para Windows x64...
  powershell -NoProfile -Command "$ErrorActionPreference='Stop'; Invoke-WebRequest -Uri 'https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe' -OutFile 'tools\cloudflared.exe'"
  if errorlevel 1 (
    echo Nao foi possivel baixar. Tente novamente ou instale cloudflared manualmente.
    if exist "tools\cloudflared.exe" del "tools\cloudflared.exe"
    pause
    exit /b 1
  )
)
echo Mantenha tambem o servidor aberto. Abra a URL HTTPS exibida abaixo.
echo O tunel e temporario e fecha quando esta janela for fechada.
"tools\cloudflared.exe" tunnel --url http://127.0.0.1:8787 --protocol http2
pause
