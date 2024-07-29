/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.xsense.internal.api.data;

import java.util.ArrayList;

import org.json.JSONObject;
import org.openhab.binding.xsense.internal.api.data.base.BaseData;

/**
 * The {@link DevicesStatus} encapsulates detailed status details for all discovered xsense devices
 *
 * @author Jakob Fellner - Initial contribution
 */
public class DevicesStatus extends BaseData {
    private ArrayList<SensorStatus> sensorsStatus = new ArrayList<>();
    private StationStatus stationStatus;

    public class DeviceStatus {
        public String serialnumber;
        private int connectionQuality = 0;

        DeviceStatus(String serialnumber, int connectionQuality) {
            this.serialnumber = serialnumber;
            this.connectionQuality = connectionQuality;
        }

        public void setConnectionQuality(int connectionQuality) {
            this.connectionQuality = connectionQuality;
        }

        public int getConnectionQuality() {
            return connectionQuality;
        }

        public String getSerialnumber() {
            return serialnumber;
        }
    }

    public class SensorStatus extends DeviceStatus {
        private int battery;
        private boolean online;

        public SensorStatus(String serialnumber, int battery, int rfLevel, boolean online) {
            super(serialnumber, rfLevel);

            this.battery = battery;
            this.online = online;
        }

        public int getBattery() {
            return battery;
        }

        public boolean isOnline() {
            return online;
        }
    }

    public class StationStatus extends DeviceStatus {
        public StationStatus(String serialnumber, int rssi) {
            super(serialnumber, 0);

            if (rssi > -60) {
                setConnectionQuality(4);
            } else if (rssi > -69 && rssi <= -60) {
                setConnectionQuality(3);
            } else if (rssi > -79 && rssi <= -69) {
                setConnectionQuality(2);
            } else if (rssi > -90 && rssi <= -79) {
                setConnectionQuality(1);
            } else {
                setConnectionQuality(0);
            }
        }
    }

    @Override
    public void deserialize(String input) {
        JSONObject obj = new JSONObject(input);

        if (obj.has("devs")) {
            JSONObject devices = obj.getJSONObject("devs");
            for (String serialnumber : devices.keySet()) {
                JSONObject device = devices.getJSONObject(serialnumber);
                boolean online = false;
                int battery = 0;
                int rfLevel = 0;

                if (device.has("online")) {
                    online = device.getString("online").equals("1");
                }
                if (device.has("batInfo")) {
                    battery = (int) (Double.parseDouble(device.getString("batInfo")) / 3.0 * 100.0);
                }
                if (device.has("rfLevel")) {
                    rfLevel = (int) (Double.parseDouble(device.getString("rfLevel")) / 3.0 * 4.0); // map max value 3 to
                                                                                                   // max
                    // value 4
                }
                sensorsStatus.add(new SensorStatus(serialnumber, battery, rfLevel, online));
            }
        }

        if (obj.has("wifiRSSI") && obj.has("stationSN")) {
            stationStatus = new StationStatus(obj.getString("stationSN"), Integer.parseInt(obj.getString("wifiRSSI")));
        }
    }

    public StationStatus getStationStatus() {
        return stationStatus;
    }

    public ArrayList<SensorStatus> getSensorStatus() {
        return sensorsStatus;
    }
}
