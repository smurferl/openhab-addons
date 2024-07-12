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
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;
import org.openhab.binding.xsense.internal.api.ApiConstants.DeviceType;

/**
 * The {@link Devices} encapsulates all devices returned by the device request
 *
 * @author Jakob Fellner - Initial contribution
 */
public class Devices extends BaseData {
    public HashMap<String, Device> devices = new HashMap<>();

    @Override
    public void deserialize(String input) {
        JSONObject obj = new JSONObject(input);

        obj.getJSONObject("reData").getJSONArray("stations").forEach(stationItem -> {
            JSONObject station = (JSONObject) stationItem;

            Station s = new Station(station.getString("stationId"), station.getString("stationName"),
                    station.getString("stationSn"), station.getString("roomId"),
                    station.getString("category").equals("SBS50") ? DeviceType.SBS50 : DeviceType.UNKNOWN,
                    station.getInt("onLine") == 1 ? true : false, station.getString("userId"));

            station.getJSONArray("devices").forEach(deviceItem -> {
                JSONObject device = (JSONObject) deviceItem;

                Sensor se = new Sensor(device.getString("deviceId"), device.getString("deviceName"),
                        device.getString("deviceSn"), device.getString("roomId"),
                        device.getString("deviceType").equals("XS01-M") ? DeviceType.XS01_M : DeviceType.UNKNOWN, false,
                        s.deviceSerialnumber, s.deviceType);

                s.addSensor(se);
            });

            devices.put(s.deviceSerialnumber, s);
        });
    }

    public List<Device> toList() {
        List<Device> d = new ArrayList<>();

        for (Device device : devices.values()) {
            d.add(device);

            if (device instanceof Station) {
                d.addAll(((Station) device).getSensors());
            }
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
