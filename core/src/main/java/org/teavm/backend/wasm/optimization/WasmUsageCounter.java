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
import org.teavm.backend.wasm.model.WasmBlockType;
import org.teavm.backend.wasm.model.WasmCompositeType;
import org.teavm.backend.wasm.model.WasmCompositeTypeVisitor;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmArrayCopyInstruction;
import org.teavm.backend.wasm.model.instruction.WasmArrayGetInstruction;
import org.teavm.backend.wasm.model.instruction.WasmArrayNewDefaultInstruction;
import org.teavm.backend.wasm.model.instruction.WasmArrayNewFixedInstruction;
import org.teavm.backend.wasm.model.instruction.WasmArraySetInstruction;
import org.teavm.backend.wasm.model.instruction.WasmBlockInstruction;
import org.teavm.backend.wasm.model.instruction.WasmCallInstruction;
import org.teavm.backend.wasm.model.instruction.WasmCallReferenceInstruction;
import org.teavm.backend.wasm.model.instruction.WasmCastBranchInstruction;
import org.teavm.backend.wasm.model.instruction.WasmCastInstruction;
import org.teavm.backend.wasm.model.instruction.WasmDefaultInstructionVisitor;
import org.teavm.backend.wasm.model.instruction.WasmFunctionReferenceInstruction;
import org.teavm.backend.wasm.model.instruction.WasmGetGlobalInstruction;
import org.teavm.backend.wasm.model.instruction.WasmIndirectCallInstruction;
import org.teavm.backend.wasm.model.instruction.WasmSetGlobalInstruction;
import org.teavm.backend.wasm.model.instruction.WasmStructGetInstruction;
import org.teavm.backend.wasm.model.instruction.WasmStructNewDefaultInstruction;
import org.teavm.backend.wasm.model.instruction.WasmStructNewInstruction;
import org.teavm.backend.wasm.model.instruction.WasmStructSetInstruction;
import org.teavm.backend.wasm.model.instruction.WasmTestInstruction;
import org.teavm.backend.wasm.model.instruction.WasmTryInstruction;

public class WasmUsageCounter extends WasmDefaultInstructionVisitor implements WasmCompositeTypeVisitor {
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
            visitMany(global.getInitialValue());
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
        for (var ret : type.getReturnTypes()) {
            addUsage(ret);
        }
        for (var param : type.getParameterTypes()) {
            addUsage(param);
        }
    }

    @Override
    public void visit(WasmCallInstruction instruction) {
        addUsage(instruction.getFunction());
    }

    @Override
    public void visit(WasmFunctionReferenceInstruction instruction) {
        addUsage(instruction.getFunction());
    }

    @Override
    public void visit(WasmGetGlobalInstruction instruction) {
        addUsage(instruction.getGlobal());
    }

    @Override
    public void visit(WasmSetGlobalInstruction instruction) {
        addUsage(instruction.getGlobal());
    }

    @Override
    public void visit(WasmBlockInstruction instruction) {
        super.visit(instruction);
        addUsage(instruction.getType());
    }

    @Override
    public void visit(WasmTryInstruction instruction) {
        super.visit(instruction);
        addUsage(instruction.getType());
    }

    @Override
    public void visit(WasmCastBranchInstruction instruction) {
        addUsage(instruction.getSourceType());
        addUsage(instruction.getTargetType());
    }

    @Override
    public void visit(WasmCallReferenceInstruction instruction) {
        addUsage(instruction.getType());
    }

    @Override
    public void visit(WasmIndirectCallInstruction instruction) {
        addUsage(instruction.getType());
    }

    @Override
    public void visit(WasmCastInstruction instruction) {
        addUsage(instruction.getTargetType());
    }

    @Override
    public void visit(WasmTestInstruction instruction) {
        addUsage(instruction.getTestType());
    }

    @Override
    public void visit(WasmStructNewInstruction instruction) {
        addUsage(instruction.getType());
    }

    @Override
    public void visit(WasmStructNewDefaultInstruction instruction) {
        addUsage(instruction.getType());
    }

    @Override
    public void visit(WasmStructGetInstruction instruction) {
        addUsage(instruction.getType());
    }

    @Override
    public void visit(WasmStructSetInstruction instruction) {
        addUsage(instruction.getType());
    }

    @Override
    public void visit(WasmArrayNewDefaultInstruction instruction) {
        addUsage(instruction.getType());
    }

    @Override
    public void visit(WasmArrayNewFixedInstruction instruction) {
        addUsage(instruction.getType());
    }

    @Override
    public void visit(WasmArrayGetInstruction instruction) {
        addUsage(instruction.getType());
    }

    @Override
    public void visit(WasmArraySetInstruction instruction) {
        addUsage(instruction.getType());
    }

    @Override
    public void visit(WasmArrayCopyInstruction instruction) {
        addUsage(instruction.getSourceArrayType());
        addUsage(instruction.getTargetArrayType());
    }

    private void addUsage(WasmBlockType type) {
        if (type == null) {
            return;
        }
        if (type instanceof WasmBlockType.Function) {
            addUsage(((WasmBlockType.Function) type).ref);
        } else {
            addUsage(((WasmBlockType.Value) type).type);
        }
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
