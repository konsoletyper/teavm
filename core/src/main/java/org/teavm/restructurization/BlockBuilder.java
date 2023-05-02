/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.restructurization;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.Variable;

public class BlockBuilder {
    private static Block block;

    public static Block build(Runnable runnable) {
        var backup = block;
        block = null;
        runnable.run();
        var result = first();
        block = backup;
        return result;
    }

    public static void simple(BasicBlock basicBlock) {
        var simpleBlock = new SimpleBlock();
        simpleBlock.basicBlock = basicBlock;
        append(simpleBlock);
    }

    public static void ret() {
        ret(null);
    }

    public static void ret(Variable var) {
        var retBlock = new ReturnBlock();
        retBlock.value = var;
        append(retBlock);
    }

    public static void raise(Variable exception) {
        var raiseBlock = new ThrowBlock();
        raiseBlock.exception = exception;
        append(raiseBlock);
    }

    public static void labeled(Consumer<LabeledBuilder> builder) {
        var labeledBlock = new SimpleLabeledBlock();
        append(labeledBlock);
        var labeledBuilder = new LabeledBuilder(labeledBlock);
        labeledBlock.body = build(() -> builder.accept(labeledBuilder));
    }

    public static IfBuilder cond(Instruction insn, Consumer<LabeledBuilder> builder) {
        return cond(insn, false, builder);
    }

    public static IfBuilder invCond(Instruction insn, Consumer<LabeledBuilder> builder) {
        return cond(insn, true, builder);
    }

    private static IfBuilder cond(Instruction insn, boolean inverted, Consumer<LabeledBuilder> builder) {
        var ifBlock = new IfBlock();
        ifBlock.condition = insn;
        ifBlock.inverted = inverted;
        append(ifBlock);
        var labeledBuilder = new LabeledBuilder(ifBlock);
        ifBlock.thenBody = build(() -> builder.accept(labeledBuilder));
        return new IfBuilder(labeledBuilder, ifBlock);
    }

    public static void loop(Consumer<LabeledBuilder> builder) {
        var loop = new LoopBlock();
        append(loop);
        var labeledBuilder = new LabeledBuilder(loop);
        loop.body = build(() -> builder.accept(labeledBuilder));
    }

    public static SwitchBuilder sw(Variable condition, Consumer<LabeledBuilder> builder) {
        var sw = new SwitchBlock();
        var list = new ArrayList<SwitchBlockEntry>();
        sw.condition = condition;
        sw.entries = list;
        append(sw);
        var labeledBuilder = new LabeledBuilder(sw);
        sw.defaultBody = build(() -> builder.accept(labeledBuilder));
        return new SwitchBuilder(labeledBuilder, list);
    }

    private static void append(Block next) {
        block = block != null ? block.append(next) : next;
    }

    private static Block first() {
        return block != null ? block.first : null;
    }

    public static class IfBuilder {
        private LabeledBuilder labeledBuilder;
        private IfBlock block;

        private IfBuilder(LabeledBuilder labeledBuilder, IfBlock block) {
            this.labeledBuilder = labeledBuilder;
            this.block = block;
        }

        public void otherwise(Consumer<LabeledBuilder> builder) {
            block.elseBody = build(() -> builder.accept(labeledBuilder));
        }
    }

    public static class SwitchBuilder {
        private LabeledBuilder labeledBuilder;
        private List<SwitchBlockEntry> entries;

        SwitchBuilder(LabeledBuilder labeledBuilder, List<SwitchBlockEntry> entries) {
            this.labeledBuilder = labeledBuilder;
            this.entries = entries;
        }

        public SwitchBuilder entry(int condition, Consumer<LabeledBuilder> builder) {
            return entry(new int[] { condition }, builder);
        }

        public SwitchBuilder entry(int[] conditions, Consumer<LabeledBuilder> builder) {
            var entry = new SwitchBlockEntry();
            entries.add(entry);
            entry.matchValues = conditions;
            entry.body = build(() -> builder.accept(labeledBuilder));
            return this;
        }
    }

    public static class LabeledBuilder {
        private LabeledBlock target;

        private LabeledBuilder(LabeledBlock target) {
            this.target = target;
        }

        public void br() {
            var brBlock = new BreakBlock();
            brBlock.target = target;
            append(brBlock);
        }
    }
}
