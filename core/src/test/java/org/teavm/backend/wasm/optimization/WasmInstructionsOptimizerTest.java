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
import org.teavm.backend.wasm.model.WasmNumType;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmBlock;
import org.teavm.backend.wasm.model.instruction.WasmBranch;
import org.teavm.backend.wasm.model.instruction.WasmBreak;
import org.teavm.backend.wasm.model.instruction.WasmCall;
import org.teavm.backend.wasm.model.instruction.WasmCatchClause;
import org.teavm.backend.wasm.model.instruction.WasmConditional;
import org.teavm.backend.wasm.model.instruction.WasmConversion;
import org.teavm.backend.wasm.model.instruction.WasmDrop;
import org.teavm.backend.wasm.model.instruction.WasmFloat32Constant;
import org.teavm.backend.wasm.model.instruction.WasmGetLocal;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmInt32Constant;
import org.teavm.backend.wasm.model.instruction.WasmIntBinary;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIntType;
import org.teavm.backend.wasm.model.instruction.WasmIntUnary;
import org.teavm.backend.wasm.model.instruction.WasmIntUnaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmReturn;
import org.teavm.backend.wasm.model.instruction.WasmSetLocal;
import org.teavm.backend.wasm.model.instruction.WasmTeeLocal;
import org.teavm.backend.wasm.model.instruction.WasmTry;
import org.teavm.backend.wasm.model.instruction.WasmUnreachable;

public class WasmInstructionsOptimizerTest {
    private WasmFunction function = function("f", WasmType.INT32);
    private WasmFunction callee = function("g", null, WasmType.INT32, WasmType.INT32);
    private WasmFunction thrower = function("h", null);
    private WasmFunction supplier = function("k", WasmType.INT32);
    private WasmLocal a = local("a");
    private WasmLocal b = local("b");
    private WasmLocal c = local("c");
    private Code body = new Code(function.getBody());

    @Test
    public void pair() {
        body
                .i32Const(1).setLocal(a)
                .i32Const(2).setLocal(b)
                .getLocal(a).getLocal(b).add()
                .return_();

        assertOptimized("""
                i32.const 1
                i32.const 2
                i32.add
                return""");
    }

    @Test
    public void pairWithLaterRead() {
        body
                .i32Const(1).setLocal(a)
                .i32Const(2).setLocal(b)
                .getLocal(a).getLocal(b).add()
                .getLocal(a).add()
                .return_();

        assertOptimized("""
                i32.const 1
                local.tee $a
                i32.const 2
                i32.add
                local.get $a
                i32.add
                return""");
    }

    @Test
    public void skipsWriteOfAnotherLocalInBetween() {
        body
                .i32Const(1).setLocal(a)
                .i32Const(2).setLocal(b)
                .i32Const(3).setLocal(c)
                .getLocal(a).getLocal(c).add()
                .return_();

        assertOptimized("""
                i32.const 1
                i32.const 3
                i32.add
                return""");
    }

    @Test
    public void writeReadAtFunctionEnd() {
        body
                .i32Const(1).setLocal(a)
                .getLocal(a);

        assertOptimized("""
                i32.const 1""");
    }

    @Test
    public void writeReadAtBlockEnd() {
        body.block(WasmType.INT32)
                .i32Const(1).setLocal(a)
                .getLocal(a);
        body.return_();

        assertOptimized("""
                block $label_0 i32
                  i32.const 1
                end (; $label_0 ;)
                return""");
    }

    @Test
    public void readsInReverseOrder() {
        body
                .i32Const(1).setLocal(a)
                .i32Const(2).setLocal(b)
                .getLocal(b).getLocal(a).sub()
                .return_();

        assertOptimized("""
                i32.const 2
                i32.const 1
                i32.sub
                return""");
    }

    @Test
    public void valuePushedBetween() {
        body
                .i32Const(1).setLocal(a)
                .i32Const(2)
                .getLocal(a).add()
                .return_();

        assertOptimized("""
                i32.const 2
                i32.const 1
                i32.add
                return""");
    }

    @Test
    public void computationMovedAcrossPushedValue() {
        body
                .getLocal(c).i32Const(48).sub().setLocal(a)
                .getLocal(b).getLocal(a).add()
                .return_();

        assertOptimized("""
                local.get $b
                local.get $c
                i32.const 48
                i32.sub
                i32.add
                return""");
    }

