package org.openhab.binding.xsense.internal.api.data.base;

public class BaseEvent {
    private Target target = null;

    public class Target {
        private String stationSerialnumber = "";
        private String deviceSerialnumber = "";

        public Target(String deviceSerialnumber) {
            this.deviceSerialnumber = deviceSerialnumber;
        }

        public Target(String stationSerialnumber, String deviceSerialnumber) {
            this.stationSerialnumber = stationSerialnumber;
            this.deviceSerialnumber = deviceSerialnumber;
        }

        public String getStationSerialnumber() {
            return stationSerialnumber;
        }

        public String getDeviceSerialnumber() {
            return deviceSerialnumber;
        }
    }

    public BaseEvent(String deviceSerialnumber) {
        target = new Target(deviceSerialnumber);
    }

    public BaseEvent(String stationSerialnumber, String deviceSerialnumber) {
        target = new Target(stationSerialnumber, deviceSerialnumber);
    }

    public Target getTarget() {
        return target;
    }
}
