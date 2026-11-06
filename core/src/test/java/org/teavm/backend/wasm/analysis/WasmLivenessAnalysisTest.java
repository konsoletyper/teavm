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
package org.teavm.backend.wasm.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmBlock;
import org.teavm.backend.wasm.model.instruction.WasmBranch;
import org.teavm.backend.wasm.model.instruction.WasmBreak;
import org.teavm.backend.wasm.model.instruction.WasmCall;
import org.teavm.backend.wasm.model.instruction.WasmCatchClause;
import org.teavm.backend.wasm.model.instruction.WasmConditional;
import org.teavm.backend.wasm.model.instruction.WasmDrop;
import org.teavm.backend.wasm.model.instruction.WasmGetLocal;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmInt32Constant;
import org.teavm.backend.wasm.model.instruction.WasmReturn;
import org.teavm.backend.wasm.model.instruction.WasmSetLocal;
import org.teavm.backend.wasm.model.instruction.WasmSwitch;
import org.teavm.backend.wasm.model.instruction.WasmTeeLocal;
import org.teavm.backend.wasm.model.instruction.WasmTry;
import org.teavm.backend.wasm.model.instruction.WasmUnreachable;

public class WasmLivenessAnalysisTest {
    private WasmFunction function = new WasmFunction(new WasmFunctionType("f", (WasmType) null, List.of()));
    private WasmFunction callee = new WasmFunction(new WasmFunctionType("g", (WasmType) null, List.of()));
    private WasmLocal a = local("a");
    private WasmLocal b = local("b");
    private WasmLocal c = local("c");
    private Code body = new Code(function.getBody());

    @Test
    public void straightLine() {
        body
                .i32Const(1).setLocal(a)
                .i32Const(2).setLocal(b)
                .getLocal(a).drop()
                .getLocal(b).drop();

        assertLiveIn(function.getBody(), "", "", "a", "a", "a b", "b", "b", "", "");
    }

    @Test
    public void teeKills() {
        body
                .i32Const(1).teeLocal(a).drop()
                .getLocal(a).drop();

        assertLiveIn(function.getBody(), "", "", "a", "a", "", "");
    }

    @Test
    public void useWithoutDefinitionIsLiveAtEntry() {
        body.getLocal(a).getLocal(b).drop().drop();

        assertLiveIn(function.getBody(), "a b", "b", "", "", "");
    }

    @Test
    public void conditional() {
        body.i32Const(1).setLocal(a);
        body.i32Const(2).setLocal(b);
        body.i32Const(0);
        var cond = body.conditional();
        new Code(cond.getThenBlock()).getLocal(a).drop();
        new Code(cond.getElseBlock()).i32Const(3).setLocal(a);
        body.getLocal(a).getLocal(b).drop().drop();

        assertLiveIn(function.getBody(), "", "", "a", "a", "a b", "a b", "a b", "b", "", "", "");
        assertLiveIn(cond.getThenBlock(), "a b", "a b", "a b");
        assertLiveIn(cond.getElseBlock(), "b", "b", "a b");
    }

    @Test
    public void conditionalWithoutElse() {
        body.i32Const(1).setLocal(a);
        body.i32Const(0);
        var cond = body.conditional();
        new Code(cond.getThenBlock()).i32Const(3).setLocal(a);
        body.getLocal(a).drop();

        assertLiveIn(function.getBody(), "", "", "a", "a", "a", "", "");
        assertLiveIn(cond.getThenBlock(), "", "", "a");
        assertLiveIn(cond.getElseBlock(), "a");
    }

    @Test
    public void loop() {
        body.i32Const(1).setLocal(a);
        var loop = body.loop();
        loop
                .getLocal(a).drop()
                .i32Const(2).setLocal(b)
                .getLocal(b).branch(loop);
        body.getLocal(b).drop();

        assertLiveIn(function.getBody(), "", "", "a", "b", "", "");
        assertLiveIn(loop.list, "a", "a", "a", "a", "a b", "a b", "b");
    }

    @Test
    public void loopReentryKillsVariable() {
        var loop = body.loop();
        loop
                .i32Const(1).setLocal(a)
                .getLocal(a).drop()
                .getLocal(b).branch(loop);

        assertLiveIn(function.getBody(), "b", "");
        assertLiveIn(loop.list, "b", "b", "a b", "b", "b", "b", "");
    }