    @Test
    public void computationMovedAcrossConditional() {
        body.getLocal(c).i32Const(1).add().setLocal(a);
        body.getLocal(b);
        var conditional = body.conditional();
        conditional.setType(WasmType.INT32.asBlock());
        new Code(conditional.getThenBlock()).i32Const(1);
        new Code(conditional.getElseBlock()).i32Const(2);
        body.getLocal(a).sub().return_();

        assertOptimized("""
                local.get $b
                if $label_0 i32
                  i32.const 1
                else (; $label_0 ;)
                  i32.const 2
                end (; $label_0 ;)
                local.get $c
                i32.const 1
                i32.add
                i32.sub
                return""");
    }

    @Test
    public void notMovedWhenCrossedReadsLocalWrittenByMoved() {
        body
                .getLocal(c).i32Const(1).add().teeLocal(b).setLocal(a)
                .getLocal(b).getLocal(a).sub()
                .return_();

        assertOptimized("""
                local.get $c
                i32.const 1
                i32.add
                local.tee $b
                local.set $a
                local.get $b
                local.get $a
                i32.sub
                return""");
    }

    @Test
    public void computationMovedAcrossCallOutsideTry() {
        body
                .i32Const(1).setLocal(a)
                .call(supplier).getLocal(a).sub()
                .return_();

        assertOptimized("""
                call $k
                i32.const 1
                i32.sub
                return""");
    }

    @Test
    public void computationMovedAcrossDivision() {
        body
                .i32Const(1).setLocal(a)
                .getLocal(b).getLocal(c).div().getLocal(a).sub()
                .return_();

        assertOptimized("""
                local.get $b
                local.get $c
                i32.div_s
                i32.const 1
                i32.sub
                return""");
    }

    @Test
    public void callMovedAcrossWriteOutsideTry() {
        body
                .call(supplier).setLocal(a)
                .getLocal(c).teeLocal(b).getLocal(a).sub()
                .getLocal(b).add()
                .return_();

        assertOptimized("""
                local.get $c
                local.tee $b
                call $k
                i32.sub
                local.get $b
                i32.add
                return""");
    }

    @Test
    public void notMovedWhenCrossedReadsLocalWrittenEarlierInMovedRange() {
        body
                .i32Const(1).setLocal(a)
                .i32Const(2).setLocal(b)
                .getLocal(a).i32Const(3).add().setLocal(c)
                .getLocal(b).getLocal(c).sub()
                .return_();

        assertOptimized("""
                i32.const 1
                i32.const 2
                local.set $b
                i32.const 3
                i32.add
                local.set $c
                local.get $b
                local.get $c
                i32.sub
                return""");
    }

    @Test
    public void writesAboveStoredValueDiscarded() {
        body
                .i32Const(1)
                .i32Const(2).setLocal(a)
                .i32Const(3).setLocal(b)
                .setLocal(c)
                .i32Const(4).teeLocal(b)
                .getLocal(a).add().getLocal(b).add().getLocal(c).add()
                .return_();

        assertOptimized("""
                i32.const 1
                i32.const 2
                local.set $a
                local.set $c
                i32.const 4
                local.tee $b
                local.get $a
                i32.add
                local.get $b
                i32.add
                local.get $c
                i32.add
                return""");
    }

    @Test
    public void writeAboveStoredValueNotTurnedIntoTee() {
        body
                .i32Const(1)
                .i32Const(2).setLocal(a)
                .setLocal(c)
                .getLocal(a).getLocal(c).sub()
                .return_();

        assertOptimized("""
                i32.const 1
                i32.const 2
                local.set $a
                local.set $c
                local.get $a
                local.get $c
                i32.sub
                return""");
    }

    @Test
    public void writeAboveValueDoesNotExtendItsComputation() {
        body
                .getLocal(a)
                .i32Const(1).setLocal(b)
                .i32Const(2).add().setLocal(c)
                .call(supplier).getLocal(c).sub()
                .return_();

        assertOptimized("""
                call $k
                local.get $a
                i32.const 2
                i32.add
                i32.sub
                return""");
    }

