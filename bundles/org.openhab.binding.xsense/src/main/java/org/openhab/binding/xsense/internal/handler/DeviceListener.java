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
package org.openhab.binding.xsense.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.xsense.internal.api.data.Devices.Device;

/**
 * The {@link DeviceListener} is notified when a xsense sensor state has changed or a sensor has been removed or added.
 * sent to one of the channels.
 *
 * @author Jakob Fellner - Initial contribution
 */
@NonNullByDefault
public interface DeviceListener {
    void onStateListenerAdded();

    void onStateListenerRemoved();

    void onUpdateDevice(Device device);

    String getDeviceSerialnumber();
}
