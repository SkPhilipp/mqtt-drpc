package com.hileco.drpc.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Ignore;
import org.junit.Test;

import java.util.UUID;

/**
 * This test is basically the sample you can find at https://eclipse.org/paho/clients/java/
 *
 * @author Philipp Gayret
 */
public class MqttLocalSetupIntegrationTest {

    private static final String topic = "topic-for-test/ing";
    private static final String content = "Hello world";
    private static final int qos = 2;
    private static final String broker = "tcp://localhost:1883";
    private static final String clientId = UUID.randomUUID().toString();

    /**
     * Verifies that a broker can be connected to.
     */
    @Ignore(value = "Integration test, relies on a local broker")
    @Test
    public void test() throws MqttException {
        MemoryPersistence persistence = new MemoryPersistence();
        MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        sampleClient.connect(connOpts);
        MqttMessage message = new MqttMessage(content.getBytes());
        message.setQos(qos);
        sampleClient.publish(topic, message);
        sampleClient.disconnect();
    }

}