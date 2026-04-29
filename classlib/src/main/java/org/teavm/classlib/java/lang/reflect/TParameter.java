/*
 *  Copyright 2025 konsoletyper.
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

import java.util.HashMap;
import java.util.Map;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.interop.Rename;

/**
 * Information about method parameters. Provides name and modifier information
 * about method and constructor parameters as defined in the class file.
 */
public class TParameter extends TObject {
    private final String name;
    private final int modifiers;
    private final TExecutable executable;
    private final int index;

    TParameter(String name, int modifiers, TExecutable executable, int index) {
        this.name = name;
        this.modifiers = modifiers;
        this.executable = executable;
        this.index = index;
    }

    public boolean equals(Object obj) {
        if (obj instanceof TParameter) {
            TParameter other = (TParameter) obj;
            return other.executable.equals(executable) && other.index == index;
        }
        return false;
    }

    public int hashCode() {
        return executable.hashCode() ^ index;
    }

    public boolean isNamePresent() {
        return name != null;
    }

    public String getName() {
        if (name == null) {
            return "arg" + index;
        }
        return name;
    }

    public TExecutable getDeclaringExecutable() {
        return executable;
    }

    public int getModifiers() {
        return modifiers;
    }

    public boolean isImplicit() {
        return (modifiers & 0x0010) != 0;
    }

    public boolean isSynthetic() {
        return (modifiers & 0x1000) != 0;
    }

    public boolean isVarArgs() {
        return (modifiers & 0x0080) != 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String typeName = getTypeName();
        sb.append(typeName).append(" ").append(getName());
        return sb.toString();
    }

    private String getTypeName() {
        return "java.lang.Object";
    }

    public Class<?> getType() {
        return Object.class;
    }

    public java.lang.reflect.AnnotatedType getAnnotatedType() {
        return null;
    }
}
