# 🌦️ MonitoreoIoT — Estación Ambiental IoT

Sistema completo de monitoreo ambiental: **ESP32-CAM** (DHT11 + APDS9960 + cámara), **backend en Java**, broker **MQTT**, base de datos **PostgreSQL** y **frontend web**.

Muestra en tiempo real temperatura, humedad y estado de luz, con historial filtrable por fecha y stream de cámara en vivo.

---

## 📋 Requisitos previos

**Para ejecución local (sin Docker):**
- Java 17 (JDK)
- Maven 3.6+
- Python 3 (los scripts `iniciar.sh` / `iniciar.bat` lo usan para servir el frontend con `http.server`)

**Para Docker / Portainer:**
- Docker Engine y Docker Compose (o Portainer)

**Para el ESP32:**
- Arduino IDE con las librerías `PubSubClient` y `SparkFun_APDS9960`
- Placa ESP32-CAM (AI-Thinker)

**Servicios externos (no incluidos en este proyecto):**
- Un broker MQTT accesible
- Una base de datos PostgreSQL accesible

---

## ⚙️ Configuración

Toda la configuración se maneja mediante **variables de entorno**. Copia el archivo de ejemplo y completá todos los valores:

```bash
cp .env.example .env
```

Luego editá `.env` con tus datos reales:

```bash
# ---- PostgreSQL (externo) ----
DB_URL=jdbc:postgresql://<ip-postgres>:5432/<nombre-db>
DB_USER=
DB_PASSWORD=

# ---- MQTT (externo) ----
MQTT_IP=
MQTT_PORT=
MQTT_TOPIC1=          # opcional, por defecto "clima"
MQTT_TOPIC2=          # opcional, por defecto "luz"
MQTT_QOS=             # opcional, por defecto 0

# ---- Backend ----
BACKEND_PORT=8082
BACKEND_IP=0.0.0.0    # ver nota importante abajo
BACKEND_CONTEXT_PATH= # opcional, ej. /api

# ---- Frontend ----
FRONTEND_IP=127.0.0.1 # IP por la que el NAVEGADOR llega al backend
FRONT_PORT=8000
ESP32_IP=192.168.1.xxx
```

### ⚠️ Importante: `BACKEND_IP` vs `FRONTEND_IP`

Estas dos variables cumplen roles distintos y **no son intercambiables**:

| Variable | Para qué se usa | Dónde se usa |
|---|---|---|
| `BACKEND_IP` | Dirección en la que el backend Java **abre el socket** del servidor HTTP (`HttpServer.create`) | Backend (Java), y también en `iniciar.sh`/`iniciar.bat` para armar la URL del frontend local |
| `FRONTEND_IP` | IP por la que el **navegador** llega al backend | Solo en Docker, dentro de `config.js.template` |

Esto genera dos comportamientos distintos según cómo corras el proyecto:

- **Local (`iniciar.sh` / `iniciar.bat`):** el `config.js` del frontend se arma usando `BACKEND_IP`, no `FRONTEND_IP`. Como el default de `.env.example` es `BACKEND_IP=0.0.0.0`, si lo dejás así **el navegador no va a poder conectarse** (0.0.0.0 no es una dirección alcanzable desde el navegador). Para local, poné `BACKEND_IP=127.0.0.1` (o la IP real de tu máquina si vas a acceder desde otro dispositivo de tu red). `FRONTEND_IP` no se usa en este modo.

- **Docker / Portainer:** el backend corre dentro de un contenedor, así que ahí sí conviene dejar `BACKEND_IP=0.0.0.0` (para que el servidor escuche todas las interfaces dentro del contenedor). La URL que efectivamente usa el navegador se arma con `FRONTEND_IP`, que tenés que setear a la IP real de la máquina/host donde corre Docker (o `127.0.0.1` si accedés desde la misma máquina).

`BACKEND_CONTEXT_PATH` permite prefijar las rutas de la API (ej. `/api`). Dejalo vacío si no lo necesitás.

---

## 🚀 Ejecución

### ▶️ Local (con scripts)

1. Verificá tener instalados **Java 17**, **Maven** y **Python 3**.
2. Completá el archivo `.env` (recordá `BACKEND_IP=127.0.0.1` en este modo).
3. Ejecutá el script según tu sistema operativo:

   **Linux / macOS:**
   ```bash
   chmod +x iniciar.sh
   ./iniciar.sh
   ```

   **Windows:**
   ```bat
   iniciar.bat
   ```

El script hace lo siguiente:
1. Carga las variables de `.env`.
2. Genera automáticamente `web/js/config.js` con las IPs y puertos.
3. Compila y ejecuta el backend con Maven (`mvn compile exec:java`).
4. Levanta el frontend con el servidor HTTP de Python en el puerto `FRONT_PORT`.
5. Abre el navegador en `http://localhost:FRONT_PORT`.

Para detenerlo: `Ctrl+C` (Linux/macOS) o cerrá las ventanas abiertas (Windows).

### 🐳 Con Docker Compose

1. Completá el archivo `.env` en la raíz del proyecto (recordá `FRONTEND_IP` con la IP real del host, y podés dejar `BACKEND_IP=0.0.0.0`).
2. Ejecutá:
   ```bash
   docker compose up -d --build
   ```

Esto construye las imágenes de backend y frontend, inyecta las variables de entorno y levanta los contenedores.