    @Test
    public void writeAboveValueNotTurnedIntoTeeAfterValueTeed() {
        body
                .i32Const(1)
                .i32Const(2).setLocal(a)
                .teeLocal(b)
                .getLocal(a).sub()
                .getLocal(b).add()
                .return_();

        assertOptimized("""
                i32.const 1
                i32.const 2
                local.set $a
                local.tee $b
                local.get $a
                i32.sub
                local.get $b
                i32.add
                return""");
    }

    @Test
    public void notMovedWhenBothHaveSideEffects() {
        body
                .call(supplier).setLocal(a)
                .i32Const(1).i32Const(2).call(callee)
                .i32Const(3).getLocal(a).sub()
                .return_();

        assertOptimized("""
                call $k
                local.set $a
                i32.const 1
                i32.const 2
                call $g
                i32.const 3
                local.get $a
                i32.sub
                return""");
    }

    @Test
    public void notMovedWhenCrossedWritesLocalReadByMoved() {
        body
                .getLocal(b).i32Const(1).add().setLocal(a)
                .i32Const(5).teeLocal(b).getLocal(a).sub()
                .getLocal(b).add()
                .return_();

        assertOptimized("""
                local.get $b
                i32.const 1
                i32.add
                local.set $a
                i32.const 5
                local.tee $b
                local.get $a
                i32.sub
                local.get $b
                i32.add
                return""");
    }

    @Test
    public void notMovedAcrossCallWhenCaught() {
        var handler = body.block();
        var tryInsn = handler.try_();
        tryInsn.getCatches().add(new WasmCatchClause(null, false, handler.list));
        new Code(tryInsn.getBody())
                .i32Const(1).setLocal(a)
                .call(supplier).getLocal(a).call(callee).return_();
        body.getLocal(a).return_();

        assertOptimized("""
                block $label_0
                  try_table $label_1
                    (catch_all $label_0)
                    i32.const 1
                    local.set $a
                    call $k
                    local.get $a
                    call $g
                    return
                  end (; $label_1 ;)
                end (; $label_0 ;)
                local.get $a
                return""");
    }

    @Test
    public void callNotMovedAcrossWriteWhenCaught() {
        var handler = body.block();
        var tryInsn = handler.try_();
        tryInsn.getCatches().add(new WasmCatchClause(null, false, handler.list));
        new Code(tryInsn.getBody())
                .call(supplier).setLocal(a)
                .getLocal(c).teeLocal(b).getLocal(a).sub()
                .drop();
        body.getLocal(b).return_();

        assertOptimized("""
                block $label_0
                  try_table $label_1
                    (catch_all $label_0)
                    call $k
                    local.set $a
                    local.get $c
                    local.tee $b
                    local.get $a
                    i32.sub
                    drop
                  end (; $label_1 ;)
                end (; $label_0 ;)
                local.get $b
                return""");
    }

    @Test
    public void notMovedAcrossBranch() {
        var loop = body.loop();
        loop
                .i32Const(5)
                .i32Const(1).setLocal(a)
                .getLocal(c).branch(loop)
                .getLocal(b).getLocal(a).sub().add().return_();

        assertOptimized("""
                loop $label_0
                  i32.const 5
                  i32.const 1
                  local.set $a
                  local.get $c
                  br_if $label_0
                  local.get $b
                  local.get $a
                  i32.sub
                  i32.add
                  return
                end (; $label_0 ;)""");
    }

    @Test
    public void movedAcrossIntegerConversion() {
        body
                .call(supplier).setLocal(a)
                .getLocal(b)
                .add(new WasmConversion(WasmNumType.INT32, WasmNumType.INT64, true))
                .add(new WasmConversion(WasmNumType.INT64, WasmNumType.INT32, false))
                .getLocal(a).sub()
                .return_();

        assertOptimized("""
                local.get $b
                i64.extend_i32_s
                i32.wrap_i64
                call $k
                i32.sub
                return""");
    }

