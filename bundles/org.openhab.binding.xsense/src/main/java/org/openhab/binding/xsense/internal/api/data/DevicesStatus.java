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

import org.json.JSONObject;

/**
 * The {@link DevicesStatus} encapsulates detailed status details for all discovered xsense devices
 *
 * @author Jakob Fellner - Initial contribution
 */
public class DevicesStatus extends BaseData {
    public ArrayList<SensorStatus> sensorsStatus = new ArrayList<>();
    public StationStatus stationStatus;

    @Override
    public void deserialize(String input) {
        JSONObject obj = new JSONObject(input);

        if (obj.has("devs")) {
            JSONObject devices = obj.getJSONObject("devs");
            for (String serialnumber : devices.keySet()) {
                JSONObject device = devices.getJSONObject(serialnumber);
                boolean online = false;
                int battery = 0;
                int rfLevel = 0;

                if (device.has("online")) {
                    online = device.getString("online").equals("1");
                }
                if (device.has("batInfo")) {
                    battery = (int) (Double.parseDouble(device.getString("batInfo")) / 3.0 * 100.0);
                }
                if (device.has("rfLevel")) {
                    rfLevel = (int) (Double.parseDouble(device.getString("rfLevel")) / 3.0 * 4.0); // map max value 3 to
                                                                                                   // max
                    // value 4
                }
                sensorsStatus.add(new SensorStatus(serialnumber, battery, rfLevel, online));
            }
        }

        if (obj.has("wifiRSSI")) {
            stationStatus = new StationStatus(Integer.parseInt(obj.getString("wifiRSSI")));
        }
    }
}
