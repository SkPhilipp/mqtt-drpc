package com.hileco.drpc.generic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * A general utility class for reading and writing packets using a given {@link Streamer}.
 *
 * @author Philipp Gayret
 */
public class RpcPacketStreamer {

    private Streamer streamer;

    /**
     * @param streamer streamer for reading and writing packet headers and bodies.
     */
    public RpcPacketStreamer(Streamer streamer) {
        this.streamer = streamer;
    }

    /**
     * Writes the given packet to the given output stream using the given streamer.
     *
     * @param outputStream a writeable stream
     * @param packet       the packet to write
     * @throws IOException on streamer failures
     */
    public void writeResponse(OutputStream outputStream, RpcResponsePacket packet) throws IOException {
        streamer.serializeTo(outputStream, Arrays.asList(packet.getCorrelationId()), Arrays.asList(packet.getBody()));
    }

    /**
     * Parses the given content stream using the given streamer as a packet containing a body of types bodyTypes.
     *
     * @param content   a readable content stream
     * @param bodyTypes types to parse the content stream as
     * @return a packet representing the parsed content
     * @throws IOException on parsing failures
     */
    public RpcResponsePacket readResponse(InputStream content, List<Class<?>> bodyTypes) throws IOException {
        Object[] deserializedPacket = streamer.deserializeFrom(content, RpcResponsePacket.HEADER_ENTRIES, bodyTypes);
        Object[] body = Arrays.copyOfRange(deserializedPacket, RpcResponsePacket.HEADER_ENTRIES.size(), deserializedPacket.length);
        return new RpcResponsePacket((String) deserializedPacket[0], body);
    }

    /**
     * Writes the given packet to the given output stream using the given streamer.
     *
     * @param outputStream a writeable stream
     * @param packet       the packet to write
     * @throws IOException on streamer failures
     */
    public void writeRequest(OutputStream outputStream, RpcRequestPacket packet) throws IOException {
        streamer.serializeTo(outputStream, Arrays.asList(packet.getClientId(), packet.getCorrelationId()), Arrays.asList(packet.getBody()));
    }

    /**
     * Parses the given content stream using the given streamer as a packet containing a body of types bodyTypes.
     *
     * @param content   a readable content stream
     * @param bodyTypes types to parse the content stream as
     * @return a packet representing the parsed content
     * @throws IOException on streamer failures
     */
    public RpcRequestPacket readRequest(InputStream content, List<Class<?>> bodyTypes) throws IOException {
        Object[] deserializedPacket = streamer.deserializeFrom(content, RpcRequestPacket.HEADER_ENTRIES, bodyTypes);
        Object[] body = Arrays.copyOfRange(deserializedPacket, RpcRequestPacket.HEADER_ENTRIES.size(), deserializedPacket.length);
        return new RpcRequestPacket((String) deserializedPacket[0], (String) deserializedPacket[1], body);
    }

}
