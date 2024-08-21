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
import org.openhab.binding.xsense.internal.api.ApiConstants;
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

    private OAuth authenticationParameters = null;
    private String clientId = "";
    private String host = "";
    private String region = "";

    private final Lock subscriptionLock = new ReentrantLock(true);
    private final Lock connectionLock = new ReentrantLock(true);

    public Mqtt(String clientId, String host, String region) {
        openRequests = new HashMap<>();
        updateListeners = new HashMap<>();
        subscribedTopics = new HashSet<>();

        this.clientId = clientId;
        this.host = host;
        this.region = region;
    }

    public void updateAuthenticationParameters(OAuth oAuth) {
        authenticationParameters = oAuth;

        // reconnect in case connection is already established
        if (client != null) {
            connect();
        }
    }

    private boolean connect() {
        boolean success = false;

        if (connectionLock.tryLock()) {
            if (client != null) {
                disconnect(false);
                logger.debug("reconnect to mqtt host for region {}", region);
            } else {
                subscribedTopics.clear();
            }

            client = createMqttClientConnection(authenticationParameters.getAccessKeyId(),
                    authenticationParameters.getSecretAccessKey(), authenticationParameters.getSessionToken(), clientId,
                    host, region);

            if (client != null) {
                CompletableFuture<Boolean> connected = client.connect();
                try {
                    connected.get();

                    if (!resubscribe()) {
                        logger.warn("resubscription failed for at least one topic");
                    }

                    success = true;
                } catch (Exception e) {
                    logger.error("failed connect to mqtt broker", e);
                }
            } else {
                logger.error("failed to create mqtt connection");
            }

            connectionLock.unlock();
        } else {
            connectionLock.lock();
            success = true;
            connectionLock.unlock();
        }

        return success;
    }

    public void disconnect(boolean clearListeners) {
        try {
            if (client != null) {
                logger.debug("disconnecting mqtt connection for region {}", region);

                if (clearListeners) {
                    updateListeners.clear();
                }

                CompletableFuture<Void> disconnected = client.disconnect();
                disconnected.get();

                openRequests.values().forEach(request -> {
                    request.completeRequest("{\"reCode\": 402, \"reMsg\":\"mqtt disconnected\"}");
                });

                openRequests.clear();
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("failed to disconnect mqtt connection", e);
        }
    }

    private MqttClientConnection createMqttClientConnection(String accessKey, String secretAccessKey,
            String sessionToken, String clientId, String host, String region) {

        StaticCredentialsProviderBuilder providerBuilder = new StaticCredentialsProviderBuilder();
        providerBuilder.withAccessKeyId(accessKey.getBytes());
        providerBuilder.withSecretAccessKey(secretAccessKey.getBytes());
        providerBuilder.withSessionToken(sessionToken.getBytes());

        AwsIotMqttConnectionBuilder iotMqttConnectionBuilder = AwsIotMqttConnectionBuilder
                .newMtlsBuilderFromPath(null, null).withConnectionEventCallbacks(this).withEndpoint(host)
                .withClientId(clientId).withWebsocketSigningRegion(region).withWebsockets(true).withPort(443)
                .withWebsocketCredentialsProvider(providerBuilder.build()).withKeepAliveSecs(30);

        if (!ApiConstants.DEBUG_PROXY_IP.isEmpty()) {
            HttpProxyOptions proxyOptions = new HttpProxyOptions();
            proxyOptions.setConnectionType(HttpProxyConnectionType.Tunneling);
            proxyOptions.setHost(ApiConstants.DEBUG_PROXY_IP);
            proxyOptions.setPort(ApiConstants.DEBUG_PROXY_PORT);

            iotMqttConnectionBuilder = iotMqttConnectionBuilder.withHttpProxyOptions(proxyOptions);
        }

        return iotMqttConnectionBuilder.build();
    }

    public void sendRequest(BaseMqttRequest<?> request) {
        boolean proceed = true;

        if (client == null) {
            proceed = connect();
        }

        if (proceed) {
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
        } else {
            logger.warn("connecting to mqtt failed");
        }
    }

    public boolean registerEventListener(Subscription subscription, EventListener listener) {
        boolean proceed = true;

        if (client == null) {
            proceed = connect();
        }

        if (proceed) {
            String topic = subscription.getTopic();

            if (subscribe(topic)) {
                ArrayList<EventListener> listeners = updateListeners.get(subscription);

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
        } else {
            logger.warn("connecting to mqtt failed");
        }

        return false;
    }

    public void unregisterEventListener(EventListener listener) {
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
    }

    private boolean subscribeTopic(String thingName, String shadowName, ShadowRequestType type) {
        boolean success = false;

        subscriptionLock.lock();
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

        subscriptionLock.unlock();

        return success;
    }

    private boolean unsubscribe(String topic) {
        subscriptionLock.lock();
        try {
            if (client != null) {
                if (subscribedTopics.contains(topic)) {
                    CompletableFuture<Integer> unsubscribed = client.unsubscribe(topic);
                    unsubscribed.get();
                    subscribedTopics.remove(topic);
                    logger.debug("unsubscribed mqtt topic {}", topic);
                }

                return true;
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("failed to unsubscribe to mqtt topic {}", topic, e);
        }

        subscriptionLock.unlock();

        return false;
    }

    private boolean subscribe(String topic) {
        boolean success = false;

        subscriptionLock.lock();
        try {
            if (client != null) {
                if (!subscribedTopics.contains(topic)) {
                    CompletableFuture<Integer> subscribed = client.subscribe(topic, QualityOfService.AT_LEAST_ONCE,
                            this::onMqttMessageReceived);
                    subscribed.get();
                    subscribedTopics.add(topic);
                    logger.debug("subscribed to mqtt topic {}", topic);
                }
                success = true;
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("failed to subscribe to mqtt topic {}", topic, e);
        }

        subscriptionLock.unlock();

        return success;
    }

    private boolean resubscribe() {
        boolean success = true;

        HashSet<String> resubscribedTopics = new HashSet<>();

        if (!subscribedTopics.isEmpty()) {
            for (String topic : subscribedTopics) {
                if (topic.startsWith("$aws") || topic.startsWith("@claybox")) {
                    resubscribedTopics.add(topic);
                }
            }

            subscribedTopics.clear();

            for (String topic : resubscribedTopics) {
                if (!subscribe(topic)) {
                    success = false;
                }
            }
        } else {
            logger.debug("no topics to resubscribe");
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
        logger.info("mqtt connection for region {} resumed: {}", region,
                sessionPresent ? "existing session" : "clean session");

        if (!sessionPresent) {
            CompletableFuture.runAsync(() -> {
                if (!resubscribe()) {
                    logger.warn("resubscription failed for at least one topic");
                }
            });
        }
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
