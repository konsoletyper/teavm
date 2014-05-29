package org.teavm.platform.metadata;

import org.teavm.model.ListableClassReaderSource;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public interface MetadataGeneratorContext {
    ListableClassReaderSource getClassSource();

    <T> T createResource(Class<T> resourceType);
}
