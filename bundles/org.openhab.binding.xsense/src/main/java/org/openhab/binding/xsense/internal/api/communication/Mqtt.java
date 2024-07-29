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
package org.openhab.binding.xsense.internal.api.communication;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONObject;
import org.openhab.binding.xsense.internal.api.ApiConstants.ShadowRequestType;
import org.openhab.binding.xsense.internal.api.EventListener;
import org.openhab.binding.xsense.internal.api.data.OAuth;
import org.openhab.binding.xsense.internal.api.data.base.BaseEvent;
import org.openhab.binding.xsense.internal.api.data.base.BaseSubscriptionMessage;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.crt.CRT;
import software.amazon.awssdk.crt.auth.credentials.StaticCredentialsProvider.StaticCredentialsProviderBuilder;
import software.amazon.awssdk.crt.http.HttpProxyOptions;
import software.amazon.awssdk.crt.http.HttpProxyOptions.HttpProxyConnectionType;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;
import software.amazon.awssdk.crt.mqtt.MqttClientConnectionEvents;
import software.amazon.awssdk.crt.mqtt.MqttMessage;
import software.amazon.awssdk.crt.mqtt.OnConnectionClosedReturn;
import software.amazon.awssdk.crt.mqtt.OnConnectionFailureReturn;
import software.amazon.awssdk.crt.mqtt.OnConnectionSuccessReturn;
import software.amazon.awssdk.crt.mqtt.QualityOfService;
import software.amazon.awssdk.iot.AwsIotMqttConnectionBuilder;
import software.amazon.awssdk.iot.iotshadow.IotShadowClient;
import software.amazon.awssdk.iot.iotshadow.model.ErrorResponse;
import software.amazon.awssdk.iot.iotshadow.model.GetNamedShadowRequest;
import software.amazon.awssdk.iot.iotshadow.model.GetNamedShadowSubscriptionRequest;
import software.amazon.awssdk.iot.iotshadow.model.GetShadowResponse;
import software.amazon.awssdk.iot.iotshadow.model.UpdateNamedShadowRequest;
import software.amazon.awssdk.iot.iotshadow.model.UpdateNamedShadowSubscriptionRequest;
import software.amazon.awssdk.iot.iotshadow.model.UpdateShadowResponse;

/**
 * The {@link Mqtt} handles connection to aws iot mqtt host and makes use of aws iot shadow system to also act as
 * request/response basis
 *
 * @author Jakob Fellner - Initial contribution
 */
public class Mqtt implements MqttClientConnectionEvents {
    private final Logger logger = LoggerFactory.getLogger(Mqtt.class);
    private MqttClientConnection client = null;
    private IotShadowClient shadow = null;

    private HashMap<String, BaseMqttRequest<?>> openRequests;
    private HashMap<Subscription, ArrayList<EventListener>> updateListeners;
    private HashSet<String> subscribedTopics;

    private String clientId = "";
    private String host = "";
    private String region = "";

    private final Lock lock = new ReentrantLock(true);

    public Mqtt(String clientId, String host, String region) {
        openRequests = new HashMap<>();
        updateListeners = new HashMap<>();
        subscribedTopics = new HashSet<>();

        this.clientId = clientId;
        this.host = host;
        this.region = region;
    }

