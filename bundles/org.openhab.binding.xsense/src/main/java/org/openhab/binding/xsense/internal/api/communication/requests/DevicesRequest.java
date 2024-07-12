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

import org.openhab.binding.xsense.internal.api.communication.BaseHttpRequest;

/**
 * The {@link DevicesRequest} represents requestdata for fetching devices
 *
 * @author Jakob Fellner - Initial contribution
 */
public class DevicesRequest extends BaseHttpRequest {
    public DevicesRequest(String houseId) {
        super(103007);

        addParameter("houseId", houseId);
        addParameter("utctimestamp", 0);
    }
}

/*
 * {
 * "appCode": "1180",
 * "appVersion": "v1.18.0_20240311",
 * "bizCode": "103007",
 * "clientType": "2",
 * "houseId": "361861ED12A011EF80F6335B9F745A86",
 * "mac": "2ad1c514e14867e871ca3a845b107c29",
 * "utctimestamp": "0"
 * }
 */
