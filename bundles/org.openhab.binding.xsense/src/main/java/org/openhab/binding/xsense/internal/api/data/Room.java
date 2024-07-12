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
 * The {@link Room} contains all relevant data, describing one room in the xsense datastructure
 *
 * @author Jakob Fellner - Initial contribution
 */
public class Room {
    public String houseId = "";
    public String roomId = "";
    public String roomName = "";

    public Room(String houseId, String roomId, String roomName) {
        this.houseId = houseId;
        this.roomId = roomId;
        this.roomName = roomName;
    }
}
