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

import java.util.Collection;
import java.util.HashMap;

import org.json.JSONObject;
import org.openhab.binding.xsense.internal.api.data.base.BaseData;

/**
 * The {@link Houses} contains all houses/homes, retured by the house request, sent to the xsense api
 *
 * @author Jakob Fellner - Initial contribution
 */
public class Houses extends BaseData {
    private HashMap<String, House> houses = new HashMap<>();

    public class House {
        private String mqttRegion = "";
        private String mqttServer = "";
        private String houseId = "";
        private String houseName = "";

        public House(String mqttRegion, String mqttServer, String houseId, String houseName) {
            this.mqttRegion = mqttRegion;
            this.mqttServer = mqttServer;
            this.houseId = houseId;
            this.houseName = houseName;
        }

        public String getMqttRegion() {
            return mqttRegion;
        }

        public String getMqttServer() {
            return mqttServer;
        }

        public String getHouseId() {
            return houseId;
        }

        public String getHouseName() {
            return houseName;
        }
    }

    @Override
    public void deserialize(String input) {
        JSONObject obj = new JSONObject(input);

        obj.getJSONArray("reData").forEach(item -> {
            JSONObject house = (JSONObject) item;
            houses.put(house.getString("houseId"), new House(house.getString("mqttRegion"),
                    house.getString("mqttServer"), house.getString("houseId"), house.getString("houseName")));
        });
    }

    public House getHouse(String houseId) {
        return houses.get(houseId);
    }

    public Collection<House> getHouses() {
        return houses.values();
    }
}
