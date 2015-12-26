package org.teavm.classlib;

import org.teavm.model.ListableClassReaderSource;

/**
 *
 * @author Alexey Andreev
 */
public interface ResourceSupplier {
    String[] supplyResources(ClassLoader classLoader, ListableClassReaderSource classSource);
}
