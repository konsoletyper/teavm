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
import org.teavm.backend.wasm.model.instruction.WasmConditional;
import org.teavm.backend.wasm.model.instruction.WasmDrop;
import org.teavm.backend.wasm.model.instruction.WasmGetLocal;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmInt32Constant;
import org.teavm.backend.wasm.model.instruction.WasmInt64Constant;
import org.teavm.backend.wasm.model.instruction.WasmReturn;
import org.teavm.backend.wasm.model.instruction.WasmSetLocal;
import org.teavm.backend.wasm.model.instruction.WasmTeeLocal;
import org.teavm.backend.wasm.model.instruction.WasmTry;

public class WasmLocalsOptimizerTest {
    private WasmFunction function = function("f", WasmType.INT32);
    private WasmFunction thrower = function("h", null);
    private Code body = new Code(function.getBody());

    @Test
    public void sequential() {
        var a = local("a");
        var b = local("b");
        body
                .i32Const(1).setLocal(a)
                .getLocal(a).drop()
                .i32Const(2).setLocal(b)
                .getLocal(b).return_();

        assertOptimized(
                "i32.const 1",
                "local.set $a",
                "local.get $a",
                "drop",
                "i32.const 2",
                "local.set $a",
                "local.get $a",
                "return"
        );
        assertLocals("a");
    }

    @Test
    public void overlapping() {
        var a = local("a");
        var b = local("b");
        body
                .i32Const(1).setLocal(a)
                .i32Const(2).setLocal(b)
                .getLocal(a).drop()
                .getLocal(b).return_();

        assertOptimized(
                "i32.const 1",
                "local.set $a",
                "i32.const 2",
                "local.set $b",
                "local.get $a",
                "drop",
                "local.get $b",
                "return"
        );
        assertLocals("a", "b");
    }

    @Test
    public void reusesColorReleasedByLastRead() {
        var a = local("a");
        var b = local("b");
        var c = local("c");
        body
                .i32Const(1).setLocal(a)
                .i32Const(2).setLocal(b)
                .getLocal(a).drop()
                .i32Const(3).setLocal(c)
                .getLocal(b).drop()
                .getLocal(c).return_();

        assertOptimized(
                "i32.const 1",
                "local.set $a",
                "i32.const 2",
                "local.set $b",
                "local.get $a",
                "drop",
                "i32.const 3",
                "local.set $a",
                "local.get $b",
                "drop",
                "local.get $a",
                "return"
        );
        assertLocals("a", "b");
    }

    @Test
    public void differentTypesNotMerged() {
        var a = local("a");
        var b = local("b", WasmType.INT64);
        body
                .i32Const(1).setLocal(a)
                .getLocal(a).drop()
                .add(new WasmInt64Constant(2)).setLocal(b)
                .getLocal(b).drop()
                .i32Const(0).return_();

        assertOptimized(
                "i32.const 1",
                "local.set $a",
                "local.get $a",
                "drop",
                "i64.const 2",
                "local.set $b",
                "local.get $b",
                "drop",
                "i32.const 0",
                "return"
        );
        assertLocals("a", "b");
    }

    @Test
    public void deadStore() {
        var a = local("a");
        var b = local("b");
        body
                .i32Const(1).setLocal(a)
                .i32Const(2).setLocal(b)
                .getLocal(a).return_();

        assertOptimized(
                "i32.const 1",
                "local.set $a",
                "i32.const 2",
                "local.set $b",
                "local.get $a",
                "return"
        );
        assertLocals("a", "b");
    }

    @Test
    public void branchesOfConditional() {
        var a = local("a");
        var b = local("b");
        var c = local("c");
        body.i32Const(0);
        var conditional = body.conditional();
        new Code(conditional.getThenBlock()).i32Const(1).setLocal(a).getLocal(a).setLocal(c);
        new Code(conditional.getElseBlock()).i32Const(2).setLocal(b).getLocal(b).setLocal(c);
        body.getLocal(c).return_();

        assertOptimized(
                "i32.const 0",
                "if $label_0",
                "  i32.const 1",
                "  local.set $a",
                "  local.get $a",
                "  local.set $a",
                "else (; $label_0 ;)",
                "  i32.const 2",
                "  local.set $a",
                "  local.get $a",
                "  local.set $a",
                "end (; $label_0 ;)",
                "local.get $a",
                "return"
        );
        assertLocals("a");
    }

