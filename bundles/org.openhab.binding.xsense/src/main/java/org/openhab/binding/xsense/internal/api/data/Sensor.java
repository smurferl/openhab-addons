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

import org.openhab.binding.xsense.internal.api.ApiConstants.DeviceType;

/**
 * The {@link Sensor} represents a sensor device from xsense (e.g. smokedetector)
 *
 * @author Jakob Fellner - Initial contribution
 */
public class Sensor extends Device {
    public int batteryInfo = 0;
    public int rfLevel = 0;
    public String stationSerialnumber = "";
    public DeviceType stationType = DeviceType.UNKNOWN;

    public Sensor(String deviceId, String deviceName, String deviceSerialnumber, String roomId, DeviceType deviceType,
            boolean online) {
        super(deviceId, deviceName, deviceSerialnumber, roomId, deviceType, online);
    }

    public Sensor(String deviceId, String deviceName, String deviceSerialnumber, String roomId, DeviceType deviceType,
            boolean online, String stationSerialnumber, DeviceType stationType) {
        super(deviceId, deviceName, deviceSerialnumber, roomId, deviceType, online);

        this.stationSerialnumber = stationSerialnumber;
        this.stationType = stationType;
    }
}
