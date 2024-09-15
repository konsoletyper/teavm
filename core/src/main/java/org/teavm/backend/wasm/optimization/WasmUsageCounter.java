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

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmCompositeType;
import org.teavm.backend.wasm.model.WasmCompositeTypeVisitor;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayCopy;
import org.teavm.backend.wasm.model.expression.WasmArrayGet;
import org.teavm.backend.wasm.model.expression.WasmArrayNewDefault;
import org.teavm.backend.wasm.model.expression.WasmArrayNewFixed;
import org.teavm.backend.wasm.model.expression.WasmArraySet;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmCastBranch;
import org.teavm.backend.wasm.model.expression.WasmDefaultExpressionVisitor;
import org.teavm.backend.wasm.model.expression.WasmFunctionReference;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmIndirectCall;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructNew;
import org.teavm.backend.wasm.model.expression.WasmStructNewDefault;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.backend.wasm.model.expression.WasmTest;

public class WasmUsageCounter extends WasmDefaultExpressionVisitor implements WasmCompositeTypeVisitor {
    private ObjectIntMap<WasmFunction> usagesByFunction = new ObjectIntHashMap<>();
    private ObjectIntMap<WasmGlobal> usagesByGlobals = new ObjectIntHashMap<>();
    private ObjectIntMap<WasmCompositeType> usagesByTypes = new ObjectIntHashMap<>();

    public void applyToModule(WasmModule module) {
        for (var type : module.types) {
            type.acceptVisitor(this);
        }
        for (var function : module.functions) {
            for (var part : function.getBody()) {
                part.acceptVisitor(this);
            }
            addUsage(function.getType());
        }
        for (var global : module.globals) {
            global.getInitialValue().acceptVisitor(this);
            addUsage(global.getType());
        }
    }

    public int usages(WasmFunction function) {
        return usagesByFunction.get(function);
    }

    public int usages(WasmGlobal global) {
        return usagesByGlobals.get(global);
    }

    public int usages(WasmCompositeType type) {
        return usagesByTypes.get(type);
    }

    @Override
    public void visit(WasmStructure type) {
        for (var field : type.getFields()) {
            addUsage(field.getUnpackedType());
        }
    }

    @Override
    public void visit(WasmArray type) {
        addUsage(type.getElementType().asUnpackedType());
    }

    @Override
    public void visit(WasmFunctionType type) {
        addUsage(type.getReturnType());
        for (var param : type.getParameterTypes()) {
            addUsage(param);
        }
    }

    @Override
    public void visit(WasmCall expression) {
        super.visit(expression);
        addUsage(expression.getFunction());
    }

    @Override
    public void visit(WasmFunctionReference expression) {
        super.visit(expression);
        addUsage(expression.getFunction());
    }

    @Override
    public void visit(WasmGetGlobal expression) {
        super.visit(expression);
        addUsage(expression.getGlobal());
    }

    @Override
    public void visit(WasmSetGlobal expression) {
        super.visit(expression);
        addUsage(expression.getGlobal());
    }

    @Override
    public void visit(WasmBlock expression) {
        super.visit(expression);
        addUsage(expression.getType());
    }

    @Override
    public void visit(WasmCastBranch expression) {
        super.visit(expression);
        addUsage(expression.getSourceType());
        addUsage(expression.getType());
    }

    @Override
    public void visit(WasmCallReference expression) {
        super.visit(expression);
        addUsage(expression.getType());
    }

    @Override
    public void visit(WasmIndirectCall expression) {
        super.visit(expression);
        addUsage(expression.getType());
    }

    @Override
    public void visit(WasmCast expression) {
        super.visit(expression);
        addUsage(expression.getTargetType());
    }

    @Override
    public void visit(WasmTest expression) {
        super.visit(expression);
        addUsage(expression.getTestType());
    }

    @Override
    public void visit(WasmStructNew expression) {
        super.visit(expression);
        addUsage(expression.getType());
    }

    @Override
    public void visit(WasmStructNewDefault expression) {
        super.visit(expression);
        addUsage(expression.getType());
    }

    @Override
    public void visit(WasmStructGet expression) {
        super.visit(expression);
        addUsage(expression.getType());
    }

    @Override
    public void visit(WasmStructSet expression) {
        super.visit(expression);
        addUsage(expression.getType());
    }

    @Override
    public void visit(WasmArrayNewDefault expression) {
        super.visit(expression);
        addUsage(expression.getType());
    }

    @Override
    public void visit(WasmArrayNewFixed expression) {
        super.visit(expression);
        addUsage(expression.getType());
    }

    @Override
    public void visit(WasmArrayGet expression) {
        super.visit(expression);
        addUsage(expression.getType());
    }

    @Override
    public void visit(WasmArraySet expression) {
        super.visit(expression);
        addUsage(expression.getType());
    }

    @Override
    public void visit(WasmArrayCopy expression) {
        super.visit(expression);
        addUsage(expression.getSourceArrayType());
        addUsage(expression.getTargetArrayType());
    }

    private void addUsage(WasmType type) {
        if (type instanceof WasmType.CompositeReference) {
            addUsage(((WasmType.CompositeReference) type).composite);
        }
    }

    private void addUsage(WasmFunction function) {
        var index = usagesByFunction.indexOf(function);
        if (index < 0) {
            usagesByFunction.put(function, 1);
        } else {
            usagesByFunction.indexReplace(index, usagesByFunction.indexGet(index) + 1);
        }
    }

    private void addUsage(WasmGlobal function) {
        var index = usagesByGlobals.indexOf(function);
        if (index < 0) {
            usagesByGlobals.put(function, 1);
        } else {
            usagesByGlobals.indexReplace(index, usagesByGlobals.indexGet(index) + 1);
        }
    }

    private void addUsage(WasmCompositeType type) {
        var index = usagesByTypes.indexOf(type);
        if (index < 0) {
            usagesByTypes.put(type, 1);
        } else {
            usagesByTypes.indexReplace(index, usagesByTypes.indexGet(index) + 1);
        }
    }
}
