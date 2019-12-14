/*
 * Copyright 2004-2019 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.mvstore.type;

import java.nio.ByteBuffer;
import java.util.Comparator;

import org.h2.mvstore.WriteBuffer;

/**
 * A data type.
 */
public interface DataType<T> extends Comparator<T> {

    /**
     * Compare two keys.
     *
     * @param a the first key
     * @param b the second key
     * @return -1 if the first key is smaller, 1 if larger, and 0 if equal
     * @throws UnsupportedOperationException if the type is not orderable
     */
    @Override
    int compare(T a, T b);

    /**
     * Estimate the used memory in bytes.
     *
     * @param obj the object
     * @return the used memory
     */
    int getMemory(T obj);

    /**
     * Write an object.
     *
     * @param buff the target buffer
     * @param obj the value
     */
    void write(WriteBuffer buff, T obj);

    /**
     * Write a list of objects.
     *
     * @param buff the target buffer
     * @param storage the objects
     * @param len the number of objects to write
     */
    void write(WriteBuffer buff, Object storage, int len);

    /**
     * Read an object.
     *
     * @param buff the source buffer
     * @return the object
     */
    T read(ByteBuffer buff);

    /**
     * Read a list of objects.
     *
     * @param buff the target buffer
     * @param storage the objects
     * @param len the number of objects to read
     */
    void read(ByteBuffer buff, Object storage, int len);

    /**
     * Create storage object of array type to hold values
     *
     * @param size number of values to hold
     * @return storage object
     */
    T[] createStorage(int size);
}