    @Test
    public void localLiveInOtherBranchOnly() {
        var a = local("a");
        var b = local("b");
        body.i32Const(1).setLocal(a);
        body.i32Const(0);
        var conditional = body.conditional();
        new Code(conditional.getThenBlock()).getLocal(a).return_();
        new Code(conditional.getElseBlock()).i32Const(2).setLocal(b).getLocal(b).return_();
        body.i32Const(0).return_();

        assertOptimized(
                "i32.const 1",
                "local.set $a",
                "i32.const 0",
                "if $label_0",
                "  local.get $a",
                "  return",
                "else (; $label_0 ;)",
                "  i32.const 2",
                "  local.set $a",
                "  local.get $a",
                "  return",
                "end (; $label_0 ;)",
                "i32.const 0",
                "return"
        );
        assertLocals("a");
    }

    @Test
    public void conflictingDefinitionsPinned() {
        var x = local("x");
        var m = local("m");
        body.i32Const(0);
        var conditional = body.conditional();
        new Code(conditional.getThenBlock()).i32Const(1).setLocal(x);
        new Code(conditional.getElseBlock())
                .i32Const(2).setLocal(m)
                .i32Const(3).setLocal(x)
                .getLocal(m).drop();
        body.getLocal(x).return_();

        assertOptimized(
                "i32.const 0",
                "if $label_0",
                "  i32.const 1",
                "  local.set $x",
                "else (; $label_0 ;)",
                "  i32.const 2",
                "  local.set $m",
                "  i32.const 3",
                "  local.set $x",
                "  local.get $m",
                "  drop",
                "end (; $label_0 ;)",
                "local.get $x",
                "return"
        );
        assertLocals("x", "m");
    }

    @Test
    public void variableLiveAcrossLoop() {
        var a = local("a");
        var b = local("b");
        var c = local("c");
        body.i32Const(1).setLocal(a);
        var loop = body.loop();
        loop
                .i32Const(2).setLocal(b)
                .getLocal(b).drop()
                .i32Const(0).branch(loop);
        body.i32Const(3).setLocal(c).getLocal(c).drop();
        body.getLocal(a).return_();

        assertOptimized(
                "i32.const 1",
                "local.set $a",
                "loop $label_0",
                "  i32.const 2",
                "  local.set $b",
                "  local.get $b",
                "  drop",
                "  i32.const 0",
                "  br_if $label_0",
                "end (; $label_0 ;)",
                "i32.const 3",
                "local.set $b",
                "local.get $b",
                "drop",
                "local.get $a",
                "return"
        );
        assertLocals("a", "b");
    }

    @Test
    public void loopCarriedVariable() {
        var i = local("i");
        var t = local("t");
        var u = local("u");
        body.i32Const(0).setLocal(i);
        var loop = body.loop();
        loop
                .getLocal(i).setLocal(t)
                .i32Const(1).setLocal(u)
                .getLocal(u).drop()
                .getLocal(t).setLocal(i)
                .i32Const(0).branch(loop);
        body.i32Const(0).return_();

        assertOptimized(
                "i32.const 0",
                "local.set $i",
                "loop $label_0",
                "  local.get $i",
                "  local.set $i",
                "  i32.const 1",
                "  local.set $u",
                "  local.get $u",
                "  drop",
                "  local.get $i",
                "  local.set $i",
                "  i32.const 0",
                "  br_if $label_0",
                "end (; $label_0 ;)",
                "i32.const 0",
                "return"
        );
        assertLocals("i", "u");
    }

    @Test
    public void variableUsedAfterBlockWithBranch() {
        var a = local("a");
        var b = local("b");
        var outer = body.block();
        outer.i32Const(1).setLocal(a);
        outer.i32Const(0).branch(outer);
        outer.i32Const(2).setLocal(b).getLocal(b).drop();
        body.getLocal(a).return_();

        assertOptimized(
                "block $label_0",
                "  i32.const 1",
                "  local.set $a",
                "  i32.const 0",
                "  br_if $label_0",
                "  i32.const 2",
                "  local.set $b",
                "  local.get $b",
                "  drop",
                "end (; $label_0 ;)",
                "local.get $a",
                "return"
        );
        assertLocals("a", "b");
    }

