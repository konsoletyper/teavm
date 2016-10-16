/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.classlib.impl;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.classlib.ReflectionContext;
import org.teavm.classlib.ReflectionSupplier;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.FieldDependency;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class ReflectionDependencyListener extends AbstractDependencyListener {
    private List<ReflectionSupplier> reflectionSuppliers;
    private MethodReference fieldGet = new MethodReference(Field.class, "get", Object.class, Object.class);
    private MethodReference fieldSet = new MethodReference(Field.class, "set", Object.class, Object.class, void.class);
    private MethodReference getFields = new MethodReference(Class.class, "getDeclaredFields", Field[].class);
    private boolean fieldGetHandled;
    private boolean fieldSetHandled;
    private Map<String, Set<String>> accessibleFieldCache = new HashMap<>();
    private Set<String> classesWithReflectableFields = new HashSet<>();

    public ReflectionDependencyListener(List<ReflectionSupplier> reflectionSuppliers) {
        this.reflectionSuppliers = reflectionSuppliers;
    }

    public Set<String> getClassesWithReflectableFields() {
        return classesWithReflectableFields;
    }

    public Set<String> getAccessibleFields(String className) {
        return accessibleFieldCache.get(className);
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method, CallLocation location) {
        if (method.getReference().equals(fieldGet)) {
            handleFieldGet(agent, method, location);
        } else if (method.getReference().equals(fieldSet)) {
            handleFieldSet(agent, method, location);
        } else if (method.getReference().equals(getFields)) {
            method.getVariable(0).getClassValueNode().addConsumer(type -> {
                if (!type.getName().startsWith("[")) {
                    classesWithReflectableFields.add(type.getName());
                }
            });
        }
    }

    private void handleFieldGet(DependencyAgent agent, MethodDependency method, CallLocation location) {
        if (fieldGetHandled) {
            return;
        }
        fieldGetHandled = true;

        DependencyNode classValueNode = agent.linkMethod(getFields, location).getVariable(0).getClassValueNode();
        classValueNode.addConsumer(reflectedType -> {
            if (reflectedType.getName().startsWith("[")) {
                return;
            }
            Set<String> accessibleFields = getAccessibleFields(agent, reflectedType.getName());
            ClassReader cls = agent.getClassSource().get(reflectedType.getName());
            for (String fieldName : accessibleFields) {
                FieldReader field = cls.getField(fieldName);
                FieldDependency fieldDep = agent.linkField(field.getReference(), location);
                propagateGet(agent, field.getType(), fieldDep.getValue(), method.getResult(), location);
                linkClassIfNecessary(agent, field, location);
            }
        });
    }

    private void handleFieldSet(DependencyAgent agent, MethodDependency method, CallLocation location) {
        if (fieldSetHandled) {
            return;
        }
        fieldSetHandled = true;

        DependencyNode classValueNode = agent.linkMethod(getFields, location).getVariable(0).getClassValueNode();
        classValueNode.addConsumer(reflectedType -> {
            if (reflectedType.getName().startsWith("[")) {
                return;
            }

            Set<String> accessibleFields = getAccessibleFields(agent, reflectedType.getName());
            ClassReader cls = agent.getClassSource().get(reflectedType.getName());
            for (String fieldName : accessibleFields) {
                FieldReader field = cls.getField(fieldName);
                FieldDependency fieldDep = agent.linkField(field.getReference(), location);
                propagateSet(agent, field.getType(), method.getVariable(2), fieldDep.getValue(), location);
                linkClassIfNecessary(agent, field, location);
            }
        });
    }

    private void linkClassIfNecessary(DependencyAgent agent, FieldReader field, CallLocation location) {
        if (field.hasModifier(ElementModifier.STATIC)) {
            agent.linkClass(field.getOwnerName(), location).initClass(location);
        }
    }

    private Set<String> getAccessibleFields(DependencyAgent agent, String className) {
        return accessibleFieldCache.computeIfAbsent(className, key -> gatherAccessibleFields(agent, key));
    }

    private Set<String> gatherAccessibleFields(DependencyAgent agent, String className) {
        ReflectionContextImpl context = new ReflectionContextImpl(agent);
        Set<String> fields = new HashSet<>();
        for (ReflectionSupplier supplier : reflectionSuppliers) {
            fields.addAll(supplier.getAccessibleFields(context, className));
        }
        return fields;
    }

    private void propagateGet(DependencyAgent agent, ValueType type, DependencyNode sourceNode,
            DependencyNode targetNode, CallLocation location) {
        if (type instanceof ValueType.Primitive) {
            MethodReference boxMethod;
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    boxMethod = new MethodReference(Boolean.class, "valueOf", boolean.class, Boolean.class);
                    break;
                case BYTE:
                    boxMethod = new MethodReference(Byte.class, "valueOf", byte.class, Byte.class);
                    break;
                case SHORT:
                    boxMethod = new MethodReference(Short.class, "valueOf", short.class, Short.class);
                    break;
                case CHARACTER:
                    boxMethod = new MethodReference(Character.class, "valueOf", char.class, Character.class);
                    break;
                case INTEGER:
                    boxMethod = new MethodReference(Integer.class, "valueOf", int.class, Integer.class);
                    break;
                case FLOAT:
                    boxMethod = new MethodReference(Float.class, "valueOf", float.class, Float.class);
                    break;
                case DOUBLE:
                    boxMethod = new MethodReference(Double.class, "valueOf", double.class, Double.class);
                    break;
                default:
                    throw new AssertionError(type.toString());
            }
            MethodDependency boxMethodDep = agent.linkMethod(boxMethod, location);
            boxMethodDep.use();
            boxMethodDep.getResult().connect(targetNode);
        } else if (type instanceof ValueType.Array || type instanceof ValueType.Object) {
            sourceNode.connect(targetNode);
        }
    }

    private void propagateSet(DependencyAgent agent, ValueType type, DependencyNode sourceNode,
            DependencyNode targetNode, CallLocation location) {
        if (type instanceof ValueType.Primitive) {
            MethodReference unboxMethod;
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    unboxMethod = new MethodReference(Boolean.class, "booleanValue", boolean.class);
                    break;
                case BYTE:
                    unboxMethod = new MethodReference(Byte.class, "byteValue", byte.class);
                    break;
                case SHORT:
                    unboxMethod = new MethodReference(Short.class, "shortValue", short.class);
                    break;
                case CHARACTER:
                    unboxMethod = new MethodReference(Character.class, "charValue", char.class);
                    break;
                case INTEGER:
                    unboxMethod = new MethodReference(Integer.class, "intValue", int.class);
                    break;
                case FLOAT:
                    unboxMethod = new MethodReference(Float.class, "floatValue", float.class);
                    break;
                case DOUBLE:
                    unboxMethod = new MethodReference(Double.class, "doubleOf", double.class);
                    break;
                default:
                    throw new AssertionError(type.toString());
            }
            MethodDependency unboxMethodDep = agent.linkMethod(unboxMethod, location);
            unboxMethodDep.use();
            sourceNode.connect(unboxMethodDep.getResult());
        } else if (type instanceof ValueType.Array || type instanceof ValueType.Object) {
            sourceNode.connect(targetNode);
        }
    }

    private static class ReflectionContextImpl implements ReflectionContext {
        private DependencyAgent agent;

        public ReflectionContextImpl(DependencyAgent agent) {
            this.agent = agent;
        }

        @Override
        public ClassLoader getClassLoader() {
            return agent.getClassLoader();
        }

        @Override
        public ClassReaderSource getClassSource() {
            return agent.getClassSource();
        }
    }
}
