/*
 *  Copyright 2012 Alexey Andreev.
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

import java.util.HashSet;
import java.util.Set;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class DefaultAliasProvider implements AliasProvider {
    private int lastSuffix;
    private int lastVirtualSuffix;
    private Set<String> usedAliases = new HashSet<>();

    @Override
    public String getAlias(String cls) {
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
        alias.append(lastSuffix++);
        return alias.toString();
    }

    @Override
    public String getAlias(MethodDescriptor method) {
        String alias = method.getName();
        if (alias.equals("<init>")) {
            alias = "$init";
        } else if (alias.equals("<clinit>")) {
            alias = "$clinit";
        }
        String result;
        do {
            result = alias + lastVirtualSuffix++;
        } while (!usedAliases.add(result));
        return result;
    }

    @Override
    public String getAlias(MethodReference method) {
        String alias = method.getDescriptor().getName();
        if (alias.equals("<init>")) {
            alias = "$init";
        } else if (alias.equals("<clinit>")) {
            alias = "$clinit";
        }
        return alias + lastSuffix++;
    }

    @Override
    public String getAlias(FieldReference field) {
        String result;
        do {
            result = field.getFieldName() + lastSuffix++;
        } while (!usedAliases.add(result));
        return result;
    }

    @Override
    public String getFunctionAlias(String name) {
        return name;
    }
}
