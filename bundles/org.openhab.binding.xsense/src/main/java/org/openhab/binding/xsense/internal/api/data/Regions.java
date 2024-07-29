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

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openhab.binding.xsense.internal.api.data.base.BaseData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Regions} represents all data relevant, received by the regions request. contained information is needed
 * for
 * mqtt connection to aws iot
 *
 * @author Jakob Fellner - Initial contribution
 */
public class Regions extends BaseData {
    private ArrayList<Region> regions = new ArrayList<>();
    private final Logger logger = LoggerFactory.getLogger(Regions.class);

    public class Region {
        private String region;
        private String server;
        private int port;

        public Region(String region, String server, int port) {
            this.region = region;
            this.server = server;
            this.port = port;
        }

        public String getMqttRegion() {
            return region;
        }

        public String getMqttServer() {
            return server;
        }

        public int getMqttPort() {
            return port;
        }
    }

    @Override
    public void deserialize(String input) {
        JSONObject obj = new JSONObject(input);

        if (obj.has("reData")) {
            JSONObject response = obj.getJSONObject("reData");

            if (response.has("regions")) {
                JSONArray regionArray = response.getJSONArray("regions");

                regionArray.forEach(item -> {
                    JSONObject r = (JSONObject) item;

                    if (r.has("mqttRegion") && r.has("mqttServer") && r.has("mqttPort")) {
                        Region region = new Region(r.getString("mqttRegion"), r.getString("mqttServer"),
                                r.getInt("mqttPort"));
                        regions.add(region);
                    } else {
                        logger.error("invalid region {}", r.toString());
                    }
                });
            } else {
                logger.error("no regions found {}", response.toString());
            }
        } else {
            logger.error("no responseData found", obj.toString());
        }
    }

    public ArrayList<Region> getRegions() {
        return regions;
    }
}
