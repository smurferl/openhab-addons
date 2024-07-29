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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;
import org.openhab.binding.xsense.internal.api.ApiConstants.DeviceType;
import org.openhab.binding.xsense.internal.api.data.DevicesStatus.SensorStatus;
import org.openhab.binding.xsense.internal.api.data.DevicesStatus.StationStatus;
import org.openhab.binding.xsense.internal.api.data.base.BaseData;

/**
 * The {@link Devices} encapsulates all devices returned by the device request
 *
 * @author Jakob Fellner - Initial contribution
 */
public class Devices extends BaseData {
    public HashMap<String, Device> devices = new HashMap<>();

    public class Device {
        private String deviceId = "";
        private String deviceName = "";
        private String deviceSerialnumber = "";
        private String roomId = "";
        private String houseId = "";
        private DeviceType deviceType;

        public Device(String deviceId, String deviceName, String deviceSerialnumber, String roomId, String houseId,
                DeviceType deviceType) {
            this.deviceId = deviceId;
            this.deviceName = deviceName;
            this.deviceSerialnumber = deviceSerialnumber;
            this.roomId = roomId;
            this.houseId = houseId;
            this.deviceType = deviceType;
        }

        public String getThingName() {
            return deviceType.toString() + deviceSerialnumber;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public String getDeviceName() {
            return deviceName;
        }

        public String getDeviceSerialnumber() {
            return deviceSerialnumber;
        }

        public String getRoomId() {
            return roomId;
        }

        public String getHouseId() {
            return houseId;
        }

        public DeviceType getDeviceType() {
            return deviceType;
        }
    }

    public class Sensor extends Device {
        private String stationSerialnumber = "";
        private DeviceType stationType = DeviceType.UNKNOWN;
        private SensorStatus status = null;

        public Sensor(String deviceId, String deviceName, String deviceSerialnumber, String roomId, String houseId,
                DeviceType deviceType) {
            super(deviceId, deviceName, deviceSerialnumber, roomId, houseId, deviceType);
        }

        public Sensor(String deviceId, String deviceName, String deviceSerialnumber, String roomId, String houseId,
                DeviceType deviceType, String stationSerialnumber, DeviceType stationType) {
            super(deviceId, deviceName, deviceSerialnumber, roomId, houseId, deviceType);

            this.stationSerialnumber = stationSerialnumber;
            this.stationType = stationType;
        }

        public String getStationSerialnumber() {
            return stationSerialnumber;
        }

        public DeviceType getStationDeviceType() {
            return stationType;
        }

        public void setSensorStatus(SensorStatus status) {
            this.status = status;
        }

        public SensorStatus getSensorStatus() {
            return status;
        }
    }

    public class Station extends Device {
        private HashMap<String, Sensor> sensors = new HashMap<>();
        private StationStatus status = null;
        private boolean online = false;

        public Station(String deviceId, String deviceName, String deviceSerialnumber, String roomId, String houseId,
                DeviceType deviceType, boolean online, String userId) {
            super(deviceId, deviceName, deviceSerialnumber, roomId, houseId, deviceType);

            this.online = online;
        }

        public void addSensor(Sensor sensor) {
            sensors.put(sensor.getDeviceSerialnumber(), sensor);
        }

        public Sensor getSensor(String serialnumber) {
            return sensors.get(serialnumber);
        }

        public Collection<Sensor> getSensors() {
            return sensors.values();
        }

        public void setStationStatus(StationStatus status) {
            this.status = status;
        }

        public StationStatus getStationStatus() {
            return status;
        }

        public boolean isOnline() {
            return online;
        }
    }

    @Override
    public void deserialize(String input) {
        JSONObject obj = new JSONObject(input);

        String houseId = obj.getJSONObject("reData").getString("houseId");

        obj.getJSONObject("reData").getJSONArray("stations").forEach(stationItem -> {
            JSONObject station = (JSONObject) stationItem;

            Station s = new Station(station.getString("stationId"), station.getString("stationName"),
                    station.getString("stationSn"), station.getString("roomId"), houseId,
                    station.getString("category").equals("SBS50") ? DeviceType.SBS50 : DeviceType.UNKNOWN,
                    station.getInt("onLine") == 1 ? true : false, station.getString("userId"));

            station.getJSONArray("devices").forEach(deviceItem -> {
                JSONObject device = (JSONObject) deviceItem;

                Sensor se = new Sensor(device.getString("deviceId"), device.getString("deviceName"),
                        device.getString("deviceSn"), device.getString("roomId"), houseId,
                        device.getString("deviceType").equals("XS01-M") ? DeviceType.XS01_M : DeviceType.UNKNOWN,
                        s.getDeviceSerialnumber(), s.getDeviceType());

                s.addSensor(se);
            });

            devices.put(s.getDeviceSerialnumber(), s);
        });
    }

    public List<Device> toList() {
        List<Device> d = new ArrayList<>();

        for (Device device : devices.values()) {
            d.add(device);
        }

        return d;
    }

    public Device getDevice(String serialnumber) {
        Device device = devices.get(serialnumber);

        if (device == null) {
            for (Device d : devices.values()) {
                if (d instanceof Station) {
                    device = ((Station) d).getSensor(serialnumber);
                }
            }
        }

        return device;
    }
}
