package com.monitoreoiot;
import com.monitoreoiot.mqtt.MqttManager;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.io.IOException;


public class Main {
    public static void main(String[] args) {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
            try {
                MqttManager mqtt = new MqttManager();
                mqtt.conect();
                MqttMessage mqttMsg = new MqttMessage("Hola".getBytes());
                mqttMsg.setQos(0);
                mqtt.mqttClient.publish("temperaturayhumedadcar", mqttMsg);
                mqtt.mqttClient.subscribe("temperaturayhumedadcar", 0);
                System.out.println("Press Enter to disconnect");
                System.in.read();
                mqtt.mqttClient.disconnect();
                mqtt.mqttClient.close();

            } catch (MqttException e) {
                System.out.println("Error Mqtt: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("Error IO: " + e.getMessage());
            }
    }
}