    @Test
    public void nestedLoops() {
        body.i32Const(1).setLocal(a);
        var outer = body.loop();
        outer.getLocal(a).drop();
        var inner = outer.loop();
        inner
                .getLocal(b).drop()
                .i32Const(0).branch(inner);
        outer.i32Const(0).branch(outer);

        assertLiveIn(function.getBody(), "b", "b", "a b", "");
        // a is live through inner loop, since it's read by the next iteration of outer loop
        assertLiveIn(outer.list, "a b", "a b", "a b", "a b", "a b", "");
        assertLiveIn(inner.list, "a b", "a b", "a b", "a b", "a b");
    }

    @Test
    public void branchOutOfLoop() {
        var block = body.block();
        var loop = block.loop();
        loop
                .getLocal(a).drop()
                .i32Const(0).branch(block)
                .i32Const(1).setLocal(b)
                .breakTo(loop);
        body.getLocal(b).drop();

        assertLiveIn(function.getBody(), "a b", "b", "", "");
        assertLiveIn(block.list, "a b", "b");
        assertLiveIn(loop.list, "a b", "a b", "a b", "a b", "a", "a", "a b", "b");
    }

    @Test
    public void conditionalBranchOutOfBlock() {
        body.i32Const(1).setLocal(a);
        var block = body.block();
        block
                .i32Const(0).branch(block)
                .i32Const(2).setLocal(a)
                .getLocal(b).drop();
        body.getLocal(a).drop();

        assertLiveIn(function.getBody(), "b", "b", "a b", "a", "", "");
        assertLiveIn(block.list, "a b", "a b", "b", "b", "a b", "a", "a");
    }

    @Test
    public void unconditionalBranch() {
        var outer = body.block();
        var inner = outer.block();
        inner
                .getLocal(c).drop()
                .breakTo(outer)
                .getLocal(a).drop();
        outer.getLocal(b).drop();
        body.getLocal(c).drop();

        assertLiveIn(function.getBody(), "c", "c", "", "");
        assertLiveIn(outer.list, "c", "b c", "c", "c");
        assertLiveIn(inner.list, "c", "c", "c", "a b c", "b c", "b c");
    }

    @Test
    public void switchTargets() {
        var outer = body.block();
        var middle = outer.block();
        var inner = middle.block();
        inner.getLocal(c);
        var sw = inner.switch_(outer);
        sw.getTargets().add(inner.list);
        sw.getTargets().add(middle.list);
        middle.getLocal(a).drop();
        outer.getLocal(b).drop();

        assertLiveIn(function.getBody(), "a b c", "");
        assertLiveIn(inner.list, "a b c", "a b", "a b");
        assertLiveIn(middle.list, "a b c", "a b", "b", "b");
        assertLiveIn(outer.list, "a b c", "b", "", "");
    }

    @Test
    public void exceptionHandler() {
        var handler = body.block();
        var tryInsn = handler.try_();
        tryInsn.getCatches().add(new WasmCatchClause(null, false, handler.list));
        new Code(tryInsn.getBody())
                .i32Const(1).setLocal(a)
                .call(callee)
                .i32Const(2).setLocal(a)
                .call(callee)
                .getLocal(b).drop();
        handler.return_();
        body.getLocal(a).drop();

        // a is read by handler, so it's live in the whole try body, including its entry and end
        assertLiveIn(function.getBody(), "a b", "a", "", "");
        assertLiveIn(handler.list, "a b", "", "");
        assertLiveIn(tryInsn.getBody(), "a b", "a b", "a b", "a b", "a b", "a b", "a b", "a", "a");
    }

    @Test
    public void exceptionHandlerLocalDefinedInNestedTry() {
        var handler = body.block();
        var outerTry = handler.try_();
        outerTry.getCatches().add(new WasmCatchClause(null, false, handler.list));
        var innerTry = new Code(outerTry.getBody())
                .i32Const(1).setLocal(a)
                .call(callee)
                .try_();
        new Code(innerTry.getBody())
                .i32Const(2).setLocal(a)
                .call(callee);
        handler.return_();
        body.getLocal(a).drop();

        assertLiveIn(function.getBody(), "a", "a", "", "");
        assertLiveIn(handler.list, "a", "", "");
        assertLiveIn(outerTry.getBody(), "a", "a", "a", "a", "a");
        assertLiveIn(innerTry.getBody(), "a", "a", "a", "a");
    }

