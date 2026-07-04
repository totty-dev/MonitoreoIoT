# 🌦️ MonitoreoIoT — Estación Ambiental IoT

Sistema completo de monitoreo ambiental: **ESP32-CAM** (DHT11 + APDS9960 + cámara), **backend en Java**, broker **MQTT**, base de datos **PostgreSQL** y **frontend web**.  
Muestra en tiempo real temperatura, humedad y estado de luz, con historial filtrable por fecha y stream de cámara en vivo.

---

## 📋 Requisitos previos

- **Para ejecución local** (sin Docker):
    - Java 17 (JDK)
    - Maven 3.6+
    - Python 3 (para el servidor web del frontend)
- **Para Docker / Portainer**:
    - Docker Engine y Docker Compose (o Portainer)
- **Para el ESP32**:
    - Arduino IDE con las librerías `PubSubClient` y `SparkFun_APDS9960`
    - Placa ESP32-CAM (AI-Thinker)

---

## ⚙️ Configuración

Toda la configuración se maneja mediante **variables de entorno**.  
Copia el archivo de ejemplo y completa todos los valores:

```bash
cp .env.example .env
Luego edita .env con tus credenciales reales:
```
### ENV

```
# ---- PostgreSQL (externo) ----
DB_URL=jdbc:postgresql://<ip-postgres>:5432/<nombre-db>
DB_USER=
DB_PASSWORD=

# ---- MQTT (externo) ----
MQTT_IP=
MQTT_PORT=
MQTT_TOPIC1=
MQTT_TOPIC2=
MQTT_QOS=0          # opcional, por defecto 0

# ---- Backend ----
BACKEND_PORT=8082
BACKEND_IP=127.0.0.1        # ⚠️ IMPORTANTE: IP ACCESIBLE DESDE EL NAVEGADOR
BACKEND_CONTEXT_PATH=       # opcional, ej. /api

# ---- Frontend ----
FRONTEND_IP=127.0.0.1       # IP donde sirves el frontend (para el script local)
FRONT_PORT=8000
ESP32_IP=192.168.1.xxx      # IP de tu ESP32-CAM
```

### Importante:

BACKEND_IP debe ser la IP por la que el navegador pueda alcanzar el backend.
En local suele ser 127.0.0.1; en una red, la IP real de la máquina. No uses 0.0.0.0 porque el navegador no lo resolverá.

BACKEND_CONTEXT_PATH permite prefijar las rutas de la API (ej. /api). Déjalo vacío si no lo necesitas.

🚀 Ejecución
▶️ Local (con scripts)
Asegúrate de tener Java 17, Maven y Python 3 instalados.

Completa el archivo .env.

Ejecuta el script según tu sistema operativo:

Linux / macOS:

bash
chmod +x iniciar.sh
./iniciar.sh
Windows:

bash
iniciar.bat
El script:

Carga las variables de .env.

Genera automáticamente web/js/config.js con las IPs y puertos.

Compila y ejecuta el backend con Maven (mvn compile exec:java).

Levanta el frontend con el servidor HTTP de Python en el puerto FRONT_PORT.

Abre el navegador en http://localhost:FRONT_PORT.

Para detenerlo, pulsa Ctrl+C.

🐳 Con Docker Compose
Asegúrate de tener el archivo .env completo en la raíz del proyecto.

Ejecuta:

bash
docker compose up -d --build
Esto construye las imágenes del backend y frontend, inyecta las variables de entorno y levanta los contenedores.

Backend: http://localhost:BACKEND_PORT

Frontend: http://localhost:FRONT_PORT

Para detener:

bash
docker compose down
📦 En Portainer (Stack)
Ve a Stacks → Add stack.

Elige Repository y pega la URL de este repositorio.

En la sección Environment variables, introduce todas las variables que aparecen en .env.example con sus valores reales (una por una).

Haz clic en Deploy the stack.

Portainer clonará el repo, construirá las imágenes y levantará los servicios con esas variables.

🧩 ¿Cómo se genera config.js del frontend?
El frontend necesita saber la URL del backend y del ESP32. Estos valores no están fijos, sino que se inyectan en tiempo de ejecución.

En local (con los scripts): iniciar.sh / iniciar.bat escriben el archivo web/js/config.js directamente a partir de las variables de entorno.

