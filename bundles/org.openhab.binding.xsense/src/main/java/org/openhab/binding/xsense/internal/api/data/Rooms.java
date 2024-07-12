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

import java.util.HashMap;

import org.json.JSONObject;

/**
 * The {@link Room} represents all rooms, returned by the room request to xsense api
 *
 * @author Jakob Fellner - Initial contribution
 */
public class Rooms extends BaseData {
    public HashMap<String, Room> rooms = new HashMap<>();

    @Override
    public void deserialize(String input) {
        JSONObject obj = new JSONObject(input);

        obj.getJSONObject("reData").getJSONArray("houseRooms").forEach(item -> {
            JSONObject room = (JSONObject) item;
            rooms.put(room.getString("roomId"),
                    new Room(room.getString("houseId"), room.getString("roomId"), room.getString("roomName")));
        });
    }
}
