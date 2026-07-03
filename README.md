# 🌦️ Monitoreo IoT - Estación Ambiental

Sistema completo de monitoreo ambiental con **ESP32**, **backend Java**, **frontend web**, **MQTT** y **PostgreSQL**.  
Mide **temperatura, humedad y luz**, muestra datos en tiempo real y guarda historial con filtros.

![Dashboard](docs/dashboard.png)
![Historial](docs/historial.png)

---

## 📋 Tabla de Contenidos

- [Características](#-características)
- [Arquitectura](#-arquitectura)
- [Requisitos previos](#-requisitos-previos)
- [Configuración](#-configuración)
    - [1. Clonar el repositorio](#1-clonar-el-repositorio)
    - [2. Variables de entorno](#2-variables-de-entorno)
    - [3. Configurar el ESP32](#3-configurar-el-esp32)
    - [4. Configurar el Frontend](#4-configurar-el-frontend)
    - [5. Base de datos](#5-base-de-datos)
- [Despliegue con Docker](#-despliegue-con-docker)
- [Uso del Frontend](#-uso-del-frontend)
- [Endpoints API](#-endpoints-api)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Personalización](#-personalización)
- [Solución de problemas](#-solución-de-problemas)
- [Autor](#-autor)
- [Licencia](#-licencia)

---

## ✨ Características

- 📡 **ESP32**: publica datos de temperatura, humedad y luz vía MQTT.
- 🎥 **Stream de cámara**: visualiza en tiempo real desde el ESP32-CAM.
- 📊 **Dashboard**: tarjetas con valores actualizados cada 5 segundos.
- 📜 **Historial**: filtro por rango de fechas y tipo de sensor (temp/hum o luz).
- 🗄️ **Base de datos**: PostgreSQL con tablas `clima` y `luz`.
- 🐳 **Docker**: despliegue fácil con `docker-compose`.
- 📱 **Responsive**: adaptable a móviles y tablets.

---

## 🧩 Arquitectura

- **ESP32**: publica datos MQTT (temp/hum y luz) y stream de cámara en puerto 81.
- **Broker MQTT**: Mosquitto (puede estar en el host o en otro contenedor).
- **Backend**: Java con Maven, suscribe a MQTT, guarda en PostgreSQL y expone API REST.
- **Frontend**: HTML/CSS/JS puro, consume API y muestra el stream de la cámara.
- **Base de datos**: PostgreSQL con tablas `clima` y `luz`.

---

## 📋 Requisitos previos

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

