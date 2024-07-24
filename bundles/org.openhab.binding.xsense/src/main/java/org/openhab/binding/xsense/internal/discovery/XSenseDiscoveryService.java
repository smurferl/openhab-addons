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

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.xsense.internal.XSenseBindingConstants;
import org.openhab.binding.xsense.internal.api.ApiConstants.DeviceType;
import org.openhab.binding.xsense.internal.api.data.Device;
import org.openhab.binding.xsense.internal.api.data.Sensor;
import org.openhab.binding.xsense.internal.handler.XSenseBridgeHandler;
import org.openhab.core.config.discovery.AbstractThingHandlerDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
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
public class XSenseDiscoveryService extends AbstractThingHandlerDiscoveryService<XSenseBridgeHandler> {
    private final Logger logger = LoggerFactory.getLogger(XSenseDiscoveryService.class);
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Set.of(XSenseBindingConstants.THING_TYPE_BRIDGE,
            XSenseBindingConstants.THING_TYPE_XS01M);
    private static final int SEARCH_TIME = 10;

    public XSenseDiscoveryService() {
        super(XSenseBridgeHandler.class, SUPPORTED_THING_TYPES, SEARCH_TIME);
    }

    @Override
    public void initialize() {
        thingHandler.registerDiscoveryListener(this);
        super.initialize();
    }

    @Override
    public void dispose() {
        super.dispose();
        removeOlderResults(Instant.now().toEpochMilli(), thingHandler.getThing().getUID());
        thingHandler.unregisterDiscoveryListener();
    }

    @Override
    protected void startScan() {
        List<Device> devices = thingHandler.getFullDevices();
        for (Device device : devices) {
            addDeviceDiscovery(device);
        }
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        removeOlderResults(getTimestampOfLastScan(), thingHandler.getThing().getUID());
    }

    public void addDeviceDiscovery(Device device) {
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
                .create(new ThingUID("xsense:" + device.deviceType + ":" + thingHandler.getThing().getUID().getId()
                        + ":" + device.deviceId.toLowerCase()))
                .withThingType(new ThingTypeUID("xsense:" + device.deviceType)).withProperties(properties)
                .withBridge(thingHandler.getThing().getUID()).withRepresentationProperty("serialnumber")
                .withLabel(device.deviceName).build();

        thingDiscovered(discoveryResult);
    }
}
