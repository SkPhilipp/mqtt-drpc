package com.hileco.drpc.mqtt;

import com.hileco.drpc.generic.ServiceConnector;
import com.hileco.drpc.generic.SilentCloseable;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Philipp Gayret
 */
public class MqttDrpcClientIntegrationTest {

    private static final String broker = "tcp://localhost.org:1883";
    private static final String identifier = "12345";

    /**
     * A basic test which verifies whether the whole process of publishing a service onto a broker, then calling
     * that service via the broker and retrieving a response via the broker, functions.
     */
    @Ignore(value = "Integration test, relies on a local broker")
    @Test
    public void test() throws MqttException {
        MqttDrpcClient mqttDrpcClient = new MqttDrpcClientBuilder().build(broker);
        mqttDrpcClient.connect();
        SilentCloseable publishedService = mqttDrpcClient.publish(CalculatorService.class, identifier, (a, b) -> a + b);
        ServiceConnector<CalculatorService> connector = mqttDrpcClient.connector(CalculatorService.class);
        CalculatorService remoteCalculator = connector.connect(identifier);
        Integer a = (int) (Math.random() * 100);
        Integer b = (int) (Math.random() * 100);
        Integer result = remoteCalculator.add(a, b);
        Assert.assertTrue(result == a + b);
        publishedService.close();
    }

}
