package com.hileco.drpc.generic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Specification for arguments ( any serializable objects ) list serializing and deserializing.
 *
 * @author Philipp Gayret
 */
public abstract class Streamer {

    /**
     * Converts a byte stream to an array of objects of type of the given elementTypes.
     * <p>
     * Does not close the stream.
     *
     * @param argsStream   stream to a JSON array
     * @param elementTypes classes to parse the elements as
     * @return instantiated objects
     */
    public abstract Object[] deserializeFrom(InputStream argsStream, List<Class<?>> elementTypes) throws IOException;

    /**
     * Converts an array of objects to bytes, bytes are written to the given outputStream.
     * <p>
     * Does not close the stream.
     *
     * @param outputStream stream to write JSON array to
     * @param arguments    serializable objects to be written
     */
    public abstract void serializeTo(OutputStream outputStream, List<?> arguments) throws IOException;

    /**
     * Converts a byte stream to an array of objects of type of the given elementTypes.
     * <p>
     * Does not close the stream.
     *
     * @param argsStream stream to a JSON array
     * @param typesList  classes to parse the elements as
     * @return instantiated objects
     */
    @SafeVarargs
    public final Object[] deserializeFrom(InputStream argsStream, List<Class<?>>... typesList) throws IOException {
        ArrayList<Class<?>> list = new ArrayList<>();
        for (List<Class<?>> types : typesList) {
            list.addAll(types);
        }
        return this.deserializeFrom(argsStream, list);
    }

    /**
     * Converts an array of objects to bytes, bytes are written to the given outputStream.
     * <p>
     * Does not close the stream.
     *
     * @param outputStream  stream to write JSON array to
     * @param argumentsList serializable objects to be written
     */
    public final void serializeTo(OutputStream outputStream, List<?>... argumentsList) throws IOException {
        ArrayList<Object> list = new ArrayList<>();
        for (List<?> arguments : argumentsList) {
            list.addAll(arguments);
        }
        this.serializeTo(outputStream, list);
    }

}
