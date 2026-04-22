package com.monitoreoiot.db;

import com.monitoreoiot.model.Humedad;
import com.monitoreoiot.model.Temperatura;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class DataBaseManager {
    private String URL;
    private String USER;
    private String PASSWORD;
    private Connection CONEC;

    public DataBaseManager(){
    }

    public void conect() throws SQLException{
        Properties props = new Properties();
        try (InputStream input = DataBaseManager.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar config.properties", e);
        }
        URL = props.getProperty("db.url");
        USER = props.getProperty("db.user");
        PASSWORD = props.getProperty("db.password");
        props.setProperty("user",USER);
        props.setProperty("password",PASSWORD);
        props.setProperty("options", "-c TimeZone=UTC");
        try {
            CONEC = DriverManager.getConnection(URL, props);
        } catch (SQLException e) {
            System.out.println("Error al insertar en DB: " + e.getMessage());
        }
    }
    public void disconnect() throws SQLException {
        if (CONEC != null) {
            CONEC.close();
        }
    }
    public void insertTempyHum(Temperatura temp, Humedad hum) throws SQLException {
        String sql = "INSERT INTO iot (temperatura, humedad, fecha) VALUES (?, ?, NOW())";
        try {
            this.conect();
            PreparedStatement ps = CONEC.prepareStatement(sql);
            ps.setFloat(1, temp.gettemp());
            ps.setFloat(2, hum.gethum());
            ps.executeUpdate();
        }catch (SQLException e){
            System.out.println("Error al insertar en DB: " + e.getMessage());
        }
    }
    public void getTempyHum() throws SQLException {
        String sql = "SELECT * FROM iot ORDER BY fecha";
        try {
            this.conect();
            Statement st = CONEC.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                System.out.println("Temp: " + rs.getFloat("temperatura"));
                System.out.println("Hum: " + rs.getFloat("humedad"));
            }
        } catch (SQLException e) {
            System.out.println("Error al insertar en DB: " + e.getMessage());
        }
    }
}
