/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.backend.wasm.model.instruction;

import java.util.ArrayList;
import java.util.List;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmBlockType;
import org.teavm.backend.wasm.model.WasmCompositeType;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmNumType;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmTag;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCastCondition;
import org.teavm.backend.wasm.model.expression.WasmExternConversionType;
import org.teavm.backend.wasm.model.expression.WasmFloatBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmFloatType;
import org.teavm.backend.wasm.model.expression.WasmFloatUnaryOperation;
import org.teavm.backend.wasm.model.expression.WasmInt32Subtype;
import org.teavm.backend.wasm.model.expression.WasmInt64Subtype;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIntUnaryOperation;
import org.teavm.backend.wasm.model.expression.WasmNullCondition;
import org.teavm.backend.wasm.model.expression.WasmSignedType;
import org.teavm.model.TextLocation;

public class WasmInstructionBuilder {
    public final WasmTypeInference typeInference = new WasmTypeInference();
    public final WasmInstructionList list;
    private List<TextLocation> locationStack = new ArrayList<>();
    private TextLocation currentLocation;

    public WasmInstructionBuilder(WasmInstructionList list) {
        this.list = list;
    }

    public WasmInstructionBuilder pushLocation(TextLocation location) {
        locationStack.add(currentLocation);
        if (location != null) {
            currentLocation = location;
        }
        return this;
    }

    public WasmInstructionBuilder popLocation() {
        currentLocation = locationStack.remove(locationStack.size() - 1);
        return this;
    }

    public WasmInstructionBuilder unreachable() {
        return add(new WasmUnreachableInstruction());
    }

    public WasmInstructionBuilder block() {
        return block(null, false);
    }

    public WasmInstructionBuilder block(WasmBlockType type) {
        return block(type, false);
    }

    public WasmInstructionBuilder block(WasmType type) {
        return block(type != null ? type.asBlock() : null, false);
    }

    public WasmInstructionBuilder loop() {
        return block(null, true);
    }

    public WasmInstructionBuilder loop(WasmBlockType type) {
        return block(type, true);
    }

    public WasmInstructionBuilder loop(WasmType type) {
        return block(type != null ? type.asBlock() : null, true);
    }

    private WasmInstructionBuilder block(WasmBlockType type, boolean loop) {
        var block = new WasmBlockInstruction(loop);
        block.setType(type);
        add(block);
        var inner = new WasmInstructionBuilder(block.getBody());
        if (type != null) {
            inner.typeInference.typeStack.addAll(type.getInputTypes());
        }
        inner.currentLocation = currentLocation;
        return inner;
    }

    public WasmConditionalInstruction conditional() {
        return conditional((WasmBlockType) null);
    }

    public WasmConditionalInstruction conditional(WasmBlockType type) {
        var cond = new WasmConditionalInstruction();
        cond.setType(type);
        add(cond);
        return cond;
    }

    public WasmConditionalInstruction conditional(WasmType type) {
        return conditional(type != null ? type.asBlock() : null);
    }

    public WasmInstructionBuilder branch(WasmInstructionList target) {
        return add(new WasmBranchInstruction(target));
    }

    public WasmInstructionBuilder branch(WasmInstructionBuilder target) {
        return branch(target.list);
    }

    public WasmInstructionBuilder nullBranch(WasmNullCondition condition, WasmInstructionList target) {
        return add(new WasmNullBranchInstruction(condition, target));
    }

    public WasmInstructionBuilder nullBranch(WasmNullCondition condition, WasmInstructionBuilder target) {
        return nullBranch(condition, target.list);
    }

    public WasmInstructionBuilder castBranch(WasmCastCondition condition, WasmType.Reference sourceType,
            WasmType.Reference targetType, WasmInstructionList target) {
        return add(new WasmCastBranchInstruction(condition, sourceType, targetType, target));
    }

