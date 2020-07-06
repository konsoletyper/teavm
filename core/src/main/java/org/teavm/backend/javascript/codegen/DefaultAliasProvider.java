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

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

public class DefaultAliasProvider implements AliasProvider {
    private int topLevelAliasLimit;
    private final Map<String, ScopedName> classAliases = new HashMap<>();
    private final Set<String> knownAliases = new HashSet<>(200, 0.5f);
    private final ObjectIntMap<String> knowAliasesCounter = new ObjectIntHashMap<>();
    private final Set<String> knownScopedAliases = new HashSet<>(200, 0.5f);
    private final ObjectIntMap<String> knowScopedAliasesCounter = new ObjectIntHashMap<>();
    private final Set<String> knownVirtualAliases = new HashSet<>(200, 0.5f);
    private final ObjectIntMap<String> knowVirtualAliasesCounter = new ObjectIntHashMap<>();

    public DefaultAliasProvider(int topLevelAliasLimit) {
        this.topLevelAliasLimit = topLevelAliasLimit;
    }

    @Override
    public ScopedName getClassAlias(String cls) {
        return classAliases.computeIfAbsent(cls, key -> makeUniqueTopLevel(suggestAliasForClass(key)));
    }

    private static String suggestAliasForClass(String cls) {
        StringBuilder alias = new StringBuilder();
        int lastIndex = 0;
        while (true) {
            int index = cls.indexOf('.', lastIndex);
            if (index == -1) {
                if (lastIndex > 0) {
                    alias.append("_");
                }
                alias.append(cls.substring(lastIndex));
                break;
            } else {
                if (index > lastIndex) {
                    alias.append(cls.charAt(lastIndex));
                }
                lastIndex = index + 1;
            }
        }

        for (int i = 1; i < alias.length(); ++i) {
            char c = alias.charAt(i);
            if (!Character.isJavaIdentifierPart(c)) {
                alias.setCharAt(i, '_');
            }
        }

        if (!Character.isJavaIdentifierStart(alias.charAt(0))) {
            alias.setCharAt(0, '_');
        }

        return alias.toString();
    }

    @Override
    public String getMethodAlias(MethodDescriptor method) {
        String alias = method.getName();
        switch (alias) {
            case "<init>":
                alias = "$_init_";
                break;
            case "<clinit>":
                alias = "$_clinit_";
                break;
            default:
                alias = "$" + alias;
                break;
        }
        return makeUnique(knownVirtualAliases, knowVirtualAliasesCounter, alias);
    }

    @Override
    public ScopedName getStaticMethodAlias(MethodReference method) {
        String suggested = method.getDescriptor().getName();
        switch (suggested) {
            case "<init>":
                suggested = "_init_";
                break;
            case "<clinit>":
                suggested = "_clinit_";
                break;
        }

        return makeUniqueTopLevel(getClassAlias(method.getClassName()).value + "_" + suggested);
    }

    @Override
    public String getFieldAlias(FieldReference field) {
        return makeUnique(knownVirtualAliases, knowVirtualAliasesCounter, "$" + field.getFieldName());
    }

    @Override
    public ScopedName getStaticFieldAlias(FieldReference field) {
        return makeUniqueTopLevel(getClassAlias(field.getClassName()).value + "_" + field.getFieldName());
    }

    @Override
    public String getFunctionAlias(String name) {
        return name;
    }

    @Override
    public ScopedName getClassInitAlias(String className) {
        return makeUniqueTopLevel(suggestAliasForClass(className) + "_$callClinit");
    }

    @Override
    public String getScopeAlias() {
        return makeUnique(knownAliases, knowAliasesCounter, "$java");
    }

    private ScopedName makeUniqueTopLevel(String suggested) {
        if (knownAliases.size() < topLevelAliasLimit) {
            return new ScopedName(false, makeUnique(knownAliases, knowAliasesCounter, suggested));
        } else {
            return new ScopedName(true, makeUnique(knownScopedAliases, knowScopedAliasesCounter, suggested));
        }
    }

    private String sanitize(String s) {
        if (s.isEmpty()) {
            return "_";
        }
        boolean changed = false;
        StringBuilder sb = new StringBuilder(s.length());
        char c = s.charAt(0);
        if (isIdentifierStart(c)) {
            sb.append(c);
        } else {
            sb.append('_');
            changed = true;
        }
        for (int i = 1; i < s.length(); ++i) {
            c = s.charAt(i);
            if (isIdentifierPart(c)) {
                sb.append(c);
            } else {
                sb.append('_');
                changed = true;
            }
        }
        return changed ? sb.toString() : s;
    }

    private static boolean isIdentifierStart(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_' || c == '$';
    }

    private static boolean isIdentifierPart(char c) {
        return isIdentifierStart(c) || c >= '0' && c <= '9';
    }

    private String makeUnique(Set<String> knowAliases, ObjectIntMap<String> indexMap, String alias) {
        alias = sanitize(alias);
        String uniqueAlias = alias;
        int index = indexMap.get(alias);
        if (index > 0) {
            uniqueAlias = alias + index++;
        }
        while (!knowAliases.add(uniqueAlias)) {
            uniqueAlias = alias + index++;
        }
        indexMap.put(alias, index);
        return uniqueAlias;
    }
}
