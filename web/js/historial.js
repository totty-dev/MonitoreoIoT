const API_BASE_URL = window.APP_CONFIG.API_BASE_URL;

const tipoSelect = document.getElementById('tipo');
const fechaInicio = document.getElementById('fechaInicio');
const fechaFin = document.getElementById('fechaFin');
const btnFiltrar = document.getElementById('btnFiltrar');
const tablaBody = document.getElementById('cuerpoTabla');
const cabecera = document.getElementById('cabeceraTabla');

// Fechas por defecto: últimos 7 días
function setDefaultDates() {
    const hoy = new Date();
    const hace7 = new Date(hoy);
    hace7.setDate(hoy.getDate() - 7);
    fechaFin.value = hoy.toISOString().split('T')[0];
    fechaInicio.value = hace7.toISOString().split('T')[0];
}

async function cargarHistorial() {
    const tipo = tipoSelect.value;
    const start = fechaInicio.value;
    const end = fechaFin.value;

    if (!start || !end) {
        alert('Selecciona un rango de fechas válido.');
        return;
    }

    let url = `${API_BASE_URL}/historico/${tipo}?start=${start}&end=${end}`;
    try {
        const response = await fetch(url);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const data = await response.json();

        // Actualizar cabecera según tipo
        if (tipo === 'tempyhum') {
            cabecera.innerHTML = `
                <th>Fecha</th>
                <th>Temperatura (°C)</th>
                <th>Humedad (%)</th>
                <th>Luz</th>
            `;
        } else {
            cabecera.innerHTML = `
                <th>Fecha</th>
                <th>Luz</th>
                <th></th>
                <th></th>
            `;
        }

        if (data.length === 0) {
            tablaBody.innerHTML = `<tr><td colspan="4">No hay registros en este rango.</td></tr>`;
            return;
        }

        let html = '';
        if (tipo === 'tempyhum') {
            data.forEach(row => {
                const fecha = new Date(row.fecha).toLocaleString('es-AR');
                html += `<tr>
                    <td>${fecha}</td>
                    <td>${row.temperatura.toFixed(1)}</td>
                    <td>${row.humedad.toFixed(0)}</td>
                    <td>—</td>
                </tr>`;
            });
        } else { // luz
            data.forEach(row => {
                const fecha = new Date(row.fecha).toLocaleString('es-AR');
                const estado = row.luz ? 'ENCENDIDA' : 'APAGADA';
                const clase = row.luz ? 'luz-on' : 'luz-off';
                html += `<tr>
                    <td>${fecha}</td>
                    <td class="${clase}">${estado}</td>
                    <td></td>
                    <td></td>
                </tr>`;
            });
        }
        tablaBody.innerHTML = html;
    } catch (error) {
        console.error('Error cargando historial:', error);
        tablaBody.innerHTML = `<tr><td colspan="4">Error al cargar los datos.</td></tr>`;
    }
}

// Eventos
btnFiltrar.addEventListener('click', cargarHistorial);

// Carga inicial
setDefaultDates();
cargarHistorial();