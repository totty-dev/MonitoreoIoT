# 🌦️ MonitoreoIoT — Estación Ambiental IoT

Sistema de monitoreo ambiental compuesto por un **ESP32-CAM**, un **backend en Java**, un **broker MQTT**, una base de datos **PostgreSQL** y un **frontend web**. Registra temperatura, humedad y nivel de luz, expone los datos mediante una API REST propia y permite ver el historial con filtros por fecha, además de un stream de cámara en vivo.

---

## 📋 Tabla de contenidos

- [Arquitectura](#-arquitectura)
- [Estructura del proyecto](#-estructura-del-proyecto)
- [Stack tecnológico](#-stack-tecnológico)
- [Requisitos previos](#-requisitos-previos)
- [Configuración](#-configuración)
- [Puesta en marcha](#-puesta-en-marcha)
  - [Con Docker (recomendado)](#con-docker-recomendado)
  - [Manual / desarrollo local](#manual--desarrollo-local)
- [Base de datos](#-base-de-datos)
- [API REST](#-api-rest)
- [ESP32](#-esp32)
- [Frontend](#-frontend)
- [Notas y pendientes](#-notas-y-pendientes)

---

## 🧩 Arquitectura

```
ESP32-CAM (DHT11 + APDS9960 + cámara OV2640)
        │  publica por MQTT (temp/hum y luz)
        │  sirve stream MJPEG en puerto 81
        ▼
Broker MQTT (Mosquitto u otro)
        │
        ▼
Backend Java (suscriptor MQTT + servidor HTTP embebido)
        │  guarda lecturas
        ▼
PostgreSQL (tablas `clima` y `luz`)
        ▲
        │  consultas vía API REST
        │
Frontend web (HTML / CSS / JS puro, servido con Nginx)
```

- **ESP32-CAM**: lee temperatura/humedad (DHT11) y luz ambiente (APDS9960), publica por MQTT y expone el stream de video en `:81/stream`.
- **Backend (Java 17 + Maven)**: se suscribe a los tópicos MQTT con Eclipse Paho, persiste las lecturas en PostgreSQL y expone una API REST propia con `com.sun.net.httpserver.HttpServer` (sin frameworks externos).
- **PostgreSQL**: almacena el historial en dos tablas, `clima` (temperatura/humedad) y `luz`.
- **Frontend**: dashboard y vista de historial en HTML/CSS/JS vanilla, consumen la API del backend y el stream de la cámara directamente.
- **Docker Compose**: levanta `backend` (puerto 8082) y `frontend` (puerto 8081 → Nginx).

---

## 📁 Estructura del proyecto

```
MonitoreoIoT/
├── docker-compose.yml
├── backend.Dockerfile
├── frontend.Dockerfile
├── pom.xml
├── src/
│   └── main/
│       ├── java/com/monitoreoiot/
│       │   ├── MonitorIoT.java          # entry point + servidor HTTP
│       │   ├── config/Config.java       # carga config.properties + ENV
│       │   ├── db/DataBaseManager.java  # acceso a PostgreSQL
│       │   └── mqtt/MqttManager.java    # cliente/suscriptor MQTT
│       └── resources/
│           └── config.properties        # config por defecto (se puede pisar con ENV)
├── esp32/
│   └── mqttpublisher/
│       ├── mqttpublisher.ino            # firmware ESP32-CAM
│       └── config.h                     # credenciales WiFi / MQTT del dispositivo
└── web/
    ├── index.html                       # dashboard
    ├── historial.html                   # historial con filtros
    ├── css/style.css
    └── js/
        ├── config.js                    # URLs de API y stream de cámara
        ├── script.js
        ├── camera.js
        └── historial.js
```

---

## 🛠️ Stack tecnológico

| Capa       | Tecnología |
|------------|------------|
| Firmware   | ESP32-CAM (Arduino, `WiFi.h`, `PubSubClient`, `SparkFun_APDS9960`, `esp_camera`) |
| Mensajería | MQTT (broker Mosquitto u otro compatible) |
| Backend    | Java 17, Maven, Eclipse Paho MQTT Client, JDBC (driver PostgreSQL), `HttpServer` nativo de la JDK |
| Base de datos | PostgreSQL |
| Frontend   | HTML5, CSS3, JavaScript vanilla |
| Infraestructura | Docker + Docker Compose (backend con Maven/Eclipse Temurin, frontend con Nginx alpine) |

---

## ✅ Requisitos previos

- Docker y Docker Compose (para el despliegue recomendado)
- Java 17 y Maven (solo si vas a compilar/correr el backend sin Docker)
- Un broker MQTT accesible (Mosquitto, por ejemplo), en el host o en otro contenedor
- Una instancia de PostgreSQL con las tablas `clima` y `luz` (ver [Base de datos](#-base-de-datos))
- Placa ESP32-CAM (AI-Thinker) con sensor DHT11 y APDS9960, y el IDE de Arduino con las librerías `PubSubClient` y `SparkFun_APDS9960` instaladas

---

## ⚙️ Configuración

> Esta sección documenta **dónde** vive cada configuración. Las credenciales y URLs de ejemplo del repo son placeholders/valores de desarrollo — ajustalas a tu entorno antes de correr el proyecto.

### 1. Backend (`docker-compose.yml` / variables de entorno)

El backend lee su configuración desde `src/main/resources/config.properties`, pero **cualquier variable de entorno con el mismo nombre la pisa** (ver `Config.java`). En `docker-compose.yml` ya están declaradas las variables a completar:

```yaml
environment:
  - DB_URL=
  - DB_USER=
  - DB_PASSWORD=
  - MQTT_BROKER=
  - MQTT_TOPIC1=
  - MQTT_TOPIC2=
  - QOS=
  - SERVER_PORT=
  - SERVER_CONTEXT_PATH=
```

Valores por defecto actuales en `config.properties` (para correr local sin Docker):

```properties
MQTT_BROKER = tcp://<ip-broker>:1883
MQTT_TOPIC1 =
MQTT_TOPIC2 =
MQTT_QOS = 0

DB_URL =
DB_USER =
DB_PASSWORD =

SERVER_PORT =
SERVER_IP = 0.0.0.0
SERVER_CONTEXT_PATH =
```

### 2. Puertos (`docker-compose.yml`)

El mapeo de puertos también se define directo en el `docker-compose.yml` y hay que completarlo a mano (son placeholders de texto, no interpolación de variables):

```yaml
backend:
  ports:
    - "SERVER_PORT:SERVER_PORT"   # reemplazar por el puerto real, ej: "8082:8082"

frontend:
  ports:
    - "FRONT_PORT:80"             # reemplazar por el puerto real, ej: "8081:80"
```

> Importante: `SERVER_PORT:SERVER_PORT` y `FRONT_PORT:80` no son variables de entorno, son texto literal a reemplazar directo en el archivo. El primer número es el puerto del host, el segundo el puerto interno del contenedor.

### 3. Firmware ESP32 (`esp32/mqttpublisher/config.h`)

```cpp
#define WIFI_SSID     ""
#define WIFI_PASSWORD ""
#define MQTT_IP       ""   // IP del broker MQTT
#define MQTT_PORT     1883
#define MQTT_TOPIC1   ""
#define MQTT_TOPIC2   ""
```

### 4. Frontend (`web/js/config.js`)

```js
window.APP_CONFIG = {
    CAMERA_STREAM_URL: "http://<ip-esp32>:81/stream",
    API_BASE_URL: "http://<ip-backend>:port",
};
```

---

## 🚀 Puesta en marcha

### Con Docker (recomendado)

```bash
git clone <url-del-repo>
cd MonitoreoIoT
# completar variables de entorno Y los puertos (SERVER_PORT:SERVER_PORT / FRONT_PORT:80) en docker-compose.yml
docker compose up -d --build
```

- Backend disponible en `http://localhost:8082`
- Frontend disponible en `http://localhost:8081`

### Manual / desarrollo local

```bash
# Backend
mvn clean package -DskipTests
java -jar target/app.jar

# Frontend
# servir la carpeta web/ con cualquier servidor estático,
# o abrir directamente los .html en el navegador
```

---

## 🗄️ Base de datos

El backend asume un esquema PostgreSQL con dos tablas (no se crean automáticamente, hay que provisionarlas):

```sql
CREATE TABLE clima (
    id          SERIAL PRIMARY KEY,
    temperatura REAL NOT NULL,
    humedad     REAL NOT NULL,
    fecha       TIMESTAMP NOT NULL
);

CREATE TABLE luz (
    id     SERIAL PRIMARY KEY,
    luz    BOOLEAN NOT NULL,
    fecha  TIMESTAMP NOT NULL
);
```

---

## 🔌 API REST

Todas las rutas cuelgan del `SERVER_CONTEXT_PATH` configurado (vacío por defecto).

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/temperaturas` | Última lectura de temperatura y humedad |
| GET | `/luz` | Último estado de luz registrado |
| GET | `/historico/tempyhum?start=YYYY-MM-DD&end=YYYY-MM-DD` | Historial de temperatura/humedad (default: últimos 7 días) |
| GET | `/historico/luz?start=YYYY-MM-DD&end=YYYY-MM-DD` | Historial de luz (default: últimos 7 días) |

Todas las respuestas son JSON y habilitan CORS (`Access-Control-Allow-Origin: *`).

---

## 📡 ESP32

- Publica cada 60 segundos la lectura de temperatura/humedad (leída por Serial desde el DHT11) al tópico `MQTT_TOPIC1`, con el payload `"<temp>,<hum>"`.
- Publica al tópico `MQTT_TOPIC2` (`"true"`/`"false"`) cuando detecta un cambio de umbral de luz ambiente (>200 / ≤200) medido con el APDS9960.
- Expone el stream MJPEG de la cámara en `http://<ip-esp32>:81/stream`.
- Reconecta WiFi y MQTT automáticamente ante caídas de conexión.

---

## 🖥️ Frontend

- **`index.html`**: dashboard con tarjetas de temperatura, humedad y luz actualizadas periódicamente, más el stream de la cámara.
- **`historial.html`**: consulta el histórico con filtro de rango de fechas y tipo de sensor.
- Ambas vistas consumen `API_BASE_URL` y `CAMERA_STREAM_URL` definidos en `web/js/config.js`.

---

## 📝 Notas y pendientes

- Las variables de entorno/config (credenciales de WiFi, broker MQTT, base de datos, URLs del frontend) están dejadas como placeholders a propósito — quedan pendientes de completar según el entorno de despliegue.
- No hay creación automática de esquema de base de datos; las tablas deben crearse a mano antes de levantar el backend.
- El proyecto no incluye tests automatizados por el momento (`src/test` está vacío).