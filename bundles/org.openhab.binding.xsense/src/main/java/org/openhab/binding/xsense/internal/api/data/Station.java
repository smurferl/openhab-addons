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

import java.util.Collection;
import java.util.HashMap;

import org.openhab.binding.xsense.internal.api.ApiConstants.DeviceType;

/**
 * The {@link Station} contains relevant data for xsense basestation device
 *
 * @author Jakob Fellner - Initial contribution
 */
public class Station extends Device {
    public HashMap<String, Sensor> sensors = new HashMap<>();
    public String userId = "";
    public int wifiRSSI = 0;

    public Station(String deviceId, String deviceName, String deviceSerialnumber, String roomId, DeviceType deviceType,
            boolean online, String userId) {
        super(deviceId, deviceName, deviceSerialnumber, roomId, deviceType, online);

        this.userId = userId;
    }

    public void addSensor(Sensor sensor) {
        sensors.put(sensor.deviceSerialnumber, sensor);
    }

    public Sensor getSensor(String serialnumber) {
        return sensors.get(serialnumber);
    }

    public Collection<Sensor> getSensors() {
        return sensors.values();
    }
}
