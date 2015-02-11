package com.hileco.drpc.mqtt;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * @author Philipp Gayret
 */
public class MqttDrpcTopicBuilderTest {

    public static final String IDENTIFIER = "1234";

    /**
     * Verifies that an operation with identifier
     * - Start with goperation and service topic string
     * - Ends with a slash and then the identifier
     * <p>
     * This is important for MQTT, messages sent to generic topics must also arrive on the identied topics.
     */
    @Test
    public void test() {
        MqttDrpcTopicBuilder mqttDrpcTopicBuilder = new MqttDrpcTopicBuilder();
        Class<CalculatorService> service = CalculatorService.class;
        Method method = service.getMethods()[0];
        String operation = mqttDrpcTopicBuilder.operation(service, method);
        String operationWithIdentifier = mqttDrpcTopicBuilder.operation(service, method, IDENTIFIER);
        Assert.assertTrue(operationWithIdentifier.startsWith(operation));
        Assert.assertTrue(operationWithIdentifier.endsWith("/" + IDENTIFIER));
    }

}
