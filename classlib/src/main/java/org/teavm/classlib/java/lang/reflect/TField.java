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

import org.teavm.classlib.java.lang.TClass;
import org.teavm.classlib.java.lang.TIllegalAccessException;
import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.lang.annotation.TAnnotation;
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
        return (TClass<?>) (Object) fieldInfo.type().classObject();
    }

    public TType getGenericType() {
        if (!genericTypeInitialized) {
            genericTypeInitialized = true;
            if (fieldInfo.genericType() != null) {
                genericType = TGenericTypeFactory.create(declaringClass, fieldInfo.genericType());
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

    public Object getWithoutCheck(Object obj) {
        return fieldInfo.reader().read(obj);
    }

    public void set(Object obj, Object value) throws TIllegalArgumentException, TIllegalAccessException {
        checkSetAccess();
        checkInstance(obj);
        setWithoutCheck(obj, value);
    }

    public void setWithoutCheck(Object obj, Object value) {
        fieldInfo.writer().write(obj, value);
    }

    private void checkInstance(Object obj) {
        if ((fieldInfo.modifiers() & ModifiersInfo.STATIC) == 0) {
            if (obj == null) {
                throw new NullPointerException();
            }
            if (!declaringClass.isInstance((TObject) obj)) {
                throw new TIllegalArgumentException();
            }
        }
    }

    public void checkGetAccess() throws TIllegalAccessException {
        if (fieldInfo.reader() == null) {
            throw new TIllegalAccessException();
        }
    }

    public void checkSetAccess() throws TIllegalAccessException {
        if (fieldInfo.reader() == null) {
            throw new TIllegalAccessException();
        }
    }

    @Override
    public TAnnotation[] getAnnotations() {
        return getDeclaredAnnotations();
    }

    @Override
    public TAnnotation[] getDeclaredAnnotations() {
        if (declaredAnnotations == null) {
            var count = fieldInfo.annotationCount();
            declaredAnnotations = new TAnnotation[count];
            for (var i = 0; i < count; ++i) {
                declaredAnnotations[i] = (TAnnotation) fieldInfo.annotation(i).createObject();
            }
        }
        return declaredAnnotations.clone();
    }
}
