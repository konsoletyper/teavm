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
package org.teavm.backend.wasm.optimization;

import static org.junit.Assert.assertEquals;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.teavm.backend.wasm.disasm.ModelDisassembler;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmArrayConstructor;
import org.teavm.backend.wasm.model.WasmField;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmArrayGet;
import org.teavm.backend.wasm.model.instruction.WasmArraySet;
import org.teavm.backend.wasm.model.instruction.WasmBlock;
import org.teavm.backend.wasm.model.instruction.WasmBranch;
import org.teavm.backend.wasm.model.instruction.WasmCall;
import org.teavm.backend.wasm.model.instruction.WasmCatchClause;
import org.teavm.backend.wasm.model.instruction.WasmDrop;
import org.teavm.backend.wasm.model.instruction.WasmGetGlobal;
import org.teavm.backend.wasm.model.instruction.WasmGetLocal;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmInt32Constant;
import org.teavm.backend.wasm.model.instruction.WasmReturn;
import org.teavm.backend.wasm.model.instruction.WasmSetLocal;
import org.teavm.backend.wasm.model.instruction.WasmStructGet;
import org.teavm.backend.wasm.model.instruction.WasmTeeLocal;
import org.teavm.backend.wasm.model.instruction.WasmTry;

public class WasmArrayConstructionOptimizerTest {
    private WasmArray data = new WasmArray("data", WasmType.INT32.asStorage());
    private WasmStructure struct = new WasmStructure("Array", fields -> {
        fields.add(new WasmField(WasmType.INT32, "header"));
        fields.add(new WasmField(data.getReference(), "data"));
    });
    private WasmArray outerData = new WasmArray("outerData", struct.getReference().asStorage());
    private WasmStructure outerStruct = new WasmStructure("OuterArray", fields -> {
        fields.add(new WasmField(WasmType.INT32, "header"));
        fields.add(new WasmField(outerData.getReference(), "data"));
    });
    private WasmGlobal classInfo = new WasmGlobal("cls", WasmType.INT32);
    private WasmGlobal vt = new WasmGlobal("vt", WasmType.INT32);
    private WasmFunction newArray = function("newArray", struct.getReference(), WasmType.INT32, WasmType.INT32);
    private WasmFunction newOuterArray = function("newOuterArray", outerStruct.getReference(),
            WasmType.INT32, WasmType.INT32);

    {
        newArray.setArrayConstructor(new WasmArrayConstructor(struct, b -> b.drop().getGlobal(vt)));
        newOuterArray.setArrayConstructor(new WasmArrayConstructor(outerStruct, b -> b.drop().getGlobal(vt)));
    }

    private WasmFunction getArrayClass = function("getArrayClass", WasmType.INT32, WasmType.INT32);
    private WasmFunction compute = function("compute", WasmType.INT32);
    private WasmFunction init = function("init", null);
    private WasmFunction consume = function("consume", null, struct.getReference());

    private WasmFunction function = function("f", struct.getReference());
    private WasmLocal size = local(WasmType.INT32, "size");
    private WasmLocal index = local(WasmType.INT32, "index");
    private WasmLocal value = local(WasmType.INT32, "value");
    private WasmLocal array = local(struct.getReference(), "array");
    private WasmLocal arrayData = local(data.getReference(), "arrayData");
    private Code body = new Code(function.getBody());

    @Test
    public void constantElements() {
        body.allocate(3);
        body.element(0, 1).element(1, 2).element(2, 3);
        body.getLocal(array).return_();

        assertOptimized(
                "global.get $vt",
                "i32.const 1",
                "i32.const 2",
                "i32.const 3",
                "array.new_fixed $data 3",
                "struct.new $Array",
                "return"
        );
    }

    @Test
    public void mergedLocalTees() {
        body.i32Const(1).setLocal(index);
        body.i32Const(2).setLocal(size).getGlobal(classInfo).getLocal(size).call(newArray).teeLocal(array);
        body.structGet(struct, 1).teeLocal(arrayData).i32Const(0).i32Const(5).arraySet();
        body.getLocal(arrayData).getLocal(index).i32Const(6).arraySet();
        body.getLocal(array).return_();

        assertOptimized(
                "global.get $vt",
                "i32.const 5",
                "i32.const 6",
                "array.new_fixed $data 2",
                "struct.new $Array",
                "return"
        );
    }

