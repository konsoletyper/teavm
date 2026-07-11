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
package org.teavm.classlib.java.lang.reflect;

import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.java.lang.TClass;
import org.teavm.classlib.java.lang.TIllegalAccessException;
import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.lang.annotation.TAnnotation;
import org.teavm.interop.Address;
import org.teavm.interop.Unmanaged;
import org.teavm.runtime.GC;
import org.teavm.runtime.reflect.AnnotationInfoUtil;
import org.teavm.runtime.reflect.ClassInfo;
import org.teavm.runtime.reflect.ClassInfoUtil;
import org.teavm.runtime.reflect.FieldInfo;
import org.teavm.runtime.reflect.ModifiersInfo;

public class TField extends TAccessibleObject implements TMember {
    private TClass<?> declaringClass;
    private FieldInfo fieldInfo;
    private TAnnotation[] declaredAnnotations;
    private TType genericType;
    private boolean genericTypeInitialized;

    public TField(TClass<?> declaringClass, FieldInfo fieldInfo) {
        this.declaringClass = declaringClass;
        this.fieldInfo = fieldInfo;
    }

    @Override
    public TClass<?> getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public String getName() {
        return fieldInfo.name().getStringObject();
    }

    @Override
    public int getModifiers() {
        return fieldInfo.modifiers() & ModifiersInfo.JVM_FLAGS_MASK;
    }

    public boolean isEnumConstant() {
        return (fieldInfo.modifiers() & ModifiersInfo.ENUM) != 0;
    }

    @Override
    public boolean isSynthetic() {
        return (fieldInfo.modifiers() & ModifiersInfo.SYNTHETIC) != 0;
    }

    public TClass<?> getType() {
        return (TClass<?>) (Object) ClassInfoUtil.resolve(fieldInfo.type()).classObject();
    }

