package com.hileco.drpc.mqtt;

import com.hileco.drpc.format.ArgumentsStreamer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Packet specification and IO utilities.
 *
 * @author Philipp Gayret
 */
public class MqttDrpcPackets {

    private final ArgumentsStreamer argumentsStreamer;

    public static final List<Class<?>> HEADER_ENTRIES = Collections.unmodifiableList(Arrays.asList(
            String.class // messageId
    ));

    public static class Packet {

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

    public MqttDrpcPackets(ArgumentsStreamer argumentsStreamer) {
        this.argumentsStreamer = argumentsStreamer;
    }

    public Packet read(InputStream content, List<Class<?>> bodyTypes) throws IOException {
        Object[] deserializedPacket = argumentsStreamer.deserializeFrom(content, HEADER_ENTRIES, bodyTypes);
        Packet drpcPacket = new Packet();
        drpcPacket.messageId = (String) deserializedPacket[0];
        drpcPacket.body = Arrays.copyOfRange(deserializedPacket, HEADER_ENTRIES.size(), deserializedPacket.length);
        return drpcPacket;
    }

    public void write(OutputStream outputStream, Packet packet) throws IOException {
        Object[] headers = new Object[]{packet.messageId};
        argumentsStreamer.serializeTo(outputStream, Arrays.asList(headers), Arrays.asList(packet.body));
    }

}
