package com.hileco.drpc.mqtt;

import com.hileco.drpc.api.Client;
import com.hileco.drpc.api.ServiceConnector;
import com.hileco.drpc.format.ArgumentsStreamer;
import com.hileco.drpc.format.JSONArgumentsStreamer;
import com.hileco.drpc.reflection.ProxyServiceConnector;
import com.hileco.drpc.transport.MessageReceiver;
import com.hileco.drpc.transport.ServiceHost;
import com.hileco.drpc.transport.SilentCloseable;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * An implementation of {@link Client} over MQTT.
 *
 * @author Philipp Gayret
 */
public class MqttDrpcClient implements Client, MqttCallback {

    public static final int DEFAULT_MILLISECONDS_TIME_TO_WAIT_LIMIT = 5000;
    public static final int DEFAULT_SECONDS_KEEP_ALIVE_INTERVAL = 30;
    public static final int DEFAULT_LEVEL_QUALITY_OF_SERVICE = 2;
    public static final int DEFAULT_THREAD_AMOUNT = 10;

    private final ServiceHost serviceHost;
    private final MqttDrpcTopicBuilder topicBuilder;
    private final ArgumentsStreamer argumentsStreamer;
    private final MqttDrpcQueue mqttDrpcQueue;

    public MqttDrpcClient(String broker) throws MqttException {
        this.topicBuilder = new MqttDrpcTopicBuilder();
        this.serviceHost = new ServiceHost();
        this.argumentsStreamer = new JSONArgumentsStreamer();

        String clientId = UUID.randomUUID().toString();
        MemoryPersistence persistence = new MemoryPersistence();
        MqttClient mqttClient = new MqttClient(broker, clientId, persistence);
        mqttClient.setCallback(this);
        mqttClient.setTimeToWait(DEFAULT_MILLISECONDS_TIME_TO_WAIT_LIMIT);
        this.mqttDrpcQueue = new MqttDrpcQueue(mqttClient, DEFAULT_THREAD_AMOUNT);
    }

    /**
     * Delegates disconnect errors to the failure handler.
     */
    @Override
    public void connectionLost(Throwable throwable) {
        mqttDrpcQueue.pause();
        // TODO: This should again call #connect, at a set interval, then resume the queue
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
     * {@inheritDoc}
     */
    public boolean connect() {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setKeepAliveInterval(DEFAULT_SECONDS_KEEP_ALIVE_INTERVAL);
        boolean connected = mqttDrpcQueue.connect(connOpts);
        if (connected) {
            mqttDrpcQueue.resume();
        }
        return connected;
    }

    /**
     * Parses the given content stream as the given body types.
     *
     * @param content   a readable content stream
     * @param bodyTypes types to parse the content stream as
     * @return a packet representing the parsed content
     * @throws IOException on parsing failures
     */
    public MqttDrpcPacket read(InputStream content, List<Class<?>> bodyTypes) throws IOException {
        Object[] deserializedPacket = argumentsStreamer.deserializeFrom(content, MqttDrpcPacket.HEADER_ENTRIES, bodyTypes);
        MqttDrpcPacket drpcPacket = new MqttDrpcPacket();
        drpcPacket.setMessageId((String) deserializedPacket[0]);
        drpcPacket.setBody(Arrays.copyOfRange(deserializedPacket, MqttDrpcPacket.HEADER_ENTRIES.size(), deserializedPacket.length));
        return drpcPacket;
    }

    /**
     * Writes the given packet to the given output stream.
     * <p>
     * The packet written can be read back using {@link #read(java.io.InputStream, java.util.List)}
     *
     * @param outputStream a writeable stream
     * @param packet       the packet to write
     * @throws IOException
     */
    public void write(OutputStream outputStream, MqttDrpcPacket packet) throws IOException {
        Object[] headers = new Object[]{packet.getMessageId()};
        argumentsStreamer.serializeTo(outputStream, Arrays.asList(headers), Arrays.asList(packet.getBody()));
    }

    /**
     * Constructs an {@link org.eclipse.paho.client.mqttv3.MqttMessage} out of packet metadata and body.
     *
     * @param messageId a unique message id
     * @param content   message body
     * @return sendable message
     * @throws IOException when serialization of given content fails
     */
    public MqttMessage build(String messageId, Object[] content) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MqttDrpcPacket packet = new MqttDrpcPacket();
        packet.setMessageId(messageId);
        packet.setBody(content == null ? new Object[]{} : content);
        write(outputStream, packet);
        MqttMessage message = new MqttMessage(outputStream.toByteArray());
        message.setQos(DEFAULT_LEVEL_QUALITY_OF_SERVICE);
        return message;
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
                    MqttDrpcPacket packet = read(content, parameterTypes);
                    try {
                        Object result = method.invoke(implementation, packet.getBody());
                        String callback = topicBuilder.callback(packet.getMessageId());
                        String id = UUID.randomUUID().toString();
                        MqttMessage message = build(id, new Object[]{result});
                        mqttDrpcQueue.queuedPublish(callback, message);
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
        mqttDrpcQueue.queuedSubscribe(topics);
        return () -> {
            for (SilentCloseable closeable : closeables) {
                closeable.close();
            }
            mqttDrpcQueue.queuedUnsubscribe(topics);
        };
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> ServiceConnector<T> connector(Class<T> type) {
        return new ProxyServiceConnector<T>(type) {
            @Override
            public <R> SilentCloseable call(Class<?> type, Method method, Object[] arguments, Consumer<R> consumer) {
                String messageId = UUID.randomUUID().toString();
                String topic = topicBuilder.operation(type, method);
                String callback = topicBuilder.callback(messageId);
                mqttDrpcQueue.queuedSubscribe(new String[]{callback});
                SilentCloseable closeable = serviceHost.register(callback, (callbackMetadata, content) -> {
                    if (method.getReturnType() != void.class) {
                        List<Class<?>> bodyTypes = Arrays.asList(method.getReturnType());
                        MqttDrpcPacket packet = read(content, bodyTypes);
                        Object result = packet.getBody()[0];
                        consumer.accept((R) result);
                    } else {
                        consumer.accept(null);
                    }
                    mqttDrpcQueue.queuedUnsubscribe(new String[]{callback});
                });
                try {
                    MqttMessage message = build(messageId, arguments);
                    mqttDrpcQueue.queuedPublish(topic, message);
                } catch (IOException e) {
                    throw new MqttDrpcRuntimeException("Serialization of arguments to message body failed.", e);
                }
                return closeable;

            }
        };
    }

}
