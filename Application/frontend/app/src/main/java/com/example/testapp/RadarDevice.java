package com.example.testapp;

public class RadarDevice {
    private final String id;
    private final String name;

    public RadarDevice(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
