@echo off
title Monitoreo IoT

echo ==============================
echo      Monitoreo IoT
echo ==============================
echo.

if not exist ".env" (
    echo ERROR: No existe el archivo .env
    echo Copia .env.example a .env y completa los datos.
    pause
    exit /b
)

echo Cargando variables de entorno...
for /f "usebackq delims=" %%a in (".env") do set %%a

echo Generando config.js...

(
echo window.APP_CONFIG = {
echo     CAMERA_STREAM_URL: "http://%ESP32_IP%:81/stream",
echo     API_BASE_URL: "http://%BACKEND_IP%:%BACKEND_PORT%%BACKEND_CONTEXT_PATH%"
echo };
) > web\js\config.js

echo ✅ config.js generado correctamente.

echo.
echo Iniciando backend (compilando primero)...
start cmd /k "mvn compile exec:java -Dexec.mainClass=com.monitoreoiot.MonitorIoT"

echo.
echo Iniciando frontend...
cd web
start cmd /k "python -m http.server %FRONT_PORT%"

timeout /t 3 >nul

start http://localhost:%FRONT_PORT%

echo.
echo Proyecto iniciado.
pause