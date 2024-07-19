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
 * The {@link Mute} represents a single result of a specific xsense smokedetector alarm mute
 *
 * @author Jakob Fellner - Initial contribution
 */
public class Mute extends BaseSubscriptionDeviceData {
    public String trigger = "";

    public Mute(String stationSerialnumber, String sensorSerialnumber, String trigger) {
        super(stationSerialnumber, sensorSerialnumber);
        this.trigger = trigger;
    }

    public Mute(String sensorSerialnumber, String trigger) {
        super(sensorSerialnumber);
        this.trigger = trigger;
    }
}
