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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.json.JSONObject;
import org.openhab.binding.xsense.internal.api.data.base.BaseData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link OAuth} represents all data relevant, received by the oauth request. contained information is needed for
 * mqtt connection to aws iot
 *
 * @author Jakob Fellner - Initial contribution
 */
public class OAuth extends BaseData {
    private String accessKeyId = "";
    private long expiresIn = 0;
    private String secretAccessKey = "";
    private String sessionToken = "";
    private final Logger logger = LoggerFactory.getLogger(OAuth.class);

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    @Override
    public void deserialize(String input) {
        JSONObject obj = new JSONObject(input);

        if (obj.has("reData")) {
            JSONObject response = obj.getJSONObject("reData");

            if (response.has("accessKeyId") && response.has("secretAccessKey") && response.has("sessionToken")
                    && response.has("expiration")) {
                accessKeyId = response.getString("accessKeyId");
                secretAccessKey = response.getString("secretAccessKey");
                sessionToken = response.getString("sessionToken");

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZZZZZ");
                ZonedDateTime expiresAt = ZonedDateTime.parse(response.getString("expiration"), formatter);

                expiresIn = expiresAt.toInstant().toEpochMilli() - System.currentTimeMillis();
            } else {
                logger.error("invalid authdata {}", response.toString());
            }
        } else {
            logger.error("no responseData found", obj.toString());
        }
    }
}
