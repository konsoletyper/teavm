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
    private final Map<String, String> fieldAliases = new HashMap<>();
    private final Map<String, String> staticFieldAliases = new HashMap<>();
    private final Map<String, String> functionAliases = new HashMap<>();

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
    public String getFullNameFor(MethodReference method) throws NamingException {
        return getFullNameFor(method, 'M');
    }

    @Override
    public String getNameForInit(MethodReference method) throws NamingException {
        return getFullNameFor(method, 'I');
    }

    private String getFullNameFor(MethodReference method, char classifier) throws NamingException {
        MethodReference originalMethod = method;
        method = getRealMethod(method);
        if (method == null) {
            throw new NamingException("Can't provide name for method as it was not found: " + originalMethod);
        }

        MethodReference resolvedMethod = method;
        return privateAliases.computeIfAbsent(classifier + method.toString(),
                key -> aliasProvider.getStaticMethodAlias(resolvedMethod));
    }

    @Override
    public String getNameFor(FieldReference field) {
        String realCls = getRealFieldOwner(field.getClassName(), field.getFieldName());
        if (!realCls.equals(field.getClassName())) {
            String alias = getNameFor(new FieldReference(realCls, field.getFieldName()));
            fieldAliases.put(field.getClassName() + "#" + field, alias);
            return alias;
        } else {
            return fieldAliases.computeIfAbsent(realCls + "#" + field, key -> aliasProvider.getFieldAlias(field));
        }
    }

    @Override
    public String getFullNameFor(FieldReference field) throws NamingException {
        String realCls = getRealFieldOwner(field.getClassName(), field.getFieldName());
        if (!realCls.equals(field.getClassName())) {
            String alias = getNameFor(new FieldReference(realCls, field.getFieldName()));
            staticFieldAliases.put(field.getClassName() + "#" + field, alias);
            return alias;
        } else {
            return staticFieldAliases.computeIfAbsent(realCls + "#" + field,
                    key -> aliasProvider.getStaticFieldAlias(field));
        }
    }

    @Override
    public String getNameForFunction(String name) throws NamingException {
        return functionAliases.computeIfAbsent(name, key -> aliasProvider.getFunctionAlias(name));
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
                return new MethodReference(className, method.getDescriptor());
            }
            className = cls.getParent();
        }
        return null;
    }

    private String getRealFieldOwner(String cls, String field) {
        String initialCls = cls;
        while (!fieldExists(cls, field)) {
            ClassReader clsHolder = classSource.get(cls);
            cls = clsHolder.getParent();
            if (cls == null) {
                throw new NamingException("Can't provide name for field as the field not found: "
                        + initialCls + "." + field);
            }
        }
        return cls;
    }

    private boolean fieldExists(String cls, String field) {
        ClassReader classHolder = classSource.get(cls);
        return classHolder.getField(field) != null;
    }
}
