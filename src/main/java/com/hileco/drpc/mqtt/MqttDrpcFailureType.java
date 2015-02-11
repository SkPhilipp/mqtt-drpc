package com.hileco.drpc.mqtt;

/**
 * Enumeration of all known and possibly recoverable distinct failures.
 *
 * @author Philipp Gayret
 */
public enum MqttDrpcFailureType {

    PUBLISH,
    SUBSCRIBE,
    UNSUBSCRIBE,
    DISCONNECTED

}