    public boolean connect(OAuth oAuth) {
        boolean success = false;
        boolean reconnect = false;

        if (client != null) {
            disconnect(false);
            // direct subscriptions, starting with $aws are kept to resubscribe after reconnect/sessiontimeout
            subscribedTopics.removeIf(t -> !t.startsWith("$aws"));
            reconnect = true;
            logger.debug("reconnect to mqtt host for region {}", region);
        } else {
            subscribedTopics.clear();
        }

        client = createMqttClientConnection(oAuth.accessKeyId, oAuth.secretAccessKey, oAuth.sessionToken, clientId,
                host, region);

        if (client != null) {
            CompletableFuture<Boolean> connected = client.connect();
            try {
                connected.get();

                if (reconnect) {
                    if (!resubscribe()) {
                        logger.warn("resubscription failed for at least one topic");
                    }
                }

                success = true;
                // JSONObject v;
                // selftest
                /*
                 * subscribe("$aws/things/SBS50139A1F89/shadow/name/2nd_selftestup/update");
                 * subscribe("$aws/things/SBS50139A1F89/shadow/name/2nd_selftest_00000004/update/accepted");
                 * subscribe("$aws/things/SBS50139A1F89/shadow/name/2nd_selftest_00000004/update/rejected");
                 * v = new JSONObject(
                 * "{\"state\":{\"desired\":{\"deviceSN\":\"00000004\",\"shadow\":\"appSelfTest\",\"stationSN\":\"139A1F89\",\"time\":\"1717165906724\",\"userId\":\"c05d6905-2b22-4bd9-ad92-61de5d1191b9\"}}}"
                 * );
                 * publish("$aws/things/SBS50139A1F89/shadow/name/2nd_selftest_00000004/update", v.toString());
                 */

                // mute key basestation
                /*
                 * subscribe("$aws/things/SBS50139A1F89/shadow/name/2nd_mutekeyup/update");
                 * subscribe("$aws/things/SBS50139A1F89/shadow/name/2nd_mutekey/update/accepted");
                 * subscribe("$aws/things/SBS50139A1F89/shadow/name/2nd_mutekey/update/rejected");
                 * v = new JSONObject(
                 * "{\"state\":{\"desired\":{\"muteKeyEnable\":\"1\",\"shadow\":\"muteKey\",\"stationSN\":\"139A1F89\",\"time\":\"20240531151041\",\"userId\":\"c05d6905-2b22-4bd9-ad92-61de5d1191b9\"}}}"
                 * );
                 * publish("$aws/things/SBS50139A1F89/shadow/name/2nd_mutekey/update", v.toString());
                 */

                // things infos
                /*
                 * subscribe("$aws/things/SBS50139A1F89/shadow/name/2nd_mainpage/get/accepted");
                 * subscribe("$aws/things/SBS50139A1F89/shadow/name/2nd_mainpage/get/rejected");
                 * publish("$aws/things/SBS50139A1F89/shadow/name/2nd_mainpage/get", "");
                 */

                // basestation infos
                /*
                 * subscribe("$aws/things/SBS50139A1F89/shadow/name/2nd_info_139A1F89/get/accepted");
                 * subscribe("$aws/things/SBS50139A1F89/shadow/name/2nd_info_139A1F89/get/rejected");
                 * publish("$aws/things/SBS50139A1F89/shadow/name/2nd_info_139A1F89/get", "");
                 */

                // device infos
                /*
                 * subscribe("$aws/things/SBS50139A1F89/shadow/name/2nd_info_00000001/get/accepted");
                 * subscribe("$aws/things/SBS50139A1F89/shadow/name/2nd_info_00000001/get/rejected");
                 * publish("$aws/things/SBS50139A1F89/shadow/name/2nd_info_00000001/get", "");
                 */
            } catch (Exception e) {
                logger.error("failed connect to mqtt broker", e);
            }
        } else {
            logger.error("failed to create mqtt connection");
        }

        return success;
    }

    public void disconnect(boolean clearListeners) {
        try {
            logger.debug("disconnecting mqtt connection for region {}", region);

            if (clearListeners) {
                updateListeners.clear();

                /*
                 * HashSet<String> unsubscribeTopics = subscribedTopics;
                 * unsubscribeTopics.forEach(topic -> {
                 * unsubscribe(topic);
                 * });
                 * logger.debug("subscribed topics {}", subscribedTopics.size());
                 */
            }

            CompletableFuture<Void> disconnected = client.disconnect();
            disconnected.get();

            openRequests.values().forEach(request -> {
                request.completeRequest("{\"reCode\": 402, \"reMsg\":\"mqtt disconnected\"}");
            });
            openRequests.clear();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("failed to disconnect mqtt connection", e);
        }
    }

