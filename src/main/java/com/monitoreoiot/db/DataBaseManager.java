package com.monitoreoiot.db;

import com.monitoreoiot.config.Config;

import java.sql.*;
import java.util.Properties;

public class DataBaseManager {
    private Connection conec;

    public DataBaseManager() {
        this.conect();
    }

    private void conect(){
        Properties props = new Properties();

        String URL = Config.getDbUrl();
        String USER = Config.getDbUser();
        String PASSWORD = Config.getDbPassword();

        props.setProperty("user", USER);
        props.setProperty("password", PASSWORD);
        props.setProperty("options", "-c TimeZone=UTC");
        try {
            conec = DriverManager.getConnection(URL, props);
        } catch (SQLException e) {
            System.out.println("Error al insertar en DB: " + e.getMessage());
        }
    }
    public void disconnect(){
        try {
            if (conec != null) {
                conec.close();
            }
        } catch (SQLException e) {
            System.out.println("Error al insertar en DB: " + e.getMessage());
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
            System.out.println("Error al insertar en DB: " + e.getMessage());
        }
    }

    public void insertLuz(String luz){
        String sql = "INSERT INTO luz (luz, fecha) VALUES (?, now())";
        try {
            PreparedStatement ps = conec.prepareStatement(sql);
            ps.setBoolean(1, Boolean.parseBoolean(luz));
            ps.executeUpdate();
        }catch (SQLException e){
            System.out.println("Error al insertar en DB: " + e.getMessage());
        }
    }

    public String getTempYHumJson(){
        String sql = "SELECT temperatura, humedad, fecha FROM clima ORDER BY fecha DESC LIMIT 1";
        StringBuilder sb = new StringBuilder("[");
        try {
            Statement st = conec.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                sb.append(String.format(
                        "{\"temperatura\":%.1f,\"humedad\":%.1f,\"fecha\":\"%s\"}",
                        rs.getFloat("temperatura"),
                        rs.getFloat("humedad"),
                        rs.getTimestamp("fecha").toString()
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error al insertar en DB: " + e.getMessage());
        }
        sb.append("]");
        return  sb.toString();
    }

    public String getLuzJson(){
        String sql = "SELECT luz, fecha FROM luz ORDER BY fecha DESC LIMIT 1";
        StringBuilder sb = new StringBuilder("[");
        try {
            Statement st = conec.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                sb.append(String.format(
                        "{\"luz\":%b,\"fecha\":\"%s\"}",
                        rs.getBoolean("luz"),
                        rs.getTimestamp("fecha").toString()
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error al insertar en DB: " + e.getMessage());
        }
        sb.append("]");
        return  sb.toString();
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
                sb.append(String.format(
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
                sb.append(String.format(
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
