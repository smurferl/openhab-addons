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
import org.openhab.binding.xsense.internal.api.EventListener;
import org.openhab.binding.xsense.internal.api.XsenseApi;
import org.openhab.binding.xsense.internal.api.data.Devices.Device;
import org.openhab.binding.xsense.internal.api.data.Devices.Sensor;
import org.openhab.binding.xsense.internal.api.data.Devices.Station;
import org.openhab.binding.xsense.internal.api.data.Houses;
import org.openhab.binding.xsense.internal.api.data.base.BaseEvent;
import org.openhab.binding.xsense.internal.api.data.events.Login.LoginEvent;
import org.openhab.binding.xsense.internal.config.XSenseConfiguration;
import org.openhab.binding.xsense.internal.discovery.XSenseDiscoveryService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link XSenseBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Jakob Fellner - Initial contribution
 */
@NonNullByDefault
public class XSenseBridgeHandler extends BaseBridgeHandler implements EventListener {
    private final Logger logger = LoggerFactory.getLogger(XSenseBridgeHandler.class);
    private @Nullable XSenseConfiguration config;
    private final static Map<String, DeviceListener> deviceListeners = new ConcurrentHashMap<>();
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

            devices.forEach(item -> {
                if (item instanceof Station) {
                    Station station = (Station) item;
                    sendDeviceToListener(station);

                    for (Sensor sensor : station.getSensors()) {
                        sendDeviceToListener(sensor);
                    }
                } else {
                    sendDeviceToListener(item);
                }
            });
            lock.unlock();
        }
    };

    private void sendDeviceToListener(Device device) {
        DeviceListener listener = deviceListeners.get(device.getDeviceSerialnumber());

        if (listener != null) {
            listener.onUpdateDevice(device);
        }

        if (discoveryService != null) {
            discoveryService.addDeviceDiscovery(device);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
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
            String error = "";

            if (api != null) {
                try {
                    api.login();
                    api.registerEventListener(api.getUserRegion(), SubscriptionTopics.LOGIN, this);
                    thingReachable = true;
                } catch (Exception e) {
                    error = e.getMessage();
                    logger.warn("failed to login: {}", error);
                }
            } else {
                logger.warn("api not initialized");
            }

            if (thingReachable) {
                updateStatus(ThingStatus.ONLINE);
                startStatePolling();
            } else {
                if (error.isEmpty()) {
                    updateStatus(ThingStatus.OFFLINE);
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, error);
                }
            }
        });
    }

    @Override
    public void dispose() {
        stopStatePolling();
        deviceListeners.clear();
        if (api != null) {
            api.logout();
        }
    }

    public List<Device> getFullDevices() {
        List<Device> ret = new ArrayList<>();
        if (api != null) {
            try {
                Houses houses = api.getHouses();
                houses.getHouses().forEach(item -> {
                    try {
                        ret.addAll(api.getDevices(item.getHouseId()).toList());
                    } catch (Exception e) {
                        logger.warn("getDevices failed {}", e.getMessage());
                    }
                });
            } catch (Exception e) {
                logger.warn("getHouses failed {}", e.getMessage());
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
                        if (device.getDeviceSerialnumber().equals(stationSerialnumber)) {
                            station = (Station) device;
                            sensor = station.getSensor(sensorSerialnumber);
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
                        if (device.getDeviceSerialnumber().equals(stationSerialnumber)) {
                            station = (Station) device;
                            sensor = station.getSensor(sensorSerialnumber);
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
                        if (device.getDeviceSerialnumber().equals(stationSerialnumber)) {
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

    public boolean registerDeviceListener(DeviceListener deviceListener) {
        final String serialnumber = deviceListener.getDeviceSerialnumber();
        if (!deviceListeners.containsKey(serialnumber)) {
            deviceListeners.put(serialnumber, deviceListener);
            deviceListener.onStateListenerAdded();

            if (thing.getStatus() == ThingStatus.ONLINE) {
                scheduler.schedule(statePollingRunnable, 0, TimeUnit.SECONDS);
            }

            return true;
        }
        return false;
    }

    public boolean unregisterDeviceListener(DeviceListener deviceListener) {
        final String serialnumber = deviceListener.getDeviceSerialnumber();
        if (deviceListeners.containsKey(serialnumber)) {
            deviceListeners.remove(serialnumber);
            deviceListener.onStateListenerRemoved();

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

    public boolean registerEventListener(String houseId, String thingName, SubscriptionTopics topic,
            EventListener listener) {
        if (thing.getStatus() == ThingStatus.ONLINE) {
            if (api != null) {
                return api.registerEventListener(houseId, thingName, topic, listener);
            }
        }

        return false;
    }

    public boolean unregisterEventListener(EventListener listener) {
        if (api != null) {
            api.unregisterEventListener(listener);
            return true;
        }

        return false;
    }

    @Override
    public void eventReceived(BaseEvent event) {
        if (event instanceof LoginEvent) {
            LoginEvent loginEvent = (LoginEvent) event;

            if (api != null) {
                if (!api.matchCurrentToken(loginEvent.getAccessToken())) {
                    logger.warn("force logout for userid {}", loginEvent.getUserId());

                    stopStatePolling();

                    scheduler.schedule(() -> {
                        if (api != null) {
                            api.logout();
                            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                    "another device logged in");
                        }
                    }, 0, TimeUnit.MILLISECONDS);
                }
            }
        } else {
            logger.warn("unknown subscriptiondata type received {}", event.getClass().toString());
        }
    }

    @Override
    public String getEventIdentifier() {
        return "";
    }
}
