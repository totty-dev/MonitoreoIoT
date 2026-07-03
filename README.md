# 🌦️ MonitoreoIoT — Estación Ambiental IoT

Sistema de monitoreo ambiental compuesto por un **ESP32-CAM**, un **backend en Java**, un broker **MQTT**, una base de datos **PostgreSQL** y un **frontend web**. Registra temperatura, humedad y nivel de luz, expone los datos mediante una API REST propia y permite ver el historial con filtros por fecha, además de un stream de cámara en vivo.

Toda la configuración (credenciales de DB, broker MQTT, puertos, URLs del frontend) se maneja por **variables de entorno**, sin nada hardcodeado en el código — pensado para desplegarse como Stack en Portainer.

---

## 📋 Tabla de contenidos

- [Arquitectura](#-arquitectura)
- [Estructura del proyecto](#-estructura-del-proyecto)
- [Stack tecnológico](#-stack-tecnológico)
- [Requisitos previos](#-requisitos-previos)
- [Configuración por variables de entorno](#-configuración-por-variables-de-entorno)
- [Despliegue](#-despliegue)
    - [Con Docker Compose](#con-docker-compose)
    - [Como Stack en Portainer](#como-stack-en-portainer)
    - [Manual / desarrollo local](#manual--desarrollo-local)
- [Cómo se arma el config.js del frontend](#-cómo-se-arma-el-configjs-del-frontend)
- [Base de datos](#-base-de-datos)
- [API REST](#-api-rest)
- [ESP32](#-esp32)
- [Notas y pendientes](#-notas-y-pendientes)

---

## 🧩 Arquitectura

```
ESP32-CAM (DHT11 + APDS9960 + cámara OV2640)
        │  publica por MQTT (temp/hum y luz)
        │  sirve stream MJPEG en puerto 81
        ▼
Broker MQTT (externo, ej. Mosquitto)
        │
        ▼
Backend Java (suscriptor MQTT + servidor HTTP embebido)
        │  guarda lecturas
        ▼
PostgreSQL (externo) — tablas `clima` y `luz`
        ▲
        │  consultas vía API REST
        │
Frontend web (Nginx, config.js generado en runtime)
```

- **ESP32-CAM**: lee temperatura/humedad (DHT11) y luz ambiente (APDS9960), publica por MQTT y expone el stream de video en `:81/stream`. Firmware totalmente aparte del stack Docker (se flashea desde el Arduino IDE).
- **Backend (Java 17 + Maven)**: se suscribe a los tópicos MQTT con Eclipse Paho, persiste las lecturas en PostgreSQL y expone una API REST propia con `com.sun.net.httpserver.HttpServer` (sin frameworks externos). Toda su config sale de variables de entorno.
- **PostgreSQL**: instancia externa (no corre dentro del stack), con dos tablas: `clima` (temperatura/humedad) y `luz`.
- **Broker MQTT**: externo también (ej. Mosquitto en el host o en otro contenedor), el backend solo necesita su URL.
- **Frontend**: dashboard y vista de historial en HTML/CSS/JS vanilla servidos con Nginx. Su `config.js` **no es un archivo estático fijo**: se genera al iniciar el contenedor a partir de variables de entorno (ver [sección dedicada](#-cómo-se-arma-el-configjs-del-frontend)).
- **Docker Compose**: levanta únicamente `backend` y `frontend`; DB y broker MQTT quedan afuera del stack, apuntados por IP/URL.

---

## 📁 Estructura del proyecto

```
MonitoreoIoT/
├── docker-compose.yml
├── backend.Dockerfile
├── frontend.Dockerfile
├── .env.example                          # referencia de todas las variables a completar
├── pom.xml
├── docker/
│   └── frontend/
│       ├── config.js.template            # template con placeholders ${VAR}
│       └── generate-config.sh            # genera config.js real al iniciar el contenedor
├── src/
│   └── main/
│       └── java/com/monitoreoiot/
│           ├── MonitorIoT.java           # entry point + servidor HTTP
│           ├── config/Config.java        # lee toda la config directo de variables de entorno
│           ├── db/DataBaseManager.java   # acceso a PostgreSQL
│           └── mqtt/MqttManager.java     # cliente/suscriptor MQTT
├── esp32/
│   └── mqttpublisher/
│       ├── mqttpublisher.ino             # firmware ESP32-CAM
│       └── config.h                      # credenciales WiFi / MQTT del dispositivo (edición manual)
└── web/
    ├── index.html                        # dashboard
    ├── historial.html                    # historial con filtros
    ├── css/style.css
    └── js/
        ├── config.js                     # se reemplaza en runtime dentro del contenedor, ver nota
        ├── script.js
        ├── camera.js
        └── historial.js
```

---

## 🛠️ Stack tecnológico

| Capa       | Tecnología |
|------------|------------|
| Firmware   | ESP32-CAM (Arduino, `WiFi.h`, `PubSubClient`, `SparkFun_APDS9960`, `esp_camera`) |
| Mensajería | MQTT (broker externo, ej. Mosquitto) |
| Backend    | Java 17, Maven, Eclipse Paho MQTT Client, JDBC (driver PostgreSQL), `HttpServer` nativo de la JDK |
| Base de datos | PostgreSQL (externa) |
| Frontend   | HTML5, CSS3, JavaScript vanilla, Nginx (alpine) con templating vía `envsubst` |
| Infraestructura | Docker + Docker Compose / Portainer (deploy tipo Stack desde repositorio) |

---

## ✅ Requisitos previos

- Docker y Docker Compose (o Portainer, para el despliegue como Stack)
- Java 17 y Maven (solo si vas a compilar/correr el backend sin Docker)
- Un broker MQTT accesible (Mosquitto, por ejemplo)
- Una instancia de PostgreSQL con las tablas `clima` y `luz` ya creadas (ver [Base de datos](#-base-de-datos))
- Placa ESP32-CAM (AI-Thinker) con sensor DHT11 y APDS9960, y el IDE de Arduino con las librerías `PubSubClient` y `SparkFun_APDS9960` instaladas

---

## ⚙️ Configuración por variables de entorno

Todas las variables están listadas en `.env.example`. Ninguna tiene valor real cargado en el repo — se completan al desplegar (en Portainer, en la sección **Environment variables** del Stack; con Docker Compose local, en un archivo `.env` en la raíz).

```env
# ---- PostgreSQL (externo) ----
DB_URL=jdbc:postgresql://<ip-postgres>:5432/<nombre-db>
DB_USER=
DB_PASSWORD=

# ---- MQTT (externo) ----
MQTT_BROKER=tcp://<ip-broker>:1883
MQTT_TOPIC1=
MQTT_TOPIC2=
MQTT_QOS=

# ---- Backend ----
BACKEND_PORT=
BACKEND_IP=0.0.0.0
BACKEND_CONTEXT_PATH=

# ---- Frontend ----
FRONT_PORT=
ESP32_IP=
```

`Config.java` lee **todo directo de variables de entorno**, ya no existe `config.properties`. Casi todas son obligatorias (si falta una, el backend rompe al arrancar), salvo dos que tienen default hardcodeado en el código si no las seteás:

- `BACKEND_IP` → default `0.0.0.0`
- `MQTT_QOS` → default `0`

> El firmware del ESP32 (`esp32/mqttpublisher/config.h`) queda **fuera** de este esquema: no es un contenedor, así que sus variables (`WIFI_SSID`, `WIFI_PASSWORD`, `MQTT_IP`, `MQTT_TOPIC1`, `MQTT_TOPIC2`) se siguen editando a mano en el archivo antes de compilar y flashear el firmware.

---

## 🚀 Despliegue

### Con Docker Compose

1. Renombrá `.env.example` a `.env`.
2. Completá **todas** las variables del `.env` con tus valores reales (DB, MQTT, puertos, IPs).
3. Ejecutá:
   ```bash
   docker compose up -d --build
   ```
4. Listo. Docker Compose lee el `.env` solo (no hace falta pasarle nada aparte) y levanta backend + frontend con esos valores ya inyectados.

- Backend queda en `http://localhost:<BACKEND_PORT>`
- Frontend queda en `http://localhost:<FRONT_PORT>`

### Como Stack en Portainer

1. **Stacks → Add stack**.
2. Ponele un nombre y elegí **Repository** como build method (no "Web editor", porque necesita clonar `docker/` y los Dockerfiles del repo).
3. Completá:
    - Repository URL: la URL del repo
    - Reference: `refs/heads/main`
    - Compose path: `docker-compose.yml`
4. Bajá hasta la sección **Environment variables** y ahí cargá, una por una, cada variable que figura en `.env.example` (nombre y valor real) — es el equivalente a llenar el `.env` pero desde la interfaz de Portainer.
5. Apretá **Deploy the stack**. Portainer se encarga de clonar el repo, buildear las imágenes e inyectar esas variables — no hay ningún otro paso manual.

Si después necesitás cambiar algún valor: volvé al stack → editá las Environment variables → **Update the stack**. Los contenedores no se actualizan solos, hay que redeployar.

### Manual / desarrollo local (sin Docker)

**Backend**: sí podés reusar el mismo `.env` — `Config.java` lee las variables de entorno igual que en Docker, no hace falta duplicar nada a mano.

1. Exportá las variables del `.env` en tu terminal:
   ```bash
   export $(grep -v '^#' .env | xargs)
   ```
2. Compilá y corré el backend:
   ```bash
   mvn clean package -DskipTests
   java -jar target/app.jar
   ```

**Frontend**: acá el `.env` **no sirve** — `config.js` es un archivo estático que carga el navegador, y el reemplazo automático con `envsubst` solo ocurre dentro del contenedor Docker al iniciar. Sin Docker de por medio, ese paso nunca se ejecuta.

3. Completá a mano `web/js/config.js` con las IPs/puertos reales.
4. Abrí los `.html` de `web/` directo en el navegador, o serví esa carpeta con cualquier servidor estático.

---

## 🧩 Cómo se arma el `config.js` del frontend

El frontend necesita saber la IP del backend y del ESP32 sin tener esos valores fijos en el código. Para lograrlo sin agregar un framework, se usa `envsubst`:

1. `docker/frontend/config.js.template` tiene placeholders:
   ```js
   window.APP_CONFIG = {
       CAMERA_STREAM_URL: "http://${ESP32_IP}:81/stream",
       API_BASE_URL: "http://${BACKEND_IP}:${BACKEND_PORT}",
   };
   ```
2. `frontend.Dockerfile` copia ese template a la imagen y registra `docker/frontend/generate-config.sh` en `/docker-entrypoint.d/`, que es una carpeta que la imagen oficial de `nginx:alpine` ejecuta automáticamente al iniciar el contenedor (antes de levantar Nginx).
3. Al arrancar el contenedor, ese script corre `envsubst` y genera el `web/js/config.js` real, reemplazando `${ESP32_IP}`, `${BACKEND_IP}` y `${BACKEND_PORT}` por los valores que le llegaron como variables de entorno del servicio `frontend` en el `docker-compose.yml`.

> Nota: el `web/js/config.js` que está versionado en el repo (con los placeholders sin reemplazar) solo sirve como plantilla de referencia — dentro del contenedor se sobreescribe siempre al arrancar. Si abrís los `.html` directo en el navegador sin pasar por Docker, vas a ver esos placeholders literales; para probar el frontend suelto localmente conviene completar ese archivo a mano con IPs reales.

---

## 🗄️ Base de datos

El backend asume un esquema PostgreSQL con dos tablas (no se crean automáticamente, hay que provisionarlas):

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

---

## 🔌 API REST

Todas las rutas cuelgan de `BACKEND_CONTEXT_PATH` (vacío por defecto).

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
- Configuración (`WIFI_SSID`, `WIFI_PASSWORD`, `MQTT_IP`, `MQTT_PORT`, `MQTT_TOPIC1`, `MQTT_TOPIC2`) en `esp32/mqttpublisher/config.h`, a completar antes de compilar el firmware.

---

## 📝 Notas y pendientes

- El `.gitignore` del repo tiene contenido pegado por error (texto/markdown ajeno a reglas de gitignore) al principio del archivo — conviene limpiarlo para que no genere reglas de ignorado involuntarias.
- No hay creación automática de esquema de base de datos; las tablas deben crearse a mano antes de levantar el backend.
- El proyecto no incluye tests automatizados por el momento (`src/test` está vacío).
- PostgreSQL y el broker MQTT quedan fuera del stack Docker a propósito (se asumen ya desplegados aparte); si en algún momento se quieren containerizar también, hay que sumarlos como servicios nuevos en `docker-compose.yml` y ajustar `DB_URL`/`MQTT_BROKER` para que apunten al nombre del servicio en vez de una IP externa.