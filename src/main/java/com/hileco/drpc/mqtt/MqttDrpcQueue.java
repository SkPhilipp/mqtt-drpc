package com.hileco.drpc.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Allows interactions with the mqtt broker to be done asynchronously.
 *
 * @author Philipp Gayret
 */
public class MqttDrpcQueue {

    // TODO: This FailureHandler should take care of automatic reconnects
    // TODO: This FailureHandler should allow for a delegate failure handler
    // TODO: This FailureHandler should and retry sending messages

    private final ExecutorService executorService;
    private final MqttClient mqttClient;

    /**
     * @param mqttClient client to send messages over
     * @param threads    amount of threads to use to interact with the client simultaneously
     */
    public MqttDrpcQueue(MqttClient mqttClient, int threads) {
        this.executorService = Executors.newFixedThreadPool(threads);
        this.mqttClient = mqttClient;
    }

    public void pause() {

    }

    public void resume() {

    }

    public boolean connect(MqttConnectOptions mqttConnectOptions) {
        boolean[] result = {false};
        MqttDrpcTask mqttDrpcTask = new MqttDrpcTask(executorService) {
            @Override
            public void run() throws Exception {
                mqttClient.connect(mqttConnectOptions);
                result[0] = true;
            }
        };
        mqttDrpcTask.start();
        mqttDrpcTask.awaitCompletion();
        return result[0];
    }

    public void queuedPublish(String callback, MqttMessage message) {
        MqttDrpcTask mqttDrpcTask = new MqttDrpcTask(executorService) {
            @Override
            public void run() throws Exception {
                mqttClient.publish(callback, message);
            }
        };
        mqttDrpcTask.start();
        mqttDrpcTask.awaitCompletion();
    }

    public void queuedSubscribe(String[] topics) {
        MqttDrpcTask mqttDrpcTask = new MqttDrpcTask(executorService) {
            @Override
            public void run() throws Exception {
                mqttClient.subscribe(topics);
            }
        };
        mqttDrpcTask.start();
        mqttDrpcTask.awaitCompletion();
    }

    public void queuedUnsubscribe(String[] topics) {
        MqttDrpcTask mqttDrpcTask = new MqttDrpcTask(executorService) {
            @Override
            public void run() throws Exception {
                mqttClient.unsubscribe(topics);
            }
        };
        mqttDrpcTask.start();
        mqttDrpcTask.awaitCompletion();
    }

}
