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
package org.openhab.binding.xsense.internal.api.communication;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jdt.annotation.Nullable;
import org.json.JSONObject;
import org.openhab.binding.xsense.internal.api.data.BaseData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link BaseResponse} minimum representation for responses received via mqtt or http rest interface
 *
 * @author Jakob Fellner - Initial contribution
 */
public class BaseResponse {
    private final Logger logger = LoggerFactory.getLogger(BaseResponse.class);
    private int returnCode;
    private String returnMessage;
    private @Nullable BaseData data = null;

    public BaseResponse(Class<?> type, String input) {
        if (type != null) {
            try {
                data = (BaseData) type.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            }
        }

        if (!input.isEmpty()) {
            JSONObject obj = new JSONObject(input);

            returnCode = obj.has("reCode") ? obj.getInt("reCode") : 500;
            returnMessage = obj.has("reMsg") ? obj.getString("reMsg") : "invalid message structure: " + input;

            if (returnCode == 200 && returnMessage.equals("success !")) {
                if (data != null) {
                    data.deserialize(input);
                }
            } else {
                logger.error("error {} in response: {}", returnCode, returnMessage);
            }
        } else {
            logger.warn("empty response");
        }
    }

    public BaseData getData() {
        return data;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public String getReturnMessage() {
        return returnMessage;
    }
}
