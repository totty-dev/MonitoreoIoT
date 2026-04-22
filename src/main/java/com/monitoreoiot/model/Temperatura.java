package com.monitoreoiot.model;

public class Temperatura {
    private float temp;

    public Temperatura(){
    }

    public Temperatura(float temp){
        this.temp = temp;
    }
    public float gettemp(){
        return temp;
    }

    public float settemp(){
        return temp;
    }

    public float toFahrenheit(){
        return ((temp * 1.8f) + 32f);
    }
}
