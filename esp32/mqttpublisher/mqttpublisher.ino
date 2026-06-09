#include <WiFi.h>
#include <PubSubClient.h>
#include <Wire.h>
#include <SparkFun_APDS9960.h>

const char* ssid        = "wifitotty2";
const char* password    = "tomas3342";
const char* mqtt_server = "192.168.1.107";
const int   mqtt_port   = 1883;
const char* topic_tempyhum = "tempyhum";
const char* topic_luz      = "luz";

#define SDA_PIN 12
#define SCL_PIN 13

SparkFun_APDS9960 apds = SparkFun_APDS9960();
uint16_t ultimaLuz = 0;
uint16_t luz = 0;

unsigned long lastTempPublish      = 0;
unsigned long lastReconnectAttempt = 0;

const unsigned long TEMP_INTERVAL = 60000;

WiFiClient espClient;
PubSubClient client(espClient);

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