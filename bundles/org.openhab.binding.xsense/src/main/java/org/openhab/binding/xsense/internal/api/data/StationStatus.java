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
 * The {@link StationStatus} represents statusinformation for xsense basestation
 *
 * @author Jakob Fellner - Initial contribution
 */
public class StationStatus {
    public int rssi;

    public StationStatus(int rssi) {
        if (rssi > -60) {
            this.rssi = 4;
        } else if (rssi > -69 && rssi <= -60) {
            this.rssi = 3;
        } else if (rssi > -79 && rssi <= -69) {
            this.rssi = 2;
        } else if (rssi > -90 && rssi <= -79) {
            this.rssi = 1;
        } else {
            this.rssi = 0;
        }
    }
}
