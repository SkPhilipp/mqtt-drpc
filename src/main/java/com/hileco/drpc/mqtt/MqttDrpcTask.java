package com.hileco.drpc.mqtt;

import java.util.concurrent.ExecutorService;

/**
 * A slight abstraction for concurrency, this class represents a task that can be retried and waited for completion.
 *
 * @author Philipp Gayret
 */
public abstract class MqttDrpcTask {

    private boolean completed = false;
    private final Object monitor = new Object();
    private final ExecutorService executorService;
    private int retries;

    public MqttDrpcTask(ExecutorService executorService) {
        this.executorService = executorService;
        this.retries = 0;
    }

    public abstract void run() throws Exception;

    public void start() {
        executorService.submit(() -> {
            try {
                run();
                markComplete();
            } catch (Exception e) {
                // TODO: This is currently just a very basic decision, should be delegated
                if (retries < 5) {
                    start();
                }
            }
        });
    }

    public void awaitCompletion() {
        if (!completed) {
            synchronized (this.monitor) {
                try {
                    this.monitor.wait();
                } catch (InterruptedException e) {
                    if (!this.completed) {
                        awaitCompletion();
                    }
                }
            }
        }
    }

    public void markComplete() {
        this.completed = true;
        this.monitor.notifyAll();
    }

}
