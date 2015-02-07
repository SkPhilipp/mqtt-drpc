package com.hileco.drpc.mqtt;

import com.hileco.drpc.format.JSONArgumentsStreamer;
import com.hileco.drpc.reflection.ProxyServiceConnector;
import com.hileco.drpc.reflection.ServiceConnector;
import com.hileco.drpc.transport.*;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author Philipp Gayret
 */
public class MqttDrpcClient {

    private static final Logger LOG = LoggerFactory.getLogger(MqttDrpcClient.class);

    private final ServiceHost serviceHost;
    private final TopicBuilder topicBuilder;
    private final int qos;
    private final MqttClient sampleClient;
    private final MqttDrpcPackets mqttDrpcPackets;

    protected class MqttDrpcCallbackHandler implements MqttCallback {

        @Override
        public void connectionLost(Throwable throwable) {
        }

        @Override
        public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
            byte[] payload = mqttMessage.getPayload();
            serviceHost.accept(topic, new ByteArrayInputStream(payload));
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        }

    }

    public MqttDrpcClient(String broker) throws MqttException {
        this.qos = 2;
        String clientId = UUID.randomUUID().toString();
        MemoryPersistence persistence = new MemoryPersistence();
        this.sampleClient = new MqttClient(broker, clientId, persistence);
        this.topicBuilder = new MqttDrpcTopicBuilder();
        MqttDrpcCallbackHandler callback = new MqttDrpcCallbackHandler();
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setKeepAliveInterval(30);
        this.sampleClient.setCallback(callback);
        this.sampleClient.setTimeToWait(5000);
        this.serviceHost = new ServiceHost();
        this.mqttDrpcPackets = new MqttDrpcPackets(new JSONArgumentsStreamer());
        this.sampleClient.connect(connOpts);
    }

    private void send(String messageId, Topic topic, Object[] content) throws IOException, MqttException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MqttDrpcPackets.Packet packet = new MqttDrpcPackets.Packet();
        packet.setMessageId(messageId);
        packet.setBody(content);
        mqttDrpcPackets.write(outputStream, packet);
        MqttMessage message = new MqttMessage(outputStream.toByteArray());
        message.setQos(qos);
        sampleClient.publish(topic.getTopic(), message);
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
        try {
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                MessageReceiver receiver = (String topic, InputStream content) -> {
                    try {
                        try {
                            List<Class<?>> parameterTypes = Arrays.asList(method.getParameterTypes());
                            MqttDrpcPackets.Packet packet = mqttDrpcPackets.read(content, parameterTypes);
                            Object result = method.invoke(implementation, packet.getBody());
                            Topic callbackMeta = topicBuilder.callback(packet.getMessageId());
                            String id = UUID.randomUUID().toString();
                            MqttDrpcClient.this.send(id, callbackMeta, new Object[]{result});
                        } catch (ReflectiveOperationException e) {
                            LOG.error("Unable to process a call for topic {} on interface {} implemented by {}", topic, type, implementation.getClass(), e);
                        }
                    } catch (Exception e) {
                        LOG.error("Erred while reading streamed content", e);
                    }
                };
                String operation = topicBuilder.operation(type.getName(), method.getName()).getTopic();
                SilentCloseable service = serviceHost.register(operation, receiver);
                String operationById = topicBuilder.operation(type.getName(), method.getName(), identifier).getTopic();
                SilentCloseable serviceById = serviceHost.register(operationById, receiver);
                topics[(i * 2)] = operationById;
                closeables[(i * 2)] = serviceById;
                topics[(i * 2) + 1] = operation;
                closeables[(i * 2) + 1] = service;
            }
            this.sampleClient.subscribe(topics);
        } catch (MqttException e) {
            LOG.error("Unable to subscribe", e);
        }
        return () -> {
            for (SilentCloseable closeable : closeables) {
                closeable.close();
            }
            try {
                this.sampleClient.unsubscribe(topics);
            } catch (MqttException e) {
                LOG.error("Unable to unsubscribe", e);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public <T> ServiceConnector<T> connector(Class<T> type) {
        return new ProxyServiceConnector<T>(type) {
            @Override
            public <R> SilentCloseable call(Class<?> type, Method method, Object[] arguments, Consumer<R> consumer) {
                try {
                    String messageId = UUID.randomUUID().toString();
                    String service = type.getName();
                    String operation = method.getName();
                    Topic topic = topicBuilder.operation(service, operation);
                    Topic callback = topicBuilder.callback(messageId);
                    sampleClient.subscribe(callback.getTopic());
                    SilentCloseable closeable = serviceHost.register(callback.getTopic(), (callbackMetadata, content) -> {
                        List<Class<?>> bodyTypes = Arrays.asList(method.getReturnType());
                        MqttDrpcPackets.Packet packet = mqttDrpcPackets.read(content, bodyTypes);
                        Object result = packet.getBody()[0];
                        consumer.accept((R) result);
                    });
                    MqttDrpcClient.this.send(messageId, topic, arguments);
                    return closeable;
                } catch (IOException | MqttException e) {
                    LOG.error("Unable to send or subscribe", e);
                    return () -> {
                    };
                }
            }
        };
    }

}
