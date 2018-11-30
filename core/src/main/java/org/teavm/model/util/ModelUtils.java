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
package org.teavm.model.util;

import java.util.ArrayList;
import java.util.List;
import org.teavm.model.*;

public final class ModelUtils {
    private ModelUtils() {
    }

    public static ClassHolder copyClass(ClassReader original, ClassHolder target) {
        target.setLevel(original.getLevel());
        target.getModifiers().addAll(original.readModifiers());
        target.setParent(original.getParent());
        target.getInterfaces().addAll(original.getInterfaces());
        for (MethodReader method : original.getMethods()) {
            target.addMethod(copyMethod(method));
        }
        for (FieldReader field : original.getFields()) {
            target.addField(copyField(field));
        }
        target.setOwnerName(original.getOwnerName());
        copyAnnotations(original.getAnnotations(), target.getAnnotations());
        return target;
    }

    public static ClassHolder copyClass(ClassReader original) {
        return copyClass(original, new ClassHolder(original.getName()));
    }

    public static MethodHolder copyMethod(MethodReader method) {
        MethodHolder copy = new MethodHolder(method.getDescriptor());
        copy.setLevel(method.getLevel());
        copy.getModifiers().addAll(method.readModifiers());
        if (method.getProgram() != null) {
            copy.setProgram(ProgramUtils.copy(method.getProgram()));
        }
        copyAnnotations(method.getAnnotations(), copy.getAnnotations());
        if (method.getAnnotationDefault() != null) {
            copy.setAnnotationDefault(copyAnnotationValue(method.getAnnotationDefault()));
        }
        for (int i = 0; i < method.parameterCount(); ++i) {
            copyAnnotations(method.parameterAnnotation(i), copy.parameterAnnotation(i));
        }
        return copy;
    }

    public static FieldHolder copyField(FieldReader field) {
        FieldHolder copy = new FieldHolder(field.getName());
        copy.setLevel(field.getLevel());
        copy.getModifiers().addAll(field.readModifiers());
        copy.setType(field.getType());
        copy.setInitialValue(field.getInitialValue());
        copyAnnotations(field.getAnnotations(), copy.getAnnotations());
        return copy;
    }

    public static void copyAnnotations(AnnotationContainerReader src, AnnotationContainer dst) {
        for (AnnotationReader annot : src.all()) {
            dst.add(copyAnnotation(annot));
        }
    }

    private static AnnotationHolder copyAnnotation(AnnotationReader annot) {
        AnnotationHolder copy = new AnnotationHolder(annot.getType());
        for (String fieldName : annot.getAvailableFields()) {
            copy.getValues().put(fieldName, copyAnnotationValue(annot.getValue(fieldName)));
        }
        return copy;
    }

    private static AnnotationValue copyAnnotationValue(AnnotationValue value) {
        switch (value.getType()) {
            case AnnotationValue.LIST: {
                List<AnnotationValue> listCopy = new ArrayList<>();
                for (AnnotationValue item : value.getList()) {
                    listCopy.add(copyAnnotationValue(item));
                }
                return new AnnotationValue(listCopy);
            }
            case AnnotationValue.ANNOTATION:
                return new AnnotationValue(copyAnnotation(value.getAnnotation()));
            default:
                return value;
        }
    }
}