- Backend: `http://<host>:BACKEND_PORT`
- Frontend: `http://<host>:FRONT_PORT`

Para detener:
```bash
docker compose down
```

### 📦 En Portainer (Stack)

1. Andá a **Stacks → Add stack**.
2. Elegí **Repository** y pegá la URL de este repositorio.
3. En **Environment variables**, cargá todas las variables que aparecen en `.env.example` con sus valores reales (una por una, incluyendo `FRONTEND_IP` con la IP del host de Portainer).
4. Hacé clic en **Deploy the stack**.

Portainer clona el repo, construye las imágenes y levanta los servicios con esas variables.

---

## 🧩 ¿Cómo se genera `config.js` del frontend?

El frontend necesita saber la URL del backend y del ESP32. Estos valores no están fijos: se inyectan en tiempo de ejecución.

- **En local (con los scripts):** `iniciar.sh` / `iniciar.bat` escriben directamente el archivo `web/js/config.js` a partir de `ESP32_IP` y **`BACKEND_IP`**.
- **En Docker:** `frontend.Dockerfile` copia una plantilla (`docker/frontend/config.js.template`) y un script (`generate-config.sh`) que corre al arrancar el contenedor. Usa `envsubst` para reemplazar los placeholders `${ESP32_IP}`, **`${FRONTEND_IP}`**, `${BACKEND_PORT}` y `${BACKEND_CONTEXT_PATH}` por los valores de las variables de entorno del contenedor.

La plantilla (`docker/frontend/config.js.template`) tiene este aspecto:

```js
window.APP_CONFIG = {
    CAMERA_STREAM_URL: "http://${ESP32_IP}:81/stream",
    API_BASE_URL: "http://${FRONTEND_IP}:${BACKEND_PORT}${BACKEND_CONTEXT_PATH}"
};
```

El resultado final se sirve como un archivo JavaScript estático (`web/js/config.js`).

---

## 🗄️ Base de datos

El backend espera dos tablas en PostgreSQL (debés crearlas previamente):

```sql
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
```

Si `DB_URL`, `DB_USER` o `DB_PASSWORD` no están definidas, el backend no arranca y lo indica por consola.

---

## 🔌 API REST

Todas las rutas cuelgan de `BACKEND_CONTEXT_PATH` (vacío por defecto). Las respuestas son JSON y tienen CORS habilitado (`*`).

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/temperaturas` | Última lectura de temperatura y humedad |
| GET | `/luz` | Último estado de luz |
| GET | `/historico/tempyhum?start=YYYY-MM-DD&end=YYYY-MM-DD` | Historial de temperatura/humedad (por defecto, últimos 7 días) |
| GET | `/historico/luz?start=YYYY-MM-DD&end=YYYY-MM-DD` | Historial de luz (por defecto, últimos 7 días) |

---

## 📡 ESP32 — Firmware

El firmware está en `esp32/mqttpublisher/`. Antes de flashear, editá `config.h` con los datos de tu red y broker MQTT:

```cpp
#define WIFI_SSID     "tu_wifi"
#define WIFI_PASSWORD "tu_password"
#define MQTT_IP       "ip_del_broker"
#define MQTT_PORT     1883
#define MQTT_TOPIC1   "clima"
#define MQTT_TOPIC2   "luz"
```

El ESP32:
- Publica temperatura y humedad cada 60 segundos en `MQTT_TOPIC1`, con formato `"<temp>,<hum>"`.
- Publica `"true"`/`"false"` en `MQTT_TOPIC2` cuando la luz ambiente cruza el umbral configurado.
- Sirve un stream MJPEG en `http://<ip-esp32>:81/stream`.

---

## 📁 Estructura del proyecto (resumen)

```
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
├── src/                       # código backend Java
│   └── main/java/com/monitoreoiot/
│       ├── MonitorIoT.java     # entrypoint + servidor HTTP
│       ├── config/Config.java  # lectura de variables de entorno
│       ├── db/DataBaseManager.java
│       └── mqtt/MqttManager.java
├── esp32/                     # firmware para ESP32-CAM
│   └── mqttpublisher/
│       ├── mqttpublisher.ino
│       └── config.h
├── web/                       # frontend (HTML, CSS, JS)
│   ├── index.html
│   ├── historial.html
│   ├── css/
│   └── js/
│       ├── config.js          # se genera automáticamente, no editar a mano
│       ├── script.js
│       ├── camera.js
│       └── historial.js
├── iniciar.sh                 # script de inicio para Linux/macOS
└── iniciar.bat                # script de inicio para Windows
```

---

## 📝 Notas finales

- El backend usa el servidor HTTP embebido de Java (`com.sun.net.httpserver`), sin frameworks externos.
- El frontend es completamente vanilla (HTML, CSS, JS) y se comunica con la API del backend mediante `fetch`.
- El broker MQTT y la base de datos se mantienen fuera del stack Docker; se asumen ya desplegados.
- Si querés probar el frontend sin los scripts (por ejemplo, abriendo los archivos HTML directamente), vas a tener que editar `web/js/config.js` a mano con las IPs correctas.
- Si algo no funciona, lo primero a revisar es que **todas** las variables de `.env` estén completas y que los servicios externos (MQTT y PostgreSQL) sean alcanzables desde donde corre el backend.