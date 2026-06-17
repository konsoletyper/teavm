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
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmBlock;
import org.teavm.backend.wasm.model.instruction.WasmBranch;
import org.teavm.backend.wasm.model.instruction.WasmCall;
import org.teavm.backend.wasm.model.instruction.WasmCatchClause;
import org.teavm.backend.wasm.model.instruction.WasmDrop;
import org.teavm.backend.wasm.model.instruction.WasmGetLocal;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmInt32Constant;
import org.teavm.backend.wasm.model.instruction.WasmIntBinary;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIntType;
import org.teavm.backend.wasm.model.instruction.WasmReturn;
import org.teavm.backend.wasm.model.instruction.WasmSetLocal;
import org.teavm.backend.wasm.model.instruction.WasmTeeLocal;
import org.teavm.backend.wasm.model.instruction.WasmTry;
import org.teavm.backend.wasm.model.instruction.WasmUnreachable;

public class WasmDeadStoreOptimizerTest {
    private WasmFunction function = function("f", WasmType.INT32);
    private WasmFunction thrower = function("h", null);
    private Code body = new Code(function.getBody());

    @Test
    public void deadTeeRemoved() {
        var a = local("a");
        body.i32Const(1).teeLocal(a).return_();

        assertOptimized(
                "i32.const 1",
                "return"
        );
        assertLocals();
    }

    @Test
    public void deadSetReplacedByDrop() {
        var a = local("a");
        body.i32Const(1).i32Const(2).add().setLocal(a).i32Const(3).return_();

        assertOptimized(
                "i32.const 1",
                "i32.const 2",
                "i32.add",
                "drop",
                "i32.const 3",
                "return"
        );
        assertLocals();
    }

    @Test
    public void deadSetOfPureValueRemoved() {
        var a = local("a");
        var b = local("b");
        body.i32Const(1).setLocal(a).getLocal(a).setLocal(b).i32Const(2).return_();

        assertOptimized(
                "i32.const 1",
                "local.set $a",
                "i32.const 2",
                "return"
        );
    }

    @Test
    public void liveTeeKept() {
        var a = local("a");
        body.i32Const(1).teeLocal(a).getLocal(a).add().return_();

        assertOptimized(
                "i32.const 1",
                "local.tee $a",
                "local.get $a",
                "i32.add",
                "return"
        );
        assertLocals("a");
    }

    @Test
    public void storeOverwrittenBeforeRead() {
        var a = local("a");
        body.i32Const(1).setLocal(a).i32Const(2).setLocal(a).getLocal(a).return_();

        assertOptimized(
                "i32.const 2",
                "local.set $a",
                "local.get $a",
                "return"
        );
    }

    @Test
    public void storeReadAtBranchTargetKept() {
        var a = local("a");
        var c = local("c");
        var outer = body.block();
        outer.i32Const(1).teeLocal(a).getLocal(c).branch(outer).return_();
        body.getLocal(a).return_();

        assertOptimized(
                "block $label_0",
                "  i32.const 1",
                "  local.tee $a",
                "  local.get $c",
                "  br_if $label_0",
                "  return",
                "end (; $label_0 ;)",
                "local.get $a",
                "return"
        );
    }

    @Test
    public void branchToInnerBlockDoesNotKeepStore() {
        var a = local("a");
        var c = local("c");
        body.i32Const(1).teeLocal(a);
        var inner = body.block();
        inner.getLocal(c).branch(inner).add(new WasmUnreachable());
        body.return_();

        assertOptimized(
                "i32.const 1",
                "block $label_0",
                "  local.get $c",
                "  br_if $label_0",
                "  unreachable",
                "end (; $label_0 ;)",
                "return"
        );
        assertLocals("c");
    }

    @Test
    public void storeReadInExceptionHandlerKept() {
        var a = local("a");
        var handler = body.block();
        var tryInsn = handler.try_();
        tryInsn.getCatches().add(new WasmCatchClause(null, false, handler.list));
        new Code(tryInsn.getBody())
                .i32Const(1).setLocal(a)
                .call(thrower)
                .i32Const(2).return_();
        body.getLocal(a).return_();

        assertOptimized(
                "block $label_0",
                "  try_table $label_1",
                "    (catch_all $label_0)",
                "    i32.const 1",
                "    local.set $a",
                "    call $h",
                "    i32.const 2",
                "    return",
                "  end (; $label_1 ;)",
                "end (; $label_0 ;)",
                "local.get $a",
                "return"
        );
    }