    public WasmInstructionBuilder castBranch(WasmCastCondition condition, WasmType.Reference sourceType,
            WasmType.Reference targetType, WasmInstructionBuilder target) {
        return castBranch(condition, sourceType, targetType, target.list);
    }

    public WasmInstructionBuilder breakTo(WasmInstructionList target) {
        return add(new WasmBreakInstruction(target));
    }

    public WasmInstructionBuilder breakTo(WasmInstructionBuilder target) {
        return breakTo(target.list);
    }

    public WasmSwitchInstruction switch_(WasmInstructionList defaultTarget) {
        var insn = new WasmSwitchInstruction(defaultTarget);
        add(insn);
        return insn;
    }

    public WasmSwitchInstruction switch_(WasmInstructionBuilder defaultTarget) {
        return switch_(defaultTarget.list);
    }

    public WasmInstructionBuilder return_() {
        return add(new WasmReturnInstruction());
    }

    public WasmInstructionBuilder i32Const(int value) {
        return add(new WasmInt32ConstantInstruction(value));
    }

    public WasmInstructionBuilder i64Const(long value) {
        return add(new WasmInt64ConstantInstruction(value));
    }

    public WasmInstructionBuilder f32Const(float value) {
        return add(new WasmFloat32ConstantInstruction(value));
    }

    public WasmInstructionBuilder f64Const(double value) {
        return add(new WasmFloat64ConstantInstruction(value));
    }

    public WasmInstructionBuilder nullConst(WasmType.Reference type) {
        return add(new WasmNullConstantInstruction(type));
    }

    public WasmInstructionBuilder isNull() {
        return add(new WasmIsNullInstruction());
    }

    public WasmInstructionBuilder getLocal(WasmLocal local) {
        if (list.getLast() instanceof WasmSetLocalInstruction) {
            var setLocal = (WasmSetLocalInstruction) list.getLast();
            if (setLocal.getLocal() == local) {
                var tee = new WasmTeeLocalInstruction(local);
                tee.setLocation(list.getLast().getLocation());
                list.getLast().delete();
                list.add(tee);
                typeInference.typeStack.add(local.getType());
                return this;
            }
        }
        return add(new WasmGetLocalInstruction(local));
    }

    public WasmInstructionBuilder setLocal(WasmLocal local) {
        return add(new WasmSetLocalInstruction(local));
    }

    public WasmInstructionBuilder teeLocal(WasmLocal local) {
        return add(new WasmTeeLocalInstruction(local));
    }

    public WasmInstructionBuilder getGlobal(WasmGlobal global) {
        return add(new WasmGetGlobalInstruction(global));
    }

    public WasmInstructionBuilder setGlobal(WasmGlobal global) {
        return add(new WasmSetGlobalInstruction(global));
    }

    public WasmInstructionBuilder intBinary(WasmIntType type, WasmIntBinaryOperation operation) {
        return add(new WasmIntBinaryInstruction(type, operation));
    }

    public WasmInstructionBuilder floatBinary(WasmFloatType type, WasmFloatBinaryOperation operation) {
        return add(new WasmFloatBinaryInstruction(type, operation));
    }

    public WasmInstructionBuilder intUnary(WasmIntType type, WasmIntUnaryOperation operation) {
        return add(new WasmIntUnaryInstruction(type, operation));
    }

    public WasmInstructionBuilder floatUnary(WasmFloatType type, WasmFloatUnaryOperation operation) {
        return add(new WasmFloatUnaryInstruction(type, operation));
    }

    public WasmInstructionBuilder convert(WasmNumType sourceType, WasmNumType targetType, boolean signed) {
        return add(new WasmConversionInstruction(sourceType, targetType, signed));
    }

    public WasmInstructionBuilder nonTrapConvert(WasmNumType sourceType, WasmNumType targetType, boolean signed) {
        var insn = new WasmConversionInstruction(sourceType, targetType, signed);
        insn.setNonTrapping(true);
        return add(insn);
    }

