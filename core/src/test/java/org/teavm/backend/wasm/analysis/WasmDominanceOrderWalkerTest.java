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
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.junit.Test;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmBlock;
import org.teavm.backend.wasm.model.instruction.WasmBranch;
import org.teavm.backend.wasm.model.instruction.WasmCall;
import org.teavm.backend.wasm.model.instruction.WasmCatchClause;
import org.teavm.backend.wasm.model.instruction.WasmConditional;
import org.teavm.backend.wasm.model.instruction.WasmGetLocal;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmInt32Constant;
import org.teavm.backend.wasm.model.instruction.WasmReturn;
import org.teavm.backend.wasm.model.instruction.WasmSetLocal;
import org.teavm.backend.wasm.model.instruction.WasmTry;

/**
 * State of the walker is the set of locals written by dominating instructions. Every {@code local.get}
 * records the state at its point.
 */
public class WasmDominanceOrderWalkerTest {
    private WasmFunction function = new WasmFunction(new WasmFunctionType("f", (WasmType) null, List.of()));
    private WasmFunction thrower = new WasmFunction(new WasmFunctionType("h", (WasmType) null, List.of()));
    private WasmLocal a = local("a");
    private WasmLocal b = local("b");
    private WasmLocal c = local("c");
    private WasmLocal x = local("x");
    private Code body = new Code(function.getBody());

    @Test
    public void straightLine() {
        body.set(a).get(x).set(b).get(x);

        assertStates("a", "a,b");
    }

    @Test
    public void exceptionHandlerNotDominatedByTryBody() {
        body.set(x);
        var handler = body.block();
        var tryInsn = handler.try_();
        tryInsn.getCatches().add(new WasmCatchClause(null, false, handler.list));
        new Code(tryInsn.getBody()).set(a).call(thrower).set(b).get(c).return_();
        body.get(c);

        assertStates("a,b,x", "x");
    }

    @Test
    public void codeAfterTryWithoutBranchesFollowsTryBody() {
        var handler = body.block();
        var tryInsn = handler.try_();
        tryInsn.getCatches().add(new WasmCatchClause(null, false, handler.list));
        new Code(tryInsn.getBody()).set(a).call(thrower);
        handler.get(c).return_();
        body.get(c);

        assertStates("a", "");
    }

    @Test
    public void conditionalBranchesStartFromConditional() {
        body.set(x).const0();
        var conditional = body.conditional();
        new Code(conditional.getThenBlock()).set(a).get(c);
        new Code(conditional.getElseBlock()).get(c).set(b);
        body.get(c);

        assertStates("a,x", "x", "x");
    }

    @Test
    public void conditionalWithTerminatingBranchContinuesFromOther() {
        var conditional = body.const0().conditional();
        new Code(conditional.getThenBlock()).set(a).return_();
        new Code(conditional.getElseBlock()).set(b);
        body.get(c);

        assertStates("b");
    }

    @Test
    public void blockTargetedByBranchRestoresState() {
        body.set(x);
        var block = body.block();
        block.set(a).const0().branch(block).set(b);
        body.get(c);

        assertStates("x");
    }

    @Test
    public void blockNotTargetedContinuesFromBody() {
        var block = body.block();
        block.set(a);
        body.get(c);

        assertStates("a");
    }

    @Test
    public void loopBodyFollowsPrecedingCode() {
        body.set(x);
        var loop = body.loop();
        loop.get(c).set(a).const0().branch(loop);
        body.get(c);

        assertStates("x", "a,x");
    }

    private WasmLocal local(String name) {
        var local = new WasmLocal(WasmType.INT32, name);
        function.add(local);
        return local;
    }

    private void assertStates(String... expected) {
        var states = new ArrayList<String>();
        new WasmDominanceOrderWalker<TreeSet<String>>() {
            TreeSet<String> written = new TreeSet<>();

            @Override
            protected TreeSet<String> saveState() {
                return new TreeSet<>(written);
            }

            @Override
            protected void restoreState(TreeSet<String> state) {
                written = new TreeSet<>(state);
            }

            @Override
            protected void enterInstruction(WasmInstruction instruction) {
                if (instruction instanceof WasmSetLocal) {
                    written.add(((WasmSetLocal) instruction).getLocal().getName());
                } else if (instruction instanceof WasmGetLocal) {
                    states.add(String.join(",", written));
                }
            }
        }.walk(function.getBody());
        assertEquals(List.of(expected), states);
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

        Code const0() {
            return add(new WasmInt32Constant(0));
        }

        Code set(WasmLocal local) {
            return add(new WasmInt32Constant(0)).add(new WasmSetLocal(local));
        }

        Code get(WasmLocal local) {
            return add(new WasmGetLocal(local));
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
