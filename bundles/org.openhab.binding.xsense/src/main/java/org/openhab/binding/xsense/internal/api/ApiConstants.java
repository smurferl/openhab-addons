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
package org.openhab.binding.xsense.internal.api;

import org.openhab.binding.xsense.internal.api.data.events.Alarms;
import org.openhab.binding.xsense.internal.api.data.events.Login;
import org.openhab.binding.xsense.internal.api.data.events.Mutes;
import org.openhab.binding.xsense.internal.api.data.events.SelfTestResults;

/**
 * The {@link ApiConstants} class defines common constants uses for the xsense api
 *
 * @author Jakob Fellner - Initial contribution
 */
public final class ApiConstants {
    public static String CLIENT_TYPE = "2";
    public static String APP_VERSION = "v1.18.0_20240311";
    public static String APP_CODE = "1180";
    public static String API_HOST = "https://api.x-sense-iot.com/app";
    public static String DEBUG_PROXY_IP = "";
    public static int DEBUG_PROXY_PORT = 0;

    public enum DeviceType {
        UNKNOWN("UNKNOWN"),
        SBS50("SBS50"),
        XS01_M("XS01M");

        private final String type;

        private DeviceType(String t) {
            type = t;
        }

        public boolean equalsType(String otherType) {
            return type.equals(otherType);
        }

        public String toString() {
            return this.type;
        }
    }

    public enum ShadowRequestType {
        GET,
        UPDATE
    }

    public enum RequestType {
        HTTP,
        MQTT
    }

    public enum EventType {
        AWS,
        CLAYBOX,
        XSENSE;
    }

    public enum SubscriptionTopics {
        SELFTEST("$aws/things/{thing}/shadow/name/2nd_selftestup/update", SelfTestResults.class),
        ALARM("$aws/things/{thing}/shadow/name/2nd_safealarm/update", Alarms.class),
        MUTE("$aws/things/{thing}/shadow/name/2nd_muteup/update", Mutes.class),
        LOGIN("@claybox/events/apptoken/{userId}", Login.class);

        private final String topic;
        private final Class<?> dataClass;

        private SubscriptionTopics(String t, Class<?> d) {
            topic = t;
            dataClass = d;
        }

        public boolean equalsType(String otherTopic) {
            return topic.equals(otherTopic);
        }

        public String toString() {
            return this.topic;
        }

        public Class<?> getDataClass() {
            return dataClass;
        }
    }
}
