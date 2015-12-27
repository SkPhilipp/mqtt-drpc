package com.hileco.drpc.reflection;

import com.hileco.drpc.generic.SilentCloseable;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * @author Philipp Gayret
 */
public class ProxyServiceConnectorTest {

    public static final int EXPECTED_RESULT = 30;

    /**
     * Verifies that a result can be obtained asynchronously, with slight delay.
     */
    @Test
    public void testDelayedResponse() {
        ProxyServiceConnector<TestInterface> connector = new ProxyServiceConnector<TestInterface>(TestInterface.class) {
            @Override
            @SuppressWarnings("unchecked")
            public <R> SilentCloseable call(Class<?> type, Method method, String identifier, Object[] arguments, Consumer<R> consumer) {
                Thread thread = new Thread(() -> {
                    try {
                        Thread.sleep(5);
                        Integer result = EXPECTED_RESULT;
                        consumer.accept((R) result);
                    } catch (InterruptedException e) {
                        Assert.fail();
                    }
                });
                thread.start();
                return () -> {
                };
            }
        };
        TestInterface connect = connector.connect("123");
        Integer result = connect.test(10, 20);
        Assert.assertTrue(result == EXPECTED_RESULT);
    }

    /**
     * Verifies that a result can be obtained immediately, without delay.
     */
    @Test
    public void testImmediateResponse() {
        ProxyServiceConnector<TestInterface> connector = new ProxyServiceConnector<TestInterface>(TestInterface.class) {
            @Override
            @SuppressWarnings("unchecked")
            public <R> SilentCloseable call(Class<?> type, Method method, String identifier, Object[] arguments, Consumer<R> consumer) {
                Integer result = EXPECTED_RESULT;
                consumer.accept((R) result);
                return () -> {
                };
            }
        };
        TestInterface connect = connector.connect("123");
        Integer result = connect.test(10, 20);
        Assert.assertTrue(result == EXPECTED_RESULT);
    }

    /**
     * Verifies that a call which returns multiple results functions using drpc.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testDistributed() {
        ProxyServiceConnector<TestInterface> connector = new ProxyServiceConnector<TestInterface>(TestInterface.class) {
            @Override
            @SuppressWarnings("unchecked")
            public <R> SilentCloseable call(Class<?> type, Method method, String identifier, Object[] arguments, Consumer<R> consumer) {
                Integer result = EXPECTED_RESULT;
                consumer.accept((R) result);
                consumer.accept((R) result);
                consumer.accept((R) result);
                return () -> {
                };
            }
        };
        Consumer<Integer> mock = Mockito.mock(Consumer.class);
        SilentCloseable drpc = connector.drpc((testInterace) -> testInterace.test(1, 2), mock);
        Mockito.verify(mock, Mockito.times(3)).accept(EXPECTED_RESULT);
        drpc.close();
    }

}