    private MqttClientConnection createMqttClientConnection(String accessKey, String secretAccessKey,
            String sessionToken, String clientId, String host, String region) {

        HttpProxyOptions proxyOptions = new HttpProxyOptions();
        proxyOptions.setConnectionType(HttpProxyConnectionType.Tunneling);
        proxyOptions.setHost("192.168.1.151");
        proxyOptions.setPort(8888);

        StaticCredentialsProviderBuilder providerBuilder = new StaticCredentialsProviderBuilder();
        providerBuilder.withAccessKeyId(accessKey.getBytes());
        providerBuilder.withSecretAccessKey(secretAccessKey.getBytes());
        providerBuilder.withSessionToken(sessionToken.getBytes());

        return AwsIotMqttConnectionBuilder.newMtlsBuilderFromPath(null, null).withConnectionEventCallbacks(this)
                .withEndpoint(host).withClientId(clientId).withWebsocketSigningRegion(region).withWebsockets(true)
                .withPort(443).withHttpProxyOptions(proxyOptions)
                .withWebsocketCredentialsProvider(providerBuilder.build()).withKeepAliveSecs(30).build();
    }

    public void sendRequest(BaseMqttRequest<?> request) {
        if (shadow != null) {
            try {
                subscribeTopic(request.thingName(), request.shadowName(), request.shadowType());

                if (request.shadowType() == ShadowRequestType.GET) {
                    openRequests.put(request.token, request);
                    shadow.PublishGetNamedShadow((GetNamedShadowRequest) request.getShadowRequest(),
                            QualityOfService.AT_LEAST_ONCE).get();
                } else if (request.shadowType() == ShadowRequestType.UPDATE) {
                    openRequests.put(request.token, request);
                    shadow.PublishUpdateNamedShadow((UpdateNamedShadowRequest) request.getShadowRequest(),
                            QualityOfService.AT_LEAST_ONCE).get();
                } else {
                    openRequests.remove(request.token);
                    request.completeRequest("{\"reCode\": 402, \"reMsg\":\"unsupported mqtt requesttype\"}");
                }
            } catch (InterruptedException | ExecutionException e) {
                openRequests.remove(request.token);
                request.completeRequest("{\"reCode\": 402, \"reMsg\":\"send request to mqtt failed for "
                        + request.thingName() + " " + request.shadowName() + " " + e.getMessage() + "\"}");
                logger.error("failed to send mqtt request {}/{}: {}", request.thingName(), request.shadowName(),
                        e.getMessage());
            }
        } else {
            openRequests.remove(request.token);
            request.completeRequest("{\"reCode\": 402, \"reMsg\":\"send request failed as mqtt not connected for "
                    + request.thingName() + " " + request.shadowName() + "\"}");
        }
    }

    public boolean registerThingUpdateListener(Subscription subscription, EventListener listener) {
        String topic = subscription.getTopic();

        if (subscribe(topic)) {
            ArrayList<EventListener> listeners = null;

            if (updateListeners.containsKey(subscription)) {
                listeners = updateListeners.get(subscription);
            } else {
                listeners = new ArrayList<>();
            }

            listeners.add(listener);
            updateListeners.put(subscription, listeners);

            logger.debug("registered listener {} for topic {}", ((BaseThingHandler) listener).getThing().getLabel(),
                    topic);

            return true;
        }
        return false;
    }

    public void unregisterThingUpdateListener(EventListener listener) {
        lock.lock();
        ArrayList<Subscription> keysToRemove = new ArrayList<>();
        updateListeners.keySet().forEach(subscription -> {
            ArrayList<EventListener> listeners = updateListeners.get(subscription);
            if (listeners.remove(listener)) {
                if (listeners.isEmpty()) {
                    keysToRemove.add(subscription);
                } else {
                    updateListeners.put(subscription, listeners);
                }

                logger.debug("unregistered listener {} for topic {}",
                        ((BaseThingHandler) listener).getThing().getLabel(), subscription.getTopic());
            }
        });

        keysToRemove.forEach(subscription -> {
            unsubscribe(subscription.getTopic());
            updateListeners.remove(subscription);
        });
        lock.unlock();
    }

