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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Login} represents reply in case another device logs in with same credentials as used by the binding
 *
 * @author Jakob Fellner - Initial contribution
 */
public class Login extends BaseSubscriptionMessage {
    private final Logger logger = LoggerFactory.getLogger(Login.class);

    public class LoginEvent extends BaseEvent {
        private String accessToken = "";
        private String userId = "";

        public LoginEvent(String identifier, String accessToken, String userId) {
            super(identifier);

            this.accessToken = accessToken;
            this.userId = userId;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getUserId() {
            return userId;
        }
    }

    @Override
    public void deserialize(String input) {
        JSONObject obj = new JSONObject(input);

        if (obj.has("accessToken") && obj.has("userId")) {
            addEvent("", new LoginEvent("", obj.getString("accessToken"), obj.getString("userId")));
        } else {
            logger.error("invalid additionalLogin reply {}", obj.toString());
        }
    }
}
