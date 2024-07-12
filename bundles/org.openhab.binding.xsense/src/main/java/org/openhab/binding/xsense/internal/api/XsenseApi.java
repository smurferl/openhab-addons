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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.openhab.binding.xsense.internal.api.ApiConstants.RequestType;
import org.openhab.binding.xsense.internal.api.ApiConstants.SubscriptionTopics;
import org.openhab.binding.xsense.internal.api.communication.BaseHttpRequest;
import org.openhab.binding.xsense.internal.api.communication.BaseMqttRequest;
import org.openhab.binding.xsense.internal.api.communication.BaseRequest;
import org.openhab.binding.xsense.internal.api.communication.BaseResponse;
import org.openhab.binding.xsense.internal.api.communication.Mqtt;
import org.openhab.binding.xsense.internal.api.communication.requests.ClientInfoRequest;
import org.openhab.binding.xsense.internal.api.communication.requests.DevicesRequest;
import org.openhab.binding.xsense.internal.api.communication.requests.DevicesStatusRequest;
import org.openhab.binding.xsense.internal.api.communication.requests.HousesRequest;
import org.openhab.binding.xsense.internal.api.communication.requests.OAuthRequest;
import org.openhab.binding.xsense.internal.api.communication.requests.RoomsRequest;
import org.openhab.binding.xsense.internal.api.communication.requests.SelfTestRequest;
import org.openhab.binding.xsense.internal.api.communication.requests.VoicePromptRequest;
import org.openhab.binding.xsense.internal.api.communication.utils.Authentication;
import org.openhab.binding.xsense.internal.api.data.BaseData;
import org.openhab.binding.xsense.internal.api.data.ClientInfo;
import org.openhab.binding.xsense.internal.api.data.Device;
import org.openhab.binding.xsense.internal.api.data.Devices;
import org.openhab.binding.xsense.internal.api.data.DevicesStatus;
import org.openhab.binding.xsense.internal.api.data.House;
import org.openhab.binding.xsense.internal.api.data.Houses;
import org.openhab.binding.xsense.internal.api.data.OAuth;
import org.openhab.binding.xsense.internal.api.data.Rooms;
import org.openhab.binding.xsense.internal.api.data.Sensor;
import org.openhab.binding.xsense.internal.api.data.SensorStatus;
import org.openhab.binding.xsense.internal.api.data.Station;
import org.openhab.binding.xsense.internal.handler.ThingUpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.util.Base64;

/**
 * The {@link XsenseApi} class represent entrypoint to use xsense api
 *
 * @author Jakob Fellner - Initial contribution
 */
public class XsenseApi {
    private final Logger logger = LoggerFactory.getLogger(XsenseApi.class);
    private Authentication authentication = null;
    private HttpClient apiClient;
    private String apiHost = "";
    private String region = "";
    private String clientId = "";
    private String clientSecret = "";
    private String accessToken = "";
    private String refreshToken = "";
    private long expiration = 0;
    private String username = "";
    private String password = "";

    private HashMap<String, Mqtt> mqttClients = new HashMap<String, Mqtt>();

    public XsenseApi(String username, String password) {
        apiClient = HttpClient.newHttpClient();
        this.apiHost = ApiConstants.API_HOST;
        this.username = username;
        this.password = password;
    }

    static private String decodeClientSecret(String clientSecret) {
        String secretDecoded = new String(Base64.decode(clientSecret));
        return secretDecoded.substring(4, secretDecoded.length() - 1);
    }

    private boolean parseAuthenticationResult(HashMap<String, String> result) {
        boolean success = true;

        if (result != null) {
            if (result.containsKey("accessToken")) {
                accessToken = result.get("accessToken");
            } else {
                success = false;
            }
            if (result.containsKey("refreshToken")) {
                refreshToken = result.get("refreshToken").isEmpty() ? refreshToken : result.get("refreshToken");
            } else {
                success = false;
            }
            if (result.containsKey("exiresIn")) {
                String value = result.get("exiresIn");
                if (value != null) {
                    int expiresIn = Integer.parseInt(value);
                    long now = System.currentTimeMillis();
                    expiration = now + TimeUnit.SECONDS.toMillis(expiresIn);
                }
            } else {
                success = false;
            }
        } else {
            success = false;
        }

        return success;
    }

