package com.hileco.drpc.reflection;

import com.hileco.drpc.generic.JSONStreamer;
import com.hileco.drpc.generic.Streamer;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Philipp Gayret
 */
public class JSONStreamerTest {

    private final List<?> values = Arrays.asList("Hello world", Long.MAX_VALUE, Long.MIN_VALUE, false, true, 0, -0, 1, -1, new TestSerializableObject(10, 20));
    private final List<Class<?>> valueClasses = values.stream().map(Object::getClass).collect(Collectors.toList());

    /**
     * Verifies that serializing and deserializing an object list matches the given original object list.
     */
    @Test
    public void test() throws IOException {
        Streamer streamer = new JSONStreamer();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        streamer.serializeTo(byteArrayOutputStream, values);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        Object[] deserialized = streamer.deserializeFrom(byteArrayInputStream, valueClasses);
        Assert.assertArrayEquals(deserialized, values.toArray());
    }

    /**
     * Verifies that serializing and deserializing an object list list matches the given original object list list.
     */
    @Test
    public void testMultiple() throws IOException {
        Streamer streamer = new JSONStreamer();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        streamer.serializeTo(byteArrayOutputStream, values, values);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        Object[] deserialized = streamer.deserializeFrom(byteArrayInputStream, valueClasses, valueClasses);
        Assert.assertArrayEquals(Arrays.copyOfRange(deserialized, 0, values.size()), values.toArray());
        Assert.assertArrayEquals(Arrays.copyOfRange(deserialized, values.size(), values.size() * 2), values.toArray());
    }

}
