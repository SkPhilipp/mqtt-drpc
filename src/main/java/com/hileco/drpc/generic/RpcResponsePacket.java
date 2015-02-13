package com.hileco.drpc.generic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The content and format of a response packet.
 *
 * @author Philipp Gayret
 */
public class RpcResponsePacket {

    public static final List<Class<?>> HEADER_ENTRIES = Collections.unmodifiableList(Arrays.asList(
            String.class // correlationId
    ));

    private String correlationId;
    private Object[] body;

    public RpcResponsePacket() {
    }

    public RpcResponsePacket(String correlationId, Object[] body) {
        this.correlationId = correlationId;
        this.body = body;
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
