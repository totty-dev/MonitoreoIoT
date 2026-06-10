package com.monitoreoiot.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    public static Properties config;

    static  {
        Properties defaults = new Properties();
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            defaults.load(input);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar config.properties", e);
        }
        config = new Properties(defaults);

        for (String envName : System.getenv().keySet()) {
            String envValue = System.getenv(envName);
            if (envValue != null && !envValue.trim().isEmpty()) {
                config.setProperty(envName, envValue);
                System.out.println("[Config] Usando ENV: " + envName + " = " + envValue);
            } else {
                System.out.println("[Config] Ignorando ENV vacía: " + envName);
            }
        }
    }

    public static String getMqttBroker()  {
        return config.getProperty("MQTT_BROKER");
    }
    public static String getMqttTopic1()  {
        return config.getProperty("MQTT_TOPIC1");
    }
    public static String getMqttTopic2()  {
        return config.getProperty("MQTT_TOPIC2");
    }
    public static String getDbName()     {
        return config.getProperty("DB_NAME");
    }
    public static String getDbUrl()       {
        return config.getProperty("DB_URL");
    }
    public static String getDbUser()      {
        return config.getProperty("DB_USER");
    }
    public static String getDbPassword()  {
        return config.getProperty("DB_PASSWORD");
    }
    public static String getServerIp()  {
        return config.getProperty("SERVER_IP");
    }
    public static String getServerContextPath()  {
        return config.getProperty("SERVER_CONTEXT_PATH");
    }
    public static int getServerPort()     {
        String port = config.getProperty("SERVER_PORT");
        return ((port != null) && (port.matches("\\d+"))) ? Integer.parseInt(port) : 8082;
    }
}