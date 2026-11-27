package com.example.testapp.models;

public final class RadarEntity {
    private final String id;
    private final String name;

    public RadarEntity(String id, String name) {
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
