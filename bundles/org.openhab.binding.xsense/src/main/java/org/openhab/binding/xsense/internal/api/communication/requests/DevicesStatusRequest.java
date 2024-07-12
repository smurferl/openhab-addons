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

import org.openhab.binding.xsense.internal.api.ApiConstants.ShadowRequestType;
import org.openhab.binding.xsense.internal.api.communication.BaseMqttRequest;
import org.openhab.binding.xsense.internal.api.data.Device;

import software.amazon.awssdk.iot.iotshadow.model.GetNamedShadowRequest;

/**
 * The {@link DevicesStatusRequest} represents requestdata for fetching the status for connected devices
 *
 * @author Jakob Fellner - Initial contribution
 */
public class DevicesStatusRequest extends BaseMqttRequest<GetNamedShadowRequest> {
    GetNamedShadowRequest getNamedShadowRequest;

    public DevicesStatusRequest(String houseId, Device device) {
        super(houseId, ShadowRequestType.GET);

        shadowRequest = new GetNamedShadowRequest();
        shadowRequest.thingName = device.deviceType.toString() + device.deviceSerialnumber;
        shadowRequest.shadowName = "2nd_mainpage";
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
