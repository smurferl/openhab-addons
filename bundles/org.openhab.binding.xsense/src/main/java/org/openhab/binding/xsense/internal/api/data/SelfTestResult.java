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
 * The {@link SelfTestResult} represents a single selftest result of xsense smokedetector
 *
 * @author Jakob Fellner - Initial contribution
 */
public class SelfTestResult extends BaseSubscriptionDeviceData {
    public boolean success = false;

    public SelfTestResult(String stationSerialnumber, String sensorSerialnumber, boolean success) {
        super(stationSerialnumber, sensorSerialnumber);
        this.success = success;
    }

    public SelfTestResult(String sensorSerialnumber, boolean success) {
        super(sensorSerialnumber);
        this.success = success;
    }
}
