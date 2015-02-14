package com.hileco.drpc.generic;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Philipp Gayret
 */
public class RpcPacketStreamerTest {

    public static final JSONStreamer STREAMER = new JSONStreamer();

    private final List<?> values = Arrays.asList("Hello world", Long.MAX_VALUE, Long.MIN_VALUE, false, true, 0, -0, 1, -1, new TestSerializableObject(10, 20));
    private final List<Class<?>> valueClasses = values.stream().map(Object::getClass).collect(Collectors.toList());

    /**
     * Verifies that serializing and deserialising a request packet yields the same packet content.
     */
    @Test
    public void testWriteReadRpcRequestPacket() throws IOException {
        RpcPacketStreamer rpcPacketStreamer = new RpcPacketStreamer(STREAMER);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        RpcRequestPacket requestPacket = new RpcRequestPacket();
        requestPacket.setClientId(UUID.randomUUID().toString());
        requestPacket.setCorrelationId(UUID.randomUUID().toString());
        requestPacket.setBody(values.toArray());
        rpcPacketStreamer.writeRequest(byteArrayOutputStream, requestPacket);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        RpcRequestPacket readRpcRequestPacket = rpcPacketStreamer.readRequest(byteArrayInputStream, valueClasses);
        Assert.assertArrayEquals(requestPacket.getBody(), readRpcRequestPacket.getBody());
        Assert.assertEquals(requestPacket.getClientId(), readRpcRequestPacket.getClientId());
        Assert.assertEquals(requestPacket.getCorrelationId(), readRpcRequestPacket.getCorrelationId());
    }

    /**
     * Verifies that serializing and deserialising a response packet yields the same packet content.
     */
    @Test
    public void testWriteReadRpcResponsePacket() throws IOException {
        RpcPacketStreamer rpcPacketStreamer = new RpcPacketStreamer(STREAMER);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        RpcResponsePacket responsePacket = new RpcResponsePacket();
        responsePacket.setCorrelationId(UUID.randomUUID().toString());
        responsePacket.setBody(values.toArray());
        rpcPacketStreamer.writeResponse(byteArrayOutputStream, responsePacket);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        RpcResponsePacket readRpcResponsePacket = rpcPacketStreamer.readResponse(byteArrayInputStream, valueClasses);
        Assert.assertArrayEquals(responsePacket.getBody(), readRpcResponsePacket.getBody());
        Assert.assertEquals(responsePacket.getCorrelationId(), readRpcResponsePacket.getCorrelationId());
    }

}
