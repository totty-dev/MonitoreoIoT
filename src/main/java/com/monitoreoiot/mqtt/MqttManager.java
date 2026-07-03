package com.monitoreoiot.mqtt;

import com.monitoreoiot.config.Config;
import com.monitoreoiot.db.DataBaseManager;
import org.eclipse.paho.client.mqttv3.*;

public class MqttManager{
    private final MqttClient mqttClient;
    private final DataBaseManager db;

    public MqttManager(DataBaseManager db) throws MqttException{
        String mqttBroker = Config.getMqttBroker();
        String mqttClientid = MqttClient.generateClientId();
        this.mqttClient = new MqttClient(mqttBroker, mqttClientid);
        this.db = db;
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
                public void messageArrived(String topic, MqttMessage message){
                    String msg = new String(message.getPayload());
                    System.out.println("Received message: " + msg);
                    if (topic.equals(Config.getMqttTopic1())) {
                        String[] tempyhum = msg.split(",");
                        float temp = Float.parseFloat(tempyhum[0]);
                        float hum = Float.parseFloat(tempyhum[1]);
                        db.insertTempYHum(temp,hum);
                    }
                    if (topic.equals(Config.getMqttTopic2())) {
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

    public void disconnect() throws MqttException{
        mqttClient.disconnect();
        mqttClient.close();
    }
}