    @Test
    public void notMovedAcrossTrappingConversion() {
        body
                .call(supplier).setLocal(a)
                .add(new WasmFloat32Constant(1))
                .add(new WasmConversion(WasmNumType.FLOAT32, WasmNumType.INT32, true))
                .getLocal(a).sub()
                .return_();

        assertOptimized("""
                call $k
                local.set $a
                f32.const 0x1.0p0
                i32.trunc_f32_s
                local.get $a
                i32.sub
                return""");
    }

    @Test
    public void rescheduledValueRescheduledAgain() {
        body
                .i32Const(10)
                .i32Const(1).setLocal(a)
                .getLocal(b).getLocal(a).setLocal(c)
                .i32Const(7).getLocal(c)
                .add().add().add().return_();

        assertOptimized("""
                i32.const 10
                local.get $b
                i32.const 7
                i32.const 1
                i32.add
                i32.add
                i32.add
                return""");
    }

    @Test
    public void notRescheduledFromOuterBlockWithParams() {
        var blockType = new WasmFunctionType("bt", WasmType.INT32, List.of(WasmType.INT32)).asBlock();
        body.i32Const(0);
        var outer = new WasmBlock(false);
        outer.setType(blockType);
        body.add(outer);
        var inner = new WasmBlock(false);
        inner.setType(blockType);
        new Code(outer.getBody())
                .i32Const(1).setLocal(a)
                .i32Const(2)
                .add(inner)
                .add();
        new Code(inner.getBody()).getLocal(a).add();
        body.return_();

        assertOptimized("""
                i32.const 0
                block $label_0 (type $bt)
                  i32.const 1
                  local.set $a
                  i32.const 2
                  block $label_1 (type $bt)
                    local.get $a
                    i32.add
                  end (; $label_1 ;)
                  i32.add
                end (; $label_0 ;)
                return""");
    }

    @Test
    public void teeForMultipleReads() {
        body
                .i32Const(1).setLocal(a)
                .getLocal(a).getLocal(a).add()
                .return_();

        assertOptimized("""
                i32.const 1
                local.tee $a
                local.get $a
                i32.add
                return""");
    }

    @Test
    public void writeSurvivesConsumptionAbove() {
        body
                .i32Const(1).setLocal(a)
                .i32Const(2).i32Const(3).add().drop()
                .getLocal(a)
                .return_();

        assertOptimized("""
                i32.const 1
                i32.const 2
                i32.const 3
                i32.add
                drop
                return""");
    }

    @Test
    public void writeDiscardedByConsumptionBelow() {
        body
                .i32Const(1)
                .i32Const(2).setLocal(a)
                .i32Const(3).add()
                .getLocal(a).add()
                .return_();

        assertOptimized("""
                i32.const 1
                i32.const 2
                local.set $a
                i32.const 3
                i32.add
                local.get $a
                i32.add
                return""");
    }

    @Test
    public void latestWriteWins() {
        body
                .i32Const(1).setLocal(a)
                .i32Const(2).setLocal(a)
                .getLocal(a)
                .return_();

        assertOptimized("""
                i32.const 2
                return""");
    }

    @Test
    public void teeInvalidatesWrite() {
        body
                .i32Const(1).setLocal(a)
                .i32Const(2).teeLocal(a).drop()
                .getLocal(a)
                .return_();

        assertOptimized("""
                i32.const 2
                local.tee $a
                drop
                local.get $a
                return""");
    }

    @Test
    public void callArguments() {
        body
                .i32Const(1).setLocal(a)
                .i32Const(2).setLocal(b)
                .getLocal(a).getLocal(b).call(callee)
                .i32Const(0).return_();

        assertOptimized("""
                i32.const 1
                i32.const 2
                call $g
                i32.const 0
                return""");
    }

    @Test
    public void valueKeptOnStackAcrossBlock() {
        body.i32Const(1).setLocal(a);
        body.block().getLocal(a).drop();
        body.getLocal(a).return_();

        assertOptimized("""
                i32.const 1
                local.tee $a
                block $label_0
                  local.get $a
                  drop
                end (; $label_0 ;)
                return""");
    }

    @Test
    public void writeInNestedBlockInvalidatesOuterWrite() {
        body.i32Const(1).setLocal(a);
        body.i32Const(0);
        var conditional = body.conditional();
        new Code(conditional.getThenBlock()).i32Const(2).setLocal(a);
        body.getLocal(a).return_();

        assertOptimized("""
                i32.const 1
                local.set $a
                i32.const 0
                if $label_0
                  i32.const 2
                  local.set $a
                end (; $label_0 ;)
                local.get $a
                return""");
    }

