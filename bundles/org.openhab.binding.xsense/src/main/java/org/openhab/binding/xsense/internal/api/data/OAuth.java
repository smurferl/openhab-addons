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

import org.json.JSONObject;

/**
 * The {@link OAuth} represents all data relevant, received by the oauth request. contained information is needed for
 * mqtt connection to aws iot
 *
 * @author Jakob Fellner - Initial contribution
 */
public class OAuth extends BaseData {
    public String accessKeyId = "";
    public String expiration = "";
    public String secretAccessKey = "";
    public String sessionToken = "";

    @Override
    public void deserialize(String input) {
        JSONObject obj = new JSONObject(input);

        accessKeyId = obj.getJSONObject("reData").getString("accessKeyId");
        expiration = obj.getJSONObject("reData").getString("expiration");
        secretAccessKey = obj.getJSONObject("reData").getString("secretAccessKey");
        sessionToken = obj.getJSONObject("reData").getString("sessionToken");
    }
}
