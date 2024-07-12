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

/**
 * The {@link SensorStatus} details about the status of a single xsense sensor device
 *
 * @author Jakob Fellner - Initial contribution
 */
public class SensorStatus {
    public String serialnumber;
    public int battery;
    public int rfLevel;
    public boolean online;

    public SensorStatus(String serialnumber, int battery, int rfLevel, boolean online) {
        this.serialnumber = serialnumber;
        this.battery = battery;
        this.rfLevel = rfLevel;
        this.online = online;
    }
}
