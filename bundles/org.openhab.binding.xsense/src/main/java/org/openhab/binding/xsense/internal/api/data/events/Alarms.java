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
package org.openhab.binding.xsense.internal.api.data.events;

import org.json.JSONObject;
import org.openhab.binding.xsense.internal.api.data.base.BaseEvent;
import org.openhab.binding.xsense.internal.api.data.base.BaseSubscriptionMessage;

/**
 * The {@link Alarms} represents all results of smokedetector alarms
 *
 * @author Jakob Fellner - Initial contribution
 */
public class Alarms extends BaseSubscriptionMessage {

    public class AlarmEvent extends BaseEvent {
        private boolean isAlarm = false;

        public AlarmEvent(String stationSerialnumber, String sensorSerialnumber, boolean isAlarm) {
            super(stationSerialnumber, sensorSerialnumber);
            this.isAlarm = isAlarm;
        }

        public AlarmEvent(String sensorSerialnumber, boolean isAlarm) {
            super(sensorSerialnumber);
            this.isAlarm = isAlarm;
        }

        public boolean isAlarm() {
            return isAlarm;
        }
    }

    @Override
    public void deserialize(String input) {
        JSONObject obj = new JSONObject(input);

        if (obj.has("state")) {
            JSONObject state = obj.getJSONObject("state");

            if (state.has("reported")) {
                JSONObject reported = state.getJSONObject("reported");

                for (String serialnumber : reported.keySet()) {
                    JSONObject result = reported.getJSONObject(serialnumber);

                    if (result.has("deviceSN") && result.has("isAlarm")) {
                        // assumed to only be present for basestation connected sensors
                        if (result.has("stationSN")) {
                            addEvent(result.getString("stationSN") + result.getString("deviceSN"),
                                    new AlarmEvent(result.getString("stationSN"), result.getString("deviceSN"),
                                            result.getString("isAlarm").equals("1") ? true : false));
                        } else {
                            addEvent(result.getString("deviceSN"), new AlarmEvent(result.getString("deviceSN"),
                                    result.getString("isAlarm").equals("1") ? true : false));
                        }
                    }
                }
            }
        }
    }
}
