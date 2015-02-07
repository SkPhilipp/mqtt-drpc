package com.hileco.drpc.transport;

import java.io.IOException;
import java.io.InputStream;

/**
 * Message consumer specification.
 *
 * @author Philipp Gayret
 */
public interface MessageReceiver {

    /**
     * Handles any message on a given topic.
     *
     * @param topic   topic mapped to a content handler
     * @param content stream to content to process
     */
    public void accept(String topic, InputStream content) throws IOException;

}