    private boolean subscribeTopic(String thingName, String shadowName, ShadowRequestType type) {
        boolean success = false;

        try {
            if (shadow != null) {
                if (!subscribedTopics.contains(thingName + shadowName + type)) {
                    if (type == ShadowRequestType.GET) {
                        GetNamedShadowSubscriptionRequest requestGetNamedShadow = new GetNamedShadowSubscriptionRequest();
                        requestGetNamedShadow.thingName = thingName;
                        requestGetNamedShadow.shadowName = shadowName;

                        CompletableFuture<Integer> subscribedToGetNamedShadowAccepted = shadow
                                .SubscribeToGetNamedShadowAccepted(requestGetNamedShadow,
                                        QualityOfService.AT_LEAST_ONCE, this::onGetShadowAccepted);
                        CompletableFuture<Integer> subscribedToGetNamedShadowRejected = shadow
                                .SubscribeToGetNamedShadowRejected(requestGetNamedShadow,
                                        QualityOfService.AT_LEAST_ONCE, this::onGetShadowRejected);

                        subscribedToGetNamedShadowAccepted.get();
                        subscribedToGetNamedShadowRejected.get();
                    }
                    if (type == ShadowRequestType.UPDATE) {
                        UpdateNamedShadowSubscriptionRequest requestUpdateNamedShadow = new UpdateNamedShadowSubscriptionRequest();
                        requestUpdateNamedShadow.thingName = thingName;
                        requestUpdateNamedShadow.shadowName = shadowName;

                        CompletableFuture<Integer> subscribedToUpdateAccepted = shadow
                                .SubscribeToUpdateNamedShadowAccepted(requestUpdateNamedShadow,
                                        QualityOfService.AT_LEAST_ONCE, this::onUpdateShadowAccepted);
                        CompletableFuture<Integer> subscribedToUpdateRejected = shadow
                                .SubscribeToUpdateNamedShadowRejected(requestUpdateNamedShadow,
                                        QualityOfService.AT_LEAST_ONCE, this::onUpdateShadowRejected);

                        subscribedToUpdateAccepted.get();
                        subscribedToUpdateRejected.get();
                    }

                    subscribedTopics.add(thingName + shadowName + type);
                }

                success = true;
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("failed to subscribe to mqtt topic {}/{}", thingName, shadowName, e);
        }

        return success;
    }

    private boolean unsubscribe(String topic) {
        try {
            if (subscribedTopics.contains(topic)) {
                CompletableFuture<Integer> unsubscribed = client.unsubscribe(topic);
                unsubscribed.get();
                subscribedTopics.remove(topic);
                logger.debug("unsubscribed mqtt topic {}", topic);
                return true;
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("failed to unsubscribe to mqtt topic {}", topic, e);
        }

        return false;
    }

    private boolean subscribe(String topic) {
        boolean success = false;

        try {
            lock.lock();
            if (!subscribedTopics.contains(topic)) {
                CompletableFuture<Integer> subscribed = client.subscribe(topic, QualityOfService.AT_LEAST_ONCE,
                        this::onMqttMessageReceived);
                subscribed.get();
                subscribedTopics.add(topic);
                logger.debug("subscribed to mqtt topic {}", topic);
            }
            success = true;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("failed to subscribe to mqtt topic {}", topic, e);
        } finally {
            lock.unlock();
        }

        return success;
    }

    private boolean resubscribe() {
        boolean success = true;

        HashSet<String> resubscribedTopics = new HashSet<>();

        for (String topic : subscribedTopics) {
            if (topic.startsWith("$aws")) {
                resubscribedTopics.add(topic);
            }
        }

        for (String topic : resubscribedTopics) {
            subscribedTopics.remove(topic);
            if (!subscribe(topic)) {
                success = false;
            }
        }

        return success;
    }

    @Override
    public void onConnectionSuccess(OnConnectionSuccessReturn data) {
        shadow = new IotShadowClient(client);
        logger.debug("connect to mqtt successful for region {}", region);
    }

    @Override
    public void onConnectionFailure(OnConnectionFailureReturn data) {
        logger.warn("failed to connect to mqtt for region {} with error {}: {}", region, data.getErrorCode(),
                CRT.awsErrorString(data.getErrorCode()));
    }

    @Override
    public void onConnectionClosed(OnConnectionClosedReturn data) {
        logger.debug("mqtt connection closed for region {}", region);

        shadow = null;

        clientId = "";
        host = "";
        region = "";
    }

    @Override
    public void onConnectionInterrupted(int errorCode) {
        if (errorCode != 0) {
            logger.warn("mqtt connection for region {} interrupted with error {}:{}", region, errorCode,
                    CRT.awsErrorString(errorCode));

            openRequests.values().forEach(request -> {
                request.completeRequest("{\"reCode\": 402, \"reMsg\":\"mqtt interrupted\"}");
            });
            openRequests.clear();
        }
    }

    @Override
    public void onConnectionResumed(boolean sessionPresent) {
        logger.debug("mqtt connection for region {} resumed: {}", region,
                sessionPresent ? "existing session" : "clean session");
    }

    private void onGetShadowAccepted(GetShadowResponse response) {
        if (openRequests.containsKey(response.clientToken)) {
            JSONObject obj = new JSONObject(response.state.reported);
            obj.put("reCode", 200);
            obj.put("reMsg", "success !");

            openRequests.get(response.clientToken).completeRequest(obj.toString());
            openRequests.remove(response.clientToken);
        } else {
            logger.warn("getShadowAccepted response received with unknown token {}", response.clientToken);
        }
    }

    private void onGetShadowRejected(ErrorResponse response) {
        if (openRequests.containsKey(response.clientToken)) {
            JSONObject obj = new JSONObject();
            obj.put("reCode", response.code);
            obj.put("reMsg", response.message);

            openRequests.get(response.clientToken).completeRequest(obj.toString());
            openRequests.remove(response.clientToken);
        } else {
            logger.warn("getShadowRejected response received with unknown token {}", response.clientToken);
        }
    }

    private void onUpdateShadowAccepted(UpdateShadowResponse response) {
        if (openRequests.containsKey(response.clientToken)) {
            JSONObject obj = new JSONObject(response.state.reported);
            obj.put("reCode", 200);
            obj.put("reMsg", "success !");

            openRequests.get(response.clientToken).completeRequest(obj.toString());
            openRequests.remove(response.clientToken);
        } else {
            logger.warn("updateShadowAccepted response received with unknown token {}", response.clientToken);
        }
    }

    private void onUpdateShadowRejected(ErrorResponse response) {
        if (openRequests.containsKey(response.clientToken)) {
            JSONObject obj = new JSONObject();
            obj.put("reCode", response.code);
            obj.put("reMsg", response.message);

            openRequests.get(response.clientToken).completeRequest(obj.toString());
            openRequests.remove(response.clientToken);
        } else {
            logger.warn("updateShadowRejected response received with unknown token {}", response.clientToken);
        }
    }

    private void onMqttMessageReceived(MqttMessage message) {
        BaseSubscriptionMessage subscriptionData = null;

        for (Subscription subscription : updateListeners.keySet()) {
            if (subscription.getTopic().equals(message.getTopic())) {
                try {
                    subscriptionData = (BaseSubscriptionMessage) subscription.getDataClass().getDeclaredConstructor()
                            .newInstance();

                    if (subscriptionData != null) {
                        subscriptionData.deserialize(new String(message.getPayload(), StandardCharsets.UTF_8));

                        for (EventListener listener : updateListeners.get(subscription)) {
                            String identifier = listener.getEventIdentifier();

                            BaseEvent deviceData = subscriptionData.eventForIdentifier(identifier);

                            if (deviceData != null) {
                                listener.eventReceived(deviceData);
                            }
                        }
                    }
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                    logger.warn("failed to create data object for mqtt message {}", message.getPayload());
                }
            }
        }
    }
}
