package com.hileco.drpc.mqtt;

import java.util.concurrent.ExecutorService;

/**
 * A slight abstraction for concurrency, this class represents a task that can be reattempted and waited on for completion.
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
        this.failure = null;
        this.completed = false;
        this.monitor = new Object();
    }

    /**
     * Queues the task for execution. Consults the {@link #mqttDrpcFailureHandler} on failures.
     */
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
                    synchronized (monitor) {
                        this.failure = e;
                        this.completed = true;
                        this.monitor.notifyAll();
                    }
                }
            }
        });
    }

    /**
     * Waits for the completion of the task, or throws a {@link MqttDrpcRuntimeException} when the task failed.
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
     * @return amount of resubmits to execution queue
     */
    public int getRetries() {
        return retries;
    }

}