    @Test
    public void writesInsideLoop() {
        body.i32Const(1).setLocal(a);
        body.loop()
                .getLocal(a).drop()
                .i32Const(2).setLocal(b)
                .getLocal(b).getLocal(b).add().drop();
        body.i32Const(0).return_();

        assertOptimized("""
                i32.const 1
                local.set $a
                loop $label_0
                  local.get $a
                  drop
                  i32.const 2
                  local.tee $b
                  local.get $b
                  i32.add
                  drop
                end (; $label_0 ;)
                i32.const 0
                return""");
    }

    @Test
    public void branchReadsValuesFromStack() {
        var block = body.block(WasmType.INT32);
        block
                .i32Const(5)
                .i32Const(1).setLocal(a)
                .i32Const(0).branch(block)
                .getLocal(a).add();
        body.return_();

        assertOptimized("""
                block $label_0 i32
                  i32.const 5
                  i32.const 1
                  local.set $a
                  i32.const 0
                  br_if $label_0
                  local.get $a
                  i32.add
                end (; $label_0 ;)
                return""");
    }

    @Test
    public void writtenValueUsedAsCondition() {
        var block = body.block();
        block.i32Const(1).setLocal(a).getLocal(a).branch(block);
        body.i32Const(0).return_();

        assertOptimized("""
                block $label_0
                  i32.const 1
                  br_if $label_0
                end (; $label_0 ;)
                i32.const 0
                return""");
    }

    @Test
    public void removesDeadTee() {
        body
                .i32Const(1).teeLocal(a)
                .return_();

        assertOptimized("""
                i32.const 1
                return""");
    }

    @Test
    public void valueSurvivesDeadTee() {
        body
                .i32Const(1).setLocal(a)
                .getLocal(a).teeLocal(b)
                .return_();

        assertOptimized("""
                i32.const 1
                return""");
    }


    @Test
    public void keepsSetWhenCallMayThrowToHandler() {
        var handler = body.block();
        var tryInsn = handler.try_();
        tryInsn.getCatches().add(new WasmCatchClause(null, false, handler.list));
        new Code(tryInsn.getBody())
                .i32Const(1).setLocal(a)
                .call(thrower)
                .getLocal(a).return_();
        body.getLocal(a).return_();

        assertOptimized("""
                block $label_0
                  try_table $label_1
                    (catch_all $label_0)
                    i32.const 1
                    local.tee $a
                    call $h
                    return
                  end (; $label_1 ;)
                end (; $label_0 ;)
                local.get $a
                return""");
    }

    @Test
    public void removesSetConsumedByCallWithinTry() {
        var handler = body.block();
        var tryInsn = handler.try_();
        tryInsn.getCatches().add(new WasmCatchClause(null, false, handler.list));
        new Code(tryInsn.getBody())
                .i32Const(1).setLocal(a)
                .getLocal(a).i32Const(2).call(callee);
        body.i32Const(0).return_();

        assertOptimized("""
                block $label_0
                  try_table $label_1
                    (catch_all $label_0)
                    i32.const 1
                    i32.const 2
                    call $g
                  end (; $label_1 ;)
                end (; $label_0 ;)
                i32.const 0
                return""");
    }

    @Test
    public void removesSetWhenCallCannotThrowToHandler() {
        body
                .i32Const(1).setLocal(a)
                .call(thrower)
                .getLocal(a).return_();

        assertOptimized("""
                i32.const 1
                call $h
                return""");
    }

    @Test
    public void identityCopyKeptOnStack() {
        body
                .getLocal(a).setLocal(a)
                .getLocal(a).return_();

        assertOptimized("""
                local.get $a
                return""");
    }

    @Test
    public void identityCopyMovedAcrossPushedValue() {
        body
                .getLocal(a).setLocal(a)
                .i32Const(2).getLocal(a).add()
                .return_();

        assertOptimized("""
                i32.const 2
                local.get $a
                i32.add
                return""");
    }


