package org.teavm.dependency;

import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public interface DependencyAgent extends DependencyInfo {
    DependencyNode createNode();

    ClassReaderSource getClassSource();

    String generateClassName();

    void submitClass(ClassHolder cls);

    MethodDependency linkMethod(MethodReference methodRef, DependencyStack stack);

    void initClass(String className, final DependencyStack stack);

    FieldDependency linkField(FieldReference fieldRef, DependencyStack stack);
}
