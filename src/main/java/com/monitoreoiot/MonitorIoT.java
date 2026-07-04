package com.monitoreoiot;

import com.monitoreoiot.config.Config;
import com.monitoreoiot.db.DataBaseManager;
import com.monitoreoiot.mqtt.MqttManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.time.LocalDate;

public class MonitorIoT {
    public static void main(String[] args) {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));

        DataBaseManager db = new DataBaseManager();
        MqttManager mqtt = new MqttManager(db);

        mqtt.conect();
        mqtt.subscribe(Config.getMqttTopic1(),Config.getMqttQos());
        mqtt.subscribe(Config.getMqttTopic2(),Config.getMqttQos());

        try {
            startHttpServer(db);
        }catch (IOException e) {
            System.err.println("Error creando serverAPI: " + e.getMessage());
        }catch (InterruptedException e) {
            System.err.println("Error Interrupted: " + e.getMessage());
        }finally {
            mqtt.disconnect();
            db.disconnect();
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

    private static String getQueryParam(HttpExchange exchange, String key) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals(key)) return pair[1];
        }
        return null;
    }

    private static void startHttpServer(DataBaseManager db) throws IOException, InterruptedException {
        HttpServer server = HttpServer.create(new InetSocketAddress(Config.getBackendIp(), Config.getBackendPort()), 0);

        String contextpath = Config.getBackendContextPath();
        server.createContext(contextpath + "/temperaturas", exchange -> {
            String json = db.getTempYHumJson();
            sendResponse(exchange, json);
        });

        server.createContext(contextpath + "/luz", exchange -> {
            String json = db.getLuzJson();
            sendResponse(exchange, json);
        });

        server.createContext(contextpath + "/historico/tempyhum", exchange -> {
            String start = getQueryParam(exchange, "start");
            String end = getQueryParam(exchange, "end");
            if (start == null || start.isEmpty()) start = LocalDate.now().minusDays(7).toString();
            if (end == null || end.isEmpty()) end = LocalDate.now().toString();
            String json = db.getTempYHumHistory(start, end);
            sendResponse(exchange, json);
        });

        server.createContext(contextpath + "/historico/luz", exchange -> {
            String start = getQueryParam(exchange, "start");
            String end = getQueryParam(exchange, "end");
            if (start == null || start.isEmpty()) start = LocalDate.now().minusDays(7).toString();
            if (end == null || end.isEmpty()) end = LocalDate.now().toString();
            String json = db.getLuzHistory(start, end);
            sendResponse(exchange, json);
        });

        server.setExecutor(null);
        server.start();

        Object lock = new Object();
        synchronized (lock) {
            lock.wait();
        }
        server.stop(0);
    }
}