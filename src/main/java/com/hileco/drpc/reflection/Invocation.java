package com.hileco.drpc.reflection;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a method invocation information.
 *
 * @author Philipp Gayret
 */
public class Invocation {

    private final Method method;
    private final Object[] arguments;

    /**
     * Returns the list of invocations made on the proxy object passed to the given `invoker` function.
     * <p>
     * The list may change even after being returned by this method.
     *
     * @param interfaceType type to proxy
     * @param invoker       consumer which should make calls on the proxy it'll be given
     * @return invocation list
     */
    @SuppressWarnings("unchecked")
    public static <T> List<Invocation> many(Class<T> interfaceType, Consumer<T> invoker) {
        List<Invocation> invocations = new ArrayList<>();
        ClassLoader classLoader = interfaceType.getClassLoader();
        T listeningProxy = (T) Proxy.newProxyInstance(classLoader, new Class[]{interfaceType}, (proxy, method, args) -> {
            Invocation invocation = new Invocation(method, args);
            invocations.add(invocation);
            if (method.getReturnType().isPrimitive()) {
                return InvocationDefaults.defaultValue(method.getReturnType());
            } else {
                return null;
            }
        });
        invoker.accept(listeningProxy);
        return invocations;
    }

    /**
     * Returns the single invocation the given invoker must make when it is given a proxy object.
     *
     * @param interfaceType type to proxy
     * @param invoker       consumer which should make calls on the proxy it'll be given
     * @return a single invocation
     */
    public static <T> Invocation one(Class<T> interfaceType, Consumer<T> invoker) {
        List<Invocation> invocations = Invocation.many(interfaceType, invoker);
        if (invocations.size() != 1) {
            throw new IllegalArgumentException("Given invoker must make exactly one call only, on " + interfaceType.getName());
        }
        return invocations.get(0);
    }

    public Invocation(Method method, Object[] arguments) {
        this.method = method;
        this.arguments = arguments;
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArguments() {
        return arguments;
    }

}
