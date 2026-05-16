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
package org.teavm.extension.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ElementModifier;
import org.teavm.model.ValueType;

public class IntrospectAnnotationImpl<T extends Annotation> implements IntrospectAnnotation<T> {
    private Introspection introspection;
    private ClassHierarchy hierarchy;
    private AnnotationReader reader;
    private IntrospectClass<T> annotationType;
    private Map<IntrospectMethod, Object> cache = new HashMap<>();

    @SuppressWarnings("unchecked")
    IntrospectAnnotationImpl(Introspection introspection, AnnotationReader reader) {
        this.introspection = introspection;
        this.hierarchy = introspection.getHierarchy();
        this.reader = reader;
        this.annotationType = (IntrospectClass<T>) introspection.findClass(reader.getType());
    }

    @Override
    public IntrospectClass<T> type() {
        return annotationType;
    }

    @Override
    public Object value(IntrospectMethod method) {
        if (!method.declaringClass().equals(annotationType)) {
            throw new IllegalArgumentException("Method " + method + " is not declared by " + annotationType);
        }
        return cache.computeIfAbsent(method, m -> {
            var methodReader = ((IntrospectMethodImpl) m).method;
            var value = reader.getValue(methodReader.getName());
            if (value == null) {
                value = methodReader.getAnnotationDefault();
            }
            try {
                return convertValue(value, methodReader.getResultType());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Object value(String attributeName) {
       return value(annotationType.declaredMethod(attributeName));
    }

    private Object convertValue(AnnotationValue value, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return value.getBoolean();
                case BYTE:
                    return value.getByte();
                case SHORT:
                    return value.getShort();
                case INTEGER:
                    return value.getInt();
                case LONG:
                    return value.getLong();
                case FLOAT:
                    return value.getFloat();
                case DOUBLE:
                    return value.getDouble();
                case CHARACTER:
                    return value.getChar();
            }
        } else if (type.isObject(String.class)) {
            return value.getString();
        } else if (type instanceof ValueType.Array) {
            List<AnnotationValue> array = value.getList();
            ValueType itemType = ((ValueType.Array) type).getItemType();
            Object result = createArray(itemType, array.size());
            for (int i = 0; i < array.size(); ++i) {
                Array.set(result, i, convertValue(array.get(i), itemType));
            }
            return result;
        } else if (type.isObject(Class.class)) {
            return introspection.getClass(value.getJavaClass());
        } else if (hierarchy.isSuperType(ValueType.parse(Enum.class), type, false)) {
            var fieldRef = value.getEnumValue();
            var cls = introspection.findClass(fieldRef.getClassName());
            return cls.declaredField(fieldRef.getFieldName());
        } else if (hierarchy.isSuperType(ValueType.parse(Annotation.class), type, false)) {
            return new IntrospectAnnotationImpl<>(introspection, value.getAnnotation());
        }

        throw new AssertionError("Unsupported type: " + type);
    }

    private Object createArray(ValueType type, int size) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return new boolean[size];
                case BYTE:
                    return new byte[size];
                case SHORT:
                    return new short[size];
                case CHARACTER:
                    return new char[size];
                case INTEGER:
                    return new int[size];
                case LONG:
                    return new long[size];
                case FLOAT:
                    return new float[size];
                case DOUBLE:
                    return new double[size];
            }
        } else if (type instanceof ValueType.Object) {
            var name = ((ValueType.Object) type).getClassName();
            if (name.equals("java.lang.String")) {
                return new String[size];
            } else if (name.equals("java.lang.Class")) {
                return new IntrospectClass[size];
            }
            var cls = introspection.getClassSource().get(name);
            if (cls != null) {
                if (cls.hasModifier(ElementModifier.ENUM)) {
                    return new IntrospectField[size];
                } else if (cls.hasModifier(ElementModifier.ANNOTATION)) {
                    return new IntrospectAnnotation[size];
                }
            }
        }
        throw new AssertionError("Unsupported type: " + type);
    }
}