    @Test
    public void notOptimizedWhenFirstElementAfterMergedTeeDoesNotMatch() {
        body.i32Const(2).setLocal(size).getGlobal(classInfo).getLocal(size).call(newArray).teeLocal(array);
        body.structGet(struct, 1).teeLocal(arrayData).i32Const(1).i32Const(5).arraySet();
        body.getLocal(arrayData).i32Const(0).i32Const(6).arraySet();
        body.getLocal(array).return_();

        assertNotOptimized();
    }

    @Test
    public void computedElements() {
        body.allocate(2);
        body.call(compute).setLocal(value);
        body.getLocal(arrayData).i32Const(0).getLocal(value).arraySet();
        body.call(compute).setLocal(value);
        body.getLocal(arrayData).i32Const(1).getLocal(value).arraySet();
        body.getLocal(array).return_();

        assertOptimized(
                "global.get $vt",
                "call $compute",
                "call $compute",
                "array.new_fixed $data 2",
                "struct.new $Array",
                "return"
        );
    }

    @Test
    public void unrelatedInstructionsBetween() {
        body.allocate(2);
        body.call(init);
        body.call(compute).setLocal(value);
        body.call(init);
        body.getLocal(arrayData).i32Const(0).getLocal(value).arraySet();
        body.call(init);
        body.element(1, 5);
        body.getLocal(array).return_();

        assertOptimized(
                "global.get $vt",
                "call $init",
                "call $compute",
                "call $init",
                "call $init",
                "i32.const 5",
                "array.new_fixed $data 2",
                "struct.new $Array",
                "return"
        );
    }

    @Test
    public void objectArrayHeader() {
        newArray.setArrayConstructor(new WasmArrayConstructor(struct, b -> b.call(getArrayClass)));
        body.allocate(1);
        body.element(0, 7);
        body.getLocal(array).return_();

        new WasmInstructionsOptimizer().optimize(function);
        new WasmDeadStoreOptimizer().optimize(function);
        assertCode(
                "global.get $cls",
                "call $getArrayClass",
                "i32.const 7",
                "array.new_fixed $data 1",
                "struct.new $Array",
                "return"
        );
    }

    @Test
    public void arrayUsedAfterwards() {
        body.allocate(1);
        body.element(0, 7);
        body.getLocal(array).call(consume);
        body.getLocal(array).return_();

        assertOptimized(
                "global.get $vt",
                "i32.const 7",
                "array.new_fixed $data 1",
                "struct.new $Array",
                "local.tee $array",
                "call $consume",
                "local.get $array",
                "return"
        );
    }

    @Test
    public void nestedArrays() {
        var outer = local(outerStruct.getReference(), "outer");
        var outerArrayData = local(outerData.getReference(), "outerData");
        body.i32Const(2).setLocal(size).getGlobal(classInfo).getLocal(size).call(newOuterArray).setLocal(outer);
        body.getLocal(outer).structGet(outerStruct, 1).setLocal(outerArrayData);
        body.allocate(1);
        body.element(0, 1);
        body.getLocal(outerArrayData).i32Const(0).getLocal(array).arraySet(outerData);
        body.allocate(1);
        body.element(0, 2);
        body.getLocal(outerArrayData).i32Const(1).getLocal(array).arraySet(outerData);
        body.getLocal(outer).drop();
        body.getLocal(array).return_();

        assertOptimized(
                "global.get $vt",
                "global.get $vt",
                "i32.const 1",
                "array.new_fixed $data 1",
                "struct.new $Array",
                "global.get $vt",
                "i32.const 2",
                "array.new_fixed $data 1",
                "struct.new $Array",
                "local.tee $array",
                "array.new_fixed $outerData 2",
                "struct.new $OuterArray",
                "drop",
                "local.get $array",
                "return"
        );
    }

