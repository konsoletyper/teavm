package org.teavm.dependency;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public interface DependencyConsumer {
    void propagate(String type);

    boolean hasType(String type);
}
