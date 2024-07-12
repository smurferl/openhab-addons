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
 * The {@link House} represents data for one house/home within the xsense structure
 *
 * @author Jakob Fellner - Initial contribution
 */
public class House {
    public String mqttRegion = "";
    public String mqttServer = "";
    public String houseId = "";
    public String houseName = "";

    public House(String mqttRegion, String mqttServer, String houseId, String houseName) {
        this.mqttRegion = mqttRegion;
        this.mqttServer = mqttServer;
        this.houseId = houseId;
        this.houseName = houseName;
    }
}
