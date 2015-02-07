package com.hileco.drpc.transport;

public interface TopicBuilder {

    public Topic operation(String service, String operation);

    public Topic operation(String service, String operation, String identifier);

    public Topic callback(String messageId);

}
