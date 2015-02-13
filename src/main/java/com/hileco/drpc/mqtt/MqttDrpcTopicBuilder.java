package com.hileco.drpc.mqtt;

import java.lang.reflect.Method;

/**
 * Constructs topics out of identifyable service operations and callbacks.
 * <p>
 * Services should be published under these topics, callbacks registered under these topics and messages sent to these topics.
 *
 * @author Philipp Gayret
 */
public class MqttDrpcTopicBuilder {

    private static final String SERVICE = "s";
    private static final String CALLBACK = "c";

    public String operation(Class<?> service, Method operation) {
        return String.format("%s/%s/%s", SERVICE, service.getName(), operation.getName());
    }

    public String operation(Class<?> service, Method operation, String identifier) {
        return String.format("%s/%s/%s/%s", SERVICE, service.getName(), operation.getName(), identifier);
    }

    public String callback(String correlationId) {
        return String.format("%s/%s", CALLBACK, correlationId);
    }

}
