package com.hileco.drpc.mqtt;

import com.hileco.drpc.generic.ServiceConnector;
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
public class MqttDrpcClientBenchmarkIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(MqttDrpcClientBenchmarkIntegrationTest.class);

    private static final String broker = "tcp://localhost:1883";
    private static final String identifier = "12345";

    /**
     * Verifies that a client can publish a service at a broker and call its own service via the broker.
     * <p>
     * Performs this verification serially until a limit is reached and then outputs some basic benchmarks.
     *
     * Client --(request)--> Broker --(request)--> Service
     *                                                |
     * Client <-(response)-- Broker <--(response)----/
     *
     */
    @Ignore(value = "Integration test, relies on a local broker")
    @Test
    public void test() throws MqttException, InterruptedException {
        MqttDrpcClient mqttDrpcClient = new MqttDrpcClientBuilder().build(broker);
        mqttDrpcClient.connect();
        mqttDrpcClient.publish(CalculatorService.class, identifier, (a, b) -> a + b);
        ServiceConnector<CalculatorService> connector = mqttDrpcClient.connector(CalculatorService.class);
        CalculatorService remoteCalculator = connector.connect(identifier);
        LOG.info("Starting test, this may take a while");
        Long start = System.currentTimeMillis();
        int limit = 10000;
        for (int j = 0; j < limit; j++) {
            Integer a = (int) (Math.random() * 100);
            Integer b = (int) (Math.random() * 100);
            Integer result = remoteCalculator.add(a, b);
            Assert.assertTrue(result == a + b);
        }
        Long diff = System.currentTimeMillis() - start;
        // multiply by two, as a message is sent both per request and response
        Long micros = TimeUnit.MILLISECONDS.toMicros(diff) / (limit * 2);
        LOG.info("Messages sent: {}", limit * 2);
        LOG.info("Time per message: {} microseconds", micros);
        LOG.info("Service invocations per second: {}", TimeUnit.SECONDS.toMicros(1) / micros);
    }

}
