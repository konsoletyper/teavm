/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.reflection;

import static org.teavm.reflection.ReflectionMethods.CLASS_GET_TYPE_PARAMS;
import static org.teavm.reflection.ReflectionMethods.EXECUTABLE_GET_GENERIC_PARAMETER_TYPES;
import static org.teavm.reflection.ReflectionMethods.EXECUTABLE_GET_PARAMETER_TYPES;
import static org.teavm.reflection.ReflectionMethods.EXECUTABLE_GET_TYPE_PARAMS;
import static org.teavm.reflection.ReflectionMethods.FIELD_GET_GENERIC_TYPE;
import static org.teavm.reflection.ReflectionMethods.GENERIC_ARRAY_TYPE_IMPL;
import static org.teavm.reflection.ReflectionMethods.METHOD_GET_GENERIC_RETURN_TYPE;
import static org.teavm.reflection.ReflectionMethods.METHOD_GET_RETURN_TYPE;
import static org.teavm.reflection.ReflectionMethods.PARAM_TYPE_IMPL;
import static org.teavm.reflection.ReflectionMethods.TYPE_VAR_GET_BOUNDS;
import static org.teavm.reflection.ReflectionMethods.TYPE_VAR_IMPL_TYPE;
import static org.teavm.reflection.ReflectionMethods.WILDCARD_TYPE_IMPL;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.classlib.ReflectionContext;
import org.teavm.classlib.ReflectionSupplier;
import org.teavm.runtime.reflect.ClassInfo;
import org.teavm.runtime.reflect.FieldInfo;
import org.teavm.runtime.reflect.MethodInfo;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyConsumer;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.DependencyType;
import org.teavm.dependency.FieldDependency;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.AccessLevel;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.GenericValueType;
import org.teavm.model.MemberReader;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class ReflectionDependencyListener extends AbstractDependencyListener {
    private List<ReflectionSupplier> reflectionSuppliers;
    private MethodReference fieldGet = new MethodReference(Field.class, "getWithoutCheck", Object.class, Object.class);
    private MethodReference fieldSet = new MethodReference(Field.class, "setWithoutCheck", Object.class, Object.class,
            void.class);
    private MethodReference newInstance = new MethodReference(Constructor.class, "newInstance", Object[].class,
            Object.class);
    private MethodReference invokeMethod = new MethodReference(Method.class, "invoke", Object.class, Object[].class,
            Object.class);
    private MethodReference getFields = new MethodReference(Class.class, "getDeclaredFields", Field[].class);
    private MethodReference getConstructors = new MethodReference(Class.class, "getDeclaredConstructors",
            Constructor[].class);
    private MethodReference getMethods = new MethodReference(Class.class, "getDeclaredMethods",
            Method[].class);
    private MethodReference getFieldAnnotations = new MethodReference(Field.class, "getDeclaredAnnotations",
            Annotation[].class);
    private MethodReference getExecutableAnnotations = new MethodReference(Executable.class, "getDeclaredAnnotations",
            Annotation[].class);
    private MethodReference getClassAnnotations = new MethodReference(Executable.class, "getDeclaredAnnotations",
            Annotation[].class);
    private MethodReference forName = new MethodReference(Class.class, "forName", String.class, Boolean.class,
            ClassLoader.class, Class.class);
    private MethodReference classNewInstance = new MethodReference(Class.class, "newInstance", Object.class);
    private MethodReference forNameShort = new MethodReference(Class.class, "forName", String.class, Class.class);
    private MethodReference fieldGetType = new MethodReference(Field.class, "getType", Class.class);
    private MethodReference fieldGetName = new MethodReference(Field.class, "getName", String.class);

    private Map<String, Set<String>> accessibleFieldCache = new LinkedHashMap<>();
    private Map<String, Set<MethodDescriptor>> accessibleMethodCache = new LinkedHashMap<>();
    private Set<String> classesWithReflectableFields = new LinkedHashSet<>();
    private Set<String> classesWithReflectableMethods = new LinkedHashSet<>();
    private DependencyNode allClasses;
    private DependencyNode typesInReflectableSignaturesNode;
    private DependencyNode typesInGenericReflectableSignaturesNode;
    private Set<MethodReference> virtualMethods = new HashSet<>();
    private Set<MethodReference> virtualCallSites = new HashSet<>();
    private Set<String> classesFoundByName = new HashSet<>();
    private Set<MethodReference> calledMethods = new HashSet<>();
    private Set<FieldReference> readFields = new HashSet<>();
    private Set<FieldReference> writtenFields = new HashSet<>();
    private List<FieldReader> fieldsReadViaReflection = new ArrayList<>();
    private List<MethodReader> methodsReadViaReflection = new ArrayList<>();
    private DependencyNode fieldsAnnotationsConsumer;
    private DependencyNode methodsAnnotationsConsumer;

    private boolean getReached;
    private boolean setReached;
    private boolean callReached;
    private AnnotationGenerationHelper annotHelper;
    private boolean withGenerics;

    public ReflectionDependencyListener(List<ReflectionSupplier> reflectionSuppliers,
            AnnotationGenerationHelper annotHelper) {
        this.reflectionSuppliers = reflectionSuppliers;
        this.annotHelper = annotHelper;
    }

    public boolean isVirtual(MethodReference methodRef) {
        return virtualMethods.contains(methodRef);
    }

    public Collection<MethodReference> getVirtualCallSites() {
        return virtualCallSites;
    }

    public boolean isCalled(MethodReference methodRef) {
        return calledMethods.contains(methodRef);
    }

    public boolean isRead(FieldReference fieldRef) {
        return readFields.contains(fieldRef);
    }

    public boolean isWritten(FieldReference fieldRef) {
        return writtenFields.contains(fieldRef);
    }

    @Override
    public void started(DependencyAgent agent) {
        allClasses = agent.createNode();
        typesInReflectableSignaturesNode = agent.createNode();
        typesInGenericReflectableSignaturesNode = agent.createNode();

        var constructorParamTypes = agent.linkField(new FieldReference(Constructor.class.getName(), "parameterTypes"))
                .getValue();
        constructorParamTypes.getArrayItem().propagate(agent.getType(ValueType.object("java.lang.Class")));
        typesInReflectableSignaturesNode.connect(constructorParamTypes.getArrayItem().getClassValueNode());

        var methodParamTypes = agent.linkField(new FieldReference(Method.class.getName(), "parameterTypes"))
                .getValue();
        methodParamTypes.getArrayItem().propagate(agent.getType(ValueType.object("java.lang.Class")));
        typesInReflectableSignaturesNode.connect(methodParamTypes.getArrayItem().getClassValueNode());

        agent.linkMethod(new MethodReference(ClassInfo.class, "classObject", Class.class))
                .getResult().propagate(agent.getType(ValueType.parse(Class.class)));

        var context = new ReflectionContextImpl(agent);
        for (var reflectionSupplier : reflectionSuppliers) {
            for (var className : reflectionSupplier.getClassesFoundByName(context)) {
                if (classesFoundByName.add(className)) {
                    agent.linkClass(className);
                }
            }
        }
        allClasses.addConsumer(type -> {
            if (type.getValueType() instanceof ValueType.Object) {
                var className = ((ValueType.Object) type.getValueType()).getClassName();
                if (reflectionSuppliers.stream().anyMatch(s -> s.isClassFoundByName(context, className))) {
                    if (classesFoundByName.add(className)) {
                        agent.linkClass(className);
                    }
                }
            }
        });
        if (!classesFoundByName.isEmpty()) {
            var getName = agent.linkMethod(new MethodReference(Class.class, "getName", String.class));
            getName.getVariable(0).propagate(agent.getType(ValueType.object("java.lang.Class")));
            for (var className : classesFoundByName) {
                getName.getVariable(0).getClassValueNode().propagate(agent.getType(ValueType.object(className)));
            }
        }
    }

    public boolean isGetReached() {
        return getReached;
    }

    public boolean isSetReached() {
        return setReached;
    }

    public boolean isCallReached() {
        return callReached;
    }

    public Set<String> getClassesWithReflectableFields() {
        return classesWithReflectableFields;
    }

    public Set<String> getClassesWithReflectableMethods() {
        return classesWithReflectableMethods;
    }

    public Set<String> getAccessibleFields(String className) {
        return accessibleFieldCache.get(className);
    }

    public Set<MethodDescriptor> getAccessibleMethods(String className) {
        return accessibleMethodCache.get(className);
    }

    @Override
    public void classReached(DependencyAgent agent, String className) {
        allClasses.propagate(agent.getType(ValueType.object(className)));
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        if (method.getReference().equals(fieldGet)) {
            handleFieldGet(agent, method);
        } else if (method.getReference().equals(fieldSet)) {
            handleFieldSet(agent, method);
        } else if (method.getReference().equals(newInstance)) {
            handleNewInstance(agent, method);
        } else if (method.getReference().equals(classNewInstance)) {
            handleClassNewInstance(agent, method);
        } else if (method.getReference().equals(invokeMethod)) {
            handleInvoke(agent, method);
        } else if (method.getReference().equals(getFields)) {
            method.getVariable(0).getClassValueNode().addConsumer(type -> {
                if (type.getValueType() instanceof ValueType.Object) {
                    var className = ((ValueType.Object) type.getValueType()).getClassName();
                    classesWithReflectableFields.add(className);

                    ClassReader cls = agent.getClassSource().get(className);
                    if (cls != null) {
                        var skipPrivates = shouldSkipPrivates(cls);
                        var reflectable = getAccessibleFields(agent, className);
                        for (FieldReader field : cls.getFields()) {
                            if (!reflectable.contains(field.getName())) {
                                continue;
                            }
                            if (skipPrivates) {
                                if (field.getLevel() == AccessLevel.PRIVATE
                                        || field.getLevel() == AccessLevel.PACKAGE_PRIVATE) {
                                    continue;
                                }
                            }
                            linkType(agent, field.getType());
                            agent.linkField(field.getReference());
                            if (field.getGenericType() != null) {
                                linkGenericType(agent, field.getGenericType());
                            }
                            fieldsReadViaReflection.add(field);
                            if (fieldsAnnotationsConsumer != null) {
                                annotHelper.propagateAnnotationImplementations(agent, field.getAnnotations().all(),
                                        fieldsAnnotationsConsumer);
                            }
                        }
                    }
                }
            });
        } else if (method.getReference().equals(getFieldAnnotations)) {
            fieldsAnnotationsConsumer = method.getResult().getArrayItem();
            method.getResult().propagate(agent.getType(ValueType.parse(Annotation[].class)));
            for (var field : fieldsReadViaReflection) {
                annotHelper.propagateAnnotationImplementations(agent, field.getAnnotations().all(),
                        fieldsAnnotationsConsumer);
            }
        } else if (method.getReference().equals(getConstructors) || method.getReference().equals(getMethods)) {
            method.getVariable(0).getClassValueNode().addConsumer(type -> {
                if (type.getValueType() instanceof ValueType.Object) {
                    var className = ((ValueType.Object) type.getValueType()).getClassName();
                    classesWithReflectableMethods.add(className);

                    ClassReader cls = agent.getClassSource().get(className);
                    if (cls != null) {
                        var skipPrivates = shouldSkipPrivates(cls);
                        var reflectable = getAccessibleMethods(agent, className);
                        for (MethodReader reflectableMethod : cls.getMethods()) {
                            if (!reflectable.contains(reflectableMethod.getDescriptor())) {
                                continue;
                            }
                            if (skipPrivates) {
                                if (reflectableMethod.getLevel() == AccessLevel.PRIVATE
                                        || reflectableMethod.getLevel() == AccessLevel.PACKAGE_PRIVATE) {
                                    continue;
                                }
                            }
                            linkType(agent, reflectableMethod.getResultType());
                            for (ValueType param : reflectableMethod.getParameterTypes()) {
                                linkType(agent, param);
                            }
                            if (reflectableMethod.getGenericResultType() != null) {
                                linkGenericType(agent, reflectableMethod.getGenericResultType());
                            }
                            var genericParamTypes = reflectableMethod.getGenericParameterTypes();
                            if (genericParamTypes != null) {
                                for (var param : genericParamTypes) {
                                    linkGenericType(agent, param);
                                }
                            }
                            if (methodsAnnotationsConsumer != null) {
                                annotHelper.propagateAnnotationImplementations(agent,
                                        reflectableMethod.getAnnotations().all(),
                                        methodsAnnotationsConsumer);
                            }
                            methodsReadViaReflection.add(reflectableMethod);
                        }
                    }
                }
            });
        } else if (method.getReference().equals(getExecutableAnnotations)) {
            methodsAnnotationsConsumer = method.getResult().getArrayItem();
            method.getResult().propagate(agent.getType(ValueType.parse(Annotation[].class)));
            for (var reflectableMethod : methodsReadViaReflection) {
                annotHelper.propagateAnnotationImplementations(agent, reflectableMethod.getAnnotations().all(),
                        methodsAnnotationsConsumer);
            }
        } else if (method.getReference().equals(getClassAnnotations)) {
            method.getVariable(0).getClassValueNode().addConsumer(type -> {
                if (type.getValueType() instanceof ValueType.Object) {
                    var cls = agent.getClassSource().get(((ValueType.Object) type.getValueType()).getClassName());
                    annotHelper.propagateAnnotationImplementations(agent, cls.getAnnotations().all(),
                            method.getResult().getArrayItem());
                }
            });
        } else if (method.getReference().equals(forName) || method.getReference().equals(forNameShort)) {
            method.getResult().propagate(agent.getType(ValueType.object("java.lang.Class")));
            for (var className : classesFoundByName) {
                method.getResult().getClassValueNode().propagate(agent.getType(ValueType.object(className)));
            }
        } else if (method.getReference().equals(fieldGetType)
                || method.getReference().equals(METHOD_GET_RETURN_TYPE)) {
            method.getResult().propagate(agent.getType(ValueType.object("java.lang.Class")));
            typesInReflectableSignaturesNode.connect(method.getResult().getClassValueNode());
        } else if (method.getReference().equals(FIELD_GET_GENERIC_TYPE)
                || method.getReference().equals(METHOD_GET_GENERIC_RETURN_TYPE)) {
            linkGenerics(agent);
            propagateGenerics(agent, method.getResult());
        } else if (method.getReference().equals(fieldGetName)) {
            method.getResult().propagate(agent.getType(ValueType.object("java.lang.String")));
        } else if (method.getReference().equals(EXECUTABLE_GET_PARAMETER_TYPES)) {
            method.getResult().propagate(agent.getType(ValueType.arrayOf(ValueType.object("java.lang.Class"))));
            method.getResult().getArrayItem().propagate(agent.getType(ValueType.object("java.lang.Class")));
            typesInReflectableSignaturesNode.connect(method.getResult().getArrayItem().getClassValueNode());
        } else if (method.getReference().equals(EXECUTABLE_GET_GENERIC_PARAMETER_TYPES)) {
            linkGenerics(agent);
            propagateGenerics(agent, method.getResult().getArrayItem());
        } else if (method.getReference().equals(CLASS_GET_TYPE_PARAMS)
                || method.getReference().equals(EXECUTABLE_GET_TYPE_PARAMS)) {
            linkGenerics(agent);
            method.getResult().getArrayItem().propagate(agent.getType(TYPE_VAR_IMPL_TYPE));
        } else if (method.getReference().equals(TYPE_VAR_GET_BOUNDS)) {
            linkGenerics(agent);
            method.getResult().propagate(agent.getType(ValueType.parse(Type[].class)));
            propagateGenerics(agent, method.getResult().getArrayItem());
        }
    }

    private void linkGenerics(DependencyAgent agent) {
        if (withGenerics) {
            return;
        }
        withGenerics = true;

        for (var className : agent.getReachableClasses()) {
            linkTypeParameterBounds(agent, className);
        }
        allClasses.addConsumer(type -> {
            var valueType = type.getValueType();
            if (valueType instanceof ValueType.Object) {
                linkTypeParameterBounds(agent, ((ValueType.Object) valueType).getClassName());
            }
        });

        typesInGenericReflectableSignaturesNode.addConsumer(refType -> {
            linkClass(agent, refType.getValueType());
        });
    }

    private void linkTypeParameterBounds(DependencyAgent agent, String className) {
        var cls = agent.getClassSource().get(className);
        if (cls != null) {
            return;
        }
        var params = cls.getGenericParameters();
        if (params != null) {
            for (var param : params) {
                if (param.getClassBound() != null) {
                    linkGenericType(agent, param.getClassBound());
                }
                for (var bound : param.getInterfaceBounds()) {
                    linkGenericType(agent, bound);
                }
            }
        }
    }

    private void propagateGenerics(DependencyAgent agent, DependencyNode target) {
        target.propagate(agent.getType(ValueType.object("java.lang.Class")));
        target.propagate(agent.getType(TYPE_VAR_IMPL_TYPE));
        target.propagate(agent.getType(ValueType.object(PARAM_TYPE_IMPL)));
        target.propagate(agent.getType(ValueType.object(WILDCARD_TYPE_IMPL)));
        target.propagate(agent.getType(ValueType.object(GENERIC_ARRAY_TYPE_IMPL)));
        typesInReflectableSignaturesNode.connect(target.getClassValueNode());
        typesInGenericReflectableSignaturesNode.connect(target.getClassValueNode());
    }

    public static boolean shouldSkipPrivates(ClassReader cls) {
        return cls.getName().equals("java.lang.Object") || cls.getName().equals("java.lang.Class");
    }

    private void handleFieldGet(DependencyAgent agent, MethodDependency method) {
        CallLocation location = new CallLocation(method.getReference());
        DependencyNode classValueNode = agent.linkMethod(getFields)
                .addLocation(location)
                .getVariable(0).getClassValueNode();
        getReached = true;
        classValueNode.addConsumer(reflectedType -> {
            if (!(reflectedType.getValueType() instanceof ValueType.Object)) {
                return;
            }
            var className = ((ValueType.Object) reflectedType.getValueType()).getClassName();
            Set<String> accessibleFields = getAccessibleFields(agent, className);
            ClassReader cls = agent.getClassSource().get(className);
            for (String fieldName : accessibleFields) {
                FieldReader field = cls.getField(fieldName);
                FieldDependency fieldDep = agent.linkField(field.getReference())
                        .addLocation(location);
                if (field.hasModifier(ElementModifier.STATIC)) {
                    readFields.add(field.getReference());
                    propagateGet(agent, field.getType(), fieldDep.getValue(), method.getResult(), location);
                } else {
                    method.getVariable(1).addConsumer(new InstanceGetConsumer(agent, field.getReference(),
                            method, location));
                }
                linkClassIfNecessary(agent, field, location);
            }
        });
    }

    private void handleFieldSet(DependencyAgent agent, MethodDependency method) {
        CallLocation location = new CallLocation(method.getReference());
        DependencyNode classValueNode = agent.linkMethod(getFields)
                .addLocation(location)
                .getVariable(0).getClassValueNode();
        setReached = true;
        classValueNode.addConsumer(reflectedType -> {
            if (!(reflectedType.getValueType() instanceof ValueType.Object)) {
                return;
            }

            var className = ((ValueType.Object) reflectedType.getValueType()).getClassName();
            Set<String> accessibleFields = getAccessibleFields(agent, className);
            ClassReader cls = agent.getClassSource().get(className);
            for (String fieldName : accessibleFields) {
                FieldReader field = cls.getField(fieldName);
                FieldDependency fieldDep = agent.linkField(field.getReference()).addLocation(location);
                if (field.hasModifier(ElementModifier.STATIC)) {
                    writtenFields.add(field.getReference());
                    propagateSet(agent, field.getType(), method.getVariable(2), fieldDep.getValue(), location);
                } else {
                    method.getVariable(1).addConsumer(new InstanceSetConsumer(agent, field.getReference(),
                            method, location));
                }
                linkClassIfNecessary(agent, field, location);
            }
        });
    }

    private void handleNewInstance(DependencyAgent agent, MethodDependency method) {
        CallLocation location = new CallLocation(method.getReference());

        DependencyNode classValueNode = agent.linkMethod(getConstructors)
                .addLocation(location)
                .getVariable(0).getClassValueNode();
        callReached = true;
        classValueNode.addConsumer(reflectedType -> {
            if (!(reflectedType.getValueType() instanceof ValueType.Object)) {
                return;
            }

            var className = ((ValueType.Object) reflectedType.getValueType()).getClassName();
            ClassReader cls = agent.getClassSource().get(className);
            if (cls == null || cls.hasModifier(ElementModifier.ABSTRACT)
                    || cls.hasModifier(ElementModifier.INTERFACE)) {
                return;
            }

            Set<MethodDescriptor> accessibleMethods = getAccessibleMethods(agent, className);

            var hasConstructors = false;
            for (MethodDescriptor methodDescriptor : accessibleMethods) {
                if (!methodDescriptor.getName().equals("<init>")) {
                    continue;
                }
                MethodReader calledMethod = cls.getMethod(methodDescriptor);
                MethodDependency calledMethodDep = agent.linkMethod(calledMethod.getReference()).addLocation(location);
                calledMethodDep.use();
                for (int i = 0; i < calledMethod.parameterCount(); ++i) {
                    propagateSet(agent, methodDescriptor.parameterType(i), method.getVariable(1).getArrayItem(),
                            calledMethodDep.getVariable(i + 1), location);
                }
                calledMethodDep.getVariable(0).propagate(reflectedType);
                calledMethods.add(calledMethod.getReference());
                hasConstructors = true;
            }

            if (hasConstructors) {
                method.getResult().propagate(reflectedType);
            }
        });
    }

    private void handleClassNewInstance(DependencyAgent agent, MethodDependency method) {
        var location = new CallLocation(method.getReference());

        var classValueNode = method.getVariable(0).getClassValueNode();
        classValueNode.addConsumer(reflectedType -> {
            if (!(reflectedType.getValueType() instanceof ValueType.Object)) {
                return;
            }

            var className = ((ValueType.Object) reflectedType.getValueType()).getClassName();
            ClassReader cls = agent.getClassSource().get(className);
            if (cls == null || cls.hasModifier(ElementModifier.ABSTRACT)
                    || cls.hasModifier(ElementModifier.INTERFACE)) {
                return;
            }

            var constructor = cls.getMethod(new MethodDescriptor("<init>", void.class));
            if (constructor == null || constructor.getProgram() == null) {
                return;
            }
            var constructorDep = agent.linkMethod(constructor.getReference()).addLocation(location);
            constructorDep.getVariable(0).propagate(reflectedType);
            constructorDep.use();

            method.getResult().propagate(reflectedType);
        });
    }

    private void handleInvoke(DependencyAgent agent, MethodDependency method) {
        CallLocation location = new CallLocation(method.getReference());
        DependencyNode classValueNode = agent.linkMethod(getMethods)
                .addLocation(location)
                .getVariable(0).getClassValueNode();
        callReached = true;
        classValueNode.addConsumer(reflectedType -> {
            if (!(reflectedType.getValueType() instanceof ValueType.Object)) {
                return;
            }

            var className = ((ValueType.Object) reflectedType.getValueType()).getClassName();
            Set<MethodDescriptor> accessibleMethods = getAccessibleMethods(agent, className);
            ClassReader cls = agent.getClassSource().get(className);
            for (MethodDescriptor methodDescriptor : accessibleMethods) {
                if (methodDescriptor.getName().equals("<init>")) {
                    continue;
                }
                MethodReader calledMethod = cls.getMethod(methodDescriptor);
                if (calledMethod.hasModifier(ElementModifier.STATIC)) {
                    var calledMethodDep = agent.linkMethod(calledMethod.getReference()).addLocation(location);
                    calledMethodDep.use();
                    for (int i = 0; i < calledMethod.parameterCount(); ++i) {
                        propagateSet(agent, methodDescriptor.parameterType(i), method.getVariable(2).getArrayItem(),
                                calledMethodDep.getVariable(i + 1), location);
                    }
                    propagateSet(agent, ValueType.object(className), method.getVariable(1),
                            calledMethodDep.getVariable(0), location);
                    propagateGet(agent, calledMethod.getResultType(), calledMethodDep.getResult(),
                            method.getResult(), location);
                    linkClassIfNecessary(agent, calledMethod, location);
                    calledMethods.add(calledMethod.getReference());
                } else {
                    var virtual = !calledMethod.hasModifier(ElementModifier.FINAL)
                            && calledMethod.getLevel() != AccessLevel.PRIVATE;
                    method.getVariable(1).addConsumer(new InstanceCallConsumer(agent, calledMethod.getReference(),
                            method, location, virtual));
                }
            }
        });
    }

    private class InstanceCallConsumer implements DependencyConsumer {
        private final DependencyAgent agent;
        private final MethodReference methodRef;
        private final MethodDependency invokeDep;
        private final CallLocation location;
        private final boolean virtual;

        private Set<DependencyType> knownTypes = new HashSet<>();
        private Set<MethodReference> knownMethods = new HashSet<>();

        InstanceCallConsumer(DependencyAgent agent, MethodReference methodRef, MethodDependency invokeDep,
                CallLocation location, boolean virtual) {
            this.agent = agent;
            this.methodRef = methodRef;
            this.invokeDep = invokeDep;
            this.location = location;
            this.virtual = virtual;
        }

        @Override
        public void consume(DependencyType type) {
            if (!knownTypes.add(type)) {
                return;
            }

            var valueType = type.getValueType();
            String className;
            if (valueType instanceof ValueType.Object) {
                className = ((ValueType.Object) valueType).getClassName();
            } else if (valueType instanceof ValueType.Array) {
                className = "java.lang.Object";
            } else {
                return;
            }

            if (!agent.getClassHierarchy().isSuperType(methodRef.getClassName(), className, false)) {
                return;
            }

            var methodDep = virtual
                    ? agent.linkMethod(className, methodRef.getDescriptor())
                    : agent.linkMethod(methodRef);
            methodDep.addLocation(location);
            if (!knownMethods.add(methodDep.getReference())) {
                return;
            }

            if (!methodDep.isMissing()) {
                virtualCallSites.add(methodRef);
                calledMethods.add(methodRef);
                methodDep.use();
                virtualMethods.add(methodDep.getReference());
                invokeDep.getVariable(1).connect(methodDep.getVariable(0));
                for (var i = 0; i < methodRef.parameterCount(); ++i) {
                    propagateSet(agent, methodRef.parameterType(i), invokeDep.getVariable(2).getArrayItem(),
                            methodDep.getVariable(i + 1), location);
                }
                if (methodDep.getResult() != null) {
                    propagateGet(agent, methodRef.getReturnType(), methodDep.getResult(),
                            invokeDep.getResult(), location);
                }
            }
        }
    }

    private class InstanceGetConsumer implements DependencyConsumer {
        private final DependencyAgent agent;
        private final FieldReference fieldRef;
        private final MethodDependency readDep;
        private final CallLocation location;

        private Set<DependencyType> knownTypes = new HashSet<>();
        private boolean done;

        InstanceGetConsumer(DependencyAgent agent, FieldReference fieldRef, MethodDependency readDep,
                CallLocation location) {
            this.agent = agent;
            this.fieldRef = fieldRef;
            this.readDep = readDep;
            this.location = location;
        }

        @Override
        public void consume(DependencyType type) {
            if (done || !knownTypes.add(type)) {
                return;
            }

            var valueType = type.getValueType();
            String className;
            if (valueType instanceof ValueType.Object) {
                className = ((ValueType.Object) valueType).getClassName();
            } else if (valueType instanceof ValueType.Array) {
                className = "java.lang.Object";
            } else {
                return;
            }

            if (!agent.getClassHierarchy().isSuperType(fieldRef.getClassName(), className, false)) {
                return;
            }

            var fieldDep = agent.linkField(fieldRef);
            done = true;

            if (!fieldDep.isMissing()) {
                readFields.add(fieldRef);
                propagateGet(agent, fieldDep.getField().getType(), fieldDep.getValue(), readDep.getResult(), location);
            }
        }
    }

    private class InstanceSetConsumer implements DependencyConsumer {
        private final DependencyAgent agent;
        private final FieldReference fieldRef;
        private final MethodDependency readDep;
        private final CallLocation location;

        private Set<DependencyType> knownTypes = new HashSet<>();
        private boolean done;

        InstanceSetConsumer(DependencyAgent agent, FieldReference fieldRef, MethodDependency readDep,
                CallLocation location) {
            this.agent = agent;
            this.fieldRef = fieldRef;
            this.readDep = readDep;
            this.location = location;
        }

        @Override
        public void consume(DependencyType type) {
            if (done || !knownTypes.add(type)) {
                return;
            }

            var valueType = type.getValueType();
            String className;
            if (valueType instanceof ValueType.Object) {
                className = ((ValueType.Object) valueType).getClassName();
            } else if (valueType instanceof ValueType.Array) {
                className = "java.lang.Object";
            } else {
                return;
            }

            if (!agent.getClassHierarchy().isSuperType(fieldRef.getClassName(), className, false)) {
                return;
            }

            var fieldDep = agent.linkField(fieldRef);
            done = true;

            if (!fieldDep.isMissing()) {
                writtenFields.add(fieldRef);
                propagateSet(agent, fieldDep.getField().getType(), readDep.getVariable(2), fieldDep.getValue(),
                        location);
            }
        }
    }

    private void linkType(DependencyAgent agent, ValueType type) {
        typesInReflectableSignaturesNode.propagate(agent.getType(type));
        linkClass(agent, type);
    }

    private void linkClass(DependencyAgent agent, ValueType type) {
        if (type instanceof ValueType.Object) {
            agent.linkClass(((ValueType.Object) type).getClassName());
        } else if (type instanceof ValueType.Array) {
            linkClass(agent, ((ValueType.Array) type).getItemType());
        }
    }

    private void linkGenericType(DependencyAgent agent, GenericValueType type) {
        var simpleType = type.asValueType();
        if (simpleType != null) {
            return;
        }
        if (type instanceof GenericValueType.Array) {
            linkGenericType(agent, ((GenericValueType.Array) type).getItemType());
        } else if (type instanceof GenericValueType.Object) {
            var objType = (GenericValueType.Object) type;
            typesInGenericReflectableSignaturesNode.propagate(agent.getType(
                    ValueType.object(objType.getFullClassName())));
            for (var arg : objType.getArguments()) {
                if (arg.getValue() != null) {
                    linkGenericType(agent, arg.getValue());
                }
            }
            if (objType.getParent() != null) {
                linkGenericType(agent, objType.getParent());
            }
        }
    }

    private void linkClassIfNecessary(DependencyAgent agent, MemberReader member, CallLocation location) {
        if (member.hasModifier(ElementModifier.STATIC)) {
            agent.linkClass(member.getOwnerName()).initClass(location);
        }
    }

    private Set<String> getAccessibleFields(DependencyAgent agent, String className) {
        return accessibleFieldCache.computeIfAbsent(className, key -> gatherAccessibleFields(agent, key));
    }

    private Set<MethodDescriptor> getAccessibleMethods(DependencyAgent agent, String className) {
        return accessibleMethodCache.computeIfAbsent(className, key -> gatherAccessibleMethods(agent, key));
    }

    private Set<String> gatherAccessibleFields(DependencyAgent agent, String className) {
        ReflectionContextImpl context = new ReflectionContextImpl(agent);
        Set<String> fields = new LinkedHashSet<>();
        for (ReflectionSupplier supplier : reflectionSuppliers) {
            fields.addAll(supplier.getAccessibleFields(context, className));
        }
        return fields;
    }

    private Set<MethodDescriptor> gatherAccessibleMethods(DependencyAgent agent, String className) {
        ReflectionContextImpl context = new ReflectionContextImpl(agent);
        Set<MethodDescriptor> methods = new LinkedHashSet<>();
        for (ReflectionSupplier supplier : reflectionSuppliers) {
            methods.addAll(supplier.getAccessibleMethods(context, className));
        }
        return methods;
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
                case LONG:
                    boxMethod = new MethodReference(Long.class, "valueOf", long.class, Long.class);
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
            MethodDependency boxMethodDep = agent.linkMethod(boxMethod).addLocation(location);
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
                case LONG:
                    unboxMethod = new MethodReference(Long.class, "longValue", long.class);
                    break;
                case FLOAT:
                    unboxMethod = new MethodReference(Float.class, "floatValue", float.class);
                    break;
                case DOUBLE:
                    unboxMethod = new MethodReference(Double.class, "doubleValue", double.class);
                    break;
                default:
                    throw new AssertionError(type.toString());
            }
            MethodDependency unboxMethodDep = agent.linkMethod(unboxMethod).addLocation(location);
            unboxMethodDep.propagateClass(0, unboxMethod.getClassName());
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
