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
package org.openhab.binding.xsense.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link XSenseBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Jakob Fellner - Initial contribution
 */
@NonNullByDefault
public class XSenseBindingConstants {

    private static final String BINDING_ID = "xsense";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public static final ThingTypeUID THING_TYPE_XS01M = new ThingTypeUID(BINDING_ID, "XS01M");
    public static final ThingTypeUID THING_TYPE_SBS50 = new ThingTypeUID(BINDING_ID, "SBS50");

    // List of all Channel ids
    public static final String CHANNEL_BATTERY_LEVEL = "battery-level";
    public static final String CHANNEL_SIGNAL_STRENGTH = "signal-strength";
    public static final String CHANNEL_COMMAND = "command";
    public static final String CHANNEL_CONDITION = "condition";
}
