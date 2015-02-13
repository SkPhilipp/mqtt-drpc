package com.hileco.drpc.generic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The content and format of a request packet.
 *
 * @author Philipp Gayret
 */
public class RpcRequestPacket {

    public static final List<Class<?>> HEADER_ENTRIES = Collections.unmodifiableList(Arrays.asList(
            String.class, // clientId
            String.class  // correlationId
    ));

    private String clientId;
    private String correlationId;
    private Object[] body;

    public RpcRequestPacket() {
    }

    public RpcRequestPacket(String clientId, String correlationId, Object[] body) {
        this.clientId = clientId;
        this.correlationId = correlationId;
        this.body = body;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Object[] getBody() {
        return body;
    }

    public void setBody(Object[] body) {
        this.body = body;
    }

}
