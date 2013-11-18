package org.teavm.model;

import java.util.Set;

/**
 *
 * @author Alexey Andreev
 */
public interface ListableClassHolderSource extends ClassHolderSource {
    Set<String> getClassNames();
}
