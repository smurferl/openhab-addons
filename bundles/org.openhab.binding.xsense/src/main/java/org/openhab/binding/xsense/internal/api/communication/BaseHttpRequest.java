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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

import org.json.JSONObject;
import org.openhab.binding.xsense.internal.api.ApiConstants;
import org.openhab.binding.xsense.internal.api.ApiConstants.RequestType;

/**
 * The {@link BaseHttpRequest} Base class for requests sent to xsense api via http rest
 *
 * @author Jakob Fellner - Initial contribution
 */
public abstract class BaseHttpRequest extends BaseRequest {
    private JSONObject parameters;
    private String macBase = "";

    public BaseHttpRequest(int bizCode) {
        super(RequestType.HTTP);

        parameters = new JSONObject();

        parameters.put("appCode", ApiConstants.APP_CODE);
        parameters.put("appVersion", ApiConstants.APP_VERSION);
        parameters.put("bizCode", String.valueOf(bizCode));
        parameters.put("clientType", ApiConstants.CLIENT_TYPE);
    }

    protected void addParameter(String key, Object value) {
        parameters.put(key, value);
        macBase = macBase + value.toString();
    }

    private String generateMac(String secretHash) {
        String mac = "0";

        try {
            if (!secretHash.isEmpty()) {
                MessageDigest md = MessageDigest.getInstance("MD5");

                macBase = macBase + secretHash;

                md.update(macBase.getBytes());
                byte[] digest = md.digest();

                mac = DatatypeConverter.printHexBinary(digest).toUpperCase();
            }
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        }

        return mac;
    }

    public String generateJson(String secretHash) {
        parameters.put("mac", generateMac(secretHash));

        return parameters.toString();
    }
}
