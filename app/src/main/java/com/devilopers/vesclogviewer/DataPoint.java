package com.devilopers.vesclogviewer;

import java.io.Serializable;

public class DataPoint implements Serializable {

    public long time;
    public float batteryLevel, motorTemp, escTemp, distance, speed, inputVoltage, averageSpeed;
    public float distanceMiles, speedMiles;

    public DataPoint(long time, float batteryLevel, float motorTemp, float distance, float speed, float inputVoltage,
                     float escTemp) {
        this.time = time;
        this.batteryLevel = roundToOneDecimal(batteryLevel * 100f);
        this.motorTemp = roundToOneDecimal(motorTemp);
        this.escTemp = roundToOneDecimal(escTemp);
        this.distance = roundToThreeDecimal(distance / 1000f);
        // m/s to km/h: x3.6
        this.speed = roundToOneDecimal(speed * 3.6f);
        this.inputVoltage = roundToOneDecimal(inputVoltage);

        this.distanceMiles = roundToThreeDecimal(distance / 1609.34f);
        this.speedMiles = roundToOneDecimal(this.speed / 1.60934f);
    }

    private static float roundToOneDecimal(float input) {
        int ten = (int)(10 * input);
        return ten / 10f;
    }

    private static float roundToThreeDecimal(float input) {
        int ten = (int)(1000 * input);
        return ten / 1000f;
    }

    public void computeAverageSpeed(long startTime) {
        float average = this.distance / (this.time - startTime); //km / ms
        this.averageSpeed = 3.6f * average * 1000000f;
    }
}
