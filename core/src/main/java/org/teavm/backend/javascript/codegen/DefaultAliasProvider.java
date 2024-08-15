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
import java.util.HashSet;
import java.util.Set;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

public class DefaultAliasProvider implements AliasProvider {
    private final int maxTopLevelNames;
    private final Set<String> knownAliases = new HashSet<>(200, 0.5f);
    private final ObjectIntMap<String> knowAliasesCounter = new ObjectIntHashMap<>();
    private final Set<String> knownInstanceAliases = new HashSet<>(200, 0.5f);
    private final ObjectIntMap<String> knowInstanceAliasesCounter = new ObjectIntHashMap<>();
    private boolean additionalScopeStarted;

    public DefaultAliasProvider(int maxTopLevelNames) {
        this.maxTopLevelNames = maxTopLevelNames;
    }

    @Override
    public ScopedName getClassAlias(String cls) {
        return makeUnique(suggestAliasForClass(cls));
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
        return makeUniqueInstance(alias);
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

        return makeUnique(suggestAliasForClass(method.getClassName()) + "_" + suggested);
    }

    @Override
    public String getFieldAlias(FieldReference field) {
        return makeUniqueInstance("$" + field.getFieldName());
    }

    @Override
    public ScopedName getStaticFieldAlias(FieldReference field) {
        return makeUnique(suggestAliasForClass(field.getClassName()) + "_" + field.getFieldName());
    }

    @Override
    public ScopedName getFunctionAlias(String name) {
        return makeUnique(name);
    }

    @Override
    public ScopedName getClassInitAlias(String className) {
        return makeUnique(suggestAliasForClass(className) + "_$callClinit");
    }

    @Override
    public String getAdditionalScopeName() {
        return makeUnique("$rt_java").name;
    }

    @Override
    public void reserveName(String name) {
    }

    private ScopedName makeUnique(String suggested) {
        suggested = sanitize(suggested);
        if (!additionalScopeStarted && knownAliases.size() >= maxTopLevelNames) {
            additionalScopeStarted = true;
            knownAliases.clear();
            knowAliasesCounter.clear();
        }
        var alias = suggested;
        int index = knowAliasesCounter.get(alias);
        if (index > 0) {
            alias = suggested + index++;
        }
        while (!knownAliases.add(alias)) {
            alias = suggested + index++;
        }
        knowAliasesCounter.put(alias, index);
        return new ScopedName(alias, additionalScopeStarted);
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

    private String makeUniqueInstance(String alias) {
        alias = sanitize(alias);
        String uniqueAlias = alias;
        int index = knowInstanceAliasesCounter.get(alias);
        if (index > 0) {
            uniqueAlias = alias + index++;
        }
        while (!knownInstanceAliases.add(uniqueAlias)) {
            uniqueAlias = alias + index++;
        }
        knowInstanceAliasesCounter.put(alias, index);
        return uniqueAlias;
    }
}
