package org.openhab.binding.xsense.internal.api.data;

public class SelfTestResult extends BaseSubscriptionDeviceData {
    public boolean success = false;

    public SelfTestResult(String stationSerialnumber, String sensorSerialnumber, boolean success) {
        super(stationSerialnumber, sensorSerialnumber);
        this.success = success;
    }

    public SelfTestResult(String sensorSerialnumber, boolean success) {
        super(sensorSerialnumber);
        this.success = success;
    }
}
