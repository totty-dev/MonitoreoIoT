package com.monitoreoiot.db;
import java.sql.*;
import java.util.Properties;

public class DataBase {
    private String URL = "jdbc:postgresql://192.168.1.107:5432/basededatos";
    private String USER = "totty";
    private String PASSWORD = "tomas3342";
    private Connection CONEC;

    public DataBase(){
    }

    public DataBase(String url, String user, String password){
        this.URL = url;
        this.USER = user;
        this.PASSWORD = password;
    }
    public void conectar() throws SQLException{
        Properties props = new Properties();
        props.setProperty("user", USER);
        props.setProperty("password", PASSWORD);
        props.setProperty("options", "-c TimeZone=UTC");
        CONEC = DriverManager.getConnection(URL,props);
    }
    public void desconectar() throws SQLException {
        if (CONEC != null) {
            CONEC.close();
        }
    }
    public void insertarTemperatura(int temp, int hum) throws SQLException {
        String sql = "INSERT INTO iot (temperatura, humedad, fecha) VALUES (?, ?, NOW())";
        try {
            this.conectar();
            PreparedStatement ps = CONEC.prepareStatement(sql);
            ps.setFloat(1, temp);
            ps.setFloat(2, hum);
            ps.executeUpdate();
        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    public void obtenerDatos() throws SQLException {
        String sql = "SELECT * FROM iot ORDER BY fecha";
        try {
            this.conectar();
            Statement st = CONEC.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                System.out.println("Temp: " + rs.getFloat("valor_temp"));
                System.out.println("Hum: " + rs.getFloat("valor_hum"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
