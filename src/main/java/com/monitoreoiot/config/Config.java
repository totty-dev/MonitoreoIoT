package com.monitoreoiot.config;

public class Config {

    private static String getEnv(String name) {
        return System.getenv(name);
    }

    private static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }
    public static String getMqttIp()  {
        return getEnv("MQTT_IP");
    }
    public static String getMqttPort()  {
        return getEnv("MQTT_PORT");
    }
    public static String getMqttTopic1()  {
        return getEnv("MQTT_TOPIC1");
    }
    public static String getMqttTopic2()  {
        return getEnv("MQTT_TOPIC2");
    }
    public static int getMqttQos()            {
        return Integer.parseInt(getEnv("MQTT_QOS", "0"));
    }
    public static String getDbUrl()       {
        return getEnv("DB_URL");
    }
    public static String getDbUser()      {
        return getEnv("DB_USER");
    }
    public static String getDbPassword()  {
        return getEnv("DB_PASSWORD");
    }
    public static String getBackendIp()  {
        return getEnv("BACKEND_IP", "0.0.0.0");
    }
    public static String getBackendContextPath()  {
        return getEnv("BACKEND_CONTEXT_PATH", "");
    }
    public static int getBackendPort()     {
        return Integer.parseInt(getEnv("BACKEND_PORT"));
    }
}