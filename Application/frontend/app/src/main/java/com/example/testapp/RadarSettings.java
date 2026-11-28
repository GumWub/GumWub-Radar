package com.example.testapp;

public class RadarSettings {
    private final int robotCoverage;
    private final int motorSweepDegrees;
    private final int sensitivityCm;

    public RadarSettings(int robotCoverage, int motorSweepDegrees, int sensitivityCm) {
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

    public String toDisplayLabel() {
        return robotCoverage + "% coverage · " + motorSweepDegrees
                + "° sweep · " + sensitivityCm + " cm range";
    }
}
