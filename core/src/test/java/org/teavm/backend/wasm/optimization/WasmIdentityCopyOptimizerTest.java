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
import org.teavm.backend.wasm.model.instruction.WasmBreak;
import org.teavm.backend.wasm.model.instruction.WasmConditional;
import org.teavm.backend.wasm.model.instruction.WasmGetLocal;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIntType;
import org.teavm.backend.wasm.model.instruction.WasmTry;

public class WasmIdentityCopyOptimizerTest {
    private WasmFunction function = new WasmFunction(new WasmFunctionType("f", WasmType.INT32, List.of()));
    private WasmLocal a = local("a");
    private WasmLocal b = local("b");
    private WasmInstructionBuilder body = function.getBody().builder();

    @Test
    public void identityCopyRemoved() {
        body.add(get(a)).setLocal(a).add(get(a)).return_();

        assertOptimized("""
                local.get $a
                return""");
    }

    @Test
    public void identityTeeReplacedWithRead() {
        body.add(get(a)).teeLocal(a).return_();

        assertOptimized("""
                local.get $a
                return""");
    }

    @Test
    public void parallelIdentityCopyRemoved() {
        body
                .add(get(a)).add(get(b)).setLocal(b).setLocal(a)
                .i32Const(2).add(get(a)).add(get(b)).intBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD)
                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD)
                .return_();

        assertOptimized("""
                i32.const 2
                local.get $a
                local.get $b
                i32.add
                i32.add
                return""");
    }

    @Test
    public void identityCopyInNestedIfRemoved() {
        body.i32Const(0);
        var conditional = new WasmConditional();
        body.add(conditional);
        conditional.getThenBlock().builder().add(get(a)).setLocal(a);
        body.add(get(a)).return_();

        assertOptimized("""
                i32.const 0
                if $label_0
                end (; $label_0 ;)
                local.get $a
                return""");
    }

    @Test
    public void copyKeptWhenLocalOverwrittenWhileValueOnStack() {
        body.add(get(a)).i32Const(5).setLocal(a).setLocal(a).add(get(a)).return_();

        assertOptimized("""
                local.get $a
                i32.const 5
                local.set $a
                local.set $a
                local.get $a
                return""");
    }

    @Test
    public void copyKeptWhenLocalOverwrittenByTeeWhileValueOnStack() {
        body.add(get(a)).i32Const(5).teeLocal(a).setLocal(b).setLocal(a).add(get(a)).return_();

        assertOptimized("""
                local.get $a
                i32.const 5
                local.tee $a
                local.set $b
                local.set $a
                local.get $a
                return""");
    }

    @Test
    public void copyKeptWhenLocalOverwrittenInNestedIf() {
        body.add(get(a)).i32Const(0);
        var conditional = new WasmConditional();
        body.add(conditional);
        conditional.getThenBlock().builder().i32Const(5).setLocal(a);
        body.setLocal(a).add(get(a)).return_();

        assertOptimized("""
                local.get $a
                i32.const 0
                if $label_0
                  i32.const 5
                  local.set $a
                end (; $label_0 ;)
                local.set $a
                local.get $a
                return""");
    }

    /*
     * Shape produced by WasmCodeGenerator for a loop-carried variable after WasmLocalsOptimizer
     * merges the phi local, the value read from it and an unrelated temporary into one local:
     * the loop-carried value travels on the operand stack across a conditional,
     * which reuses the same local for a temporary.
     */
    @Test
    public void loopCarriedValueOnStackAcrossConditionalReusingLocal() {
        var loop = new WasmBlock(true);
        body.add(loop);
        var conditional = new WasmConditional();
        conditional.setType(WasmType.INT32.asBlock());
        loop.getBody().builder()
                .add(get(a)).i32Const(1).intBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD).teeLocal(a)
                .drop()
                .add(get(a))
                .add(get(b))
                .add(conditional);
        conditional.getThenBlock().builder().i32Const(0);
        conditional.getElseBlock().builder().i32Const(7).setLocal(a).add(get(a));
        loop.getBody().builder()
                .setLocal(b)
                .setLocal(a)
                .add(new WasmBreak(loop.getBody()));

        assertOptimized("""
                loop $label_0
                  local.get $a
                  i32.const 1
                  i32.add
                  local.set $a
                  local.get $a
                  local.get $b
                  if $label_1 i32
                    i32.const 0
                  else (; $label_1 ;)
                    i32.const 7
                    local.set $a
                    local.get $a
                  end (; $label_1 ;)
                  local.set $b
                  local.set $a
                  br $label_0
                end (; $label_0 ;)""");
    }

    /**
     * Same as previous, but the local is reused inside try_table.
     */
    @Test
    public void loopCarriedValueOnStackAcrossTryReusingLocal() {
        var loop = new WasmBlock(true);
        body.add(loop);
        var tryInsn = new WasmTry();
        loop.getBody().builder()
                .add(get(a))
                .add(tryInsn);
        tryInsn.getBody().builder().i32Const(7).setLocal(a);
        loop.getBody().builder()
                .setLocal(a)
                .add(new WasmBreak(loop.getBody()));

        assertOptimized("""
                loop $label_0
                  local.get $a
                  try_table $label_1
                    i32.const 7
                    local.set $a
                  end (; $label_1 ;)
                  local.set $a
                  br $label_0
                end (; $label_0 ;)""");
    }

    @Test
    public void copyOfAnotherLocalKept() {
        body.add(get(a)).setLocal(b).add(get(b)).return_();

        assertOptimized("""
                local.get $a
                local.set $b
                local.get $b
                return""");
    }

    // Builder turns local.set followed by local.get into local.tee, which would hide identity copies
    private static WasmGetLocal get(WasmLocal local) {
        return new WasmGetLocal(local);
    }

    private WasmLocal local(String name) {
        var local = new WasmLocal(WasmType.INT32, name);
        function.add(local);
        return local;
    }

    private void assertOptimized(String expected) {
        new WasmIdentityCopyOptimizer().visitMany(function.getBody());
        var actual = ModelDisassembler.bodyToString(function).lines().collect(Collectors.joining("\n"));
        assertEquals(expected, actual);
    }
}
