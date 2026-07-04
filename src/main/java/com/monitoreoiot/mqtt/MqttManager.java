package com.monitoreoiot.mqtt;

import com.monitoreoiot.config.Config;
import com.monitoreoiot.db.DataBaseManager;
import org.eclipse.paho.client.mqttv3.*;

public class MqttManager {
    private final MqttClient mqttClient;
    private final DataBaseManager db;

    public MqttManager(DataBaseManager db){
        String brokerUrl = getBrokerUrl();
        System.out.println("✅ Conectando a broker MQTT: " + brokerUrl);

        String mqttClientid = MqttClient.generateClientId();
        try{
            this.mqttClient = new MqttClient(brokerUrl, mqttClientid);
        }catch (MqttException e) {
            throw new RuntimeException("❌ Error al crear el cliente MQTT: " + e.getMessage(), e);
        }
        this.db = db;
    }

    private static String getBrokerUrl(){
        String mqttIp = Config.getMqttIp();
        String mqttPort = Config.getMqttPort();

        if ((mqttIp == null || mqttIp.isEmpty()) && (mqttPort == null || mqttPort.isEmpty())) {
            throw new IllegalArgumentException("❌ MQTT_IP y MQTT_PORT no están definidas. Revisa tu archivo .env");
        }
        if (mqttIp == null || mqttIp.isEmpty()) {
            throw new IllegalArgumentException("❌ MQTT_IP no está definida. Revisa tu archivo .env");
        }
        if (mqttPort == null || mqttPort.isEmpty()) {
            throw new IllegalArgumentException("❌ MQTT_PORT no está definida. Revisa tu archivo .env");
        }
        return "tcp://" + mqttIp + ":" + mqttPort;
    }

    public void conect() {
        MqttConnectOptions mqttOptions = new MqttConnectOptions();
        mqttOptions.setKeepAliveInterval(60);
        mqttOptions.setAutomaticReconnect(true);
        mqttOptions.setCleanSession(true);
        try{
            mqttClient.connect(mqttOptions);
            System.out.println("✅ Conectado a MQTT broker");
            this.callback();
        }catch (MqttException e) {
            System.err.println("❌ Error al conectar MQTT: " + e.getMessage());
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

    public void subscribe(String topic, int qos){
        try{
            if (!mqttClient.isConnected()) {
                System.out.println("⚠️ No se puede suscribir, el cliente MQTT no está conectado.");
            }else {
                mqttClient.subscribe(topic, qos);
                System.out.println("✅ Suscrito a tópico: " + topic + " (QoS " + qos + ")");
            }
        }catch (MqttException e) {
            System.out.println("❌ Error al suscribirse al topic:" + topic + " del MQTT: " + e.getMessage());
        }
    }

    public void disconnect(){
        try{
            if (mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
                System.out.println("✅ Desconectado de MQTT");
            }
        }catch (MqttException e) {
            System.out.println("❌ Error al desconectar MQTT: " + e.getMessage());
        }
    }
}