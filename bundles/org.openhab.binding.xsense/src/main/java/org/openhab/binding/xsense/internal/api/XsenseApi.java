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
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Builder;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.Nullable;
import org.json.JSONException;
import org.openhab.binding.xsense.internal.api.ApiConstants.RequestType;
import org.openhab.binding.xsense.internal.api.ApiConstants.SubscriptionTopics;
import org.openhab.binding.xsense.internal.api.communication.Authentication;
import org.openhab.binding.xsense.internal.api.communication.BaseHttpRequest;
import org.openhab.binding.xsense.internal.api.communication.BaseMqttRequest;
import org.openhab.binding.xsense.internal.api.communication.BaseRequest;
import org.openhab.binding.xsense.internal.api.communication.BaseResponse;
import org.openhab.binding.xsense.internal.api.communication.Mqtt;
import org.openhab.binding.xsense.internal.api.communication.Subscription;
import org.openhab.binding.xsense.internal.api.communication.requests.ClientInfoRequest;
import org.openhab.binding.xsense.internal.api.communication.requests.DevicesRequest;
import org.openhab.binding.xsense.internal.api.communication.requests.DevicesStatusRequest;
import org.openhab.binding.xsense.internal.api.communication.requests.HousesRequest;
import org.openhab.binding.xsense.internal.api.communication.requests.MuteRequest;
import org.openhab.binding.xsense.internal.api.communication.requests.OAuthRequest;
import org.openhab.binding.xsense.internal.api.communication.requests.RegionsRequest;
import org.openhab.binding.xsense.internal.api.communication.requests.RoomsRequest;
import org.openhab.binding.xsense.internal.api.communication.requests.SelfTestRequest;
import org.openhab.binding.xsense.internal.api.communication.requests.VoicePromptRequest;
import org.openhab.binding.xsense.internal.api.data.ClientInfo;
import org.openhab.binding.xsense.internal.api.data.Devices;
import org.openhab.binding.xsense.internal.api.data.Devices.Device;
import org.openhab.binding.xsense.internal.api.data.Devices.Sensor;
import org.openhab.binding.xsense.internal.api.data.Devices.Station;
import org.openhab.binding.xsense.internal.api.data.DevicesStatus;
import org.openhab.binding.xsense.internal.api.data.DevicesStatus.SensorStatus;
import org.openhab.binding.xsense.internal.api.data.Houses;
import org.openhab.binding.xsense.internal.api.data.Houses.House;
import org.openhab.binding.xsense.internal.api.data.OAuth;
import org.openhab.binding.xsense.internal.api.data.Regions;
import org.openhab.binding.xsense.internal.api.data.Regions.Region;
import org.openhab.binding.xsense.internal.api.data.Rooms;
import org.openhab.binding.xsense.internal.api.data.base.BaseData;
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
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Lock sessionLock = new ReentrantLock(true);
    private @Nullable ScheduledFuture<?> refreshCognitoSession = null;
    private @Nullable ScheduledFuture<?> refreshOAuthSession = null;
    private Houses houses;
    private Authentication authentication = null;
    private HttpClient apiClient;
    private String apiHost = "";
    private String userRegion = "";
    private String clientId = "";
    private String clientSecret = "";
    private String accessToken = "";
    private String refreshToken = "";
    private String userId = "";
    private long expiresIn = 0;
    private String username = "";
    private String password = "";

    private HashMap<String, Mqtt> mqttClients = new HashMap<String, Mqtt>();

    public XsenseApi(String username, String password) {
        Builder httpClientBuilder = HttpClient.newBuilder();
        if (!ApiConstants.DEBUG_PROXY_IP.isEmpty()) {
            httpClientBuilder = httpClientBuilder.proxy(ProxySelector
            .of(new InetSocketAddress(ApiConstants.DEBUG_PROXY_IP, ApiConstants.DEBUG_PROXY_PORT)));
        }

        apiClient = httpClientBuilder.build();

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
            if (result.containsKey("exiresIn")) {
                String value = result.get("exiresIn");
                if (value != null) {
                    expiresIn = TimeUnit.SECONDS.toMillis(Integer.parseInt(value));
                }
            }
            if (result.containsKey("userId")) {
                userId = result.get("userId");
            } else {
                success = false;
            }

            // only present on initial login
            if (result.containsKey("refreshToken")) {
                refreshToken = result.get("refreshToken").isEmpty() ? refreshToken : result.get("refreshToken");
            }
        } else {
            success = false;
        }

        return success;
    }

    private BaseResponse sendRequest(BaseRequest req, Class<?> type) {
        sessionLock.lock();
        String result = "";

        try {
            if (req.type() == RequestType.HTTP) {
                BaseHttpRequest httpReq = (BaseHttpRequest) req;
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiHost)).version(Version.HTTP_1_1)
                        .POST(HttpRequest.BodyPublishers.ofString(httpReq.generateJson(clientSecret)))
                        .setHeader("Authorization", accessToken).build();

                result = apiClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
            } else if (req.type() == RequestType.MQTT) {
                BaseMqttRequest<?> mqttReq = (BaseMqttRequest<?>) req;
                House house = houses.getHouse(mqttReq.houseId());
                if (house != null) {
                    if (mqttClients.containsKey(house.getMqttRegion())) {
                        Mqtt mqtt = mqttClients.get(house.getMqttRegion());

                        mqtt.sendRequest(mqttReq);
                        result = mqttReq.getResponse();
                    } else {
                        logger.warn("no mqtt connection found for region {}", house.getMqttRegion());
                    }
                } else {
                    logger.warn("house with id {} not found", mqttReq.houseId());
                }
            } else {
                result = "{\"reCode\": 401, \"reMsg\":\"unsupported requesttype\"}";
            }
        } catch (IOException | InterruptedException | JSONException | IllegalArgumentException | SecurityException
                | ExecutionException e) {
            result = "{\"reCode\": 401, \"reMsg\":\"" + e.getMessage() + "\"}";
        }

        sessionLock.unlock();
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

    private Regions getRegions() throws Exception {
        BaseResponse response = sendRequest(new RegionsRequest(), Regions.class);

        if (response.getReturnCode() != 200)
            throw new Exception(
                    "Regions Request failed " + response.getReturnCode() + ": " + response.getReturnMessage());

        return (Regions) response.getData();
    }

    public void login() throws Exception {
        authenticate();
        connectMqtt();
        houses = getHouses();
    }

    private void authenticate() throws Exception {
        HashMap<String, String> result = null;

        if (authentication == null) {
            ClientInfo data = getClientInfo();
            userRegion = data.getUserRegion();
            clientId = data.getClientId();
            clientSecret = decodeClientSecret(data.getClientSecret());

            authentication = new Authentication(userRegion, data.getUserPoolId(), clientId, clientSecret);
            result = authentication.performSRPAuthentication(username, password);
        } else {
            result = authentication.performRefreshAuthentication(refreshToken);
        }

        if (parseAuthenticationResult(result)) {
            logger.debug("cognito session expires at {}", Instant.ofEpochMilli(System.currentTimeMillis() + expiresIn)
                    .atZone(ZoneId.systemDefault()).toLocalDateTime());

            refreshCognitoSession = scheduler.schedule(new Runnable() {
                public void run() {
                    sessionLock.lock();
                    logger.debug("refresh cognito session");
                    try {
                        authenticate();
                    } catch (Exception e) {
                        logger.warn("failed to refresh cognito sesseion", e);
                    } finally {
                        sessionLock.unlock();
                    }
                }
            }, expiresIn - TimeUnit.MINUTES.toMillis(5), TimeUnit.MILLISECONDS);
        } else {
            logger.warn("parsing authentication result failed: {}", result.toString());
        }
    }

    private void connectMqtt() {
        try {
            Regions regions = getRegions();
            OAuth oAuth = getOAuth(username);

            logger.debug("oauth credentials expires at {}",
                    Instant.ofEpochMilli(System.currentTimeMillis() + oAuth.getExpiresIn())
                            .atZone(ZoneId.systemDefault()).toLocalDateTime());

            refreshOAuthSession = scheduler.schedule(new Runnable() {
                public void run() {
                    sessionLock.lock();
                    logger.debug("refresh oAuth session");
                    connectMqtt();
                    sessionLock.unlock();
                }
            }, oAuth.getExpiresIn() - TimeUnit.MINUTES.toMillis(5), TimeUnit.MILLISECONDS);

            regions.getRegions().forEach(item -> {
                Region region = (Region) item;

                Mqtt mqttClient = mqttClients.get(region.getMqttRegion());
                if (mqttClient == null) {
                    mqttClient = new Mqtt(clientId, region.getMqttServer(), region.getMqttRegion());
                    mqttClients.put(region.getMqttRegion(), mqttClient);
                }

                mqttClient.updateAuthenticationParameters(oAuth);
            });
        } catch (Exception e) {
            logger.error("failed to connect mqtt", e);
        }
    }

    public void logout() {
        if (refreshCognitoSession != null) {
            sessionLock.lock();
            refreshCognitoSession.cancel(true);
            sessionLock.unlock();
        }

        if (refreshOAuthSession != null) {
            sessionLock.lock();
            refreshOAuthSession.cancel(true);
            sessionLock.unlock();
        }

        // disconnect mqtt clients and clean them up
        mqttClients.values().forEach(item -> {
            Mqtt mqttClient = (Mqtt) item;
            mqttClient.disconnect(true);
        });
        mqttClients.clear();

        // global signout of currently used accesstoken
        if (authentication != null && !accessToken.isEmpty()) {
            if (authentication.signOutUser(accessToken)) {
                logger.debug("signed out user {}", userId);
            }
        }
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
            if (device instanceof Station) {
                response = sendRequest(new DevicesStatusRequest(device.getHouseId(), device), DevicesStatus.class);

                if (response.getReturnCode() == 200) {
                    status = (DevicesStatus) response.getData();
                    ((Station) device).setStationStatus(status.getStationStatus());

                    for (SensorStatus sensorStatus : status.getSensorStatus()) {
                        Sensor sensor = (Sensor) ((Station) device).getSensor(sensorStatus.getSerialnumber());
                        sensor.setSensorStatus(sensorStatus);
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

    public boolean setVoicePromptVolume(Device station, int volume) {
        BaseResponse response = sendRequest(new VoicePromptRequest(station, volume), BaseData.class);

        return response.getReturnCode() == 200;
    }

    public boolean doSelfTest(Station station, Sensor sensor) {
        BaseResponse response = sendRequest(new SelfTestRequest(userId, station, sensor), BaseData.class);

        return response.getReturnCode() == 200;
    }

    public boolean muteSensor(Station station, Sensor sensor) {
        BaseResponse response = sendRequest(new MuteRequest(userId, station, sensor), BaseData.class);

        return response.getReturnCode() == 200;
    }

    private boolean registerEventListener(String mqttRegion, Subscription subscription, EventListener listener) {
        Mqtt mqttClient = mqttClients.get(mqttRegion);

        if (mqttClient != null) {
            return mqttClient.registerEventListener(subscription, listener);
        } else {
            logger.warn("failed to get mqttclient for region {}", mqttRegion);
        }

        return false;
    }

    public boolean registerEventListener(String mqttRegion, SubscriptionTopics topic, EventListener listener) {
        String t = topic.toString().replace("{userId}", userId);

        Subscription subscription = new Subscription(topic.getDataClass(), t);

        return registerEventListener(mqttRegion, subscription, listener);
    }

    public boolean registerEventListener(String houseId, String thing, SubscriptionTopics topic,
            EventListener listener) {
        House house = houses.getHouse(houseId);
        if (house != null) {
            String t = topic.toString().replace("{thing}", thing);

            Subscription subscription = new Subscription(topic.getDataClass(), t);

            registerEventListener(house.getMqttRegion(), subscription, listener);
        } else {
            logger.warn("failed to get house with id {}", houseId);
        }

        return false;
    }

    public void unregisterEventListener(EventListener listener) {
        for (Mqtt mqtt : mqttClients.values()) {
            mqtt.unregisterEventListener(listener);
        }
    }

    public boolean matchCurrentToken(String accessToken) {
        return accessToken.equals(this.accessToken);
    }

    public String getUserRegion() {
        return userRegion;
    }
}
