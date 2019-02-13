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

import java.util.HashSet;
import java.util.Set;
import org.teavm.backend.javascript.rendering.RenderingUtil;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

public class MinifyingAliasProvider implements AliasProvider {
    private int topLevelAliasLimit;
    private static final String startLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String startVirtualLetters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private int lastSuffix;
    private int lastScopedSuffix;
    private int lastVirtual;
    private final Set<String> usedAliases = new HashSet<>();
    private final Set<String> usedVirtualAliases = new HashSet<>();
    private final Set<String> usedScopedAliases = new HashSet<>();

    public MinifyingAliasProvider(int topLevelAliasLimit) {
        this.topLevelAliasLimit = topLevelAliasLimit;
    }

    @Override
    public String getFieldAlias(FieldReference field) {
        String result;
        do {
            result = RenderingUtil.indexToId(lastVirtual++, startVirtualLetters);
        } while (!usedVirtualAliases.add(result) || RenderingUtil.KEYWORDS.contains(result));
        return result;
    }

    @Override
    public ScopedName getStaticFieldAlias(FieldReference field) {
        return createTopLevelName();
    }

    @Override
    public ScopedName getStaticMethodAlias(MethodReference method) {
        return createTopLevelName();
    }

    @Override
    public String getMethodAlias(MethodDescriptor method) {
        String result;
        do {
            result = RenderingUtil.indexToId(lastVirtual++, startVirtualLetters);
        } while (!usedVirtualAliases.add(result) || RenderingUtil.KEYWORDS.contains(result));
        return result;
    }

    @Override
    public ScopedName getClassAlias(String className) {
        return createTopLevelName();
    }

    @Override
    public String getFunctionAlias(String className) {
        return RenderingUtil.indexToId(lastSuffix++, startLetters);
    }

    @Override
    public ScopedName getClassInitAlias(String className) {
        return createTopLevelName();
    }

    @Override
    public String getScopeAlias() {
        String result;
        do {
            result = RenderingUtil.indexToId(lastSuffix++, startLetters);
        } while (!usedAliases.add(result) || RenderingUtil.KEYWORDS.contains(result));
        return result;
    }

    private ScopedName createTopLevelName() {
        if (usedAliases.size() < topLevelAliasLimit) {
            String result;
            do {
                result = RenderingUtil.indexToId(lastSuffix++, startLetters);
            } while (!usedAliases.add(result) || RenderingUtil.KEYWORDS.contains(result));
            return new ScopedName(false, result);
        } else {
            String result;
            do {
                result = RenderingUtil.indexToId(lastScopedSuffix++, startLetters);
            } while (!usedScopedAliases.add(result) || RenderingUtil.KEYWORDS.contains(result));
            return new ScopedName(true, result);
        }
    }
}