    @Test
    public void identityCopyRestoresOverwrittenValue() {
        body
                .getLocal(a).i32Const(1).setLocal(a).setLocal(a)
                .i32Const(2).getLocal(a).add()
                .return_();

        assertOptimized("""
                i32.const 2
                local.get $a
                i32.add
                return""");
    }


    @Test
    public void storeKeptWhenReadEarlierInRegion() {
        body
                .i32Const(1).setLocal(a)
                .i32Const(2).getLocal(a).sub().drop()
                .getLocal(a).return_();

        assertOptimized("""
                i32.const 2
                i32.const 1
                local.tee $a
                i32.sub
                drop
                local.get $a
                return""");
    }

    @Test
    public void storeKeptWhenReadAtBranchTarget() {
        var outer = body.block();
        outer
                .i32Const(1).setLocal(a)
                .getLocal(c).branch(outer)
                .getLocal(a).return_();
        body.getLocal(a).return_();

        assertOptimized("""
                i32.const 1
                local.tee $a
                local.get $c
                i32.eqz
                if $label_0
                  local.get $a
                  return
                end (; $label_0 ;)
                return""");
    }

    @Test
    public void storeRemovedWhenBranchStaysInRegion() {
        body.i32Const(1).setLocal(a);
        var inner = body.block();
        inner.getLocal(c).branch(inner).add(new WasmUnreachable());
        body.getLocal(a).return_();

        assertOptimized("""
                i32.const 1
                local.get $c
                i32.eqz
                if $label_0
                  unreachable
                end (; $label_0 ;)
                return""");
    }

    @Test
    public void ifElseReconstructed() {
        var outer = body.block();
        var inner = outer.block();
        inner
                .getLocal(c).branch(inner)
                .i32Const(2).setLocal(a)
                .breakTo(outer);
        outer.i32Const(3).setLocal(a);
        body.getLocal(a).return_();

        assertOptimized("""
                local.get $c
                if $label_0 i32
                  i32.const 3
                else (; $label_0 ;)
                  i32.const 2
                end (; $label_0 ;)
                return""");
    }

    @Test
    public void ifElseReconstructedWithTerminatingElse() {
        var outer = body.block();
        var inner = outer.block();
        inner
                .getLocal(c).branch(inner)
                .i32Const(2).return_();
        outer.i32Const(3).setLocal(a);
        body.getLocal(a).return_();

        assertOptimized("""
                local.get $c
                if $label_0 i32
                  i32.const 3
                else (; $label_0 ;)
                  i32.const 2
                  return
                end (; $label_0 ;)
                return""");
    }

    @Test
    public void ifElseReconstructedWithPhiAssignments() {
        var outer = body.block();
        var inner = outer.block();
        inner.getLocal(c);
        new Code(inner.conditional().getThenBlock())
                .i32Const(2).setLocal(a)
                .breakTo(inner);
        inner
                .i32Const(3).setLocal(a)
                .breakTo(outer);
        outer.getLocal(a).getLocal(b).add().setLocal(a);
        body.getLocal(a).return_();

        assertOptimized("""
                local.get $c
                if $label_0 i32
                  i32.const 2
                  local.get $b
                  i32.add
                else (; $label_0 ;)
                  i32.const 3
                end (; $label_0 ;)
                return""");
    }

    @Test
    public void ifElseRetargetsBranchesToOuterBlock() {
        var outer = body.block();
        var inner = outer.block();
        inner
                .getLocal(c).branch(inner)
                .getLocal(b).branch(outer)
                .i32Const(2).setLocal(a)
                .breakTo(outer);
        outer
                .getLocal(b).branch(outer)
                .i32Const(3).setLocal(a);
        body.getLocal(a).return_();

        assertOptimized("""
                local.get $c
                if $label_0
                  local.get $b
                  br_if $label_0
                  i32.const 3
                  local.set $a
                else (; $label_0 ;)
                  local.get $b
                  br_if $label_0
                  i32.const 2
                  local.set $a
                end (; $label_0 ;)
                local.get $a
                return""");
    }

