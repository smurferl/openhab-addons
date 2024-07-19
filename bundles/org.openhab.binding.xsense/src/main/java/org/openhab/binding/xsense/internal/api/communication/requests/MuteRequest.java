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
package org.openhab.binding.xsense.internal.api.communication.requests;

import java.util.HashMap;

import org.openhab.binding.xsense.internal.api.ApiConstants.ShadowRequestType;
import org.openhab.binding.xsense.internal.api.communication.BaseMqttRequest;
import org.openhab.binding.xsense.internal.api.data.Sensor;
import org.openhab.binding.xsense.internal.api.data.Station;

import software.amazon.awssdk.iot.iotshadow.model.ShadowState;
import software.amazon.awssdk.iot.iotshadow.model.UpdateNamedShadowRequest;

/**
 * The {@link SelfTestRequest} represents requestdata for triggerin the device selftest
 *
 * @author Jakob Fellner - Initial contribution
 */
public class MuteRequest extends BaseMqttRequest<UpdateNamedShadowRequest> {
    UpdateNamedShadowRequest getNamedShadowRequest;

    public MuteRequest(Station station, Sensor sensor) {
        super(sensor.houseId, ShadowRequestType.UPDATE);

        ShadowState shadowState = new ShadowState();
        shadowState.desired = new HashMap<String, Object>();
        shadowState.desired.put("shadow", "appMute");
        shadowState.desired.put("deviceSN", sensor.deviceSerialnumber);
        shadowState.desired.put("stationSN", station.deviceSerialnumber);
        shadowState.desired.put("userId", station.userId);
        shadowState.desired.put("muteType", "0");

        shadowRequest = new UpdateNamedShadowRequest();
        shadowRequest.thingName = station.deviceType.toString() + station.deviceSerialnumber;
        shadowRequest.shadowName = "2nd_appmute";
        shadowRequest.state = shadowState;
        shadowRequest.clientToken = token;
    }

    @Override
    public String thingName() {
        return shadowRequest.thingName;
    }

    @Override
    public String shadowName() {
        return shadowRequest.shadowName;
    }
}
