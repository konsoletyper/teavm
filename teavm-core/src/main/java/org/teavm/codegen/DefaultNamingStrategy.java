/*
 *  Copyright 2011 Alexey Andreev.
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
package org.teavm.codegen;

import java.util.HashMap;
import java.util.Map;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
public class DefaultNamingStrategy implements NamingStrategy {
    private AliasProvider aliasProvider;
    private ClassHolderSource classSource;
    private Map<String, String> aliases = new HashMap<>();
    private Map<String, String> privateAliases = new HashMap<>();
    private Map<String, String> classAliases = new HashMap<>();
    private Map<String, String> fieldAliases = new HashMap<>();

    public DefaultNamingStrategy(AliasProvider aliasProvider, ClassHolderSource classSource) {
        this.aliasProvider = aliasProvider;
        this.classSource = classSource;
    }

    @Override
    public String getNameFor(String cls) {
        String name = classAliases.get(cls);
        if (name == null) {
            name = aliasProvider.getAlias(cls);
            classAliases.put(cls, name);
        }
        return name;
    }

    @Override
    public String getNameFor(String cls, MethodDescriptor method) {
        if (method.getName().equals("<clinit>")) {
            return "$clinit";
        }
        ClassHolder clsHolder = classSource.getClassHolder(cls);
        MethodHolder methodHolder = clsHolder.getMethod(method);
        if (methodHolder.getModifiers().contains(ElementModifier.STATIC) ||
                method.getName().equals("<init>") ||
                methodHolder.getLevel() == AccessLevel.PRIVATE) {
            String key = cls + "#" + method.toString();
            String alias = privateAliases.get(key);
            if (alias == null) {
                alias = aliasProvider.getAlias(cls, method);
                privateAliases.put(key, alias);
            }
            return alias;
        } else {
            String key = method.toString();
            String alias = aliases.get(key);
            if (alias == null) {
                alias = aliasProvider.getAlias(cls, method);
                aliases.put(key, alias);
            }
            return alias;
        }
    }

    @Override
    public String getNameFor(String cls, String field) {
        String realCls = getRealFieldOwner(cls, field);
        if (!realCls.equals(cls)) {
            String alias = getNameFor(realCls, field);
            fieldAliases.put(cls + "#" + field, alias);
            return alias;
        } else {
            String key = realCls + "#" + field;
            String alias = fieldAliases.get(key);
            if (alias == null) {
                alias = aliasProvider.getAlias(field);
                fieldAliases.put(key, alias);
            }
            return alias;
        }
    }

    private String getRealFieldOwner(String cls, String field) {
        String initialCls = cls;
        while (!fieldExists(cls, field)) {
            ClassHolder clsHolder = classSource.getClassHolder(cls);
            cls = clsHolder.getParent();
            if (cls == null) {
                throw new IllegalArgumentException("Field not found: " +
                        initialCls + "." + field);
            }
        }
        return cls;
    }

    private boolean fieldExists(String cls, String field) {
        ClassHolder classHolder = classSource.getClassHolder(cls);
        return classHolder.getField(field) != null;
    }
}
