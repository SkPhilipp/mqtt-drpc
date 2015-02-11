package com.hileco.drpc.reflection;

import com.hileco.drpc.api.ServiceConnector;
import com.hileco.drpc.transport.SilentCloseable;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Implementation of {@link com.hileco.drpc.api.ServiceConnector}, delegating transport calls to a given {@link com.hileco.drpc.transport.ServiceHost}.
 *
 * @param <T> remote service type
 * @author Philipp Gayret
 */
public abstract class ProxyServiceConnector<T> implements ServiceConnector<T> {

    private static final Object NO_RESULT = new Object();

    public static final int TIMEOUT = 60000;

    private final Class<T> type;

    public ProxyServiceConnector(Class<T> type) {
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <R> SilentCloseable drpc(Function<T, R> invoker, Consumer<R> consumer) {
        Invocation invocation = Invocation.one(type, invoker::apply);
        return this.call(type, invocation.getMethod(), invocation.getArguments(), consumer);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("all")
    @Override
    public T connect(String identifier) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, (proxy, method, arguments) -> {
            Object[] results = new Object[]{NO_RESULT};
            SilentCloseable listener = this.call(type, method, arguments, (result) -> {
                synchronized (results) {
                    results[0] = result;
                    results.notifyAll();
                }
            });
            synchronized (results) {
                if (results[0] == NO_RESULT) {
                    results.wait(TIMEOUT);
                }
                listener.close();
            }
            return results[0] == NO_RESULT ? null : results[0];
        });
    }

    /**
     * Should perform a remote procedure call, any responses must be forwarded to the consumer.
     *
     * @param type      service connector type
     * @param method    invoked method
     * @param arguments invocation arguments
     * @param consumer  response handler
     * @param <R>       response type
     * @return {@link SilentCloseable} used to remove the consumer as a response handler.
     */
    public abstract <R> SilentCloseable call(Class<?> type, Method method, Object[] arguments, Consumer<R> consumer);

}
