package org.openhab.binding.xsense.internal.api.data;

public class BaseSubscriptionDeviceData {
    public String stationSerialnumber = "";
    public String deviceSerialnumber = "";

    public BaseSubscriptionDeviceData(String stationSerialnumber, String deviceSerialnumber) {
        this.stationSerialnumber = stationSerialnumber;
        this.deviceSerialnumber = deviceSerialnumber;
    }

    public BaseSubscriptionDeviceData(String deviceSerialnumber) {
        this.deviceSerialnumber = deviceSerialnumber;
    }
}
