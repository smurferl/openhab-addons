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
package org.openhab.binding.xsense.internal.discovery;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.xsense.internal.XSenseBindingConstants;
import org.openhab.binding.xsense.internal.api.ApiConstants.DeviceType;
import org.openhab.binding.xsense.internal.api.data.Device;
import org.openhab.binding.xsense.internal.api.data.Sensor;
import org.openhab.binding.xsense.internal.handler.XSenseBridgeHandler;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link XSenseDiscoveryService} keeps track of xsense devices (basestations and sensors) which are fetched by the
 * api bridge
 *
 * @author Jakob Fellner - Initial contribution
 */
@Component(scope = ServiceScope.PROTOTYPE, service = XSenseDiscoveryService.class)
@NonNullByDefault
public class XSenseDiscoveryService extends AbstractDiscoveryService implements ThingHandlerService {
    private final Logger logger = LoggerFactory.getLogger(XSenseDiscoveryService.class);
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Set.of(XSenseBindingConstants.THING_TYPE_BRIDGE,
            XSenseBindingConstants.THING_TYPE_XS01M);
    private static final int SEARCH_TIME = 10;
    private @Nullable XSenseBridgeHandler bridgeHandler;

    public XSenseDiscoveryService() {
        super(SUPPORTED_THING_TYPES, SEARCH_TIME, true);
    }

    @Override
    public void activate() {
        if (bridgeHandler != null) {
            bridgeHandler.registerDiscoveryListener(this);
        }
    }

    @Override
    public void deactivate() {
        if (bridgeHandler != null) {
            removeOlderResults(new Date().getTime(), bridgeHandler.getThing().getUID());
            bridgeHandler.unregisterDiscoveryListener();
        }
    }

    @Override
    protected void startScan() {
        if (bridgeHandler != null) {
            List<Device> devices = bridgeHandler.getFullDevices();
            for (Device device : devices) {
                addDeviceDiscovery(device);
            }
        }
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        if (bridgeHandler != null) {
            removeOlderResults(getTimestampOfLastScan(), bridgeHandler.getThing().getUID());
        }
    }

    public void addDeviceDiscovery(Device device) {
        if (bridgeHandler != null) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("serialnumber", device.deviceSerialnumber);
            if (device instanceof Sensor) {
                String stationSerialnumber = ((Sensor) device).stationSerialnumber;
                DeviceType stationType = ((Sensor) device).stationType;
                if (!stationSerialnumber.isEmpty()) {
                    properties.put("station_serialnumber", stationSerialnumber);
                    properties.put("station_type", stationType.toString());
                }
            }

            DiscoveryResult discoveryResult = DiscoveryResultBuilder
                    .create(new ThingUID("xsense:" + device.deviceType + ":" + bridgeHandler.getThing().getUID().getId()
                            + ":" + device.deviceId))
                    .withThingType(new ThingTypeUID("xsense:" + device.deviceType)).withProperties(properties)
                    .withBridge(bridgeHandler.getThing().getUID()).withRepresentationProperty("serialnumber")
                    .withLabel(device.deviceName).build();

            thingDiscovered(discoveryResult);
        }
    }

    @Override
    public void setThingHandler(ThingHandler handler) {
        if (handler instanceof XSenseBridgeHandler) {
            bridgeHandler = (XSenseBridgeHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridgeHandler;
    }
}
