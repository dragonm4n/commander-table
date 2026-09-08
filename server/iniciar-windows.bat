@echo off
chcp 65001 >nul
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Instale Java 17 ou superior: https://adoptium.net/temurin/releases/?version=17
  echo Depois abra este arquivo novamente.
  pause
  exit /b 1
)
echo Commander Table: carregando o Forge. Isso pode levar alguns segundos.
echo Quando aparecer "servidor pronto", abra http://localhost:8787
java -Xmx3G -jar commander-table.jar --assets "%~dp0forge" --port 8787
pause
