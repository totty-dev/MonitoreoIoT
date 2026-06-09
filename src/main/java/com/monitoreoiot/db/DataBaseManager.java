package com.monitoreoiot.db;

import com.monitoreoiot.model.Humedad;
import com.monitoreoiot.model.Temperatura;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class DataBaseManager {
    private Connection conec;

    private void conect() throws SQLException{
        Properties props = new Properties();
        try (InputStream input = DataBaseManager.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar config.properties", e);
        }
        String URL = props.getProperty("db.url");
        String USER = props.getProperty("db.user");
        String PASSWORD = props.getProperty("db.password");
        props.setProperty("user", USER);
        props.setProperty("password", PASSWORD);
        props.setProperty("options", "-c TimeZone=UTC");
        try {
            conec = DriverManager.getConnection(URL, props);
        } catch (SQLException e) {
            System.out.println("Error al insertar en DB: " + e.getMessage());
        }
    }
    private void disconnect() throws SQLException {
        if (conec != null) {
            conec.close();
        }
    }
    public void insertTempyHum(Temperatura temp, Humedad hum){
        String sql = "INSERT INTO tempyhum (temperatura, humedad, fecha) VALUES (?, ?, NOW())";
        try {
            this.conect();
            PreparedStatement ps = conec.prepareStatement(sql);
            ps.setFloat(1, temp.getTemp());
            ps.setFloat(2, hum.getHum());
            ps.executeUpdate();
            this.disconnect();
        }catch (SQLException e){
            System.out.println("Error al insertar en DB: " + e.getMessage());
        }
    }

    public void insertLuz(String luz){
        String sql = "INSERT INTO luz (luz, fecha) VALUES (?, now())";
        try {
            this.conect();
            PreparedStatement ps = conec.prepareStatement(sql);
            ps.setBoolean(1, Boolean.parseBoolean(luz));
            ps.executeUpdate();
            this.disconnect();
        }catch (SQLException e){
            System.out.println("Error al insertar en DB: " + e.getMessage());
        }
    }

    public String getTempyHumJson(){
        String sql = "SELECT temperatura, humedad, fecha FROM tempyhum ORDER BY fecha DESC LIMIT 1";
        StringBuilder sb = new StringBuilder("[");
        try {
            this.conect();
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
            this.disconnect();
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
            this.conect();
            Statement st = conec.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                sb.append(String.format(
                        "{\"luz\":%b,\"fecha\":\"%s\"}",
                        rs.getBoolean("luz"),
                        rs.getTimestamp("fecha").toString()
                ));
            }
            this.disconnect();
        } catch (SQLException e) {
            System.out.println("Error al insertar en DB: " + e.getMessage());
        }
        sb.append("]");
        return  sb.toString();
    }
}