    public WasmInstructionBuilder call(WasmFunction function) {
        return call(function, false);
    }

    public WasmInstructionBuilder call(WasmFunction function, boolean suspend) {
        var insn = new WasmCallInstruction(function);
        insn.setSuspend(suspend);
        return add(insn);
    }

    public WasmInstructionBuilder callIndirect(WasmFunctionType type) {
        return add(new WasmIndirectCallInstruction(type));
    }

    public WasmInstructionBuilder callReference(WasmFunctionType type) {
        return callReference(type, false);
    }

    public WasmInstructionBuilder callReference(WasmFunctionType type, boolean suspend) {
        var insn = new WasmCallReferenceInstruction(type);
        insn.setSuspend(suspend);
        return add(insn);
    }

    public WasmInstructionBuilder drop() {
        if (list.getLast() instanceof WasmGetLocalInstruction) {
            list.getLast().delete();
            typeInference.typeStack.remove(typeInference.typeStack.size() - 1);
            return this;
        } else if (list.getLast() instanceof WasmTeeLocalInstruction) {
            var teeLocal = (WasmTeeLocalInstruction) list.getLast();
            var setLocal = new WasmSetLocalInstruction(teeLocal.getLocal());
            setLocal.setLocation(teeLocal.getLocation());
            list.getLast().delete();
            list.add(setLocal);
            typeInference.typeStack.remove(typeInference.typeStack.size() - 1);
            return this;
        } else {
            return add(new WasmDropInstruction());
        }
    }

    public WasmInstructionBuilder loadI32(int alignment, int offset, WasmInt32Subtype convertFrom) {
        var insn = new WasmLoadInt32Instruction(alignment, convertFrom);
        insn.setOffset(offset);
        return add(insn);
    }

    public WasmInstructionBuilder loadI64(int alignment, int offset, WasmInt64Subtype convertFrom) {
        var insn = new WasmLoadInt64Instruction(alignment, convertFrom);
        insn.setOffset(offset);
        return add(insn);
    }

    public WasmInstructionBuilder loadF32(int alignment, int offset) {
        var insn = new WasmLoadFloat32Instruction(alignment);
        insn.setOffset(offset);
        return add(insn);
    }

    public WasmInstructionBuilder loadF64(int alignment, int offset) {
        var insn = new WasmLoadFloat64Instruction(alignment);
        insn.setOffset(offset);
        return add(insn);
    }

    public WasmInstructionBuilder storeI32(int alignment, int offset, WasmInt32Subtype convertTo) {
        var insn = new WasmStoreInt32Instruction(alignment, convertTo);
        insn.setOffset(offset);
        return add(insn);
    }

    public WasmInstructionBuilder storeI64(int alignment, int offset, WasmInt64Subtype convertTo) {
        var insn = new WasmStoreInt64Instruction(alignment, convertTo);
        insn.setOffset(offset);
        return add(insn);
    }

    public WasmInstructionBuilder storeF32(int alignment, int offset) {
        var insn = new WasmStoreFloat32Instruction(alignment);
        insn.setOffset(offset);
        return add(insn);
    }

    public WasmInstructionBuilder storeF64(int alignment, int offset) {
        var insn = new WasmStoreFloat64Instruction(alignment);
        insn.setOffset(offset);
        return add(insn);
    }

    public WasmInstructionBuilder memoryGrow() {
        return add(new WasmMemoryGrowInstruction());
    }

    public WasmInstructionBuilder fill() {
        return add(new WasmFillInstruction());
    }

    public WasmInstructionBuilder copy() {
        return add(new WasmCopyInstruction());
    }

    public WasmTryInstruction try_(WasmType resultType) {
        var insn = new WasmTryInstruction();
        insn.setType(resultType);
        add(insn);
        return insn;
    }

    public WasmTryInstruction try_() {
        return try_(null);
    }

    public WasmInstructionBuilder throw_(WasmTag tag) {
        return add(new WasmThrowInstruction(tag));
    }

