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
package org.openhab.binding.xsense.internal.api.data.events;

import org.openhab.binding.xsense.internal.api.data.Devices.Device;

/**
 * The {@link DeviceEvent} Event containing data for one specific device
 *
 * @author Jakob Fellner - Initial contribution
 */
public class DeviceEvent {
    private Device device;

    public DeviceEvent(Device device) {
        this.device = device;
    }

    public Device getDevice() {
        return device;
    }
}
