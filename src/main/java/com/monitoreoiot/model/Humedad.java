package com.monitoreoiot.model;

public class Humedad {
    private float hum;

    public Humedad(){
    }

    public Humedad(float hum){
        this.hum = hum;
    }

    public float getHum(){
        return hum;
    }

    public void setHum(float hum){
        this.hum = hum;
    }
}