    public WasmInstructionBuilder refEqual() {
        return add(new WasmReferencesEqualInstruction());
    }

    public WasmInstructionBuilder cast(WasmType.Reference targetType) {
        return add(new WasmCastInstruction(targetType));
    }

    public WasmInstructionBuilder cast(WasmCompositeType targetType) {
        return cast(targetType.getReference());
    }

    public WasmInstructionBuilder test(WasmType.Reference testType) {
        return add(new WasmTestInstruction(testType));
    }

    public WasmInstructionBuilder externConvert(WasmExternConversionType type) {
        return add(new WasmExternConversionInstruction(type));
    }

    public WasmInstructionBuilder structNew(WasmStructure type) {
        return add(new WasmStructNewInstruction(type));
    }

    public WasmInstructionBuilder structNewDefault(WasmStructure type) {
        add(new WasmStructNewDefaultInstruction(type));
        return this;
    }

    public WasmInstructionBuilder structGet(WasmStructure type, int fieldIndex) {
        return add(new WasmStructGetInstruction(type, fieldIndex));
    }

    public WasmInstructionBuilder structGet(WasmStructure type, int fieldIndex, WasmSignedType signedType) {
        var insn = new WasmStructGetInstruction(type, fieldIndex);
        insn.setSignedType(signedType);
        return add(insn);
    }

    public WasmInstructionBuilder structSet(WasmStructure type, int fieldIndex) {
        return add(new WasmStructSetInstruction(type, fieldIndex));
    }

    public WasmInstructionBuilder arrayNewDefault(WasmArray type) {
        return add(new WasmArrayNewDefaultInstruction(type));
    }

    public WasmInstructionBuilder arrayNewFixed(WasmArray type, int size) {
        return add(new WasmArrayNewFixedInstruction(type, size));
    }

    public WasmInstructionBuilder arrayGet(WasmArray type) {
        return add(new WasmArrayGetInstruction(type));
    }

    public WasmInstructionBuilder arrayGet(WasmArray type, WasmSignedType signedType) {
        var insn = new WasmArrayGetInstruction(type);
        insn.setSignedType(signedType);
        return add(insn);
    }

    public WasmInstructionBuilder arraySet(WasmArray type) {
        return add(new WasmArraySetInstruction(type));
    }

    public WasmInstructionBuilder arrayLength() {
        return add(new WasmArrayLengthInstruction());
    }

    public WasmInstructionBuilder arrayCopy(WasmArray targetArrayType, WasmArray sourceArrayType) {
        return add(new WasmArrayCopyInstruction(targetArrayType, sourceArrayType));
    }

    public WasmInstructionBuilder funcRef(WasmFunction function) {
        return add(new WasmFunctionReferenceInstruction(function));
    }

    public WasmInstructionBuilder i31Ref() {
        return add(new WasmInt31ReferenceInstruction());
    }

    public WasmInstructionBuilder i31Get(WasmSignedType signedType) {
        return add(new WasmInt31GetInstruction(signedType));
    }

    public WasmInstructionBuilder add(WasmInstruction instruction) {
        if (!isTerminating()) {
            instruction.setLocation(currentLocation);
            list.add(instruction);
            instruction.acceptVisitor(typeInference);
        }
        return this;
    }

    public WasmInstructionBuilder append(WasmFragmentBuilder fragment) {
        fragment.emit(this);
        return this;
    }

    public boolean isTerminating() {
        return list.getLast() != null && list.getLast().isTerminating();
    }

    public WasmInstructionBuilder transferFrom(WasmInstructionBuilder src) {
        if (isTerminating()) {
            return this;
        }
        for (var insn : src.list) {
            insn.acceptVisitor(typeInference);
        }
        list.transferFrom(src.list);
        return this;
    }

    public WasmInstructionBuilder negate() {
        if (!isTerminating()) {
            WasmInstructionUtil.negate(list.getLast());
        }
        return this;
    }
}