    @Test
    public void ifElseNotReconstructedWhenThenReachedByAnotherBranch() {
        var outer = body.block();
        var inner = outer.block();
        inner
                .getLocal(c).branch(inner)
                .getLocal(b).branch(inner)
                .i32Const(2).setLocal(a)
                .breakTo(outer);
        outer.i32Const(3).setLocal(a);
        body.getLocal(a).return_();

        assertOptimized("""
                block $label_0
                  local.get $c
                  i32.eqz
                  if $label_1
                    local.get $b
                    br_if $label_1
                    i32.const 2
                    local.set $a
                    br $label_0
                  end (; $label_1 ;)
                  i32.const 3
                  local.set $a
                end (; $label_0 ;)
                local.get $a
                return""");
    }

    @Test
    public void ifElseNotReconstructedWhenConditionRefersToOuterBlock() {
        var outer = body.block();
        var inner = outer.block();
        inner
                .getLocal(b).branch(outer)
                .getLocal(c).branch(inner)
                .i32Const(2).setLocal(a)
                .breakTo(outer);
        outer.i32Const(3).setLocal(a);
        body.getLocal(a).return_();

        assertOptimized("""
                block $label_0
                  local.get $b
                  br_if $label_0
                  local.get $c
                  i32.eqz
                  if $label_1
                    i32.const 2
                    local.set $a
                    br $label_0
                  end (; $label_1 ;)
                  i32.const 3
                  local.set $a
                end (; $label_0 ;)
                local.get $a
                return""");
    }

    @Test
    public void ifReconstructedWithInvertedComparison() {
        var block = body.block();
        block
                .getLocal(b).getLocal(c).lt().branch(block)
                .i32Const(2).setLocal(a);
        body.getLocal(a).return_();

        assertOptimized("""
                local.get $b
                local.get $c
                i32.ge_s
                if $label_0
                  i32.const 2
                  local.set $a
                end (; $label_0 ;)
                local.get $a
                return""");
    }

    @Test
    public void ifReconstructedWithRemovedEqz() {
        var block = body.block();
        block
                .getLocal(c).eqz().branch(block)
                .i32Const(2).setLocal(a);
        body.getLocal(a).return_();

        assertOptimized("""
                local.get $c
                if $label_0
                  i32.const 2
                  local.set $a
                end (; $label_0 ;)
                local.get $a
                return""");
    }

    @Test
    public void ifReconstructedWithPhiAssignments() {
        var block = body.block();
        block.getLocal(c);
        new Code(block.conditional().getThenBlock())
                .i32Const(2).setLocal(a)
                .breakTo(block);
        block.i32Const(3).setLocal(a);
        body.getLocal(a).return_();

        assertOptimized("""
                local.get $c
                if $label_0 i32
                  i32.const 2
                else (; $label_0 ;)
                  i32.const 3
                end (; $label_0 ;)
                return""");
    }

    @Test
    public void ifNotReconstructedWhenConditionLeavesExtraValue() {
        var block = body.block();
        block
                .i32Const(2).getLocal(c).branch(block)
                .setLocal(a);
        body.getLocal(a).return_();

        assertOptimized("""
                block $label_0
                  i32.const 2
                  local.get $c
                  br_if $label_0
                  local.set $a
                end (; $label_0 ;)
                local.get $a
                return""");
    }

    @Test
    public void ifElseWithEmptyThenTurnedIntoIf() {
        var outer = body.block();
        var inner = outer.block();
        inner
                .getLocal(b).getLocal(c).lt().branch(inner)
                .i32Const(2).setLocal(a)
                .breakTo(outer);
        outer.breakTo(outer);
        body.getLocal(a).return_();

        assertOptimized("""
                local.get $b
                local.get $c
                i32.ge_s
                if $label_0
                  i32.const 2
                  local.set $a
                end (; $label_0 ;)
                local.get $a
                return""");
    }

    @Test
    public void ifWithSingleBreakReplacedWithBranch() {
        var loop = body.loop();
        loop.getLocal(c);
        new Code(loop.conditional().getThenBlock())
                .breakTo(loop);
        loop.i32Const(2).return_();
        body.getLocal(a).return_();

        assertOptimized("""
                loop $label_0
                  local.get $c
                  br_if $label_0
                  i32.const 2
                  return
                end (; $label_0 ;)
                local.get $a
                return""");
    }

