package com.monitoreoiot.config;

public class Config {

    private static String getEnv(String name) {
        String value = System.getenv(name);
        return (value != null) ? value.trim() : null;
    }

    private static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value != null && !value.trim().isEmpty()) ? value.trim() : defaultValue;
    }

    public static String getMqttIp() {
        String ip = getEnv("MQTT_IP");
        if (ip == null || ip.isEmpty()) {
            System.err.println("⚠️  ADVERTENCIA: MQTT_IP no está definida en el .env");
        }
        return ip;
    }

    public static String getMqttPort() {
        return getEnv("MQTT_PORT", "1883");
    }

    public static String getMqttTopic1() {
        return getEnv("MQTT_TOPIC1");
    }

    public static String getMqttTopic2() {
        return getEnv("MQTT_TOPIC2");
    }

    public static int getMqttQos() {
        return Integer.parseInt(getEnv("MQTT_QOS", "0"));
    }

    public static String getDbUrl() {
        return getEnv("DB_URL");
    }

    public static String getDbUser() {
        return getEnv("DB_USER");
    }

    public static String getDbPassword() {
        return getEnv("DB_PASSWORD");
    }

    public static String getBackendIp() {
        return getEnv("BACKEND_IP", "0.0.0.0");
    }

    public static String getBackendContextPath() {
        return getEnv("BACKEND_CONTEXT_PATH", "");
    }

    public static int getBackendPort() {
        return Integer.parseInt(getEnv("BACKEND_PORT", ""));
    }
}