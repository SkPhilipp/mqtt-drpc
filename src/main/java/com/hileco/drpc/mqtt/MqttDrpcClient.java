package com.hileco.drpc.mqtt;

import com.hileco.drpc.generic.*;
import com.hileco.drpc.reflection.ProxyServiceConnector;
import org.eclipse.paho.client.mqttv3.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Allows publishing services and invoking remote services over MQTT.
 *
 * @author Philipp Gayret
 */
public class MqttDrpcClient implements MqttCallback {

    private final ServiceHost callbackHost;
    private final ServiceHost serviceHost;
    private final MqttDrpcTopicBuilder topicBuilder;
    private final MqttClient mqttClient;
    private final RpcPacketStreamer rpcPacketStreamer;
    private final ExecutorService executorService;
    private final MqttDrpcFailureHandler mqttDrpcFailureHandler;
    private final MqttConnectOptions connectOptions;
    private final int qualityOfServiceLevel;

    /**
     * The recommended way to create an instance is with {@link com.hileco.drpc.mqtt.MqttDrpcClientBuilder}.
     */
    public MqttDrpcClient(MqttDrpcFailureHandler mqttDrpcFailureHandler, ExecutorService executorService, MqttClient mqttClient,
                          MqttDrpcTopicBuilder topicBuilder, ServiceHost serviceHost, ServiceHost callbackHost, RpcPacketStreamer rpcPacketStreamer,
                          MqttConnectOptions connectOptions, int qualityOfServiceLevel) {
        this.connectOptions = connectOptions;
        this.qualityOfServiceLevel = qualityOfServiceLevel;
        this.mqttDrpcFailureHandler = mqttDrpcFailureHandler;
        this.executorService = executorService;
        this.topicBuilder = topicBuilder;
        this.serviceHost = serviceHost;
        this.rpcPacketStreamer = rpcPacketStreamer;
        this.callbackHost = callbackHost;
        this.mqttClient = mqttClient;
        this.mqttClient.setCallback(this);
        String callback = this.topicBuilder.callback(this.mqttClient.getClientId());
        this.serviceHost.register(callback, (topic, content) -> {
            content.mark(Integer.MAX_VALUE);
            RpcResponsePacket rpcResponsePacketHeaders = rpcPacketStreamer.readResponse(content, Collections.emptyList());
            content.reset();
            callbackHost.accept(rpcResponsePacketHeaders.getCorrelationId(), content);
        });
    }

    /**
     * Delegates disconnect errors to the failure handler.
     */
    @Override
    public void connectionLost(Throwable throwable) {
        mqttDrpcFailureHandler.handleDisconnect(throwable);
    }