    @Test
    public void constantLocalsReadLaterAreKept() {
        body.allocate(1);
        body.element(0, 7);
        body.getLocal(size).getLocal(index).call(newArray).drop();
        body.getLocal(array).return_();

        assertOptimized(
                "i32.const 1",
                "global.get $vt",
                "i32.const 0",
                "local.set $index",
                "i32.const 7",
                "array.new_fixed $data 1",
                "struct.new $Array",
                "local.set $array",
                "local.get $index",
                "call $newArray",
                "drop",
                "local.get $array",
                "return"
        );
    }

    @Test
    public void notOptimizedWhenTooLong() {
        body.allocate(10001);
        for (var i = 0; i < 10001; ++i) {
            body.element(i, i);
        }
        body.getLocal(array).return_();

        assertNotOptimized();
    }

    @Test
    public void notOptimizedWhenArrayReadBetween() {
        body.allocate(2);
        body.element(0, 1);
        body.getLocal(array).call(consume);
        body.element(1, 2);
        body.getLocal(array).return_();

        assertNotOptimized();
    }

    @Test
    public void dataReadAfterwards() {
        body.allocate(1);
        body.element(0, 1);
        body.getLocal(arrayData).i32Const(0).arrayGet().drop();
        body.getLocal(array).return_();

        assertOptimized(
                "global.get $vt",
                "i32.const 1",
                "array.new_fixed $data 1",
                "local.tee $arrayData",
                "struct.new $Array",
                "local.get $arrayData",
                "i32.const 0",
                "array.get $data",
                "drop",
                "return"
        );
    }

    @Test
    public void notOptimizedWhenElementMissing() {
        body.allocate(3);
        body.element(0, 1).element(1, 2);
        body.getLocal(array).return_();

        assertNotOptimized();
    }

    @Test
    public void notOptimizedWhenElementsOutOfOrder() {
        body.allocate(2);
        body.element(1, 1).element(0, 2);
        body.getLocal(array).return_();

        assertNotOptimized();
    }

    @Test
    public void notOptimizedWhenSizeIsNotConstant() {
        body.call(compute).setLocal(size).getGlobal(classInfo).getLocal(size).call(newArray).setLocal(array);
        body.unwrap();
        body.element(0, 1);
        body.getLocal(array).return_();

        assertNotOptimized();
    }

    @Test
    public void notOptimizedWhenIndexIsOverwrittenInNestedBlock() {
        body.allocate(1);
        body.i32Const(0).setLocal(index);
        body.block().call(compute).setLocal(index);
        body.getLocal(arrayData).getLocal(index).i32Const(1).arraySet();
        body.getLocal(array).return_();

        assertNotOptimized();
    }

    @Test
    public void notOptimizedWhenLeavingListBetween() {
        var block = body.block();
        block.allocate(2);
        block.element(0, 1);
        block.call(compute).branch(block);
        block.element(1, 2);
        body.getLocal(array).return_();

        assertNotOptimized();
    }

    @Test
    public void notOptimizedWhenBranchingOutOfNestedBlockBetween() {
        var block = body.block();
        block.allocate(2);
        block.element(0, 1);
        block.block().call(compute).branch(block);
        block.element(1, 2);
        body.getLocal(array).return_();

        assertNotOptimized();
    }

    @Test
    public void branchInsideNestedBlockAllowed() {
        body.allocate(2);
        body.element(0, 1);
        var inner = body.block();
        inner.call(compute).branch(inner).call(init);
        body.element(1, 2);
        body.getLocal(array).return_();

        assertOptimized(
                "global.get $vt",
                "i32.const 1",
                "call $compute",
                "i32.eqz",
                "if $label_0",
                "  call $init",
                "end (; $label_0 ;)",
                "i32.const 2",
                "array.new_fixed $data 2",
                "struct.new $Array",
                "return"
        );
    }

    @Test
    public void notOptimizedWhenMayThrowInsideTry() {
        var handler = body.block();
        var tryInsn = new WasmTry();
        tryInsn.getCatches().add(new WasmCatchClause(null, false, handler.list));
        handler.add(tryInsn);
        var tryBody = new Code(tryInsn.getBody());
        tryBody.allocate(2);
        tryBody.element(0, 1);
        tryBody.call(init);
        tryBody.element(1, 2);
        body.getLocal(array).return_();

        assertNotOptimized();
    }

