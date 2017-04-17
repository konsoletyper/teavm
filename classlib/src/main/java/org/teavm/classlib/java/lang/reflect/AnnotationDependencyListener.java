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

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.platform.PlatformAnnotationProvider;

public class AnnotationDependencyListener extends AbstractDependencyListener {
    private String getAnnotationImplementor(DependencyAgent agent, String annotationType) {
        String implementorName = annotationType + "$$_impl";
        if (agent.getClassSource().get(implementorName) == null) {
            ClassHolder implementor = createImplementor(agent.getClassSource(), annotationType, implementorName);
            agent.submitClass(implementor);
        }
        return implementorName;
    }

    private ClassHolder createImplementor(ClassReaderSource classSource, String annotationType,
            String implementorName) {
        ClassHolder implementor = new ClassHolder(implementorName);
        implementor.setParent("java.lang.Object");
        implementor.getInterfaces().add(annotationType);
        implementor.getModifiers().add(ElementModifier.FINAL);
        implementor.setLevel(AccessLevel.PUBLIC);

        ClassReader annotation = classSource.get(annotationType);
        if (annotation == null) {
            return implementor;
        }

        List<ValueType> ctorSignature = new ArrayList<>();
        for (MethodReader methodDecl : annotation.getMethods()) {
            if (methodDecl.hasModifier(ElementModifier.STATIC)) {
                continue;
            }
            FieldHolder field = new FieldHolder("$" + methodDecl.getName());
            field.setType(methodDecl.getResultType());
            field.setLevel(AccessLevel.PRIVATE);
            implementor.addField(field);

            MethodHolder accessor = new MethodHolder(methodDecl.getDescriptor());
            ProgramEmitter pe = ProgramEmitter.create(accessor, classSource);
            ValueEmitter thisVal = pe.var(0, implementor);
            ValueEmitter result = thisVal.getField(field.getName(), field.getType());
            if (field.getType() instanceof ValueType.Array) {
                result = result.cloneArray();
            }
            result.returnValue();
            implementor.addMethod(accessor);

            ctorSignature.add(field.getType());
        }
        ctorSignature.add(ValueType.VOID);

        MethodHolder ctor = new MethodHolder("<init>", ctorSignature.toArray(new ValueType[ctorSignature.size()]));
        ProgramEmitter pe = ProgramEmitter.create(ctor, classSource);
        ValueEmitter thisVar = pe.var(0, implementor);
        thisVar.invokeSpecial(Object.class, "<init>");
        int index = 1;
        for (MethodReader methodDecl : annotation.getMethods()) {
            if (methodDecl.hasModifier(ElementModifier.STATIC)) {
                continue;
            }
            ValueEmitter param = pe.var(index++, methodDecl.getResultType());
            thisVar.setField("$" + methodDecl.getName(), param);
        }
        pe.exit();
        implementor.addMethod(ctor);

        MethodHolder annotTypeMethod = new MethodHolder("annotationType", ValueType.parse(Class.class));
        pe = ProgramEmitter.create(annotTypeMethod, classSource);
        pe.constant(ValueType.object(annotationType)).returnValue();
        implementor.addMethod(annotTypeMethod);

        return implementor;
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method, CallLocation location) {
        ValueType type = method.getMethod().getResultType();
        while (type instanceof ValueType.Array) {
            type = ((ValueType.Array) type).getItemType();
        }
        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            ClassReader cls = agent.getClassSource().get(className);
            if (cls != null && cls.hasModifier(ElementModifier.ANNOTATION)) {
                agent.linkClass(className, location);
            }
        }

        if (method.getMethod().hasModifier(ElementModifier.STATIC)
                && method.getMethod().getName().equals("$$__readAnnotations__$$")) {
            ClassReader cls = agent.getClassSource().get(method.getReference().getClassName());
            if (cls != null) {
                for (AnnotationReader annotation : cls.getAnnotations().all()) {
                    agent.linkClass(annotation.getType(), location);
                }
            }
        }

