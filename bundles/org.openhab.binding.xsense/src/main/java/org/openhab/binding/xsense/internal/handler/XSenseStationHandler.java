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

import static org.openhab.binding.xsense.internal.XSenseBindingConstants.CHANNEL_SIGNAL_STRENGTH;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.xsense.internal.api.data.Device;
import org.openhab.binding.xsense.internal.api.data.Station;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link XSenseStationHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Jakob Fellner - Initial contribution
 */
@NonNullByDefault
public class XSenseStationHandler extends BaseThingHandler implements StateListener {
    private final Logger logger = LoggerFactory.getLogger(XSenseStationHandler.class);

    public XSenseStationHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (CHANNEL_SIGNAL_STRENGTH.equals(channelUID.getId())) {
            if (command instanceof RefreshType) {
                // TODO: handle data refresh
            }
        }
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.UNKNOWN);

        scheduler.execute(() -> {
            // thing will get online after devicepolling starts
            Bridge bridge = getBridge();
            if (bridge != null) {
                XSenseBridgeHandler bridgeHandler = (XSenseBridgeHandler) bridge.getHandler();
                if (bridgeHandler != null) {
                    bridgeHandler.registerStateListener(this);
                }
            }
        });
    }

    @Override
    public void dispose() {
        Bridge bridge = getBridge();
        if (bridge != null) {
            XSenseBridgeHandler bridgeHandler = (XSenseBridgeHandler) bridge.getHandler();
            if (bridgeHandler != null) {
                bridgeHandler.unregisterStateListener(this);
            }
        }
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        Bridge bridge = getBridge();
        if (bridge != null) {
            XSenseBridgeHandler bridgeHandler = (XSenseBridgeHandler) bridge.getHandler();
            if (bridgeHandler != null) {
                if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
                    bridgeHandler.registerStateListener(this);
                } else {
                    bridgeHandler.unregisterStateListener(this);
                }
            }
        }
    }

    @Override
    public void onStateListenerAdded() {
    }

    @Override
    public void onStateListenerRemoved() {
    }

    @Override
    public void onUpdateDevice(Device device) {
        Station station = (Station) device;

        if (station.online) {
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }

        updateState(CHANNEL_SIGNAL_STRENGTH, new DecimalType(station.wifiRSSI));
    }

    @Override
    public String getDeviceSerialnumber() {
        String serialnumber = getThing().getProperties().get("serialnumber");

        return serialnumber == null ? "" : serialnumber;
    }
}
