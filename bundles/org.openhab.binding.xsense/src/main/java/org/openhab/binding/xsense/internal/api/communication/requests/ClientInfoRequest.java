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
 * The {@link ClientInfoRequest} represents requestdata for fetching clientinfo
 *
 * @author Jakob Fellner - Initial contribution
 */
public class ClientInfoRequest extends BaseHttpRequest {
    public ClientInfoRequest() {
        super(101001);
    }
}
