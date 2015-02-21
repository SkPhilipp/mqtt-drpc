package com.hileco.drpc.mqtt;

import com.hileco.drpc.generic.JSONStreamer;
import com.hileco.drpc.generic.RpcPacketStreamer;
import com.hileco.drpc.generic.ServiceHost;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility to create instances of {@link MqttDrpcClient}
 *
 * @author Philipp Gayret
 */
public class MqttDrpcClientBuilder {

    public static final int DEFAULT_MILLISECONDS_TIME_TO_WAIT_LIMIT = 5000;
    public static final int DEFAULT_SECONDS_KEEP_ALIVE_INTERVAL = 30;
    public static final int DEFAULT_LEVEL_QUALITY_OF_SERVICE = 2;
    public static final int DEFAULT_MAX_THREADS = 10;
    public static final int DEFAULT_RETRY_LIMIT = 5;

    private String clientId;
    private MqttClientPersistence mqttClientPersistence;
    private MqttDrpcFailureHandler mqttDrpcFailureHandler;
    private ExecutorService executorService;
    private ServiceHost callbackHost;
    private RpcPacketStreamer rpcPacketStreamer;
    private MqttDrpcTopicBuilder topicBuilder;
    private ServiceHost serviceHost;
    private int keepaliveInterval;
    private int qualityOfServiceLevel;
    private MqttConnectOptions connectOptions;


    public MqttDrpcClientBuilder() throws MqttException {
        this.clientId = UUID.randomUUID().toString();
        this.mqttClientPersistence = new MemoryPersistence();
        this.executorService = Executors.newScheduledThreadPool(DEFAULT_MAX_THREADS);
        this.topicBuilder = new MqttDrpcTopicBuilder();
        this.serviceHost = new ServiceHost();
        this.callbackHost = new ServiceHost();
        this.rpcPacketStreamer = new RpcPacketStreamer(new JSONStreamer());
        this.keepaliveInterval = DEFAULT_SECONDS_KEEP_ALIVE_INTERVAL;
        this.qualityOfServiceLevel = DEFAULT_LEVEL_QUALITY_OF_SERVICE;
        this.connectOptions = new MqttConnectOptions();
        this.connectOptions.setCleanSession(true);
        this.connectOptions.setKeepAliveInterval(keepaliveInterval);

        this.mqttDrpcFailureHandler = new MqttDrpcFailureHandler() {
            @Override
            public boolean shouldRetry(Exception cause, MqttDrpcTask task) {
                if (cause instanceof MqttException) {
                    if (((MqttException) cause).getReasonCode() == 32202) {
                        return true;
                    }
                }
                return task.getRetries() < DEFAULT_RETRY_LIMIT;
            }

            @Override
            public void handleDisconnect(Throwable throwable) {
                // Does not handle disconnects !
            }
        };
    }

    public MqttDrpcClientBuilder withClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public MqttDrpcClientBuilder withMqttClientPersistence(MqttClientPersistence mqttClientPersistence) {
        this.mqttClientPersistence = mqttClientPersistence;
        return this;
    }

    public MqttDrpcClientBuilder withMqttDrpcFailureHandler(MqttDrpcFailureHandler mqttDrpcFailureHandler) {
        this.mqttDrpcFailureHandler = mqttDrpcFailureHandler;
        return this;
    }

    public MqttDrpcClientBuilder withExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    public MqttDrpcClientBuilder withTopicBuilder(MqttDrpcTopicBuilder topicBuilder) {
        this.topicBuilder = topicBuilder;
        return this;
    }

    public MqttDrpcClientBuilder withServiceHost(ServiceHost serviceHost) {
        this.serviceHost = serviceHost;
        return this;
    }

    public MqttDrpcClientBuilder withKeepaliveInterval(int keepaliveInterval) {
        this.keepaliveInterval = keepaliveInterval;
        return this;
    }

    public MqttDrpcClientBuilder withQualityOfServiceLevel(int qualityOfServiceLevel) {
        this.qualityOfServiceLevel = qualityOfServiceLevel;
        return this;
    }

    public MqttDrpcClientBuilder withRpcPacketStreamer(RpcPacketStreamer rpcPacketStreamer) {
        this.rpcPacketStreamer = rpcPacketStreamer;
        return this;
    }

    public MqttDrpcClientBuilder withCallbackHost(ServiceHost callbackHost) {
        this.callbackHost = callbackHost;
        return this;
    }

    public MqttDrpcClientBuilder withConnectOptions(MqttConnectOptions connectOptions) {
        this.connectOptions = connectOptions;
        return this;
    }

    public MqttDrpcClient build(String broker) throws MqttException {
        MqttClient mqttClient = new MqttClient(broker, clientId, mqttClientPersistence);
        mqttClient.setTimeToWait(DEFAULT_MILLISECONDS_TIME_TO_WAIT_LIMIT);
        return new MqttDrpcClient(mqttDrpcFailureHandler, executorService, mqttClient, topicBuilder,
                serviceHost, callbackHost, rpcPacketStreamer, connectOptions, qualityOfServiceLevel);
    }

}
