package com.example.fast_pdr;


public class SensorData {
    private double x;
    private double y;
    private double z;
    private double timestamp;

    public SensorData(double x, double y, double z, double timestamp) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getTimestamp() {
        return timestamp;
    }
    
}


