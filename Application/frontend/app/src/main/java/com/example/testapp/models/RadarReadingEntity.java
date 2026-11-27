package com.example.testapp.models;

public final class RadarReadingEntity {
    private final String radarId;
    private final float distanceCm;
    private final float angleDegrees;
    private final long timestampMillis;

    public RadarReadingEntity(String radarId, float distanceCm, float angleDegrees,
                              long timestampMillis) {
        this.radarId = radarId;
        this.distanceCm = distanceCm;
        this.angleDegrees = angleDegrees;
        this.timestampMillis = timestampMillis;
    }

    public String getRadarId() {
        return radarId;
    }

    public float getDistanceCm() {
        return distanceCm;
    }

    public float getAngleDegrees() {
        return angleDegrees;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }
}
