package org.teavm.dependency;

/**
 *
 * @author Alexey Andreev
 */
public interface DependencyTypeFilter {
    boolean match(String type);
}
