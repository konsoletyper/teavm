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
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.model.instructions.ArrayElementType;

/**
 *
 * @author Alexey Andreev
 */
public class AnnotationClassTransformer implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource, Diagnostics diagnostics) {
        MethodHolder readerMethod = new MethodHolder("$$__readAnnotations__$$", ValueType.parse(Annotation[].class));
        readerMethod.setLevel(AccessLevel.PUBLIC);
        readerMethod.getModifiers().add(ElementModifier.STATIC);
        ProgramEmitter pe = ProgramEmitter.create(readerMethod);

        List<AnnotationReader> annotations = new ArrayList<>();
        for (AnnotationReader annot : cls.getAnnotations().all()) {
            ClassReader annotType = innerSource.get(annot.getType());
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
            array.unwrapArray(ArrayElementType.OBJECT).setElement(i,
                    generateAnnotationInstance(innerSource, pe, annotations.get(i)));
        }

        array.returnValue();

        cls.addMethod(readerMethod);
    }

    private ValueEmitter generateAnnotationInstance(ClassReaderSource classSource, ProgramEmitter pe,
            AnnotationReader annotation) {
        ClassReader annotationClass = classSource.get(annotation.getType());
        if (annotationClass == null) {
            return pe.constantNull();
        }

        String className = annotation.getType() + "$$_impl";
        List<ValueType> ctorSignature = new ArrayList<>();
        List<ValueEmitter> params = new ArrayList<>();
        for (MethodReader methodDecl : annotationClass.getMethods()) {
            ctorSignature.add(methodDecl.getResultType());
            AnnotationValue value = annotation.getValue(methodDecl.getName());
            if (value == null) {
                value = methodDecl.getAnnotationDefault();
            }
            params.add(generateAnnotationValue(classSource, pe, methodDecl.getResultType(), value));
        }
        ctorSignature.add(ValueType.VOID);

        MethodReference ctor = new MethodReference(className, "<init>", ctorSignature.toArray(
                new ValueType[ctorSignature.size()]));
        return pe.construct(ctor, params.toArray(new ValueEmitter[params.size()]));
    }

    private ValueEmitter generateAnnotationValue(ClassReaderSource classSource, ProgramEmitter pe, ValueType type,
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
                    array.unwrapArray(ArrayElementType.OBJECT).setElement(i,
                            generateAnnotationValue(classSource, pe, itemType, list.get(i)));
                }
                return array;
            }
            case AnnotationValue.ENUM:
                return pe.getField(value.getEnumValue(), type);
            case AnnotationValue.CLASS:
                return pe.constant(value.getJavaClass());
            case AnnotationValue.ANNOTATION:
                return generateAnnotationInstance(classSource, pe, value.getAnnotation());
            default:
                throw new IllegalArgumentException("Unknown annotation value type: " + value.getType());
        }
    }
}
