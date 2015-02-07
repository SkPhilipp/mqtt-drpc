package com.hileco.drpc.mqtt;

/**
 * An example interface to publish and invoke using an {@link com.hileco.drpc.mqtt.MqttDrpcClient}.
 *
 * @author Philipp Gayret
 */
public interface CalculatorInterface {

    public Integer add(Integer a, Integer b);

}
