package com.hileco.drpc.transport;

/**
 * Address which contains a topic.
 *
 * @author Philipp Gayret
 */
public interface Topic {

    /**
     * @return the topic as a string
     */
    public abstract String getTopic();

}
