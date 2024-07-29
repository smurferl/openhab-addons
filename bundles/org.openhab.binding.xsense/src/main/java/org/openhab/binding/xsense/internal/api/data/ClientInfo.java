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
import org.openhab.binding.xsense.internal.api.data.base.BaseData;

/**
 * The {@link ClientInfo} encapsulates relevant data returned for the clientinfo request
 *
 * @author Jakob Fellner - Initial contribution
 */
public class ClientInfo extends BaseData {
    public String region = "";
    public String clientId = "";
    public String clientSecret = "";
    public String userPoolId = "";

    @Override
    public void deserialize(String input) {
        JSONObject obj = new JSONObject(input);

        region = obj.getJSONObject("reData").getString("cgtRegion");
        clientId = obj.getJSONObject("reData").getString("clientId");
        clientSecret = obj.getJSONObject("reData").getString("clientSecret");
        userPoolId = obj.getJSONObject("reData").getString("userPoolId");
    }
}
