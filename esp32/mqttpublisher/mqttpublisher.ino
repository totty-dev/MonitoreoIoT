#include <WiFi.h>
#include <PubSubClient.h>
#include <Wire.h>
#include <SparkFun_APDS9960.h>
#include "esp_camera.h"
#include "esp_http_server.h"
#include "config.h"

const char* ssid        = WIFI_SSID;
const char* password    = WIFI_PASSWORD;
const char* mqtt_server = MQTT_HOST;
const int   mqtt_port   = MQTT_PORT;
const char* topic_tempyhum = MQTT_TOPIC1;
const char* topic_luz      = MQTT_TOPIC2;

#define SDA_PIN 12
#define SCL_PIN 13

// ---- Pines cámara AI-Thinker ----
#define PWDN_GPIO_NUM     32
#define RESET_GPIO_NUM    -1
#define XCLK_GPIO_NUM      0
#define SIOD_GPIO_NUM     26
#define SIOC_GPIO_NUM     27
#define Y9_GPIO_NUM       35
#define Y8_GPIO_NUM       34
#define Y7_GPIO_NUM       39
#define Y6_GPIO_NUM       36
#define Y5_GPIO_NUM       21
#define Y4_GPIO_NUM       19
#define Y3_GPIO_NUM       18
#define Y2_GPIO_NUM        5
#define VSYNC_GPIO_NUM    25
#define HREF_GPIO_NUM     23
#define PCLK_GPIO_NUM     22

SparkFun_APDS9960 apds = SparkFun_APDS9960();
uint16_t ultimaLuz = 0;
uint16_t luz = 0;

unsigned long lastTempPublish      = 0;
unsigned long lastReconnectAttempt = 0;
const unsigned long TEMP_INTERVAL = 60000;

WiFiClient espClient;
PubSubClient client(espClient);

httpd_handle_t stream_httpd = NULL;

#define PART_BOUNDARY "123456789000000000000987654321"
static const char* STREAM_CONTENT_TYPE = "multipart/x-mixed-replace;boundary=" PART_BOUNDARY;
static const char* STREAM_BOUNDARY = "\r\n--" PART_BOUNDARY "\r\n";
static const char* STREAM_PART = "Content-Type: image/jpeg\r\nContent-Length: %u\r\n\r\n";

static esp_err_t stream_handler(httpd_req_t *req) {
  camera_fb_t * fb = NULL;
  esp_err_t res = ESP_OK;
  char part_buf[64];

  res = httpd_resp_set_type(req, STREAM_CONTENT_TYPE);
  if (res != ESP_OK) return res;

  while (true) {
    fb = esp_camera_fb_get();
    if (!fb) {
      res = ESP_FAIL;
    } else {
      if (fb->format != PIXFORMAT_JPEG) {
        res = ESP_FAIL;
      } else {
        size_t hlen = snprintf(part_buf, 64, STREAM_PART, fb->len);
        if (res == ESP_OK) res = httpd_resp_send_chunk(req, STREAM_BOUNDARY, strlen(STREAM_BOUNDARY));
        if (res == ESP_OK) res = httpd_resp_send_chunk(req, part_buf, hlen);
        if (res == ESP_OK) res = httpd_resp_send_chunk(req, (const char *)fb->buf, fb->len);
      }
    }
    if (fb) {
      esp_camera_fb_return(fb);
      fb = NULL;
    }
    if (res != ESP_OK) break;
  }
  return res;
}

void startCameraServer() {
  httpd_config_t config = HTTPD_DEFAULT_CONFIG();
  config.server_port = 81;
  config.ctrl_port   = 32768;

  httpd_uri_t stream_uri = {
    .uri      = "/stream",
    .method   = HTTP_GET,
    .handler  = stream_handler,
    .user_ctx = NULL
  };

  if (httpd_start(&stream_httpd, &config) == ESP_OK) {
    httpd_register_uri_handler(stream_httpd, &stream_uri);
  }
}

bool initCamera() {
  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer   = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM;
  config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM;
  config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM;
  config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM;
  config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk  = XCLK_GPIO_NUM;
  config.pin_pclk  = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM;
  config.pin_href  = HREF_GPIO_NUM;
  config.pin_sscb_sda = SIOD_GPIO_NUM;
  config.pin_sscb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn  = PWDN_GPIO_NUM;
  config.pin_reset = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.pixel_format = PIXFORMAT_JPEG;

  if (psramFound()) {
    config.frame_size   = FRAMESIZE_CIF;   // 400x296 - liviano
    config.jpeg_quality = 15;              // más alto = menos calidad = menos peso
    config.fb_count     = 2;
  } else {
    config.frame_size   = FRAMESIZE_QVGA;  // 320x240
    config.jpeg_quality = 18;
    config.fb_count     = 1;
  }

  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    Serial.printf(">> Error iniciando cámara: 0x%x\n", err);
    return false;
  }
  return true;
}

void conectarWiFi() {
  WiFi.begin(ssid, password);
  Serial.print("Conectando WiFi");
  int intentos = 0;
  while (WiFi.status() != WL_CONNECTED && intentos < 20) {
    delay(500);
    Serial.print(".");
    intentos++;
  }
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\n>> WiFi OK: " + WiFi.localIP().toString());
  } else {
    Serial.println("\n>> WiFi FALLÓ");
  }
}

bool reconnectMQTT() {
  if (millis() - lastReconnectAttempt < 5000) return false;
  lastReconnectAttempt = millis();
  if (client.connect("ESP32CAM_DHT11")) return true;
  return false;
}

void publicarTempyHum(String data) {
  data.trim();
  int idxR  = data.indexOf("R:");
  int idxRH = data.indexOf("RH");
  int idxSp = data.indexOf(" ");
  int idxC  = data.indexOf("C");
  if (idxR == -1 || idxRH == -1 || idxSp == -1 || idxC == -1) return;

  String humStr  = data.substring(idxR + 2, idxRH);
  String tempStr = data.substring(idxSp + 1, idxC);
  float humedad     = humStr.toFloat();
  float temperatura = tempStr.toFloat();

  String payload = String(temperatura) + "," + String(humedad);
  client.publish(topic_tempyhum, payload.c_str());
}

void setup() {
  Serial.begin(9600);

  conectarWiFi();
  client.setServer(mqtt_server, mqtt_port);
  client.connect("ESP32CAM_DHT11");

  delay(500);
  Wire.begin(SDA_PIN, SCL_PIN);
  delay(500);

  apds.init();
  apds.enableLightSensor(false);
  Serial.println(">> APDS9960 listo");

  if (initCamera()) {
    startCameraServer();
    Serial.println(">> Stream disponible en:");
    Serial.println("   http://" + WiFi.localIP().toString() + ":81/stream");
  }

  lastTempPublish = millis();
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    WiFi.reconnect();
    delay(3000);
    return;
  }

  if (!client.connected()) {
    reconnectMQTT();
    return;
  }
  client.loop();

  unsigned long now = millis();

  if (now - lastTempPublish >= TEMP_INTERVAL) {
    lastTempPublish = now;
    String data = "";
    while (Serial.available()) {
      data = Serial.readStringUntil('\n');
    }
    if (data.length() > 0) publicarTempyHum(data);
  }

  apds.readAmbientLight(luz);

  if ((luz > 200) && (ultimaLuz <= 200)) {
    ultimaLuz = luz;
    client.publish(topic_luz, "true");
  }
  if ((luz <= 200) && (ultimaLuz > 200)) {
    ultimaLuz = luz;
    client.publish(topic_luz, "false");
  }
}