    @Test
    public void optimizedInsideTryWithoutCalls() {
        var handler = body.block();
        var tryInsn = new WasmTry();
        tryInsn.getCatches().add(new WasmCatchClause(null, false, handler.list));
        handler.add(tryInsn);
        var tryBody = new Code(tryInsn.getBody());
        tryBody.allocate(2);
        tryBody.element(0, 1).element(1, 2);
        body.getLocal(array).return_();

        assertOptimized(
                "block $label_0",
                "  try_table $label_1",
                "    (catch_all $label_0)",
                "    global.get $vt",
                "    i32.const 1",
                "    i32.const 2",
                "    array.new_fixed $data 2",
                "    struct.new $Array",
                "    local.set $array",
                "  end (; $label_1 ;)",
                "end (; $label_0 ;)",
                "local.get $array",
                "return"
        );
    }

    private static WasmFunction function(String name, WasmType returnType, WasmType... parameterTypes) {
        var function = new WasmFunction(new WasmFunctionType(name, returnType, List.of(parameterTypes)));
        function.setName(name);
        return function;
    }

    private WasmLocal local(WasmType type, String name) {
        var local = new WasmLocal(type, name);
        function.add(local);
        return local;
    }

    private void assertOptimized(String... expected) {
        new WasmInstructionsOptimizer().optimize(function);
        new WasmDeadStoreOptimizer().optimize(function);
        assertCode(expected);
    }

    private void assertNotOptimized() {
        new WasmInstructionsOptimizer().optimize(function);
        var actual = ModelDisassembler.bodyToString(function);
        assertEquals(false, actual.contains("array.new_fixed"));
    }

    private void assertCode(String... expected) {
        var actual = ModelDisassembler.bodyToString(function).lines().collect(Collectors.joining("\n"));
        assertEquals(String.join("\n", expected), actual);
    }

    private class Code {
        final WasmInstructionList list;

        Code(WasmInstructionList list) {
            this.list = list;
        }

        Code add(WasmInstruction insn) {
            list.add(insn);
            return this;
        }

        Code i32Const(int value) {
            return add(new WasmInt32Constant(value));
        }

        Code getLocal(WasmLocal local) {
            return add(new WasmGetLocal(local));
        }

        Code setLocal(WasmLocal local) {
            return add(new WasmSetLocal(local));
        }

        Code teeLocal(WasmLocal local) {
            return add(new WasmTeeLocal(local));
        }

        Code getGlobal(WasmGlobal global) {
            return add(new WasmGetGlobal(global));
        }

        Code structGet(WasmStructure type, int field) {
            return add(new WasmStructGet(type, field));
        }

        Code arrayGet() {
            return add(new WasmArrayGet(data));
        }

        Code arraySet() {
            return arraySet(data);
        }

        Code arraySet(WasmArray type) {
            return add(new WasmArraySet(type));
        }

        Code drop() {
            return add(new WasmDrop());
        }

        Code call(WasmFunction function) {
            return add(new WasmCall(function));
        }

        Code return_() {
            return add(new WasmReturn());
        }

        Code branch(Code target) {
            return add(new WasmBranch(target.list));
        }

        Code block() {
            var block = new WasmBlock(false);
            add(block);
            return new Code(block.getBody());
        }

        // Mimics code generator: constants are spilled to locals before use
        Code allocate(int length) {
            i32Const(length).setLocal(size).getGlobal(classInfo).getLocal(size).call(newArray).setLocal(array);
            return unwrap();
        }

        Code unwrap() {
            return getLocal(array).structGet(struct, 1).setLocal(arrayData);
        }

        Code element(int elementIndex, int elementValue) {
            i32Const(elementIndex).setLocal(index);
            i32Const(elementValue).setLocal(value);
            return getLocal(arrayData).getLocal(index).getLocal(value).arraySet();
        }
    }
}
