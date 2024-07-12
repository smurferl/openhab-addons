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

import org.openhab.binding.xsense.internal.api.ApiConstants.RequestType;

/**
 * The {@link BaseRequest} bare minimum base for http and mqtt requests to xsense backend
 *
 * @author Jakob Fellner - Initial contribution
 */
public class BaseRequest {
    private RequestType type;

    public BaseRequest(RequestType type) {
        this.type = type;
    }

    public RequestType type() {
        return type;
    }
}
