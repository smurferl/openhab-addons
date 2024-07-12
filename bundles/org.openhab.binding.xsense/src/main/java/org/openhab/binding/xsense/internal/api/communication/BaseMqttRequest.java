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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;
import org.openhab.binding.xsense.internal.api.ApiConstants.RequestType;
import org.openhab.binding.xsense.internal.api.ApiConstants.ShadowRequestType;

/**
 * The {@link BaseMqttRequest} representing base for requests, sent to xense aws iot backend via mqtt
 *
 * @author Jakob Fellner - Initial contribution
 */
public abstract class BaseMqttRequest<T> extends BaseRequest {
    private CompletableFuture<String> gotResponse;
    private ShadowRequestType type;
    private String houseId;
    protected String token;
    protected T shadowRequest;

    public BaseMqttRequest(String houseId, ShadowRequestType type) {
        super(RequestType.MQTT);

        this.houseId = houseId;
        this.type = type;
        gotResponse = new CompletableFuture<>();

        token = UUID.randomUUID().toString();
    }

    public T getShadowRequest() {
        return shadowRequest;
    }

    public String houseId() {
        return houseId;
    }

    public String token() {
        return token;
    }

    public ShadowRequestType shadowType() {
        return type;
    }

    public void completeRequest(String response) {
        gotResponse.complete(response);
    }

    public String getResponse() throws InterruptedException, ExecutionException {
        String response = "";

        try {
            response = gotResponse.get(5000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            JSONObject obj = new JSONObject();
            obj.put("reCode", 500);
            obj.put("reMsg", "timeout occured for " + shadowRequest.toString());
            response = obj.toString();
        }

        return response;
    }

    abstract public String thingName();

    abstract public String shadowName();
}
