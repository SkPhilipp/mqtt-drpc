package com.hileco.drpc.mqtt;

import com.hileco.drpc.api.ServiceConnector;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author Philipp Gayret
 */
public class MqttDrpcClientIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(MqttDrpcClientIntegrationTest.class);

    private static final String broker = "tcp://localhost:1883";
    private static final String identifier = "12345";

    /**
     * Verifies that a client can publish a service at a broker and call its own service via the broker.
     * <p>
     * Performs this verification serially until a limit is reached and then outputs some basic benchmarks.
     * <p>
     * Benchmarks do not account for:
     * - Quality of service messages
     * - Subscriptions made ( 1 per callback )
     * <p>
     * Bechmark counts messages as well as and callback messages.
     * <p>
     * Each message goes through the process of:
     * - Serialisation
     * - Transfer to broker
     * - Processing by broker
     * - Receiving from broker
     * - Deserialisation
     */
    @Ignore(value = "Integration test, must only be used to verify connectivity to a broker")
    @Test
    public void test() throws MqttException {
        MqttDrpcClient mqttDrpcClient = new MqttDrpcClient(broker);
        mqttDrpcClient.connect();
        mqttDrpcClient.publish(CalculatorService.class, identifier, (a, b) -> a + b);
        ServiceConnector<CalculatorService> connector = mqttDrpcClient.connector(CalculatorService.class);
        CalculatorService remoteCalculator = connector.connect(identifier);
        LOG.info("Starting test, this may take a while");
        Long start = System.currentTimeMillis();
        Integer limit = 100;
        for (int i = 0; i < limit; i++) {
            Integer a = (int) (Math.random() * 100);
            Integer b = (int) (Math.random() * 100);
            Integer result = remoteCalculator.add(a, b);
            Assert.assertTrue(result == a + b);
        }
        Long diff = System.currentTimeMillis() - start;
        // divive the microsecond difference in time by twice the limit, to account for regular as well as callback messages being sent
        Long micros = TimeUnit.MILLISECONDS.toMicros(diff) / (limit * 2);
        LOG.info("Messages sent       : {}", limit * 2);
        LOG.info("Time per message    : {} microseconds", micros);
        LOG.info("Messages per second : {}", TimeUnit.SECONDS.toMicros(1) / micros);
    }

}