    public TType getGenericType() {
        if (!genericTypeInitialized) {
            genericTypeInitialized = true;
            var reflectionInfo = fieldInfo.reflection();
            if (reflectionInfo != null && reflectionInfo.genericType() != null) {
                genericType = TGenericTypeFactory.create(declaringClass, reflectionInfo.genericType());
            } else {
                genericType = getType();
            }
        }
        return genericType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TModifier.toString(getModifiers()));
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(getType().getName()).append(' ').append(declaringClass.getName()).append(".").append(getName());
        return sb.toString();
    }

    public Object get(Object obj) throws TIllegalArgumentException, TIllegalAccessException {
        checkGetAccess();
        checkInstance(obj);
        return getWithoutCheck(obj);
    }

    public boolean getBoolean(Object obj) throws TIllegalArgumentException, TIllegalAccessException {
        checkGetAccess();
        checkInstance(obj);
        initClass();
        if (fieldInfo.type().arrayDegree() > 0
                || fieldInfo.type().classInfo().primitiveKind() != ClassInfo.PrimitiveKind.BOOLEAN) {
            throw new IllegalArgumentException();
        }
        return fieldInfo.readAsBoolean(obj);
    }

    public byte getByte(Object obj) throws TIllegalArgumentException, TIllegalAccessException {
        checkGetAccess();
        checkInstance(obj);
        if (fieldInfo.type().arrayDegree() > 0
                || fieldInfo.type().classInfo().primitiveKind() != ClassInfo.PrimitiveKind.BYTE) {
            throw new IllegalArgumentException();
        }
        initClass();
        return fieldInfo.readAsByte(obj);
    }

    public short getShort(Object obj) throws TIllegalArgumentException, TIllegalAccessException {
        checkGetAccess();
        checkInstance(obj);
        if (fieldInfo.type().arrayDegree() > 0) {
            throw new IllegalArgumentException();
        }
        switch (fieldInfo.type().classInfo().primitiveKind()) {
            case ClassInfo.PrimitiveKind.BYTE -> {
                initClass();
                return fieldInfo.readAsByte(obj);
            }
            case ClassInfo.PrimitiveKind.SHORT -> {
                initClass();
                return fieldInfo.readAsShort(obj);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    public char getChar(Object obj) throws TIllegalArgumentException, TIllegalAccessException {
        checkGetAccess();
        checkInstance(obj);
        if (fieldInfo.type().arrayDegree() > 0
                || fieldInfo.type().classInfo().primitiveKind() != ClassInfo.PrimitiveKind.CHAR) {
            throw new IllegalArgumentException();
        }
        initClass();
        return fieldInfo.readAsChar(obj);
    }

    public int getInt(Object obj) throws TIllegalArgumentException, TIllegalAccessException {
        checkGetAccess();
        checkInstance(obj);
        if (fieldInfo.type().arrayDegree() > 0) {
            throw new IllegalArgumentException();
        }
        switch (fieldInfo.type().classInfo().primitiveKind()) {
            case ClassInfo.PrimitiveKind.BYTE -> {
                initClass();
                return fieldInfo.readAsByte(obj);
            }
            case ClassInfo.PrimitiveKind.SHORT -> {
                initClass();
                return fieldInfo.readAsShort(obj);
            }
            case ClassInfo.PrimitiveKind.CHAR -> {
                initClass();
                return fieldInfo.readAsChar(obj);
            }
            case ClassInfo.PrimitiveKind.INT -> {
                initClass();
                return fieldInfo.readAsInt(obj);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    public long getLong(Object obj) throws TIllegalArgumentException, TIllegalAccessException {
        checkGetAccess();
        checkInstance(obj);
        if (fieldInfo.type().arrayDegree() > 0) {
            throw new IllegalArgumentException();
        }
        switch (fieldInfo.type().classInfo().primitiveKind()) {
            case ClassInfo.PrimitiveKind.BYTE -> {
                initClass();
                return fieldInfo.readAsByte(obj);
            }
            case ClassInfo.PrimitiveKind.SHORT -> {
                initClass();
                return fieldInfo.readAsShort(obj);
            }
            case ClassInfo.PrimitiveKind.CHAR -> {
                initClass();
                return fieldInfo.readAsChar(obj);
            }
            case ClassInfo.PrimitiveKind.INT -> {
                initClass();
                return fieldInfo.readAsInt(obj);
            }
            case ClassInfo.PrimitiveKind.LONG -> {
                initClass();
                return fieldInfo.readAsLong(obj);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    public float getFloat(Object obj) throws TIllegalArgumentException, TIllegalAccessException {
        checkGetAccess();
        checkInstance(obj);
        if (fieldInfo.type().arrayDegree() > 0) {
            throw new IllegalArgumentException();
        }
        switch (fieldInfo.type().classInfo().primitiveKind()) {
            case ClassInfo.PrimitiveKind.BYTE -> {
                initClass();
                return fieldInfo.readAsByte(obj);
            }
            case ClassInfo.PrimitiveKind.SHORT -> {
                initClass();
                return fieldInfo.readAsShort(obj);
            }
            case ClassInfo.PrimitiveKind.CHAR -> {
                initClass();
                return fieldInfo.readAsChar(obj);
            }
            case ClassInfo.PrimitiveKind.INT -> {
                initClass();
                return fieldInfo.readAsInt(obj);
            }
            case ClassInfo.PrimitiveKind.LONG -> {
                initClass();
                return fieldInfo.readAsLong(obj);
            }
            case ClassInfo.PrimitiveKind.FLOAT -> {
                initClass();
                return fieldInfo.readAsFloat(obj);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    public double getDouble(Object obj) throws TIllegalArgumentException, TIllegalAccessException {
        checkGetAccess();
        checkInstance(obj);
        initClass();
        if (fieldInfo.type().arrayDegree() > 0) {
            throw new IllegalArgumentException();
        }
        switch (fieldInfo.type().classInfo().primitiveKind()) {
            case ClassInfo.PrimitiveKind.BYTE -> {
                initClass();
                return fieldInfo.readAsByte(obj);
            }
            case ClassInfo.PrimitiveKind.SHORT -> {
                initClass();
                return fieldInfo.readAsShort(obj);
            }
            case ClassInfo.PrimitiveKind.CHAR -> {
                initClass();
                return fieldInfo.readAsChar(obj);
            }
            case ClassInfo.PrimitiveKind.INT -> {
                initClass();
                return fieldInfo.readAsInt(obj);
            }
            case ClassInfo.PrimitiveKind.LONG -> {
                initClass();
                return fieldInfo.readAsLong(obj);
            }
            case ClassInfo.PrimitiveKind.FLOAT -> {
                initClass();
                return fieldInfo.readAsFloat(obj);
            }
            case ClassInfo.PrimitiveKind.DOUBLE -> {
                return fieldInfo.readAsDouble(obj);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    public Object getWithoutCheck(Object obj) {
        initClass();
        return fieldInfo.read(obj);
    }

    public void set(Object obj, Object value) throws TIllegalArgumentException, TIllegalAccessException {
        checkSetAccess();
        checkInstance(obj);
        setWithoutCheck(obj, value);
    }

    public void setBoolean(Object obj, boolean value) throws TIllegalArgumentException, TIllegalAccessException {
        checkSetAccess();
        checkInstance(obj);
        if (fieldInfo.type().arrayDegree() > 0) {
            throw new IllegalArgumentException();
        }
        if (fieldInfo.type().classInfo().primitiveKind() == ClassInfo.PrimitiveKind.BOOLEAN) {
            fieldInfo.write(obj, value);
        } else if (fieldInfo.type().classInfo().classObject() == Boolean.class) {
            fieldInfo.write(obj, (Object) value);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void setByte(Object obj, byte value) throws TIllegalArgumentException, TIllegalAccessException {
        checkSetAccess();
        checkInstance(obj);
        if (fieldInfo.type().arrayDegree() > 0) {
            throw new IllegalArgumentException();
        }
        if (fieldInfo.type().classInfo().primitiveKind() == ClassInfo.PrimitiveKind.BYTE) {
            fieldInfo.write(obj, value);
        } else if (fieldInfo.type().classInfo().classObject() == Byte.class) {
            fieldInfo.write(obj, (Object) value);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void setShort(Object obj, short value) throws TIllegalArgumentException, TIllegalAccessException {
        checkSetAccess();
        checkInstance(obj);
        if (fieldInfo.type().arrayDegree() > 0) {
            throw new IllegalArgumentException();
        }
        if (fieldInfo.type().classInfo().primitiveKind() == ClassInfo.PrimitiveKind.SHORT) {
            fieldInfo.write(obj, value);
        } else if (fieldInfo.type().classInfo().classObject() == Short.class) {
            fieldInfo.write(obj, (Object) value);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void setChar(Object obj, char value) throws TIllegalArgumentException, TIllegalAccessException {
        checkSetAccess();
        checkInstance(obj);
        if (fieldInfo.type().arrayDegree() > 0) {
            throw new IllegalArgumentException();
        }
        if (fieldInfo.type().classInfo().primitiveKind() == ClassInfo.PrimitiveKind.CHAR) {
            fieldInfo.write(obj, value);
        } else if (fieldInfo.type().classInfo().classObject() == Character.class) {
            fieldInfo.write(obj, (Object) value);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void setInt(Object obj, int value) throws TIllegalArgumentException, TIllegalAccessException {
        checkSetAccess();
        checkInstance(obj);
        if (fieldInfo.type().arrayDegree() > 0) {
            throw new IllegalArgumentException();
        }
        if (fieldInfo.type().classInfo().primitiveKind() == ClassInfo.PrimitiveKind.INT) {
            fieldInfo.write(obj, value);
        } else if (fieldInfo.type().classInfo().classObject() == Integer.class) {
            fieldInfo.write(obj, (Object) value);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void setLong(Object obj, long value) throws TIllegalArgumentException, TIllegalAccessException {
        checkSetAccess();
        checkInstance(obj);
        if (fieldInfo.type().arrayDegree() > 0) {
            throw new IllegalArgumentException();
        }
        if (fieldInfo.type().classInfo().primitiveKind() == ClassInfo.PrimitiveKind.LONG) {
            fieldInfo.write(obj, value);
        } else if (fieldInfo.type().classInfo().classObject() == Long.class) {
            fieldInfo.write(obj, (Object) value);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void setFloat(Object obj, float value) throws TIllegalArgumentException, TIllegalAccessException {
        checkSetAccess();
        checkInstance(obj);
        if (fieldInfo.type().arrayDegree() > 0) {
            throw new IllegalArgumentException();
        }
        if (fieldInfo.type().classInfo().primitiveKind() == ClassInfo.PrimitiveKind.FLOAT) {
            fieldInfo.write(obj, value);
        } else if (fieldInfo.type().classInfo().classObject() == Float.class) {
            fieldInfo.write(obj, (Object) value);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void setDouble(Object obj, double value) throws TIllegalArgumentException, TIllegalAccessException {
        checkSetAccess();
        checkInstance(obj);
        if (fieldInfo.type().arrayDegree() > 0) {
            throw new IllegalArgumentException();
        }
        if (fieldInfo.type().classInfo().primitiveKind() == ClassInfo.PrimitiveKind.DOUBLE) {
            fieldInfo.write(obj, value);
        } else if (fieldInfo.type().classInfo().classObject() == Double.class) {
            fieldInfo.write(obj, (Object) value);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Unmanaged
    public void setWithoutCheck(Object obj, Object value) {
        initClass();
        if ((fieldInfo.modifiers() & ModifiersInfo.STATIC) == 0
                && PlatformDetector.requiresOwnGC() && (fieldInfo.type().arrayDegree() > 0
                || fieldInfo.type().classInfo().primitiveKind() == ClassInfo.PrimitiveKind.NOT)) {
            GC.writeBarrier(Address.ofObject(obj).toStructure());
        }
        fieldInfo.write(obj, value);
    }

    private void initClass() {
        if ((fieldInfo.modifiers() & ModifiersInfo.STATIC) != 0) {
            declaringClass.initialize();
        }
    }

    private void checkInstance(Object obj) {
        if ((fieldInfo.modifiers() & ModifiersInfo.STATIC) == 0) {
            if (obj == null) {
                throw new NullPointerException();
            }
            if (!declaringClass.isInstance(obj)) {
                throw new TIllegalArgumentException();
            }
        }
    }

    public void checkGetAccess() throws TIllegalAccessException {
    }

    public void checkSetAccess() throws TIllegalAccessException {
    }

    @Override
    public TAnnotation[] getAnnotations() {
        return getDeclaredAnnotations();
    }

    @Override
    public TAnnotation[] getDeclaredAnnotations() {
        if (declaredAnnotations == null) {
            var reflectionInfo = fieldInfo.reflection();
            if (reflectionInfo != null) {
                var count = reflectionInfo.annotationCount();
                declaredAnnotations = new TAnnotation[count];
                for (var i = 0; i < count; ++i) {
                    declaredAnnotations[i] = (TAnnotation) AnnotationInfoUtil.createAnnotation(
                            reflectionInfo.annotation(i));
                }
            } else {
                declaredAnnotations = new TAnnotation[0];
            }
        }
        return declaredAnnotations.clone();
    }
}
