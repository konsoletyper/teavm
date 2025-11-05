/*
 *  Copyright 2015 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.java.lang.reflect;

import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.ValueType;

public abstract class BaseAnnotationDependencyListener extends AbstractDependencyListener {
    protected final AnnotationGenerationHelper annotHelper;

    public BaseAnnotationDependencyListener(boolean enumsAsInts, boolean needAnnotImplCtor) {
        annotHelper = new AnnotationGenerationHelper(enumsAsInts, needAnnotImplCtor);
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        ValueType type = method.getMethod().getResultType();
        while (type instanceof ValueType.Array) {
            type = ((ValueType.Array) type).getItemType();
        }
        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            ClassReader cls = agent.getClassSource().get(className);
            if (cls != null && cls.hasModifier(ElementModifier.ANNOTATION)) {
                agent.linkClass(className);
            }
        }
    }
}
