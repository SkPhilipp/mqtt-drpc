package com.hileco.drpc.mqtt;

/**
 * An example interface to publish and invoke using an {@link MqttDrpcClient}.
 *
 * @author Philipp Gayret
 */
public interface CalculatorService {

    public Integer add(Integer a, Integer b);

}
