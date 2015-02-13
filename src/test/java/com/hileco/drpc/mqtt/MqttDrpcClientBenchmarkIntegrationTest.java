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
     * <p>
     * Benchmark does not account for:
     * - Quality of service messages
     * - Subscriptions made ( 1 per callback )
     * <p>
     * Bechmark counts messages as well as and callback messages.
     * <p>
     * Each service invocation includes:
     * - Arguments serialisation
     * - Subscribe to callback topic on broker as client
     * - Transfer to broker as client, on service topic
     * - Processing by broker
     * - Receiving from broker as service
     * - Arguments deserialisation
     * - Service implementation call
     * - Result serialisation
     * - Transfer to broker as service, on callback topic
     * - Receiving from broker as client
     * - Result deserialisation
     * - Unsubscribing from callback topic on broker as client
     * - Returning result
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
        int limit = 1000;
        int threads = 1;
        for (int j = 0; j < limit; j++) {
            Integer a = (int) (Math.random() * 100);
            Integer b = (int) (Math.random() * 100);
            Integer result = remoteCalculator.add(a, b);
            Assert.assertTrue(result == a + b);
        }
        Long diff = System.currentTimeMillis() - start;
        // divive the microsecond difference in time by twice the limit, to account for regular as well as callback messages being sent
        Long micros = TimeUnit.MILLISECONDS.toMicros(diff) / (threads * limit * 2);
        LOG.info("Messages sent       : {}", threads * limit * 2);
        LOG.info("Time per message    : {} microseconds", micros);
        LOG.info("Messages per second : {}", TimeUnit.SECONDS.toMicros(1) / micros);
    }

}
