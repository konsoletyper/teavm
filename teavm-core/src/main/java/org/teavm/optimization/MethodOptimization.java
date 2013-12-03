package org.teavm.optimization;

import org.teavm.model.MethodHolder;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public interface MethodOptimization {
    void optimize(MethodHolder method);
}
