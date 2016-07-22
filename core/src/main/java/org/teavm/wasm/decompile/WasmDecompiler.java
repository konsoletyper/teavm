/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.wasm.decompile;

import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
import org.teavm.common.Loop;
import org.teavm.common.LoopGraph;
import org.teavm.model.Program;
import org.teavm.model.util.ProgramUtils;
import org.teavm.wasm.model.expression.WasmBlock;
import org.teavm.wasm.model.expression.WasmExpression;

public class WasmDecompiler {
    private Program program;
    private LoopGraph cfg;
    private DominatorTree dom;
    private Graph domGraph;
    private Label[] labels;
    private WasmExpression[] blockExpressions;
    private Step[] stack;
    private int stackTop;

    public void decompile(Program program) {
        this.program = program;
        prepare();
        push(new EnterStep(0));
        run();
    }

    private void prepare() {
        cfg = new LoopGraph(ProgramUtils.buildControlFlowGraph(program));
        dom = GraphUtils.buildDominatorTree(cfg);
        domGraph = GraphUtils.buildDominatorGraph(dom, cfg.size());
        labels = new Label[cfg.size()];
        blockExpressions = new WasmExpression[cfg.size()];
        stack = new Step[cfg.size() * 4];
    }

    private void run() {
        while (stackTop > 0) {
            stack[stackTop--].perform();
        }
    }

    private void push(Step step) {
        stack[stackTop++] = step;
    }

    private class EnterStep implements Step {
        private int index;

        public EnterStep(int index) {
            this.index = index;
        }

        @Override
        public void perform() {
            Loop loop = cfg.loopAt(index);
            if (loop != null && loop.getHead() == index) {
                enterLoop();
            } else if (cfg.outgoingEdgesCount(index) == 1) {
                enterOrdinaryBlock();
            } else {
                enterBranching();
            }
        }

        private void enterLoop() {

        }

        private void enterOrdinaryBlock() {

        }

        private void enterBranching() {

        }
    }

    private class LoopContinueLabel implements Label {
        private int index;
        private WasmBlock wrapper;

        public LoopContinueLabel(int index) {
            this.index = index;
        }

        @Override
        public WasmBlock getTarget() {
            if (wrapper == null) {
                wrapper = new WasmBlock(false);
                wrapper.getBody().add(blockExpressions[index]);
                blockExpressions[index] = wrapper;
            }
            return wrapper;
        }
    }

    private class BreakLabel implements Label {
        private WasmBlock target;

        public BreakLabel(WasmBlock target) {
            this.target = target;
        }

        @Override
        public WasmBlock getTarget() {
            return null;
        }
    }

    private interface Step {
        void perform();
    }

    private interface Label {
        WasmBlock getTarget();
    }
}
