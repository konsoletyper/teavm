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

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyNode;
import org.teavm.interop.Unmanaged;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.runtime.StringInfo;
import org.teavm.runtime.reflect.AnnotationConstructor;
import org.teavm.runtime.reflect.AnnotationData;
import org.teavm.runtime.reflect.AnnotationInfoUtil;
import org.teavm.runtime.reflect.AnnotationValueArray;
import org.teavm.runtime.reflect.ClassInfo;
import org.teavm.runtime.reflect.ClassInfoUtil;
import org.teavm.runtime.reflect.DerivedClassInfo;

public class AnnotationGenerationHelper {
    public static final String ANNOTATION_IMPLEMENTOR_SUFFIX = "$$_impl";
    public static final String ANNOTATION_DATA_SUFFIX = "$$_nativeData";
    private Set<String> annotationImplementors = new HashSet<>();

    public final String getAnnotationImplementor(DependencyAgent agent, String annotationType) {
        String implementorName = annotationType + ANNOTATION_IMPLEMENTOR_SUFFIX;
        if (annotationImplementors.add(implementorName)) {
            createImplementor(agent, annotationType, implementorName);
        }
        return implementorName;
    }

    private void createImplementor(DependencyAgent agent, String annotationType, String implementorName) {
        var hierarchy = agent.getClassHierarchy();
        annotationImplementors.add(implementorName);
        ClassHolder implementor = new ClassHolder(implementorName);
        implementor.setParent("java.lang.Object");
        implementor.getInterfaces().add(annotationType);
        implementor.getModifiers().add(ElementModifier.FINAL);
        implementor.setLevel(AccessLevel.PUBLIC);

        ClassReader annotation = hierarchy.getClassSource().get(annotationType);
        if (annotation == null) {
            return;
        }

        var dataCls = new ClassHolder(annotationType + ANNOTATION_DATA_SUFFIX);
        dataCls.setParent(AnnotationData.class.getName());
        dataCls.getModifiers().add(ElementModifier.FINAL);
        dataCls.setLevel(AccessLevel.PUBLIC);
        dataCls.getAnnotations().add(new AnnotationHolder(Unmanaged.class.getName()));

        var infoField = new FieldHolder("info");
        infoField.setType(ValueType.object(dataCls.getName()));
        infoField.setLevel(AccessLevel.PRIVATE);
        implementor.addField(infoField);

        var dataCtorMethod = new MethodHolder("constructor", ValueType.object(AnnotationConstructor.class.getName()));
        dataCtorMethod.setLevel(AccessLevel.PUBLIC);
        dataCtorMethod.getModifiers().addAll(List.of(ElementModifier.STATIC, ElementModifier.NATIVE));
        dataCls.addMethod(dataCtorMethod);

        var ctor = new MethodHolder("<init>", infoField.getType(), ValueType.VOID);
        ctor.setLevel(AccessLevel.PUBLIC);
        var pe = ProgramEmitter.create(ctor, hierarchy);
        var thisVar = pe.var(0, implementor);
        var infoVar = pe.var(1, infoField.getType());
        thisVar.setField(infoField.getName(), infoVar);
        pe.exit();
        implementor.addMethod(ctor);

        var factory = new MethodHolder("create", infoField.getType(), ValueType.object(implementorName));
        factory.setLevel(AccessLevel.PUBLIC);
        factory.getModifiers().add(ElementModifier.STATIC);
        var factoryPe = ProgramEmitter.create(factory, hierarchy);
        factoryPe.construct(implementorName, factoryPe.var(1, infoField.getType())).returnValue();
        implementor.addMethod(factory);

        var clinit = new MethodHolder("<clinit>", ValueType.VOID);
        clinit.setLevel(AccessLevel.PUBLIC);
        clinit.getModifiers().add(ElementModifier.STATIC);
        var clinitPe = ProgramEmitter.create(clinit, hierarchy);
        var needsClinit = false;

        var postSubmitActions = new ArrayList<Runnable>();
        for (var methodDecl : annotation.getMethods()) {
            if (methodDecl.hasModifier(ElementModifier.STATIC)) {
                continue;
            }
            var type = methodDecl.getResultType();

            var accessor = new MethodHolder(methodDecl.getDescriptor());
            accessor.setLevel(AccessLevel.PUBLIC);
            pe = ProgramEmitter.create(accessor, hierarchy);
            var thisVal = pe.var(0, implementor);
            var info = thisVal.getField(infoField.getName(), infoField.getType());
            implementor.addMethod(accessor);

            String className = null;
            var implementorType = methodDecl.getResultType();
            if (type instanceof ValueType.Array) {
                implementorType = ValueType.parse(AnnotationValueArray.class);
                var itemType = ((ValueType.Array) type).getItemType();
                if (itemType instanceof ValueType.Object) {
                    className = ((ValueType.Object) itemType).getClassName();
                }
            } else if (type.isObject(String.class)) {
                implementorType = ValueType.parse(StringInfo.class);
            } else if (methodDecl.getResultType().isObject(Class.class)) {
                implementorType = ValueType.parse(DerivedClassInfo.class);
            } else if (methodDecl.getResultType() instanceof ValueType.Object) {
                className = ((ValueType.Object) methodDecl.getResultType()).getClassName();
            }

            FieldHolder enumCacheField = null;
            var isEnum = false;
            var isAnnotation = false;
            if (className != null) {
                var implementorCls = hierarchy.getClassSource().get(className);
                if (implementorCls != null) {
                    if (implementorCls.hasModifier(ElementModifier.ENUM)) {
                        isEnum = true;
                        needsClinit = true;
                        enumCacheField = new FieldHolder(methodDecl.getName() + "_$enumCache");
                        enumCacheField.setLevel(AccessLevel.PRIVATE);
                        enumCacheField.getModifiers().add(ElementModifier.STATIC);
                        enumCacheField.setType(ValueType.arrayOf(ValueType.object(className)));
                        implementor.addField(enumCacheField);
                        clinitPe.setField(enumCacheField.getReference(), clinitPe.invoke(className, "values",
                                enumCacheField.getType()));
                    } else if (implementorCls.hasModifier(ElementModifier.ANNOTATION)) {
                        isAnnotation = true;
                    }
                }
            }
            if (!(type instanceof ValueType.Array)) {
                if (isAnnotation) {
                    implementorType = ValueType.object(className + ANNOTATION_DATA_SUFFIX);
                } else if (isEnum) {
                    implementorType = ValueType.SHORT;
                }
            }

            var dataMethod = new MethodHolder(methodDecl.getName(), implementorType);
            dataMethod.setLevel(AccessLevel.PUBLIC);
            dataMethod.getModifiers().add(ElementModifier.NATIVE);
            dataCls.addMethod(dataMethod);

            var result = info.invokeSpecial(dataMethod.getReference());
            if (type instanceof ValueType.Array) {
                var itemType = ((ValueType.Array) type).getItemType();
                if (itemType instanceof ValueType.Primitive) {
                    switch (((ValueType.Primitive) itemType).getKind()) {
                        case BOOLEAN:
                            result = pe.invoke(AnnotationInfoUtil.class, "asBooleanArray", boolean[].class, result);
                            break;
                        case BYTE:
                            result = pe.invoke(AnnotationInfoUtil.class, "asByteArray", byte[].class, result);
                            break;
                        case SHORT:
                            result = pe.invoke(AnnotationInfoUtil.class, "asShortArray", short[].class, result);
                            break;
                        case CHARACTER:
                            result = pe.invoke(AnnotationInfoUtil.class, "asCharArray", char[].class, result);
                            break;
                        case INTEGER:
                            result = pe.invoke(AnnotationInfoUtil.class, "asIntArray", int[].class, result);
                            break;
                        case LONG:
                            result = pe.invoke(AnnotationInfoUtil.class, "asLongArray", long[].class, result);
                            break;
                        case FLOAT:
                            result = pe.invoke(AnnotationInfoUtil.class, "asFloatArray", float[].class, result);
                            break;
                        case DOUBLE:
                            result = pe.invoke(AnnotationInfoUtil.class, "asDoubleArray", double[].class, result);
                            break;
                    }
                } else if (itemType.isObject(String.class)) {
                    result = pe.invoke(AnnotationInfoUtil.class, "asStringArray", String[].class, result);
                } else if (itemType.isObject(Class.class)) {
                    result = pe.invoke(AnnotationInfoUtil.class, "asClassArray", Class[].class, result);
                } else if (isEnum) {
                    var newArray = pe.constructArray(itemType, result.invokeSpecial("size", int.class));
                    var fields = pe.getField(enumCacheField.getReference(), enumCacheField.getType());
                    pe.invoke(AnnotationInfoUtil.class, "fillEnumArray", result, fields.cast(Enum[].class),
                            newArray.cast(Enum[].class));
                    result = newArray;
                } else if (isAnnotation) {
                    var newArray = pe.constructArray(itemType, result.invokeSpecial("size", int.class));
                    var annotCtor = pe.invoke(new MethodReference(className + ANNOTATION_DATA_SUFFIX, "constructor",
                            ValueType.parse(AnnotationConstructor.class)));
                    pe.invoke(AnnotationInfoUtil.class, "fillAnnotationArray", result, annotCtor,
                            newArray.cast(Annotation[].class));
                    result = newArray;
                    var annotImplName = getAnnotationImplementor(agent, className);
                    postSubmitActions.add(() -> agent.linkMethod(accessor.getReference())
                            .getResult().getArrayItem()
                            .propagate(agent.getType(ValueType.object(annotImplName))));
                }
            } else if (type.isObject(String.class)) {
                result = result.invokeSpecial("getStringObject", String.class);
            } else if (type.isObject(Class.class)) {
                result = pe.invoke(ClassInfoUtil.class, "resolve", ClassInfo.class, result);
                result = result.invokeSpecial("classObject", Class.class);
            } else if (isAnnotation) {
                var annotImplName = getAnnotationImplementor(agent, className);
                result = pe.invoke(annotImplName, "create", ValueType.object(annotImplName), result);
            } else if (isEnum) {
                result = pe.getField(enumCacheField.getReference(), enumCacheField.getType()).getElement(result);
            }
            result.returnValue();
        }

        var annotTypeMethod = new MethodHolder("annotationType", ValueType.parse(Class.class));
        pe = ProgramEmitter.create(annotTypeMethod, hierarchy);
        pe.constant(ValueType.object(annotationType)).returnValue();
        implementor.addMethod(annotTypeMethod);

        if (needsClinit) {
            clinitPe.exit();
            implementor.addMethod(clinit);
        }

        agent.submitClass(implementor);
        agent.submitClass(dataCls);

        for (var action : postSubmitActions) {
            action.run();
        }
        agent.linkMethod(factory.getReference()).use();
    }

