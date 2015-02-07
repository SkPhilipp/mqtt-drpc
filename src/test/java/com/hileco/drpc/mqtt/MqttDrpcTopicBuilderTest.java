package com.hileco.drpc.mqtt;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Philipp Gayret
 */
public class MqttDrpcTopicBuilderTest {

    public static final String SERVICE = "service";
    public static final String OPERATION = "operation";
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
        String operation = mqttDrpcTopicBuilder.operation(SERVICE, OPERATION).getTopic();
        String operationWithIdentifier = mqttDrpcTopicBuilder.operation(SERVICE, OPERATION, IDENTIFIER).getTopic();
        Assert.assertTrue(operationWithIdentifier.startsWith(operation));
        Assert.assertTrue(operationWithIdentifier.endsWith("/" + IDENTIFIER));
    }

}
