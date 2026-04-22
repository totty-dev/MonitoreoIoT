package com.monitoreoiot.mqtt;

import com.monitoreoiot.db.DataBaseManager;
import com.monitoreoiot.model.Humedad;
import com.monitoreoiot.model.Temperatura;
import org.eclipse.paho.client.mqttv3.*;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

public class MqttManager{
    private final String mqttBroker;
    private final String mqttClientid;
    public MqttClient mqttClient;
    private final DataBaseManager db = new DataBaseManager();

    public MqttManager(){
        Properties props = new Properties();
        try (InputStream input = MqttManager.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar config.properties", e);
        }
        this.mqttBroker = props.getProperty("mqtt.broker");
        this.mqttClientid = MqttClient.generateClientId();
    }

    public void conect(){
        try {
            MqttClient mqttClient = new MqttClient(mqttBroker, mqttClientid);
            MqttConnectOptions mqttOptions = new MqttConnectOptions();
            mqttOptions.setKeepAliveInterval(60);
            mqttOptions.setAutomaticReconnect(true);
            mqttOptions.setCleanSession(true);
            mqttClient.connect(mqttOptions);
            this.mqttClient = mqttClient;
            this.callback();
        } catch (MqttException e) {
            System.out.println("Error al insertar en DB: " + e.getMessage());
        }
    }

    public void callback(){
        if (mqttClient.isConnected()) {
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String msg = new String(message.getPayload());
                    System.out.println("Received message: " + msg);
                    if (topic.equals("temperaturayhumedadcar")) {
                        String[] tempyhum = msg.split(",");
                        Temperatura temp = new Temperatura(Float.parseFloat(tempyhum[0]));
                        Humedad hum = new Humedad(Float.parseFloat(tempyhum[1]));
                        try {
                            db.insertTempyHum(temp,hum);
                        } catch (SQLException e) {
                            System.out.println("Error al insertar en DB: " + e.getMessage());
                        }
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
}
