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

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.teavm.metaprogramming.reflect.ReflectType;
import org.teavm.metaprogramming.reflect.ReflectTypeArgument;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.ElementReader;
import org.teavm.model.GenericValueType;
import org.teavm.model.ValueType;

public class ReflectContext {
    static final ReflectTypeArgument[] EMPTY_TYPE_ARGUMENTS = new ReflectTypeArgument[0];
    static final ReflectType[] EMPTY_TYPES = new ReflectType[0];
    static final Map<String, ReflectTypeVariableImpl> EMPTY_TYPE_VARIABLES = new LinkedHashMap<>();
    private ClassReaderSource classSource;
    private ClassHierarchy hierarchy;
    private Map<ValueType, ReflectClassImpl<?>> classes = new HashMap<>();
    private Map<GenericTypeKey, ReflectType> genericTypes = new HashMap<>();
    private Map<ReflectClassImpl<?>, ReflectType> rawGenericTypes = new HashMap<>();
    private ClassLoader classLoader;

    public ReflectContext(ClassHierarchy hierarchy, ClassLoader classLoader) {
        this.classSource = hierarchy.getClassSource();
        this.hierarchy = hierarchy;
        this.classLoader = classLoader;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public ClassReaderSource getClassSource() {
        return classSource;
    }

    public ClassHierarchy getHierarchy() {
        return hierarchy;
    }

    public ReflectClassImpl<?> getClass(ValueType type) {
        return classes.computeIfAbsent(type, t -> new ReflectClassImpl<>(type, this));
    }

    public ReflectType getGenericType(GenericValueType type, Map<String, ReflectTypeVariableImpl> variableMap) {
        Set<String> variableNameSet = new HashSet<>();
        extractVariables(type, variableNameSet);
        String[] variableNames;
        ReflectTypeVariableImpl[] variables;
        if (variableNameSet.isEmpty()) {
            variableNames = null;
            variables = null;
        } else {
            variableNames = variableNameSet.toArray(new String[0]);
            Arrays.sort(variableNames);
            variables = new ReflectTypeVariableImpl[variableNames.length];
            for (int i = 0; i < variableNames.length; ++i) {
                variables[i] = variableMap.get(variableNames[i]);
            }
        }
        GenericTypeKey key = new GenericTypeKey(variableNames, variables, type);
        return genericTypes.computeIfAbsent(key, k -> createGenericType(type, variableMap));
    }

    public ReflectType getRawGenericType(ReflectClassImpl<?> type) {
        return rawGenericTypes.computeIfAbsent(type, this::createRawGenericType);
    }

    private ReflectType createGenericType(GenericValueType type, Map<String, ReflectTypeVariableImpl> variableMap) {
        if (type == GenericValueType.VOID) {
            return getRawGenericType(getClass(ValueType.VOID));
        } else if (type instanceof GenericValueType.Primitive) {
            return getRawGenericType(getClass(ValueType.primitive(((GenericValueType.Primitive) type).getKind())));
        } else if (type instanceof GenericValueType.Object) {
            GenericValueType.Object obj = (GenericValueType.Object) type;
            return new ReflectParameterizedTypeImpl(this, obj, variableMap);
        } else if (type instanceof GenericValueType.Variable) {
            return variableMap.get(((GenericValueType.Variable) type).getName());
        } else if (type instanceof GenericValueType.Array) {
            return new ReflectArrayTypeImpl(this, (GenericValueType.Array) type, variableMap);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private ReflectType createRawGenericType(ReflectClassImpl<?> type) {
        return new ReflectRawTypeImpl(this, type);
    }

    @SuppressWarnings("unchecked")
    public <T> ReflectClassImpl<T> findClass(Class<T> cls) {
        return (ReflectClassImpl<T>) getClass(ValueType.parse(cls));
    }

    public ReflectClassImpl<?> findClass(String name) {
        if (classSource.get(name) == null) {
            return null;
        }
        return getClass(ValueType.object(name));
    }

    public static int getModifiers(ElementReader element) {
        int modifiers = 0;
        switch (element.getLevel()) {
            case PUBLIC:
                modifiers |= Modifier.PUBLIC;
                break;
            case PROTECTED:
                modifiers |= Modifier.PROTECTED;
                break;
            case PRIVATE:
                modifiers |= Modifier.PRIVATE;
                break;
            case PACKAGE_PRIVATE:
                break;
        }
        Set<ElementModifier> modifierSet = element.readModifiers();
        if (modifierSet.contains(ElementModifier.ABSTRACT)) {
            modifiers |= Modifier.ABSTRACT;
        }
        if (modifierSet.contains(ElementModifier.FINAL)) {
            modifiers |= Modifier.FINAL;
        }
        if (modifierSet.contains(ElementModifier.INTERFACE)) {
            modifiers |= Modifier.INTERFACE;
        }
        if (modifierSet.contains(ElementModifier.NATIVE)) {
            modifiers |= Modifier.NATIVE;
        }
        if (modifierSet.contains(ElementModifier.STATIC)) {
            modifiers |= Modifier.STATIC;
        }
        if (modifierSet.contains(ElementModifier.STRICT)) {
            modifiers |= Modifier.STRICT;
        }
        if (modifierSet.contains(ElementModifier.SYNCHRONIZED)) {
            modifiers |= Modifier.SYNCHRONIZED;
        }
        if (modifierSet.contains(ElementModifier.TRANSIENT)) {
            modifiers |= Modifier.TRANSIENT;
        }
        if (modifierSet.contains(ElementModifier.VOLATILE)) {
            modifiers |= Modifier.VOLATILE;
        }
        return modifiers;
    }

    static void extractVariables(GenericValueType type, Set<String> variables) {
        if (type instanceof GenericValueType.Variable) {
            variables.add(((GenericValueType.Variable) type).getName());
        } else if (type instanceof GenericValueType.Array) {
            extractVariables(((GenericValueType.Array) type).getItemType(), variables);
        } else if (type instanceof GenericValueType.Object) {
            GenericValueType.Object obj = (GenericValueType.Object) type;
            for (GenericValueType.Argument arg : obj.getArguments()) {
                if (arg.getValue() != null) {
                    extractVariables(arg.getValue(), variables);
                }
            }
        }
    }

    static final class GenericTypeKey {
        final String[] variableNames;
        final ReflectTypeVariableImpl[] variables;
        final GenericValueType type;

        GenericTypeKey(String[] variableNames, ReflectTypeVariableImpl[] variables, GenericValueType type) {
            this.variableNames = variableNames;
            this.variables = variables;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof GenericTypeKey)) {
                return false;
            }
            GenericTypeKey that = (GenericTypeKey) o;
            return Arrays.equals(variableNames, that.variableNames)
                    && Arrays.equals(variables, that.variables)
                    && type.equals(that.type);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(type);
            result = 31 * result + Arrays.hashCode(variableNames);
            result = 31 * result + Arrays.hashCode(variables);
            return result;
        }
    }
}
