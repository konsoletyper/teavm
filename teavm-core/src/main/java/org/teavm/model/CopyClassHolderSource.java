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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.teavm.common.Mapper;
import org.teavm.model.util.ProgramUtils;
import org.teavm.resource.MapperClassHolderSource;

/**
 *
 * @author Alexey Andreev
 */
public class CopyClassHolderSource implements ClassHolderSource {
    private ClassHolderSource innerSource;
    private MapperClassHolderSource mapperSource = new MapperClassHolderSource(new Mapper<String, ClassHolder>() {
        @Override public ClassHolder map(String preimage) {
            return copyClass(preimage);
        }
    });

    public CopyClassHolderSource(ClassHolderSource innerSource) {
        this.innerSource = innerSource;
    }

    @Override
    public ClassHolder getClassHolder(String name) {
        return mapperSource.getClassHolder(name);
    }

    private ClassHolder copyClass(String className) {
        ClassHolder original = innerSource.getClassHolder(className);
        if (original == null) {
            return null;
        }
        ClassHolder copy = new ClassHolder(className);
        copy.setLevel(original.getLevel());
        copy.getModifiers().addAll(original.getModifiers());
        copy.setParent(original.getParent());
        copy.getInterfaces().addAll(original.getInterfaces());
        for (MethodHolder method : original.getMethods()) {
            copy.addMethod(copyMethod(method));
        }
        for (FieldHolder field : original.getFields()) {
            copy.addField(copyField(field));
        }
        copyAnnotations(original.getAnnotations(), copy.getAnnotations());
        return copy;
    }

    private MethodHolder copyMethod(MethodHolder method) {
        MethodHolder copy = new MethodHolder(method.getDescriptor());
        copy.setLevel(method.getLevel());
        copy.getModifiers().addAll(method.getModifiers());
        copy.setProgram(ProgramUtils.copy(method.getProgram()));
        copyAnnotations(method.getAnnotations(), copy.getAnnotations());
        return copy;
    }

    private FieldHolder copyField(FieldHolder field) {
        FieldHolder copy = new FieldHolder(field.getName());
        copy.setLevel(field.getLevel());
        copy.getModifiers().addAll(field.getModifiers());
        copy.setType(field.getType());
        copy.setInitialValue(field.getInitialValue());
        copyAnnotations(field.getAnnotations(), copy.getAnnotations());
        return copy;
    }

    private void copyAnnotations(AnnotationContainer src, AnnotationContainer dst) {
        for (AnnotationHolder annot : src.all()) {
            dst.add(copyAnnotation(annot));
        }
    }

    private AnnotationHolder copyAnnotation(AnnotationHolder annot) {
        AnnotationHolder copy = new AnnotationHolder(annot.getType());
        for (Map.Entry<String, AnnotationValue> entry : annot.getValues().entrySet()) {
            copy.getValues().put(entry.getKey(), copyAnnotationValue(entry.getValue()));
        }
        return copy;
    }

    private AnnotationValue copyAnnotationValue(AnnotationValue value) {
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
