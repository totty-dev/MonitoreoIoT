const CAMERA_STREAM_URL = window.APP_CONFIG.CAMERA_STREAM_URL;

const cameraImg = document.getElementById('cameraStream');
const cameraStatus = document.getElementById('cameraStatus');

let reconnectTimer = null;
const RECONNECT_DELAY = 4000;

function setOnline() {
    cameraStatus.textContent = '● en vivo';
    cameraStatus.classList.add('online');
}

function setOffline() {
    cameraStatus.textContent = '● sin señal';
    cameraStatus.classList.remove('online');
}

function reloadStream() {
    cameraImg.src = `${CAMERA_STREAM_URL}?t=${Date.now()}`;
}

function scheduleReconnect() {
    if (reconnectTimer) return;
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

reloadStream();