    @Test
    public void storeAfterLastThrowingCallRemoved() {
        var a = local("a");
        var handler = body.block();
        var tryInsn = handler.try_();
        tryInsn.getCatches().add(new WasmCatchClause(null, false, handler.list));
        new Code(tryInsn.getBody())
                .call(thrower)
                .i32Const(1).setLocal(a)
                .i32Const(2).return_();
        body.i32Const(0).return_();

        assertOptimized(
                "block $label_0",
                "  try_table $label_1",
                "    (catch_all $label_0)",
                "    call $h",
                "    i32.const 2",
                "    return",
                "  end (; $label_1 ;)",
                "end (; $label_0 ;)",
                "i32.const 0",
                "return"
        );
    }

    @Test
    public void loopCarriedStoreKept() {
        var i = local("i");
        var c = local("c");
        body.i32Const(0).setLocal(i);
        var loop = body.loop();
        loop.getLocal(i).i32Const(1).add().setLocal(i).getLocal(c).branch(loop);
        body.i32Const(0).return_();

        assertOptimized(
                "i32.const 0",
                "local.set $i",
                "loop $label_0",
                "  local.get $i",
                "  i32.const 1",
                "  i32.add",
                "  local.set $i",
                "  local.get $c",
                "  br_if $label_0",
                "end (; $label_0 ;)",
                "i32.const 0",
                "return"
        );
    }

    @Test
    public void nonNullableStoreKept() {
        var a = local("a", WasmType.SpecialReferenceKind.ANY.asNonNullType());
        var p = local("p", WasmType.ANY);
        body.getLocal(p).setLocal(a).i32Const(0).return_();
        body.getLocal(a).drop();

        assertOptimized(
                "local.get $p",
                "local.set $a",
                "i32.const 0",
                "return",
                "local.get $a",
                "drop"
        );
    }

    @Test
    public void nonNullableStoreRemovedWhenNeverRead() {
        var a = local("a", WasmType.SpecialReferenceKind.ANY.asNonNullType());
        var p = local("p", WasmType.ANY);
        body.getLocal(p).teeLocal(a).setLocal(p).i32Const(0).return_();

        assertOptimized(
                "i32.const 0",
                "return"
        );
        assertLocals();
    }

    @Test
    public void unusedLocalsRemovedParametersKept() {
        function = function("f", WasmType.INT32, WasmType.INT32);
        body = new Code(function.getBody());
        local("p");
        var a = local("a");
        local("b");
        var c = local("c");
        body.i32Const(1).setLocal(a).i32Const(2).setLocal(c).getLocal(c).return_();

        assertOptimized(
                "i32.const 2",
                "local.set $c",
                "local.get $c",
                "return"
        );
        assertLocals("p", "c");
    }

    private static WasmFunction function(String name, WasmType returnType, WasmType... parameterTypes) {
        var function = new WasmFunction(new WasmFunctionType(name, returnType, List.of(parameterTypes)));
        function.setName(name);
        return function;
    }

    private WasmLocal local(String name) {
        return local(name, WasmType.INT32);
    }

    private WasmLocal local(String name, WasmType type) {
        var local = new WasmLocal(type, name);
        function.add(local);
        return local;
    }

    private void assertOptimized(String... expected) {
        new WasmDeadStoreOptimizer().optimize(function);
        var actual = ModelDisassembler.bodyToString(function).lines().collect(Collectors.joining("\n"));
        assertEquals(String.join("\n", expected), actual);
    }

    private void assertLocals(String... expected) {
        var actual = function.getLocalVariables().stream().map(WasmLocal::getName).collect(Collectors.toList());
        assertEquals(List.of(expected), actual);
    }

    private static class Code {
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

        Code drop() {
            return add(new WasmDrop());
        }

        Code add() {
            return add(new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD));
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

        Code loop() {
            var block = new WasmBlock(true);
            add(block);
            return new Code(block.getBody());
        }

        WasmTry try_() {
            var insn = new WasmTry();
            add(insn);
            return insn;
        }
    }
}
