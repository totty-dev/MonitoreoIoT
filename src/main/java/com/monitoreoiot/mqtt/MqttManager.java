package com.monitoreoiot.mqtt;

import com.monitoreoiot.config.Config;
import com.monitoreoiot.db.DataBaseManager;
import org.eclipse.paho.client.mqttv3.*;

public class MqttManager {
    private final MqttClient mqttClient;
    private final DataBaseManager db;

    public MqttManager(DataBaseManager db) throws MqttException {
        String mqttIp = Config.getMqttIp();
        String mqttPort = Config.getMqttPort();

        if (mqttIp == null || mqttIp.isEmpty()) {
            throw new IllegalArgumentException("❌ MQTT_IP no está definida. Revisa tu archivo .env");
        }

        String brokerUrl = "tcp://" + mqttIp + ":" + mqttPort;
        System.out.println("✅ Conectando a broker MQTT: " + brokerUrl);

        String mqttClientid = MqttClient.generateClientId();
        this.mqttClient = new MqttClient(brokerUrl, mqttClientid);
        this.db = db;
    }

    public void conect() {
        try {
            MqttConnectOptions mqttOptions = new MqttConnectOptions();
            mqttOptions.setKeepAliveInterval(60);
            mqttOptions.setAutomaticReconnect(true);
            mqttOptions.setCleanSession(true);
            mqttClient.connect(mqttOptions);
            System.out.println("✅ Conectado a MQTT broker");
            this.callback();
        } catch (MqttException e) {
            System.out.println("❌ Error al conectar MQTT: " + e.getMessage());
        }
    }

    public void callback() {
        if (mqttClient.isConnected()) {
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String msg = new String(message.getPayload());
                    System.out.println("📩 Received message: " + msg);
                    if (topic.equals(Config.getMqttTopic1())) {
                        String[] tempyhum = msg.split(",");
                        float temp = Float.parseFloat(tempyhum[0]);
                        float hum = Float.parseFloat(tempyhum[1]);
                        db.insertTempYHum(temp, hum);
                    }
                    if (topic.equals(Config.getMqttTopic2())) {
                        db.insertLuz(msg);
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("⚠️ Conexión MQTT perdida: " + cause.getMessage());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("✅ Mensaje publicado correctamente");
                }
            });
        }
    }

    public void subscribe(String topic, int qos) throws MqttException {
        mqttClient.subscribe(topic, qos);
        System.out.println("✅ Suscrito a tópico: " + topic + " (QoS " + qos + ")");
    }

    public void disconnect() throws MqttException {
        mqttClient.disconnect();
        mqttClient.close();
        System.out.println("✅ Desconectado de MQTT");
    }
}