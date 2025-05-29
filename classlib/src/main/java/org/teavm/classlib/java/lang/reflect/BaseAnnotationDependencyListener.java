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

import java.util.ArrayList;
import java.util.List;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.AccessLevel;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReader;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;

public abstract class BaseAnnotationDependencyListener extends AbstractDependencyListener {
    public static final String ANNOTATION_IMPLEMENTOR_SUFFIX = "$$_impl";
    private boolean enumsAsInts;

    public BaseAnnotationDependencyListener(boolean enumsAsInts) {
        this.enumsAsInts = enumsAsInts;
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        ValueType type = method.getMethod().getResultType();
        while (type instanceof ValueType.Array) {
            type = ((ValueType.Array) type).getItemType();
        }
        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            ClassReader cls = agent.getClassSource().get(className);
            if (cls != null && cls.hasModifier(ElementModifier.ANNOTATION)) {
                agent.linkClass(className);
            }
        }
    }

    protected final String getAnnotationImplementor(DependencyAgent agent, String annotationType) {
        String implementorName = annotationType + ANNOTATION_IMPLEMENTOR_SUFFIX;
        if (agent.getClassSource().get(implementorName) == null) {
            ClassHolder implementor = createImplementor(agent.getClassHierarchy(), annotationType, implementorName);
            agent.submitClass(implementor);
        }
        return implementorName;
    }

    private ClassHolder createImplementor(ClassHierarchy hierarchy, String annotationType, String implementorName) {
        ClassHolder implementor = new ClassHolder(implementorName);
        implementor.setParent("java.lang.Object");
        implementor.getInterfaces().add(annotationType);
        implementor.getModifiers().add(ElementModifier.FINAL);
        implementor.setLevel(AccessLevel.PUBLIC);

        ClassReader annotation = hierarchy.getClassSource().get(annotationType);
        if (annotation == null) {
            return implementor;
        }

        List<ValueType> ctorSignature = new ArrayList<>();
        for (MethodReader methodDecl : annotation.getMethods()) {
            if (methodDecl.hasModifier(ElementModifier.STATIC)) {
                continue;
            }
            FieldHolder field = new FieldHolder("$" + methodDecl.getName());
            var type = methodDecl.getResultType();
            var isEnum = false;
            var degree = 0;
            String enumCacheType = null;
            if (enumsAsInts) {
                while (type instanceof ValueType.Array) {
                    type = ((ValueType.Array) type).getItemType();
                    ++degree;
                }
                if (type instanceof ValueType.Object) {
                    var typeName = ((ValueType.Object) type).getClassName();
                    var cls = hierarchy.getClassSource().get(typeName);
                    if (cls != null && cls.hasModifier(ElementModifier.ENUM)) {
                        enumCacheType = typeName;
                        type = ValueType.INTEGER;
                        isEnum = true;
                    }
                }
                for (var i = 0; i < degree; ++i) {
                    type = ValueType.arrayOf(type);
                }
            }
            field.setType(type);
            field.setLevel(AccessLevel.PRIVATE);
            implementor.addField(field);

            MethodHolder accessor = new MethodHolder(methodDecl.getDescriptor());
            ProgramEmitter pe = ProgramEmitter.create(accessor, hierarchy);
            ValueEmitter thisVal = pe.var(0, implementor);
            if (isEnum) {
                var cacheField = new FieldHolder("enumCache$" + field.getName());
                cacheField.setLevel(AccessLevel.PRIVATE);
                cacheField.getModifiers().add(ElementModifier.STATIC);
                implementor.addField(cacheField);

                cacheField.setType(ValueType.arrayOf(ValueType.object(enumCacheType)));
                decodeEnumOrEnumArray(pe, thisVal, cacheField, enumCacheType, field, accessor.getResultType());
            } else {
                ValueEmitter result = thisVal.getField(field.getName(), field.getType());
                if (field.getType() instanceof ValueType.Array) {
                    result = result.cloneArray();
                    result = result.cast(field.getType());
                }
                result.returnValue();
            }
            implementor.addMethod(accessor);

            ctorSignature.add(field.getType());
        }
        ctorSignature.add(ValueType.VOID);

        MethodHolder ctor = new MethodHolder("<init>", ctorSignature.toArray(new ValueType[0]));
        ProgramEmitter pe = ProgramEmitter.create(ctor, hierarchy);
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
        pe = ProgramEmitter.create(annotTypeMethod, hierarchy);
        pe.constant(ValueType.object(annotationType)).returnValue();
        implementor.addMethod(annotTypeMethod);

        return implementor;
    }

    private void decodeEnumOrEnumArray(ProgramEmitter pe, ValueEmitter thisVal, FieldReader cacheField,
            String enumCacheType, FieldReader field, ValueType type) {
        pe.when(pe.getField(cacheField.getReference(), cacheField.getType()).isNull()).thenDo(() -> {
            pe.setField(cacheField.getReference(), pe.invoke(enumCacheType, "values", cacheField.getType()));
        });
        var source = thisVal.getField(field.getName(), field.getType());
        decodeEnumArrayRec(pe, source, cacheField, type).returnValue();
    }

    private ValueEmitter decodeEnumArrayRec(ProgramEmitter pe, ValueEmitter source, FieldReader cacheField,
            ValueType type) {
        if (type instanceof ValueType.Array) {
            var header = pe.prepareBlock();
            var body = pe.prepareBlock();
            var exit = pe.prepareBlock();

            var itemType = ((ValueType.Array) type).getItemType();
            var targetArray = pe.constructArray(itemType, source.arrayLength());
            var initialIndex = pe.constant(0);
            var index = pe.phi(ValueType.INTEGER, header);
            initialIndex.propagateTo(index);
            pe.jump(header);

            pe.enter(header);
            pe.when(index.getValue().isLessThan(source.arrayLength()))
                    .thenDo(() -> pe.jump(body))
                    .elseDo(() -> pe.jump(exit));

            pe.enter(body);
            var mappedValue = decodeEnumArrayRec(pe, source.getElement(index.getValue()), cacheField, itemType);
            targetArray.setElement(index.getValue(), mappedValue);
            index.getValue().add(1).propagateTo(index);
            pe.jump(header);

            pe.enter(exit);

            return targetArray;
        } else {
            return pe.getField(cacheField.getReference(), cacheField.getType()).getElement(source);
        }
    }
}
