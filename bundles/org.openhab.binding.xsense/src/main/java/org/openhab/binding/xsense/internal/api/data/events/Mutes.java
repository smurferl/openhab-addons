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
 * The {@link Mutes} represents all results of smokedetector alarms mutes
 *
 * @author Jakob Fellner - Initial contribution
 */
public class Mutes extends BaseSubscriptionMessage {

    public class MuteEvent extends BaseEvent {
        private String trigger = "";

        public MuteEvent(String stationSerialnumber, String sensorSerialnumber, String trigger) {
            super(stationSerialnumber, sensorSerialnumber);
            this.trigger = trigger;
        }

        public MuteEvent(String sensorSerialnumber, String trigger) {
            super(sensorSerialnumber);
            this.trigger = trigger;
        }

        public String getTrigger() {
            return trigger;
        }
    }

    @Override
    public void deserialize(String input) {
        JSONObject obj = new JSONObject(input);

        if (obj.has("state")) {
            JSONObject state = obj.getJSONObject("state");

            if (state.has("reported")) {
                JSONObject reported = state.getJSONObject("reported");

                if (reported.has("allMute")) {
                    JSONObject result = reported.getJSONObject("allMute");

                    if (result.has("deviceSN") && result.has("who")) {
                        if (result.has("stationSN")) {
                            addEvent(result.getString("stationSN") + result.getString("deviceSN"),
                                    new MuteEvent(result.getString("stationSN"), result.getString("deviceSN"),
                                            result.getString("who")));
                        } else {
                            addEvent(result.getString("deviceSN"),
                                    new MuteEvent(result.getString("deviceSN"), result.getString("who")));
                        }
                    }
                }
            }
        }
    }
}
