/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.backend.javascript.rendering;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.teavm.ast.MethodNode;

public class VariableNameGenerator {
    private boolean minifying;
    private final List<String> cachedVariableNames = new ArrayList<>();
    private int cachedVariableNameLastIndex;
    private MethodNode currentMethod;
    private Set<String> usedVariableNames = new HashSet<>();

    public VariableNameGenerator(boolean minifying) {
        this.minifying = minifying;

    }

    public void setCurrentMethod(MethodNode currentMethod) {
        this.currentMethod = currentMethod;
    }

    public void clear() {
        cachedVariableNames.clear();
        usedVariableNames.clear();
        cachedVariableNameLastIndex = 0;
        init();
    }

    private void init() {
        if (!minifying) {
            usedVariableNames.add("$tmp");
            usedVariableNames.add("$ptr");
            usedVariableNames.add("$thread");
        }
    }

    public String variableName(int index) {
        if (!minifying) {
            while (index >= cachedVariableNames.size()) {
                cachedVariableNames.add(null);
            }
            String name = cachedVariableNames.get(index);
            if (name == null) {
                name = generateVariableName(index);
                cachedVariableNames.set(index, name);
            }
            return name;
        } else {
            while (index >= cachedVariableNames.size()) {
                String name;
                do {
                    name = RenderingUtil.indexToId(cachedVariableNameLastIndex++);
                } while (RenderingUtil.KEYWORDS.contains(name));
                cachedVariableNames.add(name);
            }
            return cachedVariableNames.get(index);
        }
    }

    private String generateVariableName(int index) {
        var variable = currentMethod != null && index < currentMethod.getVariables().size()
                ? currentMethod.getVariables().get(index)
                : null;
        if (variable != null && variable.getName() != null) {
            String result = "$" + RenderingUtil.escapeName(variable.getName());
            if (RenderingUtil.KEYWORDS.contains(result) || !usedVariableNames.add(result)) {
                String base = result;
                int suffix = 0;
                do {
                    result = base + "_" + suffix++;
                } while (!usedVariableNames.add(result));
            }
            return result;
        } else {
            return "var$" + index;
        }
    }
}
