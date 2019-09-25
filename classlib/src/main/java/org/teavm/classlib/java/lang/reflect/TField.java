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

import org.teavm.classlib.impl.reflection.Converter;
import org.teavm.classlib.impl.reflection.Flags;
import org.teavm.classlib.impl.reflection.JSFieldGetter;
import org.teavm.classlib.impl.reflection.JSFieldSetter;
import org.teavm.classlib.java.lang.TClass;
import org.teavm.classlib.java.lang.TIllegalAccessException;
import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformObject;

public class TField extends TAccessibleObject implements TMember {
    private TClass<?> declaringClass;
    private String name;
    private int modifiers;
    private int accessLevel;
    private TClass<?> type;
    private JSFieldGetter getter;
    private JSFieldSetter setter;

    public TField(TClass<?> declaringClass, String name, int modifiers, int accessLevel, TClass<?> type,
            JSFieldGetter getter, JSFieldSetter setter) {
        this.declaringClass = declaringClass;
        this.name = name;
        this.modifiers = modifiers;
        this.accessLevel = accessLevel;
        this.type = type;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public TClass<?> getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getModifiers() {
        return Flags.getModifiers(modifiers, accessLevel);
    }

    public boolean isEnumConstant() {
        return (modifiers & Flags.ENUM) != 0;
    }

    @Override
    public boolean isSynthetic() {
        return (modifiers & Flags.SYNTHETIC) != 0;
    }

    public TClass<?> getType() {
        return type;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TModifier.toString(getModifiers()));
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(getType().getName()).append(' ').append(declaringClass.getName()).append(".").append(name);
        return sb.toString();
    }

    public Object get(Object obj) throws TIllegalArgumentException, TIllegalAccessException {
        if (getter == null) {
            throw new TIllegalAccessException();
        }
        checkInstance(obj);
        PlatformObject result = getter.get(Platform.getPlatformObject(obj));
        return Converter.toJava(result);
    }

    public void set(Object obj, Object value) throws TIllegalArgumentException, TIllegalAccessException {
        if (setter == null) {
            throw new TIllegalAccessException();
        }
        checkInstance(obj);
        setter.set(Platform.getPlatformObject(obj), Converter.fromJava(value));
    }

    private void checkInstance(Object obj) {
        if ((modifiers & Flags.STATIC) == 0) {
            if (obj == null) {
                throw new NullPointerException();
            }
            if (!declaringClass.isInstance((TObject) obj)) {
                throw new TIllegalArgumentException();
            }
        }
    }
}
