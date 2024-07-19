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
import static org.openhab.binding.xsense.internal.XSenseBindingConstants.CHANNEL_COMMAND;
import static org.openhab.binding.xsense.internal.XSenseBindingConstants.CHANNEL_CONDITION;
import static org.openhab.binding.xsense.internal.XSenseBindingConstants.CHANNEL_SIGNAL_STRENGTH;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.xsense.internal.api.ApiConstants.SubscriptionTopics;
import org.openhab.binding.xsense.internal.api.data.Alarm;
import org.openhab.binding.xsense.internal.api.data.BaseSubscriptionDeviceData;
import org.openhab.binding.xsense.internal.api.data.Device;
import org.openhab.binding.xsense.internal.api.data.Mute;
import org.openhab.binding.xsense.internal.api.data.SelfTestResult;
import org.openhab.binding.xsense.internal.api.data.Sensor;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link XSenseSensorHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Jakob Fellner - Initial contribution
 */
@NonNullByDefault
public class XSenseSensorHandler extends BaseThingHandler implements StateListener, ThingUpdateListener {
    private final Logger logger = LoggerFactory.getLogger(XSenseSensorHandler.class);

    public XSenseSensorHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        Bridge bridge = getBridge();
        if (bridge != null) {
            XSenseBridgeHandler bridgeHandler = (XSenseBridgeHandler) bridge.getHandler();
            if (bridgeHandler != null) {
                String sensorSerialnumber = getThing().getProperties().get("serialnumber");
                String stationSerialnumber = getThing().getProperties().get("station_serialnumber");

                sensorSerialnumber = sensorSerialnumber == null ? "" : sensorSerialnumber;
                stationSerialnumber = stationSerialnumber == null ? "" : stationSerialnumber;

                if (CHANNEL_COMMAND.equals(channelUID.getId())) {
                    if (command.equals("TEST")) {
                        bridgeHandler.doSelfTest(stationSerialnumber, sensorSerialnumber);
                    } else if (command.equals("MUTE")) {
                        bridgeHandler.muteSensor(stationSerialnumber, sensorSerialnumber);
                    } else {
                        logger.info("invalid sensor command {}", command);
                    }
                }
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
                    String thingName = getThingName();

                    bridgeHandler.registerStateListener(this);
                    bridgeHandler.registerThingUpdateListener(thingName, SubscriptionTopics.SELFTEST, this);
                    bridgeHandler.registerThingUpdateListener(thingName, SubscriptionTopics.ALARM, this);
                    bridgeHandler.registerThingUpdateListener(thingName, SubscriptionTopics.MUTE, this);
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
                bridgeHandler.unregisterThingUpdateListener(this);
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
                    String thingName = getThingName();

                    bridgeHandler.registerStateListener(this);
                    bridgeHandler.registerThingUpdateListener(thingName, SubscriptionTopics.SELFTEST, this);
                    bridgeHandler.registerThingUpdateListener(thingName, SubscriptionTopics.ALARM, this);
                    bridgeHandler.registerThingUpdateListener(thingName, SubscriptionTopics.MUTE, this);
                } else {
                    bridgeHandler.unregisterStateListener(this);
                    bridgeHandler.unregisterThingUpdateListener(this);
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
        Sensor sensor = (Sensor) device;

        if (sensor.online) {
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }

        updateState(CHANNEL_SIGNAL_STRENGTH, new DecimalType(sensor.rfLevel));
        updateState(CHANNEL_BATTERY_LEVEL, new DecimalType(sensor.batteryInfo));
    }

    @Override
    public String getDeviceSerialnumber() {
        String serialnumber = getThing().getProperties().get("serialnumber");

        return serialnumber == null ? "" : serialnumber;
    }

    public String getStationSerialnumber() {
        String serialnumber = getThing().getProperties().get("station_serialnumber");

        return serialnumber == null ? "" : serialnumber;
    }

    @Override
    public void thingUpdateReceived(BaseSubscriptionDeviceData data) {
        if (data instanceof SelfTestResult) {
            SelfTestResult selfTestResult = (SelfTestResult) data;

            triggerChannel(CHANNEL_CONDITION, selfTestResult.success ? "SELFTEST_OK" : "SELFTEST_FAILED");
            logger.info("selftest result for {} {}: {}", selfTestResult.stationSerialnumber,
                    selfTestResult.deviceSerialnumber, selfTestResult.success);
        } else if (data instanceof Alarm) {
            Alarm alarm = (Alarm) data;

            triggerChannel(CHANNEL_CONDITION, alarm.isAlarm ? "ALARM_ON" : "ALARM_OFF");
            logger.info("alarm for {} {}: {}", alarm.stationSerialnumber, alarm.deviceSerialnumber, alarm.isAlarm);
        } else if (data instanceof Mute) {
            Mute mute = (Mute) data;

            triggerChannel(CHANNEL_CONDITION, "MUTED");
            logger.info("muted for {} {}: {}", mute.stationSerialnumber, mute.deviceSerialnumber, mute.trigger);
        } else {
            logger.warn("unknown subscriptiondata type received {}", data.getClass().toString());
        }
    }

    private String getThingName() {
        Map<String, String> properties = getThing().getProperties();
        String thingName = "";

        if (properties.containsKey("station_type") && properties.containsKey("station_serialnumber")) {
            thingName = properties.get("station_type") + properties.get("station_serialnumber");
        } else {
            thingName = getThing().getThingTypeUID() + properties.get("serialnumber");
        }

        return thingName;
    }
}
