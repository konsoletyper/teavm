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
package org.teavm.backend.wasm.gc;

import org.teavm.model.ClassHierarchy;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.util.VariableCategoryProvider;

public class WasmGCVariableCategoryProvider implements VariableCategoryProvider {
    private ClassHierarchy hierarchy;
    private PreciseTypeInference inference;

    public WasmGCVariableCategoryProvider(ClassHierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }

    public PreciseTypeInference getTypeInference() {
        return inference;
    }

    @Override
    public Object[] getCategories(Program program, MethodReference method) {
        inference = new PreciseTypeInference(program, method, hierarchy);
        var result = new Object[program.variableCount()];
        for (int i = 0; i < program.variableCount(); ++i) {
            var type = inference.typeOf(program.variableAt(i));
            result[i] = type != null ? type : PreciseTypeInference.OBJECT_TYPE;
        }
        return result;
    }
}
