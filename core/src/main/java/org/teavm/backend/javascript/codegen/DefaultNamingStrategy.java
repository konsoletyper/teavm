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
package org.teavm.backend.javascript.codegen;

import java.util.HashMap;
import java.util.Map;
import org.teavm.model.*;

public class DefaultNamingStrategy implements NamingStrategy {
    private final AliasProvider aliasProvider;
    private final ClassReaderSource classSource;
    private final Map<String, String> aliases = new HashMap<>();
    private final Map<String, String> privateAliases = new HashMap<>();
    private final Map<String, String> classAliases = new HashMap<>();
    private final Map<FieldReference, String> fieldAliases = new HashMap<>();
    private final Map<FieldReference, String> staticFieldAliases = new HashMap<>();
    private final Map<String, String> functionAliases = new HashMap<>();
    private final Map<String, String> classInitAliases = new HashMap<>();

    public DefaultNamingStrategy(AliasProvider aliasProvider, ClassReaderSource classSource) {
        this.aliasProvider = aliasProvider;
        this.classSource = classSource;
    }

    @Override
    public String getNameFor(String cls) {
        return classAliases.computeIfAbsent(cls, key -> aliasProvider.getClassAlias(cls));
    }

    @Override
    public String getNameFor(MethodDescriptor method) {
        String key = method.toString();
        String alias = aliases.get(key);
        if (alias == null) {
            alias = aliasProvider.getMethodAlias(method);
            aliases.put(key, alias);
        }
        return alias;
    }

    @Override
    public String getFullNameFor(MethodReference method) {
        return getFullNameFor(method, 'M');
    }

    @Override
    public String getNameForInit(MethodReference method) {
        return getFullNameFor(method, 'I');
    }

    private String getFullNameFor(MethodReference method, char classifier) {
        MethodReference originalMethod = method;
        method = getRealMethod(method);
        if (method == null) {
            method = originalMethod;
        }

        MethodReference resolvedMethod = method;
        return privateAliases.computeIfAbsent(classifier + method.toString(),
                key -> aliasProvider.getStaticMethodAlias(resolvedMethod));
    }

    @Override
    public String getNameFor(FieldReference field) {
        String alias = fieldAliases.get(field);
        if (alias == null) {
            FieldReference realField = getRealField(field);
            if (realField.equals(field)) {
                alias = aliasProvider.getFieldAlias(realField);
            } else {
                alias = getNameFor(realField);
            }
            fieldAliases.put(field, alias);
        }
        return alias;
    }

    @Override
    public String getFullNameFor(FieldReference field) {
        String alias = staticFieldAliases.get(field);
        if (alias == null) {
            FieldReference realField = getRealField(field);
            if (realField.equals(field)) {
                alias = aliasProvider.getStaticFieldAlias(realField);
            } else {
                alias = getNameFor(realField);
            }
            staticFieldAliases.put(field, alias);
        }
        return alias;
    }

    @Override
    public String getNameForFunction(String name) {
        return functionAliases.computeIfAbsent(name, key -> aliasProvider.getFunctionAlias(key));
    }

    @Override
    public String getNameForClassInit(String className) {
        return classInitAliases.computeIfAbsent(className, key -> aliasProvider.getClassInitAlias(key));
    }

    private MethodReference getRealMethod(MethodReference methodRef) {
        String className = methodRef.getClassName();
        while (className != null) {
            ClassReader cls = classSource.get(className);
            if (cls == null) {
                return null;
            }
            MethodReader method = cls.getMethod(methodRef.getDescriptor());
            if (method != null) {
                if (method.getLevel() == AccessLevel.PRIVATE && !className.equals(methodRef.getClassName())) {
                    return null;
                }
                return method.getReference();
            }
            className = cls.getParent();
        }
        return null;
    }

    private FieldReference getRealField(FieldReference fieldRef) {
        String initialCls = fieldRef.getClassName();
        String cls = fieldRef.getClassName();
        while (cls != null) {
            ClassReader clsReader = classSource.get(cls);
            if (clsReader != null) {
                FieldReader fieldReader = clsReader.getField(fieldRef.getFieldName());
                if (fieldReader != null) {
                    return fieldReader.getReference();
                }
            }
            cls = clsReader.getParent();
        }
        return fieldRef;
    }
}
