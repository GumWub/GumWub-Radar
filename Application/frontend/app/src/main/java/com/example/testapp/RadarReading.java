package com.example.testapp;

import java.util.Locale;

public class RadarReading {
    private final float distanceCm;
    private final float angleDegrees;
    private final long timestampMillis;

    public RadarReading(float distanceCm, float angleDegrees, long timestampMillis) {
        this.distanceCm = distanceCm;
        this.angleDegrees = angleDegrees;
        this.timestampMillis = timestampMillis;
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

    public String getDistanceLabel() {
        return String.format(Locale.US, "%.1f cm", distanceCm);
    }

    public String getAngleLabel() {
        return String.format(Locale.US, "%.0f deg", angleDegrees);
    }
}
