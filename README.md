
- **ESP32**: publica datos MQTT (temp/hum y luz) y stream de cámara.
- **Broker MQTT**: Mosquitto (puede estar en el mismo host o en otro).
- **Backend**: Java con Maven, suscribe a MQTT, guarda en PostgreSQL y expone API REST.
- **Frontend**: HTML/CSS/JS puro, consume API y muestra el stream de la cámara.
- **Base de datos**: PostgreSQL con tablas `clima` y `luz`.

---

## 🚀 Requisitos previos

- **Docker** y **Docker Compose** (para despliegue fácil)
- **Java 17** (si compilas localmente)
- **Maven** (si compilas localmente)
- **Mosquitto MQTT broker** (puede ser en otro host)
- **PostgreSQL** (puede ser en otro host)
- **ESP32** con cámara OV2640 y sensores DHT11 + APDS9960

---

## ⚙️ Configuración

### 1. Clonar el repositorio

```bash
git clone https://github.com/totty-dev/MonitoreoIoT.git
cd MonitoreoIoT