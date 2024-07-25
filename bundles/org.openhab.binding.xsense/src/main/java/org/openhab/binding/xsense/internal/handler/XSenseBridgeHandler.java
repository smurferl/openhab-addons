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

import static org.openhab.binding.xsense.internal.XSenseBindingConstants.CHANNEL_BATTERY_LEVEL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.xsense.internal.api.ApiConstants.SubscriptionTopics;
import org.openhab.binding.xsense.internal.api.XsenseApi;
import org.openhab.binding.xsense.internal.api.data.Device;
import org.openhab.binding.xsense.internal.api.data.Houses;
import org.openhab.binding.xsense.internal.api.data.Sensor;
import org.openhab.binding.xsense.internal.api.data.Station;
import org.openhab.binding.xsense.internal.config.XSenseConfiguration;
import org.openhab.binding.xsense.internal.discovery.XSenseDiscoveryService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link XSenseBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Jakob Fellner - Initial contribution
 */
@NonNullByDefault
public class XSenseBridgeHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(XSenseBridgeHandler.class);
    private @Nullable XSenseConfiguration config;
    private final static Map<String, StateListener> stateListeners = new ConcurrentHashMap<>();
    private @Nullable ScheduledFuture<?> statePollingJob;
    private @Nullable XsenseApi api = null;
    private @Nullable XSenseDiscoveryService discoveryService;
    private final Lock lock = new ReentrantLock(true);

    public XSenseBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(XSenseDiscoveryService.class);
    }

    @SuppressWarnings("null")
    private void startStatePolling() {
        if (statePollingJob == null || statePollingJob.isCancelled()) {
            config = getConfigAs(XSenseConfiguration.class);

            long configStatusPollingInterval = config.refreshInterval;
            if (configStatusPollingInterval > 0) {
                // Delay the first execution to give a chance to have all sensor things registered
                statePollingJob = (ScheduledFuture<?>) scheduler.scheduleWithFixedDelay(statePollingRunnable, 0,
                        configStatusPollingInterval, TimeUnit.SECONDS);
            }
        }
    }

    private void stopStatePolling() {
        lock.lock();
        if (statePollingJob != null) {
            statePollingJob.cancel(true);
        }
        statePollingJob = null;
        lock.unlock();
    }

    private final Runnable statePollingRunnable = new PollingRunnable() {
        @Override
        protected void doConnectedRun() {
            lock.lock();
            List<Device> devices = getFullDevices();

            if (discoveryService != null) {
                devices.forEach(discoveryService::addDeviceDiscovery);
            }

            devices.forEach(item -> {
                Device device = (Device) item;
                StateListener listener = stateListeners.get(device.deviceSerialnumber);

                if (listener != null) {
                    listener.onUpdateDevice(device);
                }
            });
            lock.unlock();
        }
    };

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (CHANNEL_BATTERY_LEVEL.equals(channelUID.getId())) {
            if (command instanceof RefreshType) {
                // TODO: handle data refresh
            }

            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information:
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(XSenseConfiguration.class);
        if (config != null) {
            api = new XsenseApi(config.username, config.password);
        }

        updateStatus(ThingStatus.UNKNOWN);

        scheduler.execute(() -> {
            boolean thingReachable = false;

            try {
                if (api != null) {
                    api.login();
                    thingReachable = true;
                } else {
                    logger.warn("api not initialized");
                }

            } catch (Exception e) {
                logger.error("failed to login", e);
            }

            // when done do:
            if (thingReachable) {
                updateStatus(ThingStatus.ONLINE);
                startStatePolling();
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        });

        // These logging types should be primarily used by bindings
        // logger.trace("Example trace message");
        // logger.debug("Example debug message");
        // logger.warn("Example warn message");
        //
        // Logging to INFO should be avoided normally.
        // See https://www.openhab.org/docs/developer/guidelines.html#f-logging

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }

    @Override
    public void dispose() {
        stopStatePolling();
        stateListeners.clear();
        if (api != null) {
            api.logout();
        }
    }

    public List<Device> getFullDevices() {
        List<Device> ret = new ArrayList<>();
        if (api != null) {
            try {
                Houses houses = api.getHouses();
                houses.houses.values().forEach(item -> {
                    try {
                        ret.addAll(api.getDevices(item.houseId).toList());
                    } catch (Exception e) {
                        logger.warn("getDevices failed {}", e.getMessage(), e);
                    }
                });
            } catch (Exception e) {
                logger.warn("getHouses failed {}", e.getMessage(), e);
            }
        } else {
            logger.warn("uninitialized api handler");
        }

        return ret;
    }

    public void doSelfTest(String stationSerialnumber, String sensorSerialnumber) {
        scheduler.execute(() -> {
            if (api != null) {
                List<Device> devices = getFullDevices();
                Station station = null;
                Sensor sensor = null;

                for (Device device : devices) {
                    if (device instanceof Station) {
                        if (device.deviceSerialnumber.equals(stationSerialnumber)) {
                            station = (Station) device;
                        }
                    }

                    if (device instanceof Sensor) {
                        Sensor s = (Sensor) device;

                        if (s.deviceSerialnumber.equals(sensorSerialnumber)
                                && s.stationSerialnumber.equals(stationSerialnumber)) {
                            sensor = s;
                        }
                    }
                }

                if (sensor != null && station != null) {
                    api.doSelfTest(station, sensor);
                } else {
                    logger.warn("sensor or station not found for selftest");
                }
            } else {
                logger.error("api not initialized");
            }
        });
    }

    public void muteSensor(String stationSerialnumber, String sensorSerialnumber) {
        scheduler.execute(() -> {
            if (api != null) {
                List<Device> devices = getFullDevices();
                Station station = null;
                Sensor sensor = null;

                for (Device device : devices) {
                    if (device instanceof Station) {
                        if (device.deviceSerialnumber.equals(stationSerialnumber)) {
                            station = (Station) device;
                        }
                    }

                    if (device instanceof Sensor) {
                        Sensor s = (Sensor) device;

                        if (s.deviceSerialnumber.equals(sensorSerialnumber)
                                && s.stationSerialnumber.equals(stationSerialnumber)) {
                            sensor = s;
                        }
                    }
                }

                if (sensor != null && station != null) {
                    api.muteSensor(station, sensor);
                } else {
                    logger.warn("sensor or station not found for muting alarm");
                }
            } else {
                logger.error("api not initialized");
            }
        });
    }

    public void setVoicePromptVolume(String stationSerialnumber, int volume) {
        scheduler.execute(() -> {
            if (api != null) {
                List<Device> devices = getFullDevices();
                Station station = null;

                for (Device device : devices) {
                    if (device instanceof Station) {
                        if (device.deviceSerialnumber.equals(stationSerialnumber)) {
                            station = (Station) device;
                        }
                    }
                }

                if (station != null) {
                    api.setVoicePromptVolume(station, volume);
                } else {
                    logger.warn("sensor or station not found for selftest");
                }
            } else {
                logger.error("api not initialized");
            }
        });
    }

    public boolean registerStateListener(StateListener stateListener) {
        final String serialnumber = stateListener.getDeviceSerialnumber();
        if (!stateListeners.containsKey(serialnumber)) {
            stateListeners.put(serialnumber, stateListener);
            stateListener.onStateListenerAdded();

            if (thing.getStatus() == ThingStatus.ONLINE) {
                scheduler.schedule(statePollingRunnable, 0, TimeUnit.SECONDS);
            }

            return true;
        }
        return false;
    }

    public boolean unregisterStateListener(StateListener stateListener) {
        final String serialnumber = stateListener.getDeviceSerialnumber();
        if (stateListeners.containsKey(serialnumber)) {
            stateListeners.remove(serialnumber);
            stateListener.onStateListenerRemoved();

            return true;
        }
        return false;
    }

    public boolean registerDiscoveryListener(XSenseDiscoveryService listener) {
        if (discoveryService == null) {
            discoveryService = listener;
            return true;
        }

        return false;
    }

    public boolean unregisterDiscoveryListener() {
        if (discoveryService != null) {
            discoveryService = null;
            return true;
        }

        return false;
    }

    public boolean registerThingUpdateListener(String thingName, SubscriptionTopics topic,
            ThingUpdateListener listener) {
        if (thing.getStatus() == ThingStatus.ONLINE) {
            if (api != null) {
                return api.registerThingUpdateListener(thingName, topic, listener);
            }
        }

        return false;
    }

    public boolean unregisterThingUpdateListener(ThingUpdateListener listener) {
        if (api != null) {
            api.unregisterThingUpdateListener(listener);
            return true;
        }

        return false;
    }
}
