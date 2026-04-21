package com.monitoreoiot;
import com.monitoreoiot.db.DataBase;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttClient;
import java.io.IOException;
import java.sql.SQLException;


public class Main {
    public static void main(String[] args) {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
        String mqttBroker = "tcp://broker.hivemq.com:1883";
            try {
                MqttClient mqttClient = new MqttClient(mqttBroker, MqttClient.generateClientId());
                MqttConnectOptions mqttOptions = new MqttConnectOptions();
                mqttOptions.setKeepAliveInterval(60);
                mqttOptions.setAutomaticReconnect(true);
                mqttOptions.setCleanSession(true);
                mqttClient.connect(mqttOptions);

                if (mqttClient.isConnected()) {
                    mqttClient.setCallback(new MqttCallback() {
                        @Override
                        public void messageArrived(String topic, MqttMessage message) throws Exception {
                            String msg = new String(message.getPayload());
                            System.out.println("Received message: " + msg);
                            if (topic.equals("temperaturayhumedadcar")) {
                                String[] tempyhum = msg.split(",");
                                DataBase db = new DataBase();
                                try {
                                    db.insertarTemperatura(Integer.parseInt(tempyhum[0]), Integer.parseInt(tempyhum[1]));
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
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

                    /*MqttMessage mqttMsg = new MqttMessage("Hola".getBytes());
                    mqttMsg.setQos(0);
                    mqttClient.publish("temperaturayhumedadcar", mqttMsg);*/
                    mqttClient.subscribe("temperaturayhumedadcar", 0);
                }

                /* Keep the application open, so that the subscribe operation can tested */
                System.out.println("Press Enter to disconnect");
                System.in.read();
                /* Proceed with disconnecting */
                mqttClient.disconnect();
                mqttClient.close();

            } catch (MqttException e) {
                e.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
    }
}