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
 * The {@link ClientInfo} encapsulates relevant data for one device returned for the device request, whereas it acts as
 * the basis for sensors and basestations
 *
 * @author Jakob Fellner - Initial contribution
 */
public class Device {
    public String deviceId = "";
    public String deviceName = "";
    public String deviceSerialnumber = "";
    public String roomId = "";
    public String houseId = "";
    public DeviceType deviceType;
    public boolean online;

    public Device(String deviceId, String deviceName, String deviceSerialnumber, String roomId, DeviceType deviceType,
            boolean online) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.deviceSerialnumber = deviceSerialnumber;
        this.roomId = roomId;
        this.deviceType = deviceType;
        this.online = online;
    }

    public String getThingName() {
        return deviceType.toString() + deviceSerialnumber;
    }
}
