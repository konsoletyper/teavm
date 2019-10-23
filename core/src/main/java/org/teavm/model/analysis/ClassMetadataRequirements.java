/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.model.analysis;

import java.util.HashMap;
import java.util.Map;
import org.teavm.dependency.DependencyInfo;
import org.teavm.dependency.MethodDependencyInfo;
import org.teavm.model.MethodReference;

public class ClassMetadataRequirements {
    private static final MethodReference GET_NAME_METHOD = new MethodReference(Class.class, "getName", String.class);
    private static final MethodReference GET_SIMPLE_NAME_METHOD = new MethodReference(Class.class,
            "getSimpleName", String.class);
    private static final MethodReference GET_DECLARING_CLASS_METHOD = new MethodReference(Class.class,
            "getDeclaringClass", Class.class);
    private static final MethodReference GET_ENCLOSING_CLASS_METHOD = new MethodReference(Class.class,
            "getEnclosingClass", Class.class);
    private static final ClassInfo EMPTY_INFO = new ClassInfo();
    private Map<String, ClassInfo> requirements = new HashMap<>();

    public ClassMetadataRequirements(DependencyInfo dependencyInfo) {
        MethodDependencyInfo getNameMethod = dependencyInfo.getMethod(GET_NAME_METHOD);
        if (getNameMethod != null) {
            addClassesRequiringName(requirements, getNameMethod.getVariable(0).getClassValueNode().getTypes());
        }

        MethodDependencyInfo getSimpleNameMethod = dependencyInfo.getMethod(GET_SIMPLE_NAME_METHOD);
        if (getSimpleNameMethod != null) {
            String[] classNames = getSimpleNameMethod.getVariable(0).getClassValueNode().getTypes();
            addClassesRequiringName(requirements, classNames);
            for (String className : classNames) {
                ClassInfo classInfo = requirements.computeIfAbsent(className, k -> new ClassInfo());
                classInfo.simpleName = true;
                classInfo.enclosingClass = true;
            }
        }

        MethodDependencyInfo getDeclaringClassMethod = dependencyInfo.getMethod(GET_DECLARING_CLASS_METHOD);
        if (getDeclaringClassMethod != null) {
            String[] classNames = getDeclaringClassMethod.getVariable(0).getClassValueNode().getTypes();
            for (String className : classNames) {
                requirements.computeIfAbsent(className, k -> new ClassInfo()).declaringClass = true;
            }
        }

        MethodDependencyInfo getEnclosingClassMethod = dependencyInfo.getMethod(GET_ENCLOSING_CLASS_METHOD);
        if (getEnclosingClassMethod != null) {
            String[] classNames = getEnclosingClassMethod.getVariable(0).getClassValueNode().getTypes();
            for (String className : classNames) {
                requirements.computeIfAbsent(className, k -> new ClassInfo()).enclosingClass = true;
            }
        }
    }

    public Info getInfo(String className) {
        ClassInfo result = requirements.get(className);
        if (result == null) {
            result = EMPTY_INFO;
        }
        return result;
    }

    private void addClassesRequiringName(Map<String, ClassInfo> target, String[] source) {
        for (String typeName : source) {
            if (typeName.startsWith("[")) {
                if (!typeName.endsWith(";")) {
                    continue;
                }
                int index = 0;
                while (typeName.charAt(index) == '[') {
                    ++index;
                }
                typeName = typeName.substring(index, typeName.length() - 1).replace('/', '.');
            }
            target.computeIfAbsent(typeName, k -> new ClassInfo()).name = true;
        }
    }

    static class ClassInfo implements Info {
        boolean name;
        boolean simpleName;
        boolean declaringClass;
        boolean enclosingClass;

        @Override
        public boolean name() {
            return name;
        }

        @Override
        public boolean simpleName() {
            return simpleName;
        }

        @Override
        public boolean declaringClass() {
            return declaringClass;
        }

        @Override
        public boolean enclosingClass() {
            return enclosingClass;
        }
    }

    public interface Info {
        boolean name();

        boolean simpleName();

        boolean declaringClass();

        boolean enclosingClass();
    }
}
