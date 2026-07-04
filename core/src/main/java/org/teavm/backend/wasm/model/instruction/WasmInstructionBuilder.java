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
import org.teavm.model.TextLocation;

public class WasmInstructionBuilder {
    public final WasmInstructionList list;
    private TextLocation currentLocation;

    public final WasmTypeInference typeInference = null;

    public WasmInstructionBuilder(WasmInstructionList list) {
        this.list = list;
    }

    public void setCurrentLocation(TextLocation currentLocation) {
        this.currentLocation = currentLocation;
    }

    public void pushLocation(TextLocation location) {
        throw new UnsupportedOperationException();
    }

    public void popLocation() {
        throw new UnsupportedOperationException();
    }

    public WasmInstructionBuilder unreachable() {
        return add(new WasmUnreachable());
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
        var block = new WasmBlock(loop);
        block.setType(type);
        add(block);
        var inner = new WasmInstructionBuilder(block.getBody());
        inner.currentLocation = currentLocation;
        return inner;
    }

    public WasmConditional conditional() {
        return conditional((WasmBlockType) null);
    }

    public WasmConditional conditional(WasmBlockType type) {
        var cond = new WasmConditional();
        cond.setType(type);
        add(cond);
        return cond;
    }

    public WasmConditional conditional(WasmType type) {
        return conditional(type != null ? type.asBlock() : null);
    }

    public WasmInstructionBuilder branch(WasmInstructionList target) {
        return add(new WasmBranch(target));
    }

    public WasmInstructionBuilder branch(WasmInstructionBuilder target) {
        return branch(target.list);
    }

    public WasmInstructionBuilder nullBranch(WasmNullCondition condition, WasmInstructionList target) {
        return add(new WasmNullBranch(condition, target));
    }

    public WasmInstructionBuilder nullBranch(WasmNullCondition condition, WasmInstructionBuilder target) {
        return nullBranch(condition, target.list);
    }

    public WasmInstructionBuilder castBranch(WasmCastCondition condition, WasmType.Reference sourceType,
            WasmType.Reference targetType, WasmInstructionList target) {
        return add(new WasmCastBranch(condition, sourceType, targetType, target));
    }

    public WasmInstructionBuilder castBranch(WasmCastCondition condition, WasmType.Reference sourceType,
            WasmType.Reference targetType, WasmInstructionBuilder target) {
        return castBranch(condition, sourceType, targetType, target.list);
    }

    public WasmInstructionBuilder breakTo(WasmInstructionList target) {
        return add(new WasmBreak(target));
    }

    public WasmInstructionBuilder breakTo(WasmInstructionBuilder target) {
        return breakTo(target.list);
    }

    public WasmSwitch switch_(WasmInstructionList defaultTarget) {
        var insn = new WasmSwitch(defaultTarget);
        add(insn);
        return insn;
    }

    public WasmSwitch switch_(WasmInstructionBuilder defaultTarget) {
        return switch_(defaultTarget.list);
    }

    public WasmInstructionBuilder return_() {
        return add(new WasmReturn());
    }

    public WasmInstructionBuilder i32Const(int value) {
        return add(new WasmInt32Constant(value));
    }

    public WasmInstructionBuilder i64Const(long value) {
        return add(new WasmInt64Constant(value));
    }

    public WasmInstructionBuilder f32Const(float value) {
        return add(new WasmFloat32Constant(value));
    }

    public WasmInstructionBuilder f64Const(double value) {
        return add(new WasmFloat64Constant(value));
    }

    public WasmInstructionBuilder nullConst(WasmType.Reference type) {
        return add(new WasmNullConstant(type));
    }

    public WasmInstructionBuilder isNull() {
        return add(new WasmIsNull());
    }

    public WasmInstructionBuilder getLocal(WasmLocal local) {
        if (list.getLast() instanceof WasmSetLocal setLocal) {
            if (setLocal.getLocal() == local) {
                var tee = new WasmTeeLocal(local);
                tee.setLocation(list.getLast().getLocation());
                list.getLast().delete();
                list.add(tee);
                return this;
            }
        }
        return add(new WasmGetLocal(local));
    }

    public WasmInstructionBuilder setLocal(WasmLocal local) {
        return add(new WasmSetLocal(local));
    }

    public WasmInstructionBuilder teeLocal(WasmLocal local) {
        return add(new WasmTeeLocal(local));
    }

