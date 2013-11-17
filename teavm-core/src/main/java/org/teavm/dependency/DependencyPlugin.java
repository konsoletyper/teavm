package org.teavm.dependency;

import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public interface DependencyPlugin {
    void methodAchieved(DependencyChecker checker, MethodReference method);
}
