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

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.teavm.dependency.DependencyInfo;
import org.teavm.dependency.MethodDependencyInfo;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class ClassMetadataRequirements {
    private static final MethodReference GET_NAME_METHOD = new MethodReference(Class.class, "getName", String.class);
    private static final MethodReference GET_SIMPLE_NAME_METHOD = new MethodReference(Class.class,
            "getSimpleName", String.class);
    private static final MethodReference GET_SUPERCLASS_METHOD = new MethodReference(Class.class, "getSuperclass",
            Class.class);
    private static final MethodReference IS_ASSIGNABLE_METHOD = new MethodReference(Class.class, "isAssignableFrom",
            Class.class, boolean.class);
    private static final MethodReference GET_DECLARING_CLASS_METHOD = new MethodReference(Class.class,
            "getDeclaringClass", Class.class);
    private static final MethodReference GET_ENCLOSING_CLASS_METHOD = new MethodReference(Class.class,
            "getEnclosingClass", Class.class);
    private static final MethodReference NEW_ARRAY = new MethodReference(Array.class,
            "newInstance", Class.class, int.class, Object.class);
    private static final MethodReference ARRAY_GET = new MethodReference(Array.class,
            "get", Object.class, int.class, Object.class);
    private static final MethodReference ARRAY_SET = new MethodReference(Array.class,
            "set", Object.class, int.class, Object.class, void.class);
    private static final MethodReference ARRAY_LENGTH = new MethodReference(Array.class,
            "getLength", Object.class, int.class);
    private static final MethodReference ARRAY_COPY = new MethodReference(System.class,
            "arraycopy", Object.class, int.class, Object.class, int.class, int.class, void.class);
    private static final ClassInfo EMPTY_INFO = new ClassInfo();
    private Map<ValueType, ClassInfo> requirements = new HashMap<>();
    private boolean hasArrayGet;
    private boolean hasArraySet;
    private boolean hasArrayLength;
    private boolean hasArrayCopy;
    private boolean hasEnumConstants;
    private boolean hasSuperclass;
    private boolean hasIsAssignable;
    private boolean hasArrayNewInstance;
    private boolean hasNewInstance;
    private boolean hasClassInit;
    private boolean hasEnclosingClass;
    private boolean hasDeclaringClass;
    private boolean hasSimpleName;
    private boolean hasName;
    private boolean hasGetAnnotations;
    private boolean hasGetInterfaces;
    private boolean hasGetFields;
    private boolean hasGetMethods;

    public ClassMetadataRequirements(DependencyInfo dependencyInfo) {
        MethodDependencyInfo getNameMethod = dependencyInfo.getMethod(GET_NAME_METHOD);
        if (getNameMethod != null) {
            hasName = true;
            addClassesRequiringName(requirements, getNameMethod.getVariable(0).getClassValueNode().getTypes());
        }

        MethodDependencyInfo getSimpleNameMethod = dependencyInfo.getMethod(GET_SIMPLE_NAME_METHOD);
        if (getSimpleNameMethod != null) {
            hasSimpleName = true;
            var types = getSimpleNameMethod.getVariable(0).getClassValueNode().getTypes();
            addClassesRequiringName(requirements, types);
            for (var type : types) {
                ClassInfo classInfo = requirements.computeIfAbsent(type, k -> new ClassInfo());
                classInfo.simpleName = true;
                classInfo.enclosingClass = true;
            }
        }

        var getSuperclassMethod = dependencyInfo.getMethod(GET_SUPERCLASS_METHOD);
        if (getSuperclassMethod != null) {
            hasSuperclass = true;
            var types = getSuperclassMethod.getVariable(0).getClassValueNode().getTypes();
            for (var type : types) {
                requirements.computeIfAbsent(type, k -> new ClassInfo()).superclass = true;
            }
        }

        var isAssignableMethod = dependencyInfo.getMethod(IS_ASSIGNABLE_METHOD);
        if (isAssignableMethod != null) {
            hasIsAssignable = true;
            var types = isAssignableMethod.getVariable(0).getClassValueNode().getTypes();
            for (var type : types) {
                requirements.computeIfAbsent(type, k -> new ClassInfo()).isAssignable = true;
            }
        }

        MethodDependencyInfo getDeclaringClassMethod = dependencyInfo.getMethod(GET_DECLARING_CLASS_METHOD);
        if (getDeclaringClassMethod != null) {
            hasDeclaringClass = true;
            var types = getDeclaringClassMethod.getVariable(0).getClassValueNode().getTypes();
            for (var type : types) {
                requirements.computeIfAbsent(type, k -> new ClassInfo()).declaringClass = true;
            }
        }

        MethodDependencyInfo getEnclosingClassMethod = dependencyInfo.getMethod(GET_ENCLOSING_CLASS_METHOD);
        if (getEnclosingClassMethod != null) {
            hasEnclosingClass = true;
            var types = getEnclosingClassMethod.getVariable(0).getClassValueNode().getTypes();
            for (var type : types) {
                requirements.computeIfAbsent(type, k -> new ClassInfo()).enclosingClass = true;
            }
        }

        var newArrayMethod = dependencyInfo.getMethod(NEW_ARRAY);
        if (newArrayMethod != null) {
            hasArrayNewInstance = true;
            var types = newArrayMethod.getVariable(1).getClassValueNode().getTypes();
            for (var type : types) {
                requirements.computeIfAbsent(type, k -> new ClassInfo()).newArray = true;
            }
        }

        var arrayGet = dependencyInfo.getMethod(ARRAY_GET);
        if (arrayGet != null) {
            hasArrayGet = arrayGet.isUsed();
            var types = arrayGet.getVariable(1).getTypes();
            for (var type : types) {
                requirements.computeIfAbsent(type, k -> new ClassInfo()).arrayGet = true;
            }
        }

        var arraySet = dependencyInfo.getMethod(ARRAY_SET);
        if (arraySet != null) {
            hasArraySet = arraySet.isUsed();
            var types = arraySet.getVariable(1).getTypes();
            for (var type : types) {
                requirements.computeIfAbsent(type, k -> new ClassInfo()).arraySet = true;
            }
        }

        var arrayLength = dependencyInfo.getMethod(ARRAY_LENGTH);
        if (arrayLength != null) {
            hasArrayLength = arrayLength.isUsed();
            var types = arrayLength.getVariable(1).getTypes();
            for (var type : types) {
                requirements.computeIfAbsent(type, k -> new ClassInfo()).arrayLength = true;
            }
        }

        var arrayCopy = dependencyInfo.getMethod(ARRAY_COPY);
        if (arrayCopy != null) {
            hasArrayCopy = arrayCopy.isUsed();
            var types = arrayCopy.getVariable(1).getTypes();
            for (var type : types) {
                requirements.computeIfAbsent(type, k -> new ClassInfo()).arrayCopy = true;
            }
        }

        var clone = dependencyInfo.getMethod(new MethodReference(Object.class, "cloneObject", Object.class));
        if (clone != null) {
            var types = clone.getVariable(0).getTypes();
            for (var type : types) {
                requirements.computeIfAbsent(type, k -> new ClassInfo()).cloneMethod = true;
            }
        }

        var enumConstants = Arrays.asList(
            dependencyInfo.getMethod(new MethodReference("org.teavm.platform.Platform", "getEnumConstants",
                    ValueType.object("org.teavm.platform.PlatformClass"), ValueType.parse(Enum[].class))),
            dependencyInfo.getMethod(new MethodReference("org.teavm.classlib.impl.reflection.ClassSupport",
                    "getEnumConstants", ValueType.parse(Class.class), ValueType.parse(Enum[].class)))
        );
        for (var enumConstantsDep : enumConstants) {
            if (enumConstantsDep != null) {
                hasEnumConstants = true;
                var types = enumConstantsDep.getVariable(1).getClassValueNode().getTypes();
                for (var type : types) {
                    requirements.computeIfAbsent(type, k -> new ClassInfo()).enumConstants = true;
                }
            }
        }

        var getAnnotations = dependencyInfo.getMethod(new MethodReference(Class.class, "getDeclaredAnnotations",
                Annotation[].class));
        if (getAnnotations != null && getAnnotations.isUsed()) {
            hasGetAnnotations = true;
            var types = getAnnotations.getVariable(0).getClassValueNode().getTypes();
            for (var type : types) {
                requirements.computeIfAbsent(type, k -> new ClassInfo()).annotations = true;
            }
        }

        var getInterfaces = dependencyInfo.getMethod(new MethodReference(Class.class, "getInterfaces",
                Class[].class));
        if (getInterfaces != null && getInterfaces.isUsed()) {
            hasGetInterfaces = true;
            var types = getInterfaces.getVariable(0).getClassValueNode().getTypes();
            for (var type : types) {
                requirements.computeIfAbsent(type, k -> new ClassInfo()).interfaces = true;
            }
        }

        var getFields = dependencyInfo.getMethod(new MethodReference(Class.class, "getDeclaredFields",
                Field[].class));
        if (getFields != null && getFields.isUsed()) {
            hasGetFields = true;
        }

        var getMethods = dependencyInfo.getMethod(new MethodReference(Class.class, "getDeclaredMethods",
                Method[].class));
        if (getMethods != null && getMethods.isUsed()) {
            hasGetMethods = true;
        }

        var getConstructors = dependencyInfo.getMethod(new MethodReference(Class.class, "getDeclaredConstructors",
                Constructor[].class));
        if (getConstructors != null && getConstructors.isUsed()) {
            hasGetMethods = true;
        }

        var newInstance = dependencyInfo.getMethod(new MethodReference(Class.class, "newInstance", Object.class));
        if (newInstance != null && newInstance.isUsed()) {
            hasNewInstance = true;
        }

        var classInit = dependencyInfo.getMethod(new MethodReference(Class.class, "initialize", void.class));
        if (classInit != null && classInit.isUsed()) {
            hasClassInit = true;
            var types = classInit.getVariable(0).getClassValueNode().getTypes();
            for (var type : types) {
                requirements.computeIfAbsent(type, k -> new ClassInfo()).classInit = true;
            }
        }
    }

    public Info getInfo(String className) {
        return getInfo(ValueType.object(className));
    }

    public Info getInfo(ValueType className) {
        ClassInfo result = requirements.get(className);
        if (result == null) {
            result = EMPTY_INFO;
        }
        return result;
    }

    public boolean hasArrayGet() {
        return hasArrayGet;
    }

    public boolean hasArraySet() {
        return hasArraySet;
    }

    public boolean hasArrayLength() {
        return hasArrayLength;
    }

    public boolean hasArrayCopy() {
        return hasArrayCopy;
    }

    public boolean hasEnumConstants() {
        return hasEnumConstants;
    }

    public boolean hasSuperclass() {
        return hasSuperclass;
    }

    public boolean hasIsAssignable() {
        return hasIsAssignable;
    }

    public boolean hasArrayNewInstance() {
        return hasArrayNewInstance;
    }

    public boolean hasEnclosingClass() {
        return hasEnclosingClass;
    }

    public boolean hasDeclaringClass() {
        return hasDeclaringClass;
    }

    public boolean hasSimpleName() {
        return hasSimpleName;
    }

    public boolean hasName() {
        return hasName;
    }

    public boolean hasGetAnnotations() {
        return hasGetAnnotations;
    }

    public boolean hasGetInterfaces() {
        return hasGetInterfaces;
    }

    public boolean hasGetFields() {
        return hasGetFields;
    }

    public boolean hasGetMethods() {
        return hasGetMethods;
    }

    public boolean hasNewInstance() {
        return hasNewInstance;
    }

    public boolean hasClassInit() {
        return hasClassInit;
    }

    private void addClassesRequiringName(Map<ValueType, ClassInfo> target, ValueType[] source) {
        for (var typeName : source) {
            target.computeIfAbsent(typeName, k -> new ClassInfo()).name = true;
        }
    }

    static class ClassInfo implements Info {
        boolean name;
        boolean simpleName;
        boolean declaringClass;
        boolean enclosingClass;
        boolean superclass;
        boolean isAssignable;
        boolean newArray;
        boolean arrayLength;
        boolean arrayGet;
        boolean arraySet;
        boolean arrayCopy;
        boolean cloneMethod;
        boolean enumConstants;
        boolean annotations;
        boolean interfaces;
        boolean classInit;

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

        @Override
        public boolean superclass() {
            return superclass;
        }

        @Override
        public boolean isAssignable() {
            return isAssignable;
        }

        @Override
        public boolean newArray() {
            return newArray;
        }

        @Override
        public boolean arrayLength() {
            return arrayLength;
        }

        @Override
        public boolean arrayCopy() {
            return arrayCopy;
        }

        @Override
        public boolean arrayGet() {
            return arrayGet;
        }

        @Override
        public boolean arraySet() {
            return arraySet;
        }

        @Override
        public boolean cloneMethod() {
            return cloneMethod;
        }

        @Override
        public boolean enumConstants() {
            return enumConstants;
        }

        @Override
        public boolean annotations() {
            return annotations;
        }

        @Override
        public boolean interfaces() {
            return interfaces;
        }

        @Override
        public boolean classInit() {
            return classInit;
        }
    }

    public interface Info {
        boolean name();

        boolean simpleName();

        boolean declaringClass();

        boolean enclosingClass();

        boolean superclass();

        boolean isAssignable();

        boolean newArray();

        boolean arrayLength();

        boolean arrayGet();

        boolean arraySet();

        boolean arrayCopy();

        boolean cloneMethod();

        boolean enumConstants();

        boolean annotations();

        boolean interfaces();

        boolean classInit();
    }
}
