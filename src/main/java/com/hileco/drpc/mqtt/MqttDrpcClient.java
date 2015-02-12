package com.hileco.drpc.mqtt;

import com.hileco.drpc.generic.MessageReceiver;
import com.hileco.drpc.generic.ServiceConnector;
import com.hileco.drpc.generic.ServiceHost;
import com.hileco.drpc.generic.SilentCloseable;
import com.hileco.drpc.reflection.ArgumentsStreamer;
import com.hileco.drpc.reflection.ProxyServiceConnector;
import org.eclipse.paho.client.mqttv3.*;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Arrays;
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

    private final ServiceHost serviceHost;
    private final MqttDrpcTopicBuilder topicBuilder;
    private final ArgumentsStreamer argumentsStreamer;
    private final MqttClient mqttClient;
    private ExecutorService executorService;
    private MqttDrpcFailureHandler mqttDrpcFailureHandler;
    private int keepaliveInterval;
    private int qualityOfServiceLevel;

    /**
     * Creates a new MqttDrpcClient, the recommended way to obtain an instance is using the {@link com.hileco.drpc.mqtt.MqttDrpcClientBuilder}
     */
    public MqttDrpcClient(MqttDrpcFailureHandler mqttDrpcFailureHandler, ExecutorService executorService, MqttClient mqttClient,
                          MqttDrpcTopicBuilder topicBuilder, ServiceHost serviceHost, ArgumentsStreamer argumentsStreamer,
                          int keepaliveInterval, int qualityOfServiceLevel) {
        this.keepaliveInterval = keepaliveInterval;
        this.qualityOfServiceLevel = qualityOfServiceLevel;
        this.mqttDrpcFailureHandler = mqttDrpcFailureHandler;
        this.executorService = executorService;
        this.topicBuilder = topicBuilder;
        this.serviceHost = serviceHost;
        this.argumentsStreamer = argumentsStreamer;
        this.mqttClient = mqttClient;
        this.mqttClient.setCallback(this);
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

    // TODO: Move connection code to outside of mqtt drpc client ? failure handler should be in charge ? maybe a connection manager ?
    public void connect() throws MqttException {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setKeepAliveInterval(keepaliveInterval);
        mqttClient.connect(connOpts);
    }

    /**
     * Parses the given content stream as the given body types.
     *
     * @param content   a readable content stream
     * @param bodyTypes types to parse the content stream as
     * @return a packet representing the parsed content
     * @throws IOException on parsing failures
     */
    private MqttDrpcPacket read(InputStream content, List<Class<?>> bodyTypes) throws IOException {
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
    private void write(OutputStream outputStream, MqttDrpcPacket packet) throws IOException {
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
    private MqttMessage build(String messageId, Object[] content) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MqttDrpcPacket packet = new MqttDrpcPacket();
        packet.setMessageId(messageId);
        packet.setBody(content == null ? new Object[]{} : content);
        write(outputStream, packet);
        MqttMessage message = new MqttMessage(outputStream.toByteArray());
        message.setQos(qualityOfServiceLevel);
        return message;
    }

    /**
     * Creates a new task out of a given task body, then schedules it, and awaits its completion.
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
                    MqttDrpcPacket packet = read(content, parameterTypes);
                    try {
                        Object result = method.invoke(implementation, packet.getBody());
                        String callback = topicBuilder.callback(packet.getMessageId());
                        String id = UUID.randomUUID().toString();
                        MqttMessage message = build(id, new Object[]{result});
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
            public <R> SilentCloseable call(Class<?> type, Method method, Object[] arguments, Consumer<R> consumer) {
                String messageId = UUID.randomUUID().toString();
                String topic = topicBuilder.operation(type, method);
                String callback = topicBuilder.callback(messageId);
                await(() -> mqttClient.subscribe(new String[]{callback}));
                SilentCloseable closeable = serviceHost.register(callback, (callbackMetadata, content) -> {
                    if (method.getReturnType() != void.class) {
                        List<Class<?>> bodyTypes = Arrays.asList(method.getReturnType());
                        MqttDrpcPacket packet = read(content, bodyTypes);
                        Object result = packet.getBody()[0];
                        consumer.accept((R) result);
                    } else {
                        consumer.accept(null);
                    }
                    await(() -> mqttClient.unsubscribe(new String[]{callback}));
                });
                try {
                    MqttMessage message = build(messageId, arguments);
                    await(() -> mqttClient.publish(topic, message));
                } catch (IOException e) {
                    throw new MqttDrpcRuntimeException("Serialization of arguments to message body failed.", e);
                }
                return closeable;

            }
        };
    }

}