    public WasmInstructionBuilder getGlobal(WasmGlobal global) {
        return add(new WasmGetGlobal(global));
    }

    public WasmInstructionBuilder setGlobal(WasmGlobal global) {
        return add(new WasmSetGlobal(global));
    }

    public WasmInstructionBuilder intBinary(WasmIntType type, WasmIntBinaryOperation operation) {
        return add(new WasmIntBinary(type, operation));
    }

    public WasmInstructionBuilder floatBinary(WasmFloatType type, WasmFloatBinaryOperation operation) {
        return add(new WasmFloatBinary(type, operation));
    }

    public WasmInstructionBuilder intUnary(WasmIntType type, WasmIntUnaryOperation operation) {
        return add(new WasmIntUnary(type, operation));
    }

    public WasmInstructionBuilder floatUnary(WasmFloatType type, WasmFloatUnaryOperation operation) {
        return add(new WasmFloatUnary(type, operation));
    }

    public WasmInstructionBuilder convert(WasmNumType sourceType, WasmNumType targetType, boolean signed) {
        return add(new WasmConversion(sourceType, targetType, signed));
    }

    public WasmInstructionBuilder reinterpret(WasmNumType sourceType, WasmNumType targetType) {
        var insn = new WasmConversion(sourceType, targetType, false);
        insn.setReinterpret(true);
        return add(insn);
    }

    public WasmInstructionBuilder nonTrapConvert(WasmNumType sourceType, WasmNumType targetType, boolean signed) {
        var insn = new WasmConversion(sourceType, targetType, signed);
        insn.setNonTrapping(true);
        return add(insn);
    }

    public WasmInstructionBuilder call(WasmFunction function) {
        return call(function, false);
    }

    public WasmInstructionBuilder call(WasmFunction function, boolean suspend) {
        var insn = new WasmCall(function);
        insn.setSuspend(suspend);
        return add(insn);
    }

    public WasmInstructionBuilder callIndirect(WasmFunctionType type) {
        return add(new WasmIndirectCall(type));
    }

    public WasmInstructionBuilder callReference(WasmFunctionType type) {
        return callReference(type, false);
    }

    public WasmInstructionBuilder callReference(WasmFunctionType type, boolean suspend) {
        var insn = new WasmCallReference(type);
        insn.setSuspend(suspend);
        return add(insn);
    }

    public WasmInstructionBuilder drop() {
        if (list.getLast() instanceof WasmGetLocal) {
            list.getLast().delete();
            return this;
        } else if (list.getLast() instanceof WasmTeeLocal teeLocal) {
            var setLocal = new WasmSetLocal(teeLocal.getLocal());
            setLocal.setLocation(teeLocal.getLocation());
            list.getLast().delete();
            list.add(setLocal);
            return this;
        } else {
            return add(new WasmDrop());
        }
    }

    public WasmInstructionBuilder loadI32(int alignment, int offset, WasmInt32Subtype convertFrom) {
        var insn = new WasmLoadInt32(alignment, convertFrom);
        insn.setOffset(offset);
        return add(insn);
    }

    public WasmInstructionBuilder loadI64(int alignment, int offset, WasmInt64Subtype convertFrom) {
        var insn = new WasmLoadInt64(alignment, convertFrom);
        insn.setOffset(offset);
        return add(insn);
    }

    public WasmInstructionBuilder loadF32(int alignment, int offset) {
        var insn = new WasmLoadFloat32(alignment);
        insn.setOffset(offset);
        return add(insn);
    }

    public WasmInstructionBuilder loadF64(int alignment, int offset) {
        var insn = new WasmLoadFloat64(alignment);
        insn.setOffset(offset);
        return add(insn);
    }

    public WasmInstructionBuilder storeI32(int alignment, int offset, WasmInt32Subtype convertTo) {
        var insn = new WasmStoreInt32(alignment, convertTo);
        insn.setOffset(offset);
        return add(insn);
    }

    public WasmInstructionBuilder storeI64(int alignment, int offset, WasmInt64Subtype convertTo) {
        var insn = new WasmStoreInt64(alignment, convertTo);
        insn.setOffset(offset);
        return add(insn);
    }

    public WasmInstructionBuilder storeF32(int alignment, int offset) {
        var insn = new WasmStoreFloat32(alignment);
        insn.setOffset(offset);
        return add(insn);
    }

