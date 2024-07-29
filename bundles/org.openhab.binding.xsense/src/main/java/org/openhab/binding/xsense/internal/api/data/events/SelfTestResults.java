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
 * The {@link SelfTestResults} represents all results of smokedetector selftests
 *
 * @author Jakob Fellner - Initial contribution
 */
public class SelfTestResults extends BaseSubscriptionMessage {

    public class SelfTestResultEvent extends BaseEvent {
        private boolean success = false;

        public SelfTestResultEvent(String stationSerialnumber, String sensorSerialnumber, boolean success) {
            super(stationSerialnumber, sensorSerialnumber);
            this.success = success;
        }

        public SelfTestResultEvent(String sensorSerialnumber, boolean success) {
            super(sensorSerialnumber);
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
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

                    if (result.has("deviceSN") && result.has("selfTest")) {
                        // assumed to only be present for basestation connected sensors
                        if (result.has("stationSN")) {
                            addEvent(result.getString("stationSN") + result.getString("deviceSN"),
                                    new SelfTestResultEvent(result.getString("stationSN"), result.getString("deviceSN"),
                                            result.getString("selfTest").equals("0") ? true : false));
                        } else {
                            addEvent(result.getString("deviceSN"), new SelfTestResultEvent(result.getString("deviceSN"),
                                    result.getString("selfTest").equals("0") ? true : false));
                        }
                    }
                }
            }
        }
    }
}
