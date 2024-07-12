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
 * The {@link Houses} contains all houses/homes, retured by the house request, sent to the xsense api
 *
 * @author Jakob Fellner - Initial contribution
 */
public class Houses extends BaseData {
    public HashMap<String, House> houses = new HashMap<>();

    @Override
    public void deserialize(String input) {
        JSONObject obj = new JSONObject(input);

        obj.getJSONArray("reData").forEach(item -> {
            JSONObject house = (JSONObject) item;
            houses.put(house.getString("houseId"), new House(house.getString("mqttRegion"),
                    house.getString("mqttServer"), house.getString("houseId"), house.getString("houseName")));
        });
    }
}
