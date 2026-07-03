const API_BASE_URL = window.APP_CONFIG.API_BASE_URL;

const tempElement = document.getElementById('tempValue');
const humElement = document.getElementById('humValue');
const lightValueElement = document.getElementById('lightValue');
const lightIconElement = document.getElementById('lightIcon');
const lastUpdateElement = document.getElementById('lastUpdate');
lightIconElement.className = 'card-icon sol';

async function fetchTempHum() {
    try {
        const response = await fetch(`${API_BASE_URL}/temperaturas`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const data = await response.json();

        if (data && data.length > 0) {
            const last = data[0];
            const temp = last.temperatura;
            const hum = last.humedad;
            tempElement.textContent = temp !== undefined ? temp.toFixed(1) : '--';
            humElement.textContent = hum !== undefined ? hum.toFixed(0) : '--';
        } else {
            tempElement.textContent = '--';
            humElement.textContent = '--';
        }
    } catch (error) {
        console.error('Error al obtener temperatura/humedad:', error);
        tempElement.textContent = 'Err';
        humElement.textContent = 'Err';
    }
}

async function fetchLuz() {
    try {
        const response = await fetch(`${API_BASE_URL}/luz`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const data = await response.json();

        if (data && data.length > 0) {
            const last = data[0];
            const luz = last.luz;
            if (luz === true || luz === 'true') {
                lightValueElement.textContent = 'ENCENDIDA';
                lightValueElement.classList.remove('apagada');
                lightIconElement.className = 'card-icon sol'; // Cambia a sol
                lightIconElement.textContent = '☀️';
                lightIconElement.style.filter = 'drop-shadow(0 0 5px rgba(245,197,66,0.5))';
            } else {
                lightValueElement.textContent = 'APAGADA';
                lightValueElement.classList.add('apagada');
                lightIconElement.className = 'card-icon luna'; // Cambia a luna
                lightIconElement.textContent = '🌙';
                lightIconElement.style.filter = 'none';
            }
        } else {
            lightValueElement.textContent = '--';
            lightValueElement.classList.remove('apagada');
            lightIconElement.className = 'card-icon'; // Vuelve a clase base
            lightIconElement.textContent = '💡';
            lightIconElement.style.filter = 'none';
        }
    } catch (error) {
        console.error('Error al obtener estado de luz:', error);
        lightValueElement.textContent = 'Err';
        lightValueElement.classList.remove('apagada');
        lightIconElement.className = 'card-icon';
        lightIconElement.textContent = '💡';
        lightIconElement.style.filter = 'none';
    }
}

function updateTimestamp() {
    const now = new Date();
    const timeStr = now.toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    lastUpdateElement.textContent = timeStr;
}

async function refreshData() {
    await Promise.all([fetchTempHum(), fetchLuz()]);
    updateTimestamp();
}

refreshData();
setInterval(refreshData, 5000);