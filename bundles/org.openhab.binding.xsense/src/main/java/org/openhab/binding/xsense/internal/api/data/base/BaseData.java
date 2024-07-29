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
package org.openhab.binding.xsense.internal.api.data.base;

/**
 * The {@link BaseData} Basis for all datamodels available from xsense api and providing the deserialisation interface
 *
 * @author Jakob Fellner - Initial contribution
 */
public abstract class BaseData {
    public abstract void deserialize(String input);
}
