package com.monitoreoiot.model;

public class Temperatura {
    private float temp;

    public Temperatura(){
    }

    public Temperatura(float temp){
        this.temp = temp;
    }

    public float getTemp(){
        return temp;
    }

    public void setTemp(float temp){
        this.temp = temp;
    }

    public float toFahrenheit(){
        return ((temp * 1.8f) + 32f);
    }
}
