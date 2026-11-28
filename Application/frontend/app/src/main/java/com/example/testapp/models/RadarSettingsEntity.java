package com.example.testapp.models;

public final class RadarSettingsEntity {
    private final int robotCoverage;
    private final int motorSweepDegrees;
    private final int sensitivityCm;

    public RadarSettingsEntity(int robotCoverage, int motorSweepDegrees,
                               int sensitivityCm) {
        this.robotCoverage = robotCoverage;
        this.motorSweepDegrees = motorSweepDegrees;
        this.sensitivityCm = sensitivityCm;
    }

    public int getRobotCoverage() {
        return robotCoverage;
    }

    public int getMotorSweepDegrees() {
        return motorSweepDegrees;
    }

    public int getSensitivityCm() {
        return sensitivityCm;
    }
}
