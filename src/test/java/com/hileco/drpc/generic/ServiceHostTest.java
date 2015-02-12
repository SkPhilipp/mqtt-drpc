package com.hileco.drpc.generic;

import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Philipp Gayret
 */
public class ServiceHostTest {

    public static final String TOPIC = "topic-for-test/ing";

    /**
     * Verifies that registering for a topic once, then accepting a message once results in one accessed mock.
     */
    @Test
    public void testOneRegisterOneAccept() throws IOException {
        ServiceHost serviceHost = new ServiceHost();
        serviceHost.register(TOPIC, (topic, content) -> content.reset());
        InputStream mockInputStream = Mockito.mock(InputStream.class);
        serviceHost.accept(TOPIC, mockInputStream);
        Mockito.verify(mockInputStream, Mockito.times(1)).reset();
    }

    /**
     * Verifies that registering for a topic once, then accepting a message twice results in a twice accessed mock.
     */
    @Test
    public void testOneRegisterManyAccept() throws IOException {
        ServiceHost serviceHost = new ServiceHost();
        serviceHost.register(TOPIC, (topic, content) -> content.reset());
        InputStream mockInputStream = Mockito.mock(InputStream.class);
        serviceHost.accept(TOPIC, mockInputStream);
        serviceHost.accept(TOPIC, mockInputStream);
        Mockito.verify(mockInputStream, Mockito.times(2)).reset();
    }

    /**
     * Verifies that registering for a topic twice, then accepting a message once results in a twice accessed mock.
     */
    @Test
    public void testManyRegisterOneAccept() throws IOException {
        ServiceHost serviceHost = new ServiceHost();
        serviceHost.register(TOPIC, (topic, content) -> content.reset());
        serviceHost.register(TOPIC, (topic, content) -> content.reset());
        InputStream mockInputStream = Mockito.mock(InputStream.class);
        serviceHost.accept(TOPIC, mockInputStream);
        Mockito.verify(mockInputStream, Mockito.times(2)).reset();
    }

    /**
     * Verifies that registering for a topic twice, then accepting a message twice results in a four times accessed mock.
     */
    @Test
    public void testManyRegisterManyAccept() throws IOException {
        ServiceHost serviceHost = new ServiceHost();
        serviceHost.register(TOPIC, (topic, content) -> content.reset());
        serviceHost.register(TOPIC, (topic, content) -> content.reset());
        InputStream mockInputStream = Mockito.mock(InputStream.class);
        serviceHost.accept(TOPIC, mockInputStream);
        serviceHost.accept(TOPIC, mockInputStream);
        Mockito.verify(mockInputStream, Mockito.times(4)).reset();
    }

    /**
     * Verifies that registering for a topic once and unregistering ocne, then accepting a message results in no mock accesses.
     */
    @Test
    public void testOneRegisterOneUnregister() throws IOException {
        ServiceHost serviceHost = new ServiceHost();
        SilentCloseable closeable = serviceHost.register(TOPIC, (topic, content) -> content.reset());
        closeable.close();
        InputStream mockInputStream = Mockito.mock(InputStream.class);
        serviceHost.accept(TOPIC, mockInputStream);
        Mockito.verify(mockInputStream, Mockito.times(0)).reset();
    }

    /**
     * Verifies that registering for a topic twice and unregistering ocne, then accepting a message results in one mock access.
     */
    @Test
    public void testManyRegisterOneUnregister() throws IOException {
        ServiceHost serviceHost = new ServiceHost();
        serviceHost.register(TOPIC, (topic, content) -> content.reset());
        SilentCloseable closeable = serviceHost.register(TOPIC, (topic, content) -> content.reset());
        closeable.close();
        InputStream mockInputStream = Mockito.mock(InputStream.class);
        serviceHost.accept(TOPIC, mockInputStream);
        Mockito.verify(mockInputStream, Mockito.times(1)).reset();
    }

}
