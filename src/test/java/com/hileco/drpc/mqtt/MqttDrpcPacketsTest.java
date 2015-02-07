package com.hileco.drpc.mqtt;

import com.hileco.drpc.format.JSONArgumentsStreamer;
import com.hileco.drpc.format.TestSerializableObject;
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
public class MqttDrpcPacketsTest {

    private final List<?> values = Arrays.asList("Hello world", Long.MAX_VALUE, Long.MIN_VALUE, false, true, 0, -0, 1, -1, new TestSerializableObject(10, 20));
    private final List<Class<?>> valueClasses = values.stream().map(Object::getClass).collect(Collectors.toList());

    /**
     * Verifies serializing and deserializing an {@link MqttDrpcPackets.Packet} with {@link MqttDrpcPackets} results in the same object content as the original.
     */
    @Test
    public void test() throws IOException {
        MqttDrpcPackets mqttDrpcPackets = new MqttDrpcPackets(new JSONArgumentsStreamer());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        MqttDrpcPackets.Packet packet = new MqttDrpcPackets.Packet();
        packet.setMessageId(UUID.randomUUID().toString());
        packet.setBody(values.toArray());
        mqttDrpcPackets.write(byteArrayOutputStream, packet);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        MqttDrpcPackets.Packet read = mqttDrpcPackets.read(byteArrayInputStream, valueClasses);
        Assert.assertEquals(packet.getMessageId(), read.getMessageId());
        Assert.assertArrayEquals(packet.getBody(), read.getBody());
    }

}
