package org.teavm.platform.metadata;

import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public interface MetadataGenerator {
    Object generateMetadata(MetadataGeneratorContext context, MethodReference method);
}
