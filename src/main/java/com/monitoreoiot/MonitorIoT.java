package com.monitoreoiot;

import com.monitoreoiot.db.DataBaseManager;
import com.monitoreoiot.mqtt.MqttManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Properties;


public class MonitorIoT {
    public static void main(String[] args) {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
        try {
            DataBaseManager db = new DataBaseManager();

            Properties props = new Properties();
            try (InputStream input = DataBaseManager.class.getClassLoader()
                    .getResourceAsStream("config.properties")) {
                props.load(input);
            } catch (IOException e) {
                throw new RuntimeException("No se pudo cargar config.properties", e);
            }
            int port = Integer.parseInt(props.getProperty("apiweb.port"));
            String ip = props.getProperty("apiweb.ip");
            String contextpath = props.getProperty("apiweb.contextPath");
            String topic1 = props.getProperty("mqtt.topic1");
            String topic2 = props.getProperty("mqtt.topic2");

            HttpServer server = HttpServer.create(new InetSocketAddress(ip,port), 0);

            server.createContext(contextpath + "/temperaturas", exchange -> {
                String json = db.getTempyHumJson();
                sendResponse(exchange, json);
            });

            server.createContext(contextpath + "/luz", exchange -> {
                String json = db.getLuzJson();
                sendResponse(exchange, json);
            });

            server.setExecutor(null);
            server.start();
            System.out.println("API corriendo en http://" + InetAddress.getLocalHost().getHostAddress() + ":8082");

            MqttManager mqtt = new MqttManager(db);
            mqtt.conect();
            mqtt.subscribe(topic1,0);
            mqtt.subscribe(topic2,0);

            Object lock = new Object();
            synchronized (lock) {
                lock.wait();
            }

            mqtt.disconnect();
            server.stop(0);

        } catch (MqttException e) {
            System.out.println("Error Mqtt: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error IO: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    private static void sendResponse(HttpExchange exchange, String json) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        byte[] bytes = json.getBytes();
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}