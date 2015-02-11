package com.hileco.drpc.api;

import com.hileco.drpc.transport.SilentCloseable;

/**
 * The main interface which defines a DRPC capable system.
 *
 * @author Philipp Gayret
 */
public interface Client {

    /**
     * Creates a {@link com.hileco.drpc.api.ServiceConnector} for the given type, through which remote services
     * can be invoked.
     *
     * @param type any interface which strictly defines functionality
     * @param <T>  type of the interface
     * @return connector for the given type
     */
    public <T> ServiceConnector<T> connector(Class<T> type);

    /**
     * Publishes a given implementation of a given interface as a remotely available service.
     *
     * @param type           any interface which strictly defines functionality
     * @param identifier     identifier unique to the service registry; only one implementation of the given type may have this identifier
     * @param implementation an implementation of the given type
     * @param <T>            type of the interface
     * @return closeable through which the service may be removed from the service registry
     */
    public <T> SilentCloseable publish(Class<T> type, String identifier, T implementation);

}
