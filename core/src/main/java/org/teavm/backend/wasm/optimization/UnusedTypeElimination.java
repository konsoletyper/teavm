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
package org.teavm.backend.wasm.optimization;

import java.util.HashSet;
import java.util.Set;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmCompositeType;
import org.teavm.backend.wasm.model.WasmCompositeTypeVisitor;
import org.teavm.backend.wasm.model.WasmDefaultCompositeTypeVisitor;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmStorageType;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayCopy;
import org.teavm.backend.wasm.model.expression.WasmArrayGet;
import org.teavm.backend.wasm.model.expression.WasmArraySet;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmDefaultExpressionVisitor;
import org.teavm.backend.wasm.model.expression.WasmExpressionVisitor;
import org.teavm.backend.wasm.model.expression.WasmFunctionReference;
import org.teavm.backend.wasm.model.expression.WasmIndirectCall;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructNew;
import org.teavm.backend.wasm.model.expression.WasmStructNewDefault;
import org.teavm.backend.wasm.model.expression.WasmStructSet;

public class UnusedTypeElimination {
    private WasmModule module;
    private Set<WasmCompositeType> usedTypes = new HashSet<>();

    public UnusedTypeElimination(WasmModule module) {
        this.module = module;
    }

    public void apply() {
        collect();
        module.types.removeIf(type -> !usedTypes.contains(type));
    }

    private void collect() {
        for (var function : module.functions) {
            useFrom(function);
        }
        for (var tag : module.tags) {
            use(tag.getType());
        }
        for (var global : module.globals) {
            use(global.getType());
        }
    }

    private void useFrom(WasmFunction function) {
        use(function.getType());
        for (var local : function.getLocalVariables()) {
            use(local.getType());
        }
        for (var part : function.getBody()) {
            part.acceptVisitor(exprVisitor);
        }
    }

    private void use(WasmType type) {
        if (type instanceof WasmType.CompositeReference) {
            use(((WasmType.CompositeReference) type).composite);
        }
    }

    private void use(WasmCompositeType type) {
        if (!usedTypes.add(type)) {
            return;
        }
        type.acceptVisitor(typeVisitor);
    }

    private WasmExpressionVisitor exprVisitor = new WasmDefaultExpressionVisitor() {
        @Override
        public void visit(WasmIndirectCall expression) {
            super.visit(expression);
            use(expression.getType());
        }

        @Override
        public void visit(WasmCast expression) {
            super.visit(expression);
            use(expression.getTargetType());
        }


        @Override
        public void visit(WasmArrayGet expression) {
            super.visit(expression);
            use(expression.getType());
        }

        @Override
        public void visit(WasmArraySet expression) {
            super.visit(expression);
            use(expression.getType());
        }

        @Override
        public void visit(WasmStructNew expression) {
            super.visit(expression);
            use(expression.getType());
        }

        @Override
        public void visit(WasmStructNewDefault expression) {
            super.visit(expression);
            use(expression.getType());
        }

        @Override
        public void visit(WasmStructGet expression) {
            super.visit(expression);
            use(expression.getType());
        }

        @Override
        public void visit(WasmStructSet expression) {
            super.visit(expression);
            use(expression.getType());
        }

        @Override
        public void visit(WasmNullConstant expression) {
            super.visit(expression);
            use(expression.getType());
        }

        @Override
        public void visit(WasmFunctionReference expression) {
            super.visit(expression);
            useFrom(expression.getFunction());
        }

        @Override
        public void visit(WasmCallReference expression) {
            super.visit(expression);
            use(expression.getType());
        }

        @Override
        public void visit(WasmArrayCopy expression) {
            super.visit(expression);
            use(expression.getSourceArrayType());
            use(expression.getTargetArrayType());
        }
    };

    private WasmCompositeTypeVisitor typeVisitor = new WasmDefaultCompositeTypeVisitor() {
        @Override
        public void visit(WasmStructure type) {
            for (var field : type.getFields()) {
                visit(field);
            }
        }

        @Override
        public void visit(WasmArray type) {
            visit(type.getElementType());
        }

        @Override
        public void visit(WasmFunctionType type) {
            if (type.getReturnType() != null) {
                visit(type.getReturnType());
            }
            for (var parameter : type.getParameterTypes()) {
                visit(parameter);
            }
        }

        private void visit(WasmStorageType type) {
            if (type instanceof WasmStorageType.Regular) {
                visit(((WasmStorageType.Regular) type).type);
            }
        }

        private void visit(WasmType type) {
            if (type instanceof WasmType.CompositeReference) {
                use(((WasmType.CompositeReference) type).composite);
            }
        }
    };
}
