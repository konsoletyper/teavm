/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.dependency;

import org.teavm.model.ClassHolder;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;

public class Linker {
    private DependencyInfo dependency;

    public Linker(DependencyInfo dependency) {
        this.dependency = dependency;
    }

    public void link(ClassHolder cls) {
        for (MethodHolder method : cls.getMethods().toArray(new MethodHolder[0])) {
            MethodReference methodRef = method.getReference();
            MethodDependencyInfo methodDep = dependency.getMethod(methodRef);
            if (methodDep == null || !methodDep.isUsed()) {
                if (method.hasModifier(ElementModifier.STATIC)) {
                    cls.removeMethod(method);
                } else {
                    method.getModifiers().add(ElementModifier.ABSTRACT);
                    method.getModifiers().remove(ElementModifier.NATIVE);
                    method.setProgram(null);
                }
            }
        }
        for (FieldHolder field : cls.getFields().toArray(new FieldHolder[0])) {
            FieldReference fieldRef = new FieldReference(cls.getName(), field.getName());
            if (dependency.getField(fieldRef) == null) {
                cls.removeField(field);
            }
        }

    }
}
