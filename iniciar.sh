#!/bin/bash
set -e

echo "=============================="
echo "      Monitoreo IoT"
echo "=============================="
echo

if [ ! -f ".env" ]; then
    echo "ERROR: No existe el archivo .env"
    echo "Copia .env.example a .env y completa los datos."
    exit 1
fi

# Cargar variables de entorno
export $(grep -v '^#' .env | xargs)

# Generar config.js
cat > web/js/config.js <<EOF
window.APP_CONFIG = {
    CAMERA_STREAM_URL: "http://${ESP32_IP}:81/stream",
    API_BASE_URL: "http://${BACKEND_IP}:${BACKEND_PORT}${BACKEND_CONTEXT_PATH}"
};
EOF

echo "✅ config.js generado correctamente."

echo
echo "Iniciando backend (compilando primero)..."
mvn compile exec:java -Dexec.mainClass=com.monitoreoiot.MonitorIoT &
BACKEND_PID=$!

echo "Iniciando frontend..."
cd web
python3 -m http.server $FRONT_PORT &
FRONT_PID=$!
cd ..

sleep 2
xdg-open "http://localhost:$FRONT_PORT" 2>/dev/null || echo "Abre http://localhost:$FRONT_PORT en tu navegador."

echo
echo "Proyecto iniciado. Presiona Ctrl+C para detener."
wait $BACKEND_PID $FRONT_PID