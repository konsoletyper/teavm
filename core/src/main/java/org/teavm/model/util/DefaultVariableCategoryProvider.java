/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.model.util;

import org.teavm.model.MethodReference;
import org.teavm.model.Program;

public class DefaultVariableCategoryProvider implements VariableCategoryProvider {
    @Override
    public Object[] getCategories(Program program, MethodReference method) {
        TypeInferer inferer = new TypeInferer();
        inferer.inferTypes(program, method);
        var categories = new Object[program.variableCount()];
        for (int i = 0; i < program.variableCount(); ++i) {
            categories[i] = getCategory(inferer.typeOf(i));
        }
        return categories;
    }

    private int getCategory(VariableType type) {
        if (type == null) {
            return 255;
        }
        switch (type) {
            case INT:
                return 0;
            case LONG:
                return 1;
            case FLOAT:
                return 2;
            case DOUBLE:
                return 3;
            case OBJECT:
                return 4;
            default:
                return 5;
        }
    }
}
