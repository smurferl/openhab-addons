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
import org.openhab.binding.xsense.internal.api.data.Devices.Device;

import software.amazon.awssdk.iot.iotshadow.model.ShadowState;
import software.amazon.awssdk.iot.iotshadow.model.UpdateNamedShadowRequest;

/**
 * The {@link VoicePromptRequest} represents requestdata enabling/disabling voiceprompt of basestation
 *
 * @author Jakob Fellner - Initial contribution
 */
public class VoicePromptRequest extends BaseMqttRequest<UpdateNamedShadowRequest> {
    UpdateNamedShadowRequest getNamedShadowRequest;

    public VoicePromptRequest(Device station, int volume) {
        super(station.getHouseId(), ShadowRequestType.UPDATE);

        ShadowState shadowState = new ShadowState();
        shadowState.desired = new HashMap<String, Object>();
        shadowState.desired.put("shadow", "infoBase");
        shadowState.desired.put("stationSN", station.getDeviceSerialnumber());
        shadowState.desired.put("voiceVol", Integer.toString(volume));

        shadowRequest = new UpdateNamedShadowRequest();
        shadowRequest.thingName = station.getDeviceType().toString() + station.getDeviceSerialnumber();
        shadowRequest.shadowName = "2nd_cfg_" + station.getDeviceSerialnumber();
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
