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
package org.teavm.metaprogramming.impl.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReader;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.ValueType;

class AnnotationProxy implements InvocationHandler {
    private ClassLoader classLoader;
    private ClassHierarchy hierarchy;
    private AnnotationReader reader;
    private Class<?> annotationType;
    private Map<String, Object> cache = new HashMap<>();

    AnnotationProxy(ClassLoader classLoader, ClassHierarchy hierarchy, AnnotationReader reader,
            Class<?> annotationType) {
        this.classLoader = classLoader;
        this.hierarchy = hierarchy;
        this.reader = reader;
        this.annotationType = annotationType;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (method.getName().equals("annotationType")) {
             return annotationType;
        } else {
            ClassReader cls = hierarchy.getClassSource().get(reader.getType());
            return cache.computeIfAbsent(method.getName(), name -> {
                MethodDescriptor desc = new MethodDescriptor(name, ValueType.parse(method.getReturnType()));
                MethodReader methodReader = cls.getMethod(desc);
                AnnotationValue value = reader.getValue(name);
                if (value == null) {
                    value = methodReader.getAnnotationDefault();
                }
                try {
                    return convertValue(value, desc.getResultType());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private Object convertValue(AnnotationValue value, ValueType type) throws Exception {
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
                    break;
            }
        } else if (type.isObject(String.class)) {
            return value.getString();
        } else if (type instanceof ValueType.Array) {
            List<AnnotationValue> array = value.getList();
            ValueType itemType = ((ValueType.Array) type).getItemType();
            Class<?> componentType = convertClass(itemType);
            Object result = Array.newInstance(componentType, array.size());
            for (int i = 0; i < array.size(); ++i) {
                Array.set(result, i, convertValue(array.get(i), itemType));
            }
            return result;
        } else if (type.isObject(Class.class)) {
            return convertClass(value.getJavaClass());
        } else if (hierarchy.isSuperType(ValueType.parse(Enum.class), type, false)) {
            FieldReference fieldRef = value.getEnumValue();
            Class<?> enumClass = Class.forName(fieldRef.getClassName(), true, classLoader);
            return enumClass.getField(fieldRef.getFieldName()).get(null);
        } else if (hierarchy.isSuperType(ValueType.parse(Annotation.class), type, false)) {
            Class<?> annotType = convertClass(type);
            AnnotationProxy handler = new AnnotationProxy(classLoader, hierarchy, value.getAnnotation(), annotType);
            return Proxy.newProxyInstance(classLoader, new Class<?>[] { annotType }, handler);
        }

        throw new AssertionError("Unsupported type: " + type);
    }

    private Class<?> convertClass(ValueType type) throws Exception {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return boolean.class;
                case BYTE:
                    return byte.class;
                case SHORT:
                    return short.class;
                case CHARACTER:
                    return char.class;
                case INTEGER:
                    return int.class;
                case LONG:
                    return long.class;
                case FLOAT:
                    return float.class;
                case DOUBLE:
                    return double.class;
            }
        } else if (type instanceof ValueType.Array) {
            Class<?> componentType = convertClass(((ValueType.Array) type).getItemType());
            return Array.newInstance(componentType, 0).getClass();
        } else if (type == ValueType.VOID) {
            return void.class;
        } else if (type instanceof ValueType.Object) {
            String name = ((ValueType.Object) type).getClassName();
            return Class.forName(name, true, classLoader);
        }
        throw new AssertionError("Unsupported type: " + type);
    }
}
