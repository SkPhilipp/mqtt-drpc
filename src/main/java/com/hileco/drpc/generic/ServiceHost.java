package com.hileco.drpc.generic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * A simple service host, allowing for services to be registered by identifier, and connected to.
 *
 * @author Philipp Gayret
 */
public class ServiceHost {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceHost.class);

    private final Map<String, List<MessageReceiver>> consumers;

    public ServiceHost() {
        this.consumers = new HashMap<>();
    }

    /**
     * Begins listening on the given topic, any messages received on it will be delegated to the given consumer.
     *
     * @param topic    the topic to listen on
     * @param consumer handler to accept messages
     * @return the closeable useable to revert the process of this call
     * @throws IllegalArgumentException when there is an open consumer already registered on this serviceName
     */
    public SilentCloseable register(String topic, MessageReceiver consumer) throws IllegalArgumentException {
        synchronized (consumers) {
            if (!this.consumers.containsKey(topic)) {
                this.consumers.put(topic, new ArrayList<>());
            }
            List<MessageReceiver> messageReceivers = this.consumers.get(topic);
            messageReceivers.add(consumer);
            return () -> {
                synchronized (consumers) {
                    messageReceivers.remove(consumer);
                    if (messageReceivers.size() == 0) {
                        this.consumers.remove(topic);
                    }
                }
            };
        }
    }

    /**
     * Accepts a content stream for a given topic, forwards the stream to any active consumers for the given topic.
     *
     * @param topic   the consumers' topic
     * @param content a content stream
     * @throws IOException
     */
    public void accept(String topic, InputStream content) throws IOException {
        List<MessageReceiver> messageReceivers = this.consumers.get(topic);
        if (messageReceivers != null) {
            List<MessageReceiver> unmodifyableCopy = new ArrayList<>(messageReceivers);
            unmodifyableCopy.forEach(receiver -> {
                try {
                    receiver.accept(topic, content);
                } catch (IOException e) {
                    LOG.error("Receiver erred", e);
                }
            });
        }
    }

}
