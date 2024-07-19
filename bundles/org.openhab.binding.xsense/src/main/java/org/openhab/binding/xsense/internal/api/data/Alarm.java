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
 * The {@link Alarm} represents a single result of a specific xsense smokedetector alarm
 *
 * @author Jakob Fellner - Initial contribution
 */
public class Alarm extends BaseSubscriptionDeviceData {
    public boolean isAlarm = false;

    public Alarm(String stationSerialnumber, String sensorSerialnumber, boolean isAlarm) {
        super(stationSerialnumber, sensorSerialnumber);
        this.isAlarm = isAlarm;
    }

    public Alarm(String sensorSerialnumber, boolean isAlarm) {
        super(sensorSerialnumber);
        this.isAlarm = isAlarm;
    }
}
