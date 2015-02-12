package com.hileco.drpc.mqtt;

/**
 * Decides on how failed interactions with the broker should act, and how failures should be processed.
 *
 * @author Philipp Gayret
 */
public interface MqttDrpcFailureHandler {

    /**
     * Decides whether a failed task should be reattempted.
     *
     * @param cause failure cause
     * @param task  failed task metadata
     * @return true to reschedule the task
     */
    public boolean shouldRetry(Exception cause, MqttDrpcTask task);

    /**
     * Handles a disconnect between the client and broker.
     *
     * @param throwable disconnect cause
     */
    public void handleDisconnect(Throwable throwable);

}