    @Test
    public void nestedTryWithCatchAll() {
        var outerHandler = body.block();
        var outerTry = outerHandler.try_();
        outerTry.getCatches().add(new WasmCatchClause(null, false, outerHandler.list));
        var innerHandler = new Code(outerTry.getBody()).block();
        var innerTry = innerHandler.try_();
        innerTry.getCatches().add(new WasmCatchClause(null, false, innerHandler.list));
        new Code(innerTry.getBody()).call(callee);
        innerHandler.return_();
        new Code(outerTry.getBody()).getLocal(b).drop();
        outerHandler.return_();
        body.getLocal(a).drop();

        // Outer handler locals are live in the entire outer body, including inner body
        assertLiveIn(innerTry.getBody(), "a b", "a b");
        assertLiveIn(outerTry.getBody(), "a b", "a b", "a", "a");
    }

    @Test
    public void exceptionHandlerWithBranchOutOfTry() {
        var after = body.block();
        var handler = after.block();
        var tryInsn = handler.try_();
        tryInsn.getCatches().add(new WasmCatchClause(null, false, handler.list));
        handler.unreachable();
        new Code(tryInsn.getBody())
                .i32Const(1).setLocal(a)
                .call(callee)
                .getLocal(b).drop()
                .breakTo(after);
        after.getLocal(a).drop().return_();
        body.getLocal(c).drop();

        // End of try body is not reachable, still a must be live before call
        assertLiveIn(tryInsn.getBody(), "a b c", "a b c", "a b c", "a b c", "a c", "a c", "a");
        assertLiveIn(handler.list, "a b c", "", "");
    }

    @Test
    public void exceptionHandlerInLoop() {
        var loop = body.loop();
        loop.getLocal(a).drop();
        var handler = loop.block();
        var tryInsn = handler.try_();
        tryInsn.getCatches().add(new WasmCatchClause(null, false, handler.list));
        handler.unreachable();
        new Code(tryInsn.getBody())
                .i32Const(1).setLocal(a)
                .call(callee)
                .return_();
        loop.breakTo(loop);

        // Handler reaches loop entry, which reads a, so a is live in the whole try body
        assertLiveIn(function.getBody(), "a", "");
        assertLiveIn(loop.list, "a", "a", "a", "a", "");
        assertLiveIn(tryInsn.getBody(), "a", "a", "a", "a", "a");
    }

    @Test
    public void cursor() {
        body
                .i32Const(1).setLocal(a)
                .getLocal(a).drop();
        var analysis = new WasmLivenessAnalysis(function);
        var cursor = analysis.iterate(function.getBody());

        assertEquals(function.getBody().getFirst(), cursor.getInstruction());
        assertFalse(cursor.isLiveIn(a));
        assertFalse(cursor.isLiveOut(a));
        assertTrue(cursor.next());
        assertFalse(cursor.isLiveIn(a));
        assertTrue(cursor.isLiveOut(a));
        assertEquals(List.of(a), cursor.getLiveOut());
        assertTrue(cursor.next());
        assertTrue(cursor.isLiveIn(a));
        assertFalse(cursor.isLiveOut(a));
        assertTrue(cursor.next());
        assertFalse(cursor.next());
        assertTrue(cursor.isAtEnd());
        assertNull(cursor.getInstruction());
        assertEquals(List.of(), cursor.getLiveIn());
    }

    @Test
    public void blockLiveIn() {
        var block = body.block();
        block.getLocal(a).drop();
        var analysis = new WasmLivenessAnalysis(function);
        var cursor = analysis.iterate(function.getBody());

        assertTrue(cursor.getInstruction() instanceof WasmBlock);
        assertTrue(cursor.isLiveIn(a));
        assertFalse(cursor.isLiveOut(a));
    }

    private WasmLocal local(String name) {
        var local = new WasmLocal(WasmType.INT32, name);
        function.add(local);
        return local;
    }

    private void assertLiveIn(WasmInstructionList list, String... expected) {
        var analysis = new WasmLivenessAnalysis(function);
        var cursor = analysis.iterate(list);
        var actual = new ArrayList<String>();
        while (true) {
            actual.add(cursor.getLiveIn().stream().map(WasmLocal::getName).collect(Collectors.joining(" ")));
            if (cursor.isAtEnd()) {
                break;
            }
            cursor.next();
        }
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

        Code unreachable() {
            return add(new WasmUnreachable());
        }

        Code branch(Code target) {
            return add(new WasmBranch(target.list));
        }

        Code breakTo(Code target) {
            return add(new WasmBreak(target.list));
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

        WasmConditional conditional() {
            var insn = new WasmConditional();
            add(insn);
            return insn;
        }

        WasmSwitch switch_(Code defaultTarget) {
            var insn = new WasmSwitch(defaultTarget.list);
            add(insn);
            return insn;
        }

        WasmTry try_() {
            var insn = new WasmTry();
            add(insn);
            return insn;
        }
    }
}