    private boolean needAuthenticationRefresh() {
        long now = System.currentTimeMillis();
        long expires = expiration - TimeUnit.SECONDS.toMillis(60);

        return now > expires;
    }

    private BaseResponse sendRequest(BaseRequest req, Class<?> type) {
        String result = "";

        try {
            if (req.type() == RequestType.HTTP) {
                if (authentication != null && needAuthenticationRefresh()) {
                    HashMap<String, String> res = authentication.PerformRefreshAuthentication(refreshToken);
                    if (!parseAuthenticationResult(res)) {
                        logger.warn("parsing authentication result for refreshing failed: {}", res.toString());
                    }

                    connectMqtt(true);
                }

                BaseHttpRequest httpReq = (BaseHttpRequest) req;
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiHost)).version(Version.HTTP_1_1)
                        .POST(HttpRequest.BodyPublishers.ofString(httpReq.generateJson(clientSecret)))
                        .setHeader("Authorization", accessToken).build();

                result = apiClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
            } else if (req.type() == RequestType.MQTT) {
                BaseMqttRequest<?> mqttReq = (BaseMqttRequest<?>) req;
                if (mqttClients.containsKey(mqttReq.houseId())) {
                    Mqtt mqtt = mqttClients.get(mqttReq.houseId());

                    mqtt.sendRequest(mqttReq);
                    result = mqttReq.getResponse();
                }
            } else {
                result = "{\"reCode\": 401, \"reMsg\":\"unsupported requesttype\"}";
            }
        } catch (IOException | InterruptedException | JSONException | IllegalArgumentException | SecurityException
                | ExecutionException e) {
            result = "{\"reCode\": 401, \"reMsg\":\"" + e.getMessage() + "\"}";
        }

        return new BaseResponse(type, result);
    }

    private ClientInfo getClientInfo() throws Exception {
        BaseResponse response = sendRequest(new ClientInfoRequest(), ClientInfo.class);

        if (response.getReturnCode() != 200)
            throw new Exception(
                    "ClientInfo Request failed " + response.getReturnCode() + ": " + response.getReturnMessage());

        return (ClientInfo) response.getData();
    }

    private OAuth getOAuth(String username) throws Exception {
        BaseResponse response = sendRequest(new OAuthRequest(username), OAuth.class);

        if (response.getReturnCode() != 200)
            throw new Exception(
                    "OAuth Request failed " + response.getReturnCode() + ": " + response.getReturnMessage());

        return (OAuth) response.getData();
    }

    public void login() throws Exception {
        authenticate();
        connectMqtt(false);
    }

    private void authenticate() throws Exception {
        ClientInfo data = getClientInfo();
        region = data.region;
        clientId = data.clientId;
        clientSecret = decodeClientSecret(data.clientSecret);

        authentication = new Authentication(region, data.userPoolId, clientId, clientSecret);
        HashMap<String, String> result = authentication.PerformSRPAuthentication(username, password);
        if (!parseAuthenticationResult(result)) {
            logger.warn("parsing authentication result for srp failed: {}", result.toString());
        }
    }

    private void connectMqtt(boolean reconnect) {
        try {
            Houses houses = getHouses();
            OAuth oAuth = getOAuth(username);

            houses.houses.values().forEach(item -> {
                House house = (House) item;

                if (reconnect) {
                    if (mqttClients.containsKey(house.houseId)) {
                        Mqtt client = mqttClients.get(house.houseId);
                        if (client != null) {
                            client.connect(oAuth.accessKeyId, oAuth.secretAccessKey, oAuth.sessionToken, clientId,
                                    house.mqttServer, house.mqttRegion);
                        }
                    }
                } else {
                    Mqtt mqtt = new Mqtt();
                    if (mqtt.connect(oAuth.accessKeyId, oAuth.secretAccessKey, oAuth.sessionToken, clientId,
                            house.mqttServer, house.mqttRegion)) {
                        mqttClients.put(house.houseId, mqtt);
                    }
                }
            });
        } catch (Exception e) {
            logger.error("failed to connect mqtt", e);
        }
    }

    public void logout() {
        mqttClients.values().forEach(item -> {
            Mqtt mqttClient = (Mqtt) item;
            mqttClient.disconnect(true);
        });

        // TODO send logout request
    }

    public Houses getHouses() throws Exception {
        BaseResponse response = sendRequest(new HousesRequest(), Houses.class);

        if (response.getReturnCode() != 200)
            throw new Exception(
                    "Houses Request failed " + response.getReturnCode() + ": " + response.getReturnMessage());

        return (Houses) response.getData();
    }

    public Rooms getRooms(String houseId) throws Exception {
        BaseResponse response = sendRequest(new RoomsRequest(houseId), Rooms.class);

        if (response.getReturnCode() != 200)
            throw new Exception(
                    "Rooms Request failed " + response.getReturnCode() + ": " + response.getReturnMessage());

        return (Rooms) response.getData();
    }

    public Devices getDevices(String houseId) throws Exception {
        BaseResponse response = sendRequest(new DevicesRequest(houseId), Devices.class);

        if (response.getReturnCode() != 200)
            throw new Exception(
                    "Devices Request failed " + response.getReturnCode() + ": " + response.getReturnMessage());

        Devices devices = (Devices) response.getData();

        DevicesStatus status = null;
        for (Device device : devices.devices.values()) {
            device.houseId = houseId;
            if (device instanceof Station) {
                response = sendRequest(new DevicesStatusRequest(houseId, device), DevicesStatus.class);

                if (response.getReturnCode() == 200) {
                    status = (DevicesStatus) response.getData();
                    ((Station) device).wifiRSSI = status.stationStatus.rssi;

                    for (SensorStatus sensorStatus : status.sensorsStatus) {
                        Sensor sensor = (Sensor) ((Station) device).getSensor(sensorStatus.serialnumber);
                        sensor.houseId = houseId;
                        sensor.batteryInfo = sensorStatus.battery;
                        sensor.rfLevel = sensorStatus.rfLevel;
                        sensor.online = sensorStatus.online;
                    }
                } else {
                    logger.warn("DevicesStatus Request failed {}: {}", response.getReturnCode(),
                            response.getReturnMessage());
                }
            } else {
                // TODO handle directly added sensors
            }
        }

        return devices;
    }

    public boolean setVoicePromptEnabled(String houseId, Device station, boolean enabled) {
        BaseResponse response = sendRequest(new VoicePromptRequest(houseId, station, enabled), BaseData.class);

        return response.getReturnCode() == 200;
    }

    public boolean doSelfTest(String houseId, Station station, Sensor sensor) {
        BaseResponse response = sendRequest(new SelfTestRequest(houseId, station, sensor), BaseData.class);

        return response.getReturnCode() == 200;
    }

    public boolean registerThingUpdateListner(String thing, SubscriptionTopics topic, ThingUpdateListener listener) {
        String t = topic.toString().replace("{}", thing);

        for (Mqtt mqtt : mqttClients.values()) {
            if (!mqtt.registerThingUpdateListner(t, listener)) {
                return false;
            }
        }

        return true;
    }

    public void unregisterThingUpdateListener(ThingUpdateListener listener) {
        for (Mqtt mqtt : mqttClients.values()) {
            mqtt.unregisterThingUpdateListener(listener);
        }
    }
}