    @Test
    public void variableUsedInExceptionHandler() {
        var a = local("a");
        var b = local("b");
        var handler = body.block();
        var tryInsn = handler.try_();
        tryInsn.getCatches().add(new WasmCatchClause(null, false, handler.list));
        new Code(tryInsn.getBody())
                .i32Const(1).setLocal(a)
                .call(thrower)
                .i32Const(2).setLocal(b)
                .getLocal(b).drop()
                .getLocal(a).return_();
        body.getLocal(a).return_();

        assertOptimized(
                "block $label_0",
                "  try_table $label_1",
                "    (catch_all $label_0)",
                "    i32.const 1",
                "    local.set $a",
                "    call $h",
                "    i32.const 2",
                "    local.set $b",
                "    local.get $b",
                "    drop",
                "    local.get $a",
                "    return",
                "  end (; $label_1 ;)",
                "end (; $label_0 ;)",
                "local.get $a",
                "return"
        );
        assertLocals("a", "b");
    }

    @Test
    public void variableDeadAfterLastThrowingCall() {
        var a = local("a");
        var b = local("b");
        var handler = body.block();
        var tryInsn = handler.try_();
        tryInsn.getCatches().add(new WasmCatchClause(null, false, handler.list));
        new Code(tryInsn.getBody())
                .i32Const(1).setLocal(a)
                .call(thrower)
                .i32Const(2).setLocal(b)
                .getLocal(b).return_();
        body.getLocal(a).return_();

        assertOptimized(
                "block $label_0",
                "  try_table $label_1",
                "    (catch_all $label_0)",
                "    i32.const 1",
                "    local.set $a",
                "    call $h",
                "    i32.const 2",
                "    local.set $a",
                "    local.get $a",
                "    return",
                "  end (; $label_1 ;)",
                "end (; $label_0 ;)",
                "local.get $a",
                "return"
        );
        assertLocals("a");
    }

    @Test
    public void parameterReused() {
        function = function("f", WasmType.INT32, WasmType.INT32);
        body = new Code(function.getBody());
        var p = local("p");
        var a = local("a");
        body
                .getLocal(p).drop()
                .i32Const(1).setLocal(a)
                .getLocal(a).return_();

        assertOptimized(
                "local.get $p",
                "drop",
                "i32.const 1",
                "local.set $p",
                "local.get $p",
                "return"
        );
        assertLocals("p");
    }

    @Test
    public void unusedParameterKept() {
        function = function("f", WasmType.INT32, WasmType.INT32, WasmType.INT32);
        body = new Code(function.getBody());
        local("p");
        local("q");
        var a = local("a");
        local("unused");
        body.i32Const(1).setLocal(a).getLocal(a).return_();

        assertOptimized(
                "i32.const 1",
                "local.set $p",
                "local.get $p",
                "return"
        );
        assertLocals("p", "q");
    }

    @Test
    public void defaultValueReadNotMerged() {
        var a = local("a");
        var b = local("b");
        body
                .i32Const(1).setLocal(b)
                .getLocal(b).drop()
                .getLocal(a).return_();

        assertOptimized(
                "i32.const 1",
                "local.set $b",
                "local.get $b",
                "drop",
                "local.get $a",
                "return"
        );
        assertLocals("a", "b");
    }

    @Test
    public void tee() {
        var a = local("a");
        var b = local("b");
        body
                .i32Const(1).teeLocal(a).drop()
                .getLocal(a).drop()
                .i32Const(2).teeLocal(b).drop()
                .getLocal(b).return_();

        assertOptimized(
                "i32.const 1",
                "local.tee $a",
                "drop",
                "local.get $a",
                "drop",
                "i32.const 2",
                "local.tee $a",
                "drop",
                "local.get $a",
                "return"
        );
        assertLocals("a");
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
        new WasmLocalsOptimizer().optimize(function);
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

        WasmConditional conditional() {
            var insn = new WasmConditional();
            add(insn);
            return insn;
        }
    }
}
