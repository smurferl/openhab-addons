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
import org.openhab.binding.xsense.internal.api.data.BaseSubscriptionDeviceData;

/**
 * The {@link ThingUpdateListener} is notified when a mqtt message arrives for the listener registerd for the specific
 * topic
 *
 * @author Jakob Fellner - Initial contribution
 */
@NonNullByDefault
public interface ThingUpdateListener {
    void thingUpdateReceived(BaseSubscriptionDeviceData data);
}
