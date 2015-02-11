package com.hileco.drpc.mqtt;

import com.hileco.drpc.format.TestSerializableObject;
import org.eclipse.paho.client.mqttv3.MqttException;
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
public class MqttDrpcClientTest {

    private final List<?> values = Arrays.asList("Hello world", Long.MAX_VALUE, Long.MIN_VALUE, false, true, 0, -0, 1, -1, new TestSerializableObject(10, 20));
    private final List<Class<?>> valueClasses = values.stream().map(Object::getClass).collect(Collectors.toList());

    /**
     * Verifies serializing and deserializing an {@link MqttDrpcPacket} with {@link com.hileco.drpc.mqtt.MqttDrpcClient} results in the same object content as the original.
     */
    @Test
    public void test() throws MqttException, IOException {
        MqttDrpcClient mqttDrpcClient = new MqttDrpcClient("tcp://iot.eclipse.org:1883");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        MqttDrpcPacket packet = new MqttDrpcPacket();
        packet.setMessageId(UUID.randomUUID().toString());
        packet.setBody(values.toArray());
        mqttDrpcClient.write(byteArrayOutputStream, packet);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        MqttDrpcPacket read = mqttDrpcClient.read(byteArrayInputStream, valueClasses);
        Assert.assertEquals(packet.getMessageId(), read.getMessageId());
        Assert.assertArrayEquals(packet.getBody(), read.getBody());
    }
}
