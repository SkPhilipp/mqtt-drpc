package com.hileco.drpc.generic;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Philipp Gayret
 */
public interface MessageReceiver {

    /**
     * Handles a message for a given topic.
     *
     * @param topic   topic mapped to a content handler
     * @param content content to process
     */
    public void accept(String topic, InputStream content) throws IOException;

}