    /**
     * Delegates incoming messages to the service host.
     */
    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        byte[] payload = mqttMessage.getPayload();
        serviceHost.accept(topic, new ByteArrayInputStream(payload));
    }

    /**
     * Null implementation, delivery is assured by quality of service level.
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
    }

    /**
     * Connects the internal {@link #mqttClient} to the broker, automatically begins listening for callbacks.
     *
     * @throws MqttException
     */
    public void connect() throws MqttException {
        mqttClient.connect(connectOptions);
        String callbacks = topicBuilder.callback(this.mqttClient.getClientId());
        mqttClient.unsubscribe(callbacks);
        mqttClient.subscribe(callbacks);
    }

    /**
     * Creates a new task out of a given task body, schedules it, and awaits its completion.
     *
     * @param taskBody task body to execute
     */
    private void await(MqttDrpcTask.TaskBody taskBody) {
        MqttDrpcTask mqttDrpcTask = new MqttDrpcTask(executorService, mqttDrpcFailureHandler, taskBody);
        mqttDrpcTask.start();
        mqttDrpcTask.join();
    }

    /**
     * Publishes a service, informs the router that this client wants to receive messages for the given service.
     *
     * @param type           type to publish, and class' defined methods to allow access to
     * @param identifier     identifier of the implementation
     * @param implementation remote procedure call receiver
     * @param <T>            type of implementation
     * @return closeable to use for unregistering
     */
    public <T> SilentCloseable publish(Class<T> type, String identifier, T implementation) {
        Method[] methods = type.getMethods();
        SilentCloseable[] closeables = new SilentCloseable[methods.length * 2];
        String[] topics = new String[methods.length * 2];
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            MessageReceiver receiver = (String topic, InputStream content) -> {
                try {
                    List<Class<?>> parameterTypes = Arrays.asList(method.getParameterTypes());
                    RpcRequestPacket request = rpcPacketStreamer.readRequest(content, parameterTypes);
                    try {
                        Object result = method.invoke(implementation, request.getBody());
                        String callback = topicBuilder.callback(request.getClientId());
                        RpcResponsePacket response = new RpcResponsePacket(request.getCorrelationId(), new Object[]{result});
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        rpcPacketStreamer.writeResponse(outputStream, response);
                        MqttMessage message = new MqttMessage(outputStream.toByteArray());
                        message.setQos(qualityOfServiceLevel);
                        await(() -> mqttClient.publish(callback, message));
                    } catch (ReflectiveOperationException e) {
                        throw new MqttDrpcRuntimeException("Erred invoking a service method.", e);
                    }
                } catch (IOException e) {
                    throw new MqttDrpcRuntimeException("Deserialization of response message body failed.", e);
                }
            };
            String operation = topicBuilder.operation(type, method);
            SilentCloseable service = serviceHost.register(operation, receiver);
            String operationById = topicBuilder.operation(type, method, identifier);
            SilentCloseable serviceById = serviceHost.register(operationById, receiver);
            topics[(i * 2)] = operationById;
            closeables[(i * 2)] = serviceById;
            topics[(i * 2) + 1] = operation;
            closeables[(i * 2) + 1] = service;
        }
        await(() -> mqttClient.subscribe(topics));
        return () -> {
            for (SilentCloseable closeable : closeables) {
                closeable.close();
            }
            await(() -> mqttClient.unsubscribe(topics));
        };
    }

    /**
     * Creates a {@link ServiceConnector} for the given type, through which remote services
     * can be invoked.
     *
     * @param type any interface which strictly defines functionality
     * @param <T>  type of the interface
     * @return connector for the given type
     */
    @SuppressWarnings("unchecked")
    public <T> ServiceConnector<T> connector(Class<T> type) {
        return new ProxyServiceConnector<T>(type) {
            @Override
            public <R> SilentCloseable call(Class<?> type, Method method, String identifier, Object[] arguments, Consumer<R> consumer) {
                String correlationId = UUID.randomUUID().toString();
                SilentCloseable closeable = callbackHost.register(correlationId, (callbackMetadata, content) -> {
                    if (method.getReturnType() != void.class) {
                        List<Class<?>> bodyTypes = Arrays.asList(method.getReturnType());
                        RpcResponsePacket packet = rpcPacketStreamer.readResponse(content, bodyTypes);
                        Object result = packet.getBody()[0];
                        consumer.accept((R) result);
                    } else {
                        consumer.accept(null);
                    }
                });
                try {
                    RpcRequestPacket packet = new RpcRequestPacket();
                    packet.setClientId(mqttClient.getClientId());
                    packet.setCorrelationId(correlationId);
                    packet.setBody(arguments == null ? new Object[]{} : arguments);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    rpcPacketStreamer.writeRequest(outputStream, packet);
                    MqttMessage message = new MqttMessage(outputStream.toByteArray());
                    message.setQos(qualityOfServiceLevel);
                    String topic = identifier == null ? topicBuilder.operation(type, method) : topicBuilder.operation(type, method, identifier);
                    await(() -> mqttClient.publish(topic, message));
                } catch (IOException e) {
                    throw new MqttDrpcRuntimeException("Serialization of arguments to message body failed.", e);
                }
                return closeable;
            }
        };
    }

}
