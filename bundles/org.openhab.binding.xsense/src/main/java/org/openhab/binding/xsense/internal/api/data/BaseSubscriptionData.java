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

import java.util.HashMap;

/**
 * The {@link BaseSubscriptionData} Basis for all datamodels available from xsense mqtt subscriptions without explicit
 * request
 *
 * @author Jakob Fellner - Initial contribution
 */
public abstract class BaseSubscriptionData extends BaseData {
    protected HashMap<String, BaseSubscriptionDeviceData> responses = new HashMap<String, BaseSubscriptionDeviceData>();

    public BaseSubscriptionDeviceData responseForSerialnumber(String serialnumber) {
        return responses.get(serialnumber);
    }

    protected void addResponse(String serialnumber, BaseSubscriptionDeviceData response) {
        responses.put(serialnumber, response);
    }
}
