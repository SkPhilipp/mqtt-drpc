package com.hileco.drpc.reflection;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author Philipp Gayret
 */
public class InvocationTest {

    /**
     * Verifies that {@link Invocation#one(Class, java.util.function.Consumer)} returns the correct information when the proxy is invoked once.
     */
    @Test
    public void testOneOnce() {
        Invocation invocation = Invocation.one(TestInterface.class, (proxy) -> proxy.test(10, 20));
        Assert.assertEquals(invocation.getMethod().getName(), "test");
        Assert.assertEquals(invocation.getArguments()[0], 10);
        Assert.assertEquals(invocation.getArguments()[1], 20);
    }

    /**
     * Verifies that {@link Invocation#one(Class, java.util.function.Consumer)} throws an exception when the proxy is invoked twice.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testOneMany() {
        Invocation.one(TestInterface.class, (proxy) -> {
            proxy.test(10, 20);
            proxy.test(10, 20);
        });
    }

    /**
     * Verifies that {@link Invocation#many(Class, java.util.function.Consumer)} returns the correct information when the proxy is invoked once.
     */
    @Test
    public void testManyOnce() {
        List<Invocation> invocations = Invocation.many(TestInterface.class, (proxy) -> proxy.test(10, 20));
        Assert.assertEquals(invocations.get(0).getMethod().getName(), "test");
        Assert.assertEquals(invocations.get(0).getArguments()[0], 10);
        Assert.assertEquals(invocations.get(0).getArguments()[1], 20);
    }

    /**
     * Verifies that {@link Invocation#many(Class, java.util.function.Consumer)} returns the correct information when the proxy is invoked twice.
     */
    @Test
    public void testManyMany() {
        List<Invocation> invocations = Invocation.many(TestInterface.class, (proxy) -> {
            proxy.test(10, 20);
            proxy.test(30, 40);
        });
        Assert.assertEquals(invocations.get(0).getMethod().getName(), "test");
        Assert.assertEquals(invocations.get(0).getArguments()[0], 10);
        Assert.assertEquals(invocations.get(0).getArguments()[1], 20);
        Assert.assertEquals(invocations.get(1).getMethod().getName(), "test");
        Assert.assertEquals(invocations.get(1).getArguments()[0], 30);
        Assert.assertEquals(invocations.get(1).getArguments()[1], 40);
    }


}
