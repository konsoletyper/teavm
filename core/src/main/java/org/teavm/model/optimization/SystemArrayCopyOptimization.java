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
package org.teavm.model.optimization;

import java.util.Objects;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.analysis.BaseTypeInference;
import org.teavm.model.instructions.InvokeInstruction;

public class SystemArrayCopyOptimization implements MethodOptimization {
    private static final MethodReference ARRAY_COPY_METHOD = new MethodReference(System.class,
            "arraycopy", Object.class, int.class, Object.class, int.class, int.class, void.class);
    private static final MethodReference FAST_ARRAY_COPY_METHOD = new MethodReference(System.class,
            "fastArraycopy", Object.class, int.class, Object.class, int.class, int.class, void.class);
    private static final ValueType DEFAULT_TYPE = ValueType.object("java.lang.Object");

    @Override
    public boolean optimize(MethodOptimizationContext context, Program program) {
        var typeInference = new TypeInference(program, context.getMethod().getReference());
        var somethingChanged = false;
        for (var block : program.getBasicBlocks()) {
            for (var instruction : block) {
                if (instruction instanceof InvokeInstruction) {
                    var invoke = (InvokeInstruction) instruction;
                    var method = invoke.getMethod();
                    if (method.equals(ARRAY_COPY_METHOD)) {
                        var sourceType = typeInference.typeOf(invoke.getArguments().get(0));
                        var destType = typeInference.typeOf(invoke.getArguments().get(2));
                        if (sourceType instanceof ValueType.Array && destType instanceof ValueType.Array) {
                            if (sourceType.equals(destType)
                                    || context.getHierarchy().isSuperType(destType, sourceType, false)) {
                                invoke.setMethod(FAST_ARRAY_COPY_METHOD);
                                somethingChanged = true;
                            }
                        }
                    }
                }
            }
        }
        return somethingChanged;
    }

    private static class TypeInference extends BaseTypeInference<ValueType> {
        TypeInference(Program program, MethodReference reference) {
            super(program, reference);
        }

        @Override
        public ValueType merge(ValueType a, ValueType b) {
            if (!Objects.equals(a, b)) {
                return DEFAULT_TYPE;
            }
            return a;
        }

        @Override
        public ValueType elementType(ValueType valueType) {
            return valueType instanceof ValueType.Array ? ((ValueType.Array) valueType).getItemType() : DEFAULT_TYPE;
        }

        @Override
        public ValueType nullType() {
            return DEFAULT_TYPE;
        }

        @Override
        public ValueType mapType(ValueType type) {
            return type instanceof ValueType.Array ? type : DEFAULT_TYPE;
        }
    }
}
