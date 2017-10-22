/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class ClassReaderSourceHelper {
    private ClassReaderSourceHelper() {
    }

    static MethodReader resolveMethodImplementation(ClassReaderSource classSource, String className,
            MethodDescriptor methodDescriptor, Set<String> visited) {
        if (!visited.add(className)) {
            return null;
        }

        ClassReader cls = classSource.get(className);
        if (cls == null) {
            return null;
        }

        MethodReader method = cls.getMethod(methodDescriptor);
        if (method != null && !method.hasModifier(ElementModifier.ABSTRACT)) {
            return method;
        }

        MethodReader mostSpecificMethod = null;
        List<String> superClasses = new ArrayList<>();
        if (cls.getParent() != null) {
            superClasses.add(cls.getParent());
        }
        superClasses.addAll(cls.getInterfaces());

        for (String superClass : superClasses) {
            MethodReader resultFromSuperClass = resolveMethodImplementation(classSource, superClass,
                    methodDescriptor, visited);
            if (resultFromSuperClass != null) {
                if (mostSpecificMethod == null || classSource.isSuperType(mostSpecificMethod.getOwnerName(),
                        resultFromSuperClass.getOwnerName()).orElse(false)) {
                    mostSpecificMethod = resultFromSuperClass;
                }
            }
        }

        return mostSpecificMethod;
    }
}
