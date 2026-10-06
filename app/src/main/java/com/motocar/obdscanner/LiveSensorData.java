package com.motocar.obdscanner;

public class LiveSensorData {
    public int rpm = 0;
    public int speed = 0;
    public int coolantTemp = -999;
    public int throttlePos = -1;
    public int engineLoad = -1;
    public int intakeAirTemp = -999;
    public int mapPressure = -1;
    public String batteryVoltage = "--.-V";
    public long lastUpdate = 0;

    public boolean hasRpm() { return rpm >= 0; }
    public boolean hasSpeed() { return speed >= 0; }
    public boolean hasCoolant() { return coolantTemp != -999; }
    public boolean hasThrottle() { return throttlePos >= 0; }
    public boolean hasEngineLoad() { return engineLoad >= 0; }
    public boolean hasIntakeTemp() { return intakeAirTemp != -999; }
    public boolean hasMapPressure() { return mapPressure >= 0; }

    public void reset() {
        rpm = 0;
        speed = 0;
        coolantTemp = -999;
        throttlePos = -1;
        engineLoad = -1;
        intakeAirTemp = -999;
        mapPressure = -1;
        batteryVoltage = "--.-V";
        lastUpdate = 0;
    }
}
