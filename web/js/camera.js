// IP fija del ESP32-CAM. Si el día de mañana cambia, solo hay que editar esta línea.
const CAMERA_IP = '192.168.1.106';
const CAMERA_STREAM_URL = `http://${CAMERA_IP}:81/stream`;

const cameraImg = document.getElementById('cameraStream');
const cameraStatus = document.getElementById('cameraStatus');

let reconnectTimer = null;
const RECONNECT_DELAY = 4000; // ms entre reintentos si se cae el stream

function setOnline() {
    cameraStatus.textContent = '● en vivo';
    cameraStatus.classList.add('online');
}

function setOffline() {
    cameraStatus.textContent = '● sin señal';
    cameraStatus.classList.remove('online');
}

function reloadStream() {
    // Forzamos recarga agregando un parámetro único, para evitar cache del navegador
    cameraImg.src = `${CAMERA_STREAM_URL}?t=${Date.now()}`;
}

function scheduleReconnect() {
    if (reconnectTimer) return; // ya hay un reintento programado
    reconnectTimer = setTimeout(() => {
        reconnectTimer = null;
        reloadStream();
    }, RECONNECT_DELAY);
}

cameraImg.addEventListener('load', () => {
    setOnline();
});

cameraImg.addEventListener('error', () => {
    setOffline();
    scheduleReconnect();
});

// Primera carga
reloadStream();
