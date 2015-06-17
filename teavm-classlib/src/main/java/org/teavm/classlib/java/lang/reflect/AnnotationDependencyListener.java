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
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyListener;
import org.teavm.dependency.FieldDependency;
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
import org.teavm.model.FieldReference;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;

/**
 *
 * @author Alexey Andreev
 */
public class AnnotationDependencyListener implements DependencyListener {
    @Override
    public void started(DependencyAgent agent) {
    }

    @Override
    public void classAchieved(DependencyAgent agent, String className, CallLocation location) {
        ClassReader cls = agent.getClassSource().get(className);
        if (cls == null) {
            return;
        }

        MethodHolder readerMethod = new MethodHolder("$$_readAnnotations_$$", ValueType.parse(Annotation[].class));
        readerMethod.setLevel(AccessLevel.PUBLIC);
        readerMethod.getModifiers().add(ElementModifier.STATIC);
        ProgramEmitter pe = ProgramEmitter.create(readerMethod);

        List<AnnotationReader> annotations = new ArrayList<>();
        for (AnnotationReader annot : cls.getAnnotations().all()) {
            ClassReader annotType = agent.getClassSource().get(annot.getType());
            if (annotType == null) {
                continue;
            }

            AnnotationReader retention = annotType.getAnnotations().get(Retention.class.getName());
            String retentionPolicy = retention.getValue("value").getEnumValue().getFieldName();
            if (retentionPolicy.equals("RUNTIME")) {
                annotations.add(annot);
            }
        }

        ValueEmitter array = pe.constructArray(Annotation.class, annotations.size());
        for (int i = 0; i < annotations.size(); ++i) {
            array.setElement(i, generateAnnotationInstance(agent, pe, annotations.get(i)));
        }

        array.returnValue();
    }

    private ValueEmitter generateAnnotationInstance(DependencyAgent agent, ProgramEmitter pe,
            AnnotationReader annotation) {
        ClassReader annotationClass = agent.getClassSource().get(annotation.getType());
        if (annotationClass == null) {
            return pe.constantNull();
        }

        String className = getAnnotationImplementor(agent, annotation.getType());
        List<ValueType> ctorSignature = new ArrayList<>();
        List<ValueEmitter> params = new ArrayList<>();
        for (MethodReader methodDecl : annotationClass.getMethods()) {
            ctorSignature.add(methodDecl.getResultType());
            AnnotationValue value = annotation.getValue(className);
            params.add(value != null ? generateAnnotationValue(agent, pe, methodDecl.getResultType(), value) :
                    pe.constantNull());
        }
        ctorSignature.add(ValueType.VOID);

        MethodReference ctor = new MethodReference(className, "<init>", ctorSignature.toArray(
                new ValueType[ctorSignature.size()]));
        return pe.construct(ctor, params.toArray(new ValueEmitter[params.size()]));
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
                ValueType itemType = ((ValueType.Array)type).getItemType();
                ValueEmitter array = pe.constructArray(itemType, list.size());
                for (int i = 0; i < list.size(); ++i) {
                    array.setElement(i, generateAnnotationValue(agent, pe, itemType, list.get(i)));
                }
                return array;
            }
            case AnnotationValue.ENUM:
                return pe.getField(value.getEnumValue(), type);
            case AnnotationValue.CLASS:
                return pe.constant(value.getJavaClass());
            case AnnotationValue.ANNOTATION:
                return generateAnnotationInstance(agent, pe, value.getAnnotation());
            default:
                throw new IllegalArgumentException("Unknown annotation value type: " + value.getType());
        }
    }

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
            FieldHolder field = new FieldHolder("$" + methodDecl.getName());
            field.setType(methodDecl.getResultType());
            field.setLevel(AccessLevel.PRIVATE);
            implementor.addField(field);

            MethodHolder accessor = new MethodHolder(methodDecl.getDescriptor());
            ProgramEmitter pe = ProgramEmitter.create(accessor);
            ValueEmitter thisVal = pe.wrapNew();
            ValueEmitter result = thisVal.getField(field.getReference(), field.getType());
            if (field.getType() instanceof ValueType.Array) {
                result = result.cloneArray();
            }
            result.returnValue();
            implementor.addMethod(accessor);

            ctorSignature.add(field.getType());
        }
        ctorSignature.add(ValueType.VOID);

        MethodHolder ctor = new MethodHolder("<init>", ctorSignature.toArray(new ValueType[ctorSignature.size()]));
        ProgramEmitter pe = ProgramEmitter.create(ctor);
        ValueEmitter thisVal = pe.wrapNew();
        thisVal.invokeSpecial(new MethodReference(Object.class, "<init>", void.class));
        for (MethodReader methodDecl : annotation.getMethods()) {
            ValueEmitter param = pe.wrapNew();
            FieldReference field = new FieldReference(implementorName, "$" + methodDecl.getName());
            thisVal.setField(field, methodDecl.getResultType(), param);
        }
        pe.exit();
        implementor.addMethod(ctor);

        return implementor;
    }

    @Override
    public void methodAchieved(DependencyAgent agent, MethodDependency method, CallLocation location) {
    }

    @Override
    public void fieldAchieved(DependencyAgent agent, FieldDependency field, CallLocation location) {
    }
}