En Docker: el frontend.Dockerfile copia una plantilla (docker/frontend/config.js.template) y un script (generate-config.sh) que se ejecuta al arrancar el contenedor. Usa envsubst para reemplazar los placeholders {ESP32_IP}, {BACKEND_IP}, {BACKEND_PORT} y {BACKEND_CONTEXT_PATH} por los valores de las variables de entorno del contenedor.

La plantilla tiene este aspecto:

js
window.APP_CONFIG = {
    CAMERA_STREAM_URL: "http://${ESP32_IP}:81/stream",
    API_BASE_URL: "http://${BACKEND_IP}:${BACKEND_PORT}${BACKEND_CONTEXT_PATH}"
};
El resultado final se sirve como un archivo JavaScript estático.

🗄️ Base de datos
El backend espera dos tablas en PostgreSQL (debes crearlas previamente):

sql
CREATE TABLE clima (
    id          SERIAL PRIMARY KEY,
    temperatura DOUBLE PRECISION NOT NULL,
    humedad     DOUBLE PRECISION NOT NULL,
    fecha       TIMESTAMP NOT NULL
);

CREATE TABLE luz (
    id     SERIAL PRIMARY KEY,
    luz    BOOLEAN NOT NULL,
    fecha  TIMESTAMP NOT NULL
);
🔌 API REST
Todas las rutas cuelgan de BACKEND_CONTEXT_PATH (vacío por defecto).
Las respuestas son JSON y tienen CORS habilitado (*).

Método	Endpoint	Descripción
GET	/temperaturas	Última lectura de temperatura y humedad
GET	/luz	Último estado de luz
GET	/historico/tempyhum?start=YYYY-MM-DD&end=YYYY-MM-DD	Historial de temperatura/humedad (por defecto últimos 7 días)
GET	/historico/luz?start=YYYY-MM-DD&end=YYYY-MM-DD	Historial de luz (por defecto últimos 7 días)
📡 ESP32 – Firmware
El firmware se encuentra en esp32/mqttpublisher/.
Debes editar config.h con los datos de tu red y broker MQTT:

cpp
#define WIFI_SSID     "tu_wifi"
#define WIFI_PASSWORD "tu_password"
#define MQTT_IP       "ip_del_broker"
#define MQTT_PORT     1883
#define MQTT_TOPIC1   "clima"
#define MQTT_TOPIC2   "luz"
El ESP32:

Publica temperatura y humedad cada 60 segundos en MQTT_TOPIC1 con formato "<temp>,<hum>".

Publica "true"/"false" en MQTT_TOPIC2 cuando la luz ambiente cruza el umbral (200).

Sirve un stream MJPEG en http://<ip-esp32>:81/stream.

📁 Estructura del proyecto (resumen)
text
MonitoreoIoT/
├── docker-compose.yml
├── backend.Dockerfile
├── frontend.Dockerfile
├── .env.example
├── pom.xml
├── docker/
│   └── frontend/
│       ├── config.js.template
│       └── generate-config.sh
├── src/                      # código backend Java
├── esp32/                    # firmware para ESP32-CAM
│   └── mqttpublisher/
│       ├── mqttpublisher.ino
│       └── config.h
├── web/                      # frontend (HTML, CSS, JS)
│   ├── index.html
│   ├── historial.html
│   ├── css/
│   └── js/
│       ├── config.js         # se genera automáticamente
│       ├── script.js
│       ├── camera.js
│       └── historial.js
├── iniciar.sh                # script de inicio para Linux/macOS
└── iniciar.bat               # script de inicio para Windows
📝 Notas finales
El backend usa el servidor HTTP embebido de Java (com.sun.net.httpserver), sin frameworks externos.

El frontend es completamente vanilla (HTML, CSS, JS) y se comunica con la API del backend mediante fetch.

El broker MQTT y la base de datos se mantienen fuera del stack Docker; se asumen ya desplegados.

Para probar el frontend sin los scripts (por ejemplo, abriendo los HTML directamente), deberás editar web/js/config.js a mano con las IPs correctas.

¡Listo! Con estos pasos deberías tener el sistema funcionando en pocos minutos. Si encuentras algún problema, revisa que todas las variables de entorno estén correctamente definidas y que los servicios externos (MQTT y PostgreSQL) estén accesibles.

text

---

Ya está. Si necesitas algún cambio adicional, avísame.