    @Test
    public void setHoistedFromIf() {
        body.getLocal(c);
        var conditional = body.conditional();
        new Code(conditional.getThenBlock()).getLocal(b).i32Const(1).add().setLocal(a);
        new Code(conditional.getElseBlock()).i32Const(2).setLocal(a);
        body.getLocal(a).return_();

        assertOptimized("""
                local.get $c
                if $label_0 i32
                  local.get $b
                  i32.const 1
                  i32.add
                else (; $label_0 ;)
                  i32.const 2
                end (; $label_0 ;)
                return""");
    }

    @Test
    public void setHoistedFromIfWithTerminatingPart() {
        body.getLocal(c);
        var conditional = body.conditional();
        new Code(conditional.getThenBlock()).i32Const(1).setLocal(a);
        new Code(conditional.getElseBlock()).i32Const(2).return_();
        body.getLocal(a).getLocal(a).add().return_();

        assertOptimized("""
                local.get $c
                if $label_0 i32
                  i32.const 1
                else (; $label_0 ;)
                  i32.const 2
                  return
                end (; $label_0 ;)
                local.tee $a
                local.get $a
                i32.add
                return""");
    }

    @Test
    public void setNotHoistedWhenDifferentLocals() {
        body.getLocal(c);
        var conditional = body.conditional();
        new Code(conditional.getThenBlock()).i32Const(1).setLocal(a);
        new Code(conditional.getElseBlock()).i32Const(2).setLocal(b);
        body.getLocal(a).getLocal(b).add().return_();

        assertOptimized("""
                local.get $c
                if $label_0
                  i32.const 1
                  local.set $a
                else (; $label_0 ;)
                  i32.const 2
                  local.set $b
                end (; $label_0 ;)
                local.get $a
                local.get $b
                i32.add
                return""");
    }

    @Test
    public void setNotHoistedWhenBranchToEndOfIf() {
        body.getLocal(c);
        var conditional = body.conditional();
        var thenPart = new Code(conditional.getThenBlock());
        thenPart.getLocal(b).branch(thenPart).i32Const(1).setLocal(a);
        new Code(conditional.getElseBlock()).i32Const(2).setLocal(a);
        body.getLocal(a).return_();

        assertOptimized("""
                local.get $c
                if $label_0
                  local.get $b
                  br_if $label_0
                  i32.const 1
                  local.set $a
                else (; $label_0 ;)
                  i32.const 2
                  local.set $a
                end (; $label_0 ;)
                local.get $a
                return""");
    }


    @Test
    public void invertedConditionRemoved() {
        body.getLocal(c).eqz();
        var conditional = body.conditional();
        var thenPart = new Code(conditional.getThenBlock());
        thenPart.getLocal(b).branch(thenPart).call(thrower);
        new Code(conditional.getElseBlock()).i32Const(2).return_();
        body.getLocal(a).return_();

        assertOptimized("""
                local.get $c
                if $label_0
                  i32.const 2
                  return
                else (; $label_0 ;)
                  local.get $b
                  br_if $label_0
                  call $h
                end (; $label_0 ;)
                local.get $a
                return""");
    }

    private static WasmFunction function(String name, WasmType returnType, WasmType... parameterTypes) {
        var function = new WasmFunction(new WasmFunctionType(name, returnType, List.of(parameterTypes)));
        function.setName(name);
        return function;
    }

    private WasmLocal local(String name) {
        var local = new WasmLocal(WasmType.INT32, name);
        function.add(local);
        return local;
    }

    private void assertOptimized(String expected) {
        new WasmInstructionsOptimizer().optimize(function);
        new WasmDeadStoreOptimizer().optimize(function);
        var actual = ModelDisassembler.bodyToString(function).lines().collect(Collectors.joining("\n"));
        assertEquals(expected, actual);
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

        Code add() {
            return add(new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD));
        }

        Code sub() {
            return add(new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB));
        }

        Code div() {
            return add(new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.DIV_SIGNED));
        }

        Code lt() {
            return add(new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED));
        }

        Code eqz() {
            return add(new WasmIntUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ));
        }

        Code breakTo(Code target) {
            return add(new WasmBreak(target.list));
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

        Code block(WasmType type) {
            var block = new WasmBlock(false);
            block.setType(type.asBlock());
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
