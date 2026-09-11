@echo off
chcp 65001 >nul
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Install Java 17 or later: https://adoptium.net/temurin/releases/?version=17
  echo Then run this file again.
  pause
  exit /b 1
)
echo Commander Table alpha 0.6: loading Forge. This may take a few seconds.
echo When "server ready" appears, open http://localhost:8787
java -Xmx3G -jar commander-table.jar --assets "%~dp0forge" --port 8787
pause