    public WasmInstructionBuilder storeF64(int alignment, int offset) {
        var insn = new WasmStoreFloat64(alignment);
        insn.setOffset(offset);
        return add(insn);
    }

    public WasmInstructionBuilder memoryGrow() {
        return add(new WasmMemoryGrow());
    }

    public WasmInstructionBuilder fill() {
        return add(new WasmFill());
    }

    public WasmInstructionBuilder copy() {
        return add(new WasmCopy());
    }

    public WasmTry try_(WasmType resultType) {
        var insn = new WasmTry();
        insn.setType(resultType);
        add(insn);
        return insn;
    }

    public WasmTry try_() {
        return try_(null);
    }

    public WasmInstructionBuilder throw_(WasmTag tag) {
        return add(new WasmThrow(tag));
    }

    public WasmInstructionBuilder refEqual() {
        return add(new WasmReferencesEqual());
    }

    public WasmInstructionBuilder cast(WasmType.Reference targetType) {
        return add(new WasmCast(targetType));
    }

    public WasmInstructionBuilder cast(WasmCompositeType targetType) {
        return cast(targetType.getReference());
    }

    public WasmInstructionBuilder test(WasmType.Reference testType) {
        return add(new WasmTest(testType));
    }

    public WasmInstructionBuilder externConvert(WasmExternConversionType type) {
        return add(new WasmExternConversion(type));
    }

    public WasmInstructionBuilder structNew(WasmStructure type) {
        return add(new WasmStructNew(type));
    }

    public WasmInstructionBuilder structNewDefault(WasmStructure type) {
        add(new WasmStructNewDefault(type));
        return this;
    }

    public WasmInstructionBuilder structGet(WasmStructure type, int fieldIndex) {
        return add(new WasmStructGet(type, fieldIndex));
    }

    public WasmInstructionBuilder structGet(WasmStructure type, int fieldIndex, WasmSignedType signedType) {
        var insn = new WasmStructGet(type, fieldIndex);
        insn.setSignedType(signedType);
        return add(insn);
    }

    public WasmInstructionBuilder structSet(WasmStructure type, int fieldIndex) {
        return add(new WasmStructSet(type, fieldIndex));
    }

    public WasmInstructionBuilder arrayNewDefault(WasmArray type) {
        return add(new WasmArrayNewDefault(type));
    }

    public WasmInstructionBuilder arrayNewFixed(WasmArray type, int size) {
        return add(new WasmArrayNewFixed(type, size));
    }

    public WasmInstructionBuilder arrayGet(WasmArray type) {
        return add(new WasmArrayGet(type));
    }

    public WasmInstructionBuilder arrayGet(WasmArray type, WasmSignedType signedType) {
        var insn = new WasmArrayGet(type);
        insn.setSignedType(signedType);
        return add(insn);
    }

    public WasmInstructionBuilder arraySet(WasmArray type) {
        return add(new WasmArraySet(type));
    }

    public WasmInstructionBuilder arrayLength() {
        return add(new WasmArrayLength());
    }

    public WasmInstructionBuilder arrayCopy(WasmArray targetArrayType, WasmArray sourceArrayType) {
        return add(new WasmArrayCopy(targetArrayType, sourceArrayType));
    }

    public WasmInstructionBuilder funcRef(WasmFunction function) {
        return add(new WasmFunctionReference(function));
    }

    public WasmInstructionBuilder i31Ref() {
        return add(new WasmInt31Reference());
    }

    public WasmInstructionBuilder i31Get(WasmSignedType signedType) {
        return add(new WasmInt31Get(signedType));
    }

    public WasmInstructionBuilder add(WasmInstruction instruction) {
        if (!isTerminating()) {
            instruction.setLocation(currentLocation);
            list.add(instruction);
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

    public WasmInstructionBuilder transferFrom(WasmInstructionList src) {
        if (isTerminating()) {
            return this;
        }
        list.transferFrom(src);
        return this;
    }

    public WasmInstructionBuilder transferFrom(WasmInstructionBuilder src) {
        return transferFrom(src.list);
    }

    public WasmInstructionBuilder negate() {
        if (!isTerminating()) {
            WasmInstructionUtil.negate(list.getLast());
        }
        return this;
    }
}
