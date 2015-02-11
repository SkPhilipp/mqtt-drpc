package com.hileco.drpc.api;

import com.hileco.drpc.transport.SilentCloseable;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Philipp Gayret
 */
public interface ServiceConnector<T> {

    /**
     * Constructs a dynamically generated implementation of {@link T}, any method calls on the retuned object will be
     * converted to RPC calls.
     *
     * @param identifier remote object identifier
     * @return dynamically generated implementation of type {@link T}
     */
    public T connect(String identifier);

    /**
     * Performs a distributed remote procedure call, using the given invoker to provide the method call information.
     * Responses are forwarded to the given consumer.
     *
     * @param invoker  a function which must immediately make one single call on given type {@link T} instance
     * @param consumer the response consumer
     * @param <R>      the response type
     * @return the closeable useable to end listening for responses
     */
    public <R> SilentCloseable drpc(Function<T, R> invoker, Consumer<R> consumer);

}
