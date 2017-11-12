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
package org.teavm.model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface ClassReaderSource {
    ClassReader get(String name);

    default Stream<ClassReader> getAncestorClasses(String name) {
        return StreamSupport.stream(((Iterable<ClassReader>) () -> new Iterator<ClassReader>() {
            ClassReader currentClass = get(name);
            @Override public ClassReader next() {
                ClassReader result = currentClass;
                if (currentClass.getParent() != null) {
                    currentClass = get(currentClass.getParent());
                } else {
                    currentClass = null;
                }
                return result;
            }
            @Override public boolean hasNext() {
                return currentClass != null;
            }
        }).spliterator(), false);
    }

    default Stream<ClassReader> getAncestors(String name) {
        return StreamSupport.stream(((Iterable<ClassReader>) () -> new Iterator<ClassReader>() {
            Deque<Deque<ClassReader>> state = new ArrayDeque<>();
            private Set<ClassReader> visited = new HashSet<>();
            {
                state.push(new ArrayDeque<>());
                add(name);
            }
            @Override public ClassReader next() {
                while (!state.isEmpty()) {
                    Deque<ClassReader> level = state.peek();
                    if (!level.isEmpty()) {
                        ClassReader result = level.removeFirst();
                        follow(result);
                        return result;
                    }
                    state.pop();
                }
                return null;
            }
            @Override public boolean hasNext() {
                return !this.state.stream().allMatch(e -> e.isEmpty());
            }
            private void follow(ClassReader cls) {
                state.push(new ArrayDeque<>());
                if (cls.getParent() != null) {
                    add(cls.getParent());
                }
                for (String iface : cls.getInterfaces()) {
                    add(iface);
                }
            }
            private void add(String name) {
                ClassReader cls = get(name);
                if (cls != null && visited.add(cls)) {
                    state.peek().addLast(cls);
                }
            }
        }).spliterator(), false);
    }

    default MethodReader resolve(MethodReference method) {
        return getAncestors(method.getClassName())
                .map(cls -> cls.getMethod(method.getDescriptor()))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    default MethodReader resolveImplementation(MethodReference methodReference) {
        return ClassReaderSourceHelper.resolveMethodImplementation(this, methodReference.getClassName(),
                methodReference.getDescriptor(), new HashSet<>());
    }

    default FieldReader resolve(FieldReference field) {
        return getAncestors(field.getClassName())
                .map(cls -> cls.getField(field.getFieldName()))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    default Stream<MethodReader> overriddenMethods(MethodReference method) {
        return getAncestorClasses(method.getClassName())
                .map(cls -> cls.getMethod(method.getDescriptor()))
                .filter(Objects::nonNull);
    }

    default Optional<Boolean> isSuperType(String superType, String subType) {
        return ClassReaderSourceHelper.isSuperType(this, superType, subType);
    }

    default Optional<Boolean> isSuperType(ValueType superType, ValueType subType) {
        if (superType.equals(subType)) {
            return Optional.of(true);
        }
        if (superType instanceof ValueType.Primitive || subType instanceof ValueType.Primitive) {
            return Optional.of(false);
        }
        if (superType.isObject("java.lang.Object")) {
            return Optional.of(true);
        }
        if (superType instanceof ValueType.Object && subType instanceof ValueType.Object) {
            return isSuperType(((ValueType.Object) superType).getClassName(),
                    ((ValueType.Object) subType).getClassName());
        } else if (superType instanceof ValueType.Array & subType instanceof ValueType.Array) {
            return isSuperType(((ValueType.Array) superType).getItemType(), ((ValueType.Array) subType).getItemType());
        } else {
            return Optional.of(false);
        }
    }
}
