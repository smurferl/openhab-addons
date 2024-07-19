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

    public enum SubscriptionTopics {
        SELFTEST("$aws/things/{}/shadow/name/2nd_selftestup/update"),
        ALARM("$aws/things/{}/shadow/name/2nd_safealarm/update"),
        MUTE("$aws/things/{}/shadow/name/2nd_muteup/update");

        private final String topic;

        private SubscriptionTopics(String t) {
            topic = t;
        }

        public boolean equalsType(String otherTopic) {
            return topic.equals(otherTopic);
        }

        public String toString() {
            return this.topic;
        }

        public String getShadowName() {
            String[] parts = topic.split("/");
            if (parts.length >= 6) {
                return parts[5];
            }

            return "";
        }
    }
}