        MethodReference methodRef = method.getMethod().getReference();
        if (methodRef.getClassName().equals("java.lang.Class") && methodRef.getName().equals("getAnnotations")) {
            reachGetAnnotations(agent, location, method.getVariable(0));
        }
    }

    private void reachGetAnnotations(DependencyAgent agent, CallLocation location, DependencyNode node) {
        node.getClassValueNode().addConsumer(type -> {
            String className = type.getName();

            ClassReader cls = agent.getClassSource().get(className);
            if (cls == null) {
                return;
            }

            for (AnnotationReader annotation : cls.getAnnotations().all()) {
                agent.linkClass(annotation.getType(), location);
            }

            createAnnotationClass(agent, className);
        });
    }

    private void createAnnotationClass(DependencyAgent agent, String className) {
        String readerClassName = className + "$$__annotations__$$";
        if (agent.getClassSource().get(readerClassName) != null) {
            return;
        }

        ClassHolder cls = new ClassHolder(className + "$$__annotations__$$");
        cls.setLevel(AccessLevel.PUBLIC);
        cls.setOwnerName("java.lang.Object");
        cls.getInterfaces().add(PlatformAnnotationProvider.class.getName());

        MethodHolder ctor = new MethodHolder("<init>", ValueType.VOID);
        ctor.setLevel(AccessLevel.PUBLIC);
        ProgramEmitter pe = ProgramEmitter.create(ctor, agent.getClassSource());
        ValueEmitter thisVar = pe.var(0, cls);
        thisVar.invokeSpecial(Object.class, "<init>").exit();

        ClassReader annotatedClass = agent.getClassSource().get(className);
        cls.addMethod(ctor);
        cls.addMethod(addReader(agent, annotatedClass));

        agent.submitClass(cls);
    }

    private MethodHolder addReader(DependencyAgent agent, ClassReader cls) {
        MethodHolder readerMethod = new MethodHolder("getAnnotations", ValueType.parse(Annotation[].class));
        readerMethod.setLevel(AccessLevel.PUBLIC);
        ProgramEmitter pe = ProgramEmitter.create(readerMethod, agent.getClassSource());

        List<AnnotationReader> annotations = new ArrayList<>();
        for (AnnotationReader annot : cls.getAnnotations().all()) {
            ClassReader annotType = agent.getClassSource().get(annot.getType());
            if (annotType == null) {
                continue;
            }

            AnnotationReader retention = annotType.getAnnotations().get(Retention.class.getName());
            if (retention != null) {
                String retentionPolicy = retention.getValue("value").getEnumValue().getFieldName();
                if (retentionPolicy.equals("RUNTIME")) {
                    annotations.add(annot);
                }
            }
        }

        ValueEmitter array = pe.constructArray(Annotation.class, annotations.size());
        for (int i = 0; i < annotations.size(); ++i) {
            array.setElement(i, generateAnnotationInstance(agent, pe, annotations.get(i)));
        }

        array.returnValue();

        return readerMethod;
    }

    private ValueEmitter generateAnnotationInstance(DependencyAgent agent, ProgramEmitter pe,
            AnnotationReader annotation) {
        ClassReader annotationClass = agent.getClassSource().get(annotation.getType());
        if (annotationClass == null) {
            return pe.constantNull(ValueType.object(annotation.getType()));
        }

        String className = getAnnotationImplementor(agent, annotation.getType());
        List<ValueEmitter> params = new ArrayList<>();
        for (MethodReader methodDecl : annotationClass.getMethods()) {
            AnnotationValue value = annotation.getValue(methodDecl.getName());
            if (value == null) {
                value = methodDecl.getAnnotationDefault();
            }
            params.add(generateAnnotationValue(agent, pe, methodDecl.getResultType(), value)
                    .cast(methodDecl.getResultType()));
        }

        return pe.construct(className, params.toArray(new ValueEmitter[params.size()]));
    }

    private ValueEmitter generateAnnotationValue(DependencyAgent agent, ProgramEmitter pe, ValueType type,
            AnnotationValue value) {
        switch (value.getType()) {
            case AnnotationValue.BOOLEAN:
                return pe.constant(value.getBoolean() ? 1 : 0);
            case AnnotationValue.BYTE:
                return pe.constant(value.getByte());
            case AnnotationValue.SHORT:
                return pe.constant(value.getShort());
            case AnnotationValue.INT:
                return pe.constant(value.getInt());
            case AnnotationValue.LONG:
                return pe.constant(value.getLong());
            case AnnotationValue.FLOAT:
                return pe.constant(value.getFloat());
            case AnnotationValue.DOUBLE:
                return pe.constant(value.getDouble());
            case AnnotationValue.STRING:
                return pe.constant(value.getString());
            case AnnotationValue.LIST: {
                List<AnnotationValue> list = value.getList();
                ValueType itemType = ((ValueType.Array) type).getItemType();
                ValueEmitter array = pe.constructArray(itemType, list.size());
                for (int i = 0; i < list.size(); ++i) {
                    array.setElement(i, generateAnnotationValue(agent, pe, itemType, list.get(i)));
                }
                return array;
            }
            case AnnotationValue.ENUM:
                pe.initClass(value.getEnumValue().getClassName());
                return pe.getField(value.getEnumValue(), type);
            case AnnotationValue.CLASS:
                return pe.constant(value.getJavaClass());
            case AnnotationValue.ANNOTATION:
                return generateAnnotationInstance(agent, pe, value.getAnnotation());
            default:
                throw new IllegalArgumentException("Unknown annotation value type: " + value.getType());
        }
    }
}
