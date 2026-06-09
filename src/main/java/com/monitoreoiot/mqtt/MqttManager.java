package com.monitoreoiot.mqtt;

import com.monitoreoiot.db.DataBaseManager;
import com.monitoreoiot.model.Humedad;
import com.monitoreoiot.model.Temperatura;
import org.eclipse.paho.client.mqttv3.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MqttManager{
    private final MqttClient mqttClient;
    private final MqttMessage mqttMsg;
    private final DataBaseManager db;

    public MqttManager() throws MqttException{
        Properties props = new Properties();
        try (InputStream input = MqttManager.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar config.properties", e);
        }
        String mqttBroker = props.getProperty("mqtt.broker");
        String mqttClientid = MqttClient.generateClientId();
        this.mqttClient = new MqttClient(mqttBroker, mqttClientid);
        this.db = new DataBaseManager();
        this.mqttMsg = new MqttMessage();
    }

    public MqttManager(DataBaseManager db) throws MqttException{
        Properties props = new Properties();
        try (InputStream input = MqttManager.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar config.properties", e);
        }
        String mqttBroker = props.getProperty("mqtt.broker");
        String mqttClientid = MqttClient.generateClientId();
        this.mqttClient = new MqttClient(mqttBroker, mqttClientid);
        this.db = db;
        this.mqttMsg = new MqttMessage();
    }

    public void conect(){
        try {
            MqttConnectOptions mqttOptions = new MqttConnectOptions();
            mqttOptions.setKeepAliveInterval(60);
            mqttOptions.setAutomaticReconnect(true);
            mqttOptions.setCleanSession(true);
            mqttClient.connect(mqttOptions);
            this.callback();
        } catch (MqttException e) {
            System.out.println("Error al conectar mqtt: " + e.getMessage());
        }
    }

    public void callback(){
        if (mqttClient.isConnected()) {
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String msg = new String(message.getPayload());
                    System.out.println("Received message: " + msg);
                    if (topic.equals("tempyhum")) {
                        String[] tempyhum = msg.split(",");
                        Temperatura temp = new Temperatura(Float.parseFloat(tempyhum[0]));
                        Humedad hum = new Humedad(Float.parseFloat(tempyhum[1]));
                        db.insertTempyHum(temp,hum);
                    }
                    if (topic.equals("luz")) {
                        db.insertLuz(msg);
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection is lost: " + cause.getMessage());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message publish is complete: " + token.isComplete());
                }
            });
        }
    }

    public void subscribe(String topic, int qos) throws MqttException{
        mqttClient.subscribe(topic, qos);
    }

    public void publish(String topic,String msg, int qos) throws MqttException {
        mqttMsg.setPayload(msg.getBytes());
        mqttMsg.setQos(qos);
        mqttClient.publish(topic, mqttMsg);
    }

    public void disconnect() throws MqttException{
        mqttClient.disconnect();
        mqttClient.close();
    }
}
