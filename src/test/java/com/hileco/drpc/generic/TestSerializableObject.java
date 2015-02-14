package com.hileco.drpc.generic;

import java.io.Serializable;

/**
 * An example of a basic object useable for serialization by an {@link com.hileco.drpc.generic.Streamer}.
 *
 * @author Philipp Gayret
 */
public class TestSerializableObject implements Serializable {

    private int x;
    private int y;

    public TestSerializableObject() {
    }

    public TestSerializableObject(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestSerializableObject that = (TestSerializableObject) o;

        if (x != that.x) return false;
        if (y != that.y) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }

}
