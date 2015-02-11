package com.hileco.drpc.mqtt;

/**
 * An exception for any kind of error which cannot be attempted to recover from.
 *
 * @author Philipp Gayret
 */
public class MqttDrpcRuntimeException extends RuntimeException {

    public MqttDrpcRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

}
