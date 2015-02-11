package com.hileco.drpc.mqtt;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The content and format of a DRPC packet, the format for requests and responses is the same.
 *
 * @author Philipp Gayret
 */
public class MqttDrpcPacket {

    public static final List<Class<?>> HEADER_ENTRIES = Collections.unmodifiableList(Arrays.asList(
            String.class // messageId
    ));

    private String messageId;
    private Object[] body;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Object[] getBody() {
        return body;
    }

    public void setBody(Object[] body) {
        this.body = body;
    }

}
