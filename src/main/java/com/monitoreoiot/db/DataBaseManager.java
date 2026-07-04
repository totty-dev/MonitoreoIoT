package com.monitoreoiot.db;

import com.monitoreoiot.config.Config;

import java.sql.*;
import java.util.Locale;
import java.util.Properties;

public class DataBaseManager {
    private static Connection conec;
    private static String url;
    private static final Properties props = new Properties();

    public DataBaseManager() {
        getConnectionProperties();
        try {
            conec = DriverManager.getConnection(url, props);
        } catch (SQLException e) {
            System.err.println("Error al Conectar DB: " + e.getMessage());
        }
    }

    private static void getConnectionProperties() {
        url = Config.getDbUrl();
        String user = Config.getDbUser();
        String password = Config.getDbPassword();

        if ((url == null || url.isEmpty()) && (user == null || user.isEmpty()) && (password == null || password.isEmpty())) {
            throw new IllegalArgumentException("❌ DB_URL, DB_USER y DB_PASSWORD no están definidas. Revisa tu archivo .env");
        }
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("❌ DB_URL no está definida. Revisa tu archivo .env");
        }
        if (user == null || user.isEmpty()) {
            throw new IllegalArgumentException("❌ DB_USER no está definida. Revisa tu archivo .env");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("❌ DB_PASSWORD no está definida. Revisa tu archivo .env");
        }
        props.setProperty("user", Config.getDbUser());
        props.setProperty("password", Config.getDbPassword());
        props.setProperty("options", "-c TimeZone=UTC");
    }

    public void disconnect(){
        try {
            if (conec != null) {
                conec.close();
            }
        } catch (SQLException e) {
            System.err.println("Error al desconectar el DB: " + e.getMessage());
        }
    }

    public void insertTempYHum(float temp, float hum){
        String sql = "INSERT INTO clima (temperatura, humedad, fecha) VALUES (?, ?, NOW())";
        try {
            PreparedStatement ps = conec.prepareStatement(sql);
            ps.setFloat(1, temp);
            ps.setFloat(2, hum);
            ps.executeUpdate();
        }catch (SQLException e){
            System.out.println("Error al insertar temp,hum en DB: " + e.getMessage());
        }
    }

    public void insertLuz(String luz){
        String sql = "INSERT INTO luz (luz, fecha) VALUES (?, now())";
        try {
            PreparedStatement ps = conec.prepareStatement(sql);
            ps.setBoolean(1, Boolean.parseBoolean(luz));
            ps.executeUpdate();
        }catch (SQLException e){
            System.out.println("Error al insertar luz en DB: " + e.getMessage());
        }
    }

    public String getTempYHumJson(){
        String sql = "SELECT temperatura, humedad, fecha FROM clima ORDER BY fecha DESC LIMIT 1";
        StringBuilder sb = new StringBuilder("[");
        try {
            Statement st = conec.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                sb.append(String.format(Locale.US,
                        "{\"temperatura\":%.1f,\"humedad\":%.1f,\"fecha\":\"%s\"}",
                        rs.getFloat("temperatura"),
                        rs.getFloat("humedad"),
                        rs.getTimestamp("fecha").toString()
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener temperatura/humedad: " + e.getMessage());
        }
        sb.append("]");
        return sb.toString();
    }

    public String getLuzJson(){
        String sql = "SELECT luz, fecha FROM luz ORDER BY fecha DESC LIMIT 1";
        StringBuilder sb = new StringBuilder("[");
        try {
            Statement st = conec.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                sb.append(String.format(Locale.US,
                        "{\"luz\":%b,\"fecha\":\"%s\"}",
                        rs.getBoolean("luz"),
                        rs.getTimestamp("fecha").toString()
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener luz: " + e.getMessage());
        }
        sb.append("]");
        return sb.toString();
    }

    public String getTempYHumHistory(String startDate, String endDate) {
        StringBuilder sb = new StringBuilder("[");
        String sql = "SELECT temperatura, humedad, fecha FROM clima WHERE fecha >= ? AND fecha <= ? ORDER BY fecha DESC";
        try {
            PreparedStatement ps = conec.prepareStatement(sql);
            ps.setTimestamp(1, Timestamp.valueOf(startDate + " 00:00:00"));
            ps.setTimestamp(2, Timestamp.valueOf(endDate + " 23:59:59"));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (sb.length() > 1) sb.append(",");
                sb.append(String.format(Locale.US,
                        "{\"temperatura\":%.1f,\"humedad\":%.1f,\"fecha\":\"%s\"}",
                        rs.getFloat("temperatura"),
                        rs.getFloat("humedad"),
                        rs.getTimestamp("fecha").toString()
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener historial temp/hum: " + e.getMessage());
        }
        sb.append("]");
        return sb.toString();
    }

    public String getLuzHistory(String startDate, String endDate) {
        StringBuilder sb = new StringBuilder("[");
        String sql = "SELECT luz, fecha FROM luz WHERE fecha >= ? AND fecha <= ? ORDER BY fecha DESC";
        try {
            PreparedStatement ps = conec.prepareStatement(sql);
            ps.setTimestamp(1, Timestamp.valueOf(startDate + " 00:00:00"));
            ps.setTimestamp(2, Timestamp.valueOf(endDate + " 23:59:59"));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (sb.length() > 1) sb.append(",");
                sb.append(String.format(Locale.US,
                        "{\"luz\":%b,\"fecha\":\"%s\"}",
                        rs.getBoolean("luz"),
                        rs.getTimestamp("fecha").toString()
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener historial luz: " + e.getMessage());
        }
        sb.append("]");
        return sb.toString();
    }
}