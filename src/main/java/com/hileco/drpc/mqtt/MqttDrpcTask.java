package com.hileco.drpc.mqtt;

import java.util.concurrent.ExecutorService;

/**
 * A slight abstraction for concurrency, this class represents a task that can be reattempted and waited for completion.
 *
 * @author Philipp Gayret
 */
public class MqttDrpcTask {

    private final MqttDrpcFailureHandler mqttDrpcFailureHandler;
    private final ExecutorService executorService;
    private final TaskBody taskBody;
    private boolean completed;
    private Exception failure;
    private int retries;
    private final Object monitor;

    public static interface TaskBody {

        public void run() throws Exception;

    }

    public MqttDrpcTask(ExecutorService executorService, MqttDrpcFailureHandler mqttDrpcFailureHandler, TaskBody taskBody) {
        this.executorService = executorService;
        this.mqttDrpcFailureHandler = mqttDrpcFailureHandler;
        this.taskBody = taskBody;
        this.retries = 0;
        failure = null;
        completed = false;
        monitor = new Object();
    }

    public void start() {
        executorService.submit(() -> {
            try {
                taskBody.run();
                synchronized (monitor) {
                    this.completed = true;
                    this.monitor.notifyAll();
                }
            } catch (Exception e) {
                if (mqttDrpcFailureHandler.shouldRetry(e, this)) {
                    retries++;
                    start();
                } else {
                    this.failure = e;
                    this.completed = true;
                    this.monitor.notifyAll();
                }
            }
        });
    }

    /**
     * This method waits for the successful completion of the task, or throws a {@link MqttDrpcRuntimeException} if the
     * failure handler decides the task is no longer worth retrying.
     */
    public void join() {
        synchronized (this.monitor) {
            while (!completed) {
                try {
                    this.monitor.wait();
                } catch (InterruptedException e) {
                    // ignore, and re-wait
                }
            }
        }
        if (failure != null) {
            throw new MqttDrpcRuntimeException("Task failed, and unable to retry", failure);
        }
    }

    /**
     * @return the amount of times the task has been resubmitted for execution
     */
    public int getRetries() {
        return retries;
    }

}