    public void propagateAnnotationImplementations(DependencyAgent agent,
            Iterable<? extends AnnotationReader> inputAnnotations, DependencyNode outputNode) {
        for (var annotation : inputAnnotations) {
            agent.linkClass(annotation.getType());
        }

        for (var annotation : collectRuntimeAnnotations(agent.getClassSource(), inputAnnotations)) {
            propagateAnnotationInstance(agent, annotation, outputNode);
        }
    }

    public static List<AnnotationReader> collectRuntimeAnnotations(ClassReaderSource classes,
            Iterable<? extends AnnotationReader> inputAnnotations) {
        var annotations = new ArrayList<AnnotationReader>();
        for (var annot : inputAnnotations) {
            var annotType = classes.get(annot.getType());
            if (annotType == null) {
                continue;
            }

            var retention = annotType.getAnnotations().get(Retention.class.getName());
            if (retention != null) {
                String retentionPolicy = retention.getValue("value").getEnumValue().getFieldName();
                if (retentionPolicy.equals("RUNTIME")) {
                    annotations.add(annot);
                }
            }
        }
        return annotations;
    }

    private void propagateAnnotationInstance(DependencyAgent agent, AnnotationReader annotation,
            DependencyNode outputNode) {
        ClassReader annotationClass = agent.getClassSource().get(annotation.getType());
        if (annotationClass == null) {
            return;
        }

        var implementor = getAnnotationImplementor(agent, annotation.getType());
        if (implementor != null) {
            agent.linkClass(implementor).initClass(null);
            agent.linkClass(annotation.getType() + ANNOTATION_DATA_SUFFIX);
            outputNode.propagate(agent.getType(ValueType.object(implementor)));
            for (var methodDecl : annotationClass.getMethods()) {
                if (methodDecl.hasModifier(ElementModifier.STATIC)) {
                    continue;
                }
                var value = annotation.getValue(methodDecl.getName());
                if (value == null) {
                    value = methodDecl.getAnnotationDefault();
                }
                var field = agent.linkField(new FieldReference(implementor, "$" + methodDecl.getName()));
                propagateAnnotationValue(agent, value, methodDecl.getResultType(), field.getValue());
            }
            var initParamType = ValueType.object(annotation.getType() + ANNOTATION_DATA_SUFFIX);
            agent.linkMethod(new MethodReference(implementor, "<init>", initParamType, ValueType.VOID)).use();
        }
    }

    private void propagateAnnotationValue(DependencyAgent agent, AnnotationValue value, ValueType type,
            DependencyNode outputNode) {
        switch (value.getType()) {
            case AnnotationValue.LIST: {
                outputNode.propagate(agent.getType(type));
                var itemType = ((ValueType.Array) type).getItemType();
                for (var annotationValue : value.getList()) {
                    propagateAnnotationValue(agent, annotationValue, itemType, outputNode.getArrayItem());
                }
                break;
            }
            case AnnotationValue.ENUM:
                break;
            case AnnotationValue.CLASS: {
                var cls = value.getJavaClass();
                while (cls instanceof ValueType.Array) {
                    cls = ((ValueType.Array) cls).getItemType();
                }
                if (cls instanceof ValueType.Object) {
                    var className = ((ValueType.Object) cls).getClassName();
                    agent.linkClass(className).initClass(null);
                }
                outputNode.getClassValueNode().propagate(agent.getType(value.getJavaClass()));
                outputNode.propagate(agent.getType(ValueType.object("java.lang.Class")));
                break;
            }
            case AnnotationValue.ANNOTATION:
                propagateAnnotationInstance(agent, value.getAnnotation(), outputNode);
                break;
        }
    }
}
