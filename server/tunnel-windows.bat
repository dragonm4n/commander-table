@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist "tools" mkdir "tools"
if not exist "tools\cloudflared.exe" (
  echo Downloading cloudflared for Windows x64 from the official Cloudflare repository...
  powershell -NoProfile -Command "$ErrorActionPreference='Stop'; Invoke-WebRequest -Uri 'https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe' -OutFile 'tools\cloudflared.exe'"
  if errorlevel 1 (
    echo Download failed. Try again or install cloudflared manually.
    if exist "tools\cloudflared.exe" del "tools\cloudflared.exe"
    pause
    exit /b 1
  )
)
echo Keep the server open too. Open the HTTPS URL shown below.
echo The tunnel is temporary and stops when this window closes.
"tools\cloudflared.exe" tunnel --url http://127.0.0.1:8787 --protocol http2
pause
