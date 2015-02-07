package com.hileco.drpc.mqtt;

import com.hileco.drpc.transport.Topic;
import com.hileco.drpc.transport.TopicBuilder;

public class MqttDrpcTopicBuilder implements TopicBuilder {

    private static final String SERVICE = "s";
    private static final String CALLBACK = "c";

    @Override
    public Topic operation(String service, String operation) {
        return () -> String.format("%s/%s/%s", SERVICE, service, operation);
    }

    @Override
    public Topic operation(String service, String operation, String identifier) {
        return () -> String.format("%s/%s/%s/%s", SERVICE, service, operation, identifier);
    }

    @Override
    public Topic callback(String messageId) {
        return () -> String.format("%s/%s", CALLBACK, messageId);
    }

}
