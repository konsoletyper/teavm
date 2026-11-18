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
package org.teavm.model.util.test;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.ListingParseUtils;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.text.ListingBuilder;
import org.teavm.model.util.BasicBlockSplitter;
import org.teavm.model.util.PhiUpdater;

/**
 * Splits each block of the original program after every call to {@code Split.here()},
 * connecting the parts with jumps, as clients of {@link BasicBlockSplitter} do.
 */
public class BasicBlockSplitterTest {
    private static final String PREFIX = "model/util/basic-block-splitter/";
    private static final MethodReference SPLIT_MARKER = new MethodReference("Split", "here", ValueType.VOID);
    @Rule
    public TestName name = new TestName();

    @Test
    public void exceptionPhiSplitAroundDefinition() {
        doTest();
    }

    @Test
    public void exceptionPhiSplitAfterDefinition() {
        doTest();
    }

    @Test
    public void exceptionPhiPartWithoutDefinitions() {
        doTest();
    }

    @Test
    public void exceptionPhiWithoutEntryValue() {
        doTest();
    }

    @Test
    public void exceptionPhiMultipleSources() {
        doTest();
    }

    @Test
    public void exceptionPhiMultipleVariables() {
        doTest();
    }

    private void doTest() {
        String originalPath = PREFIX + name.getMethodName() + ".original.txt";
        String expectedPath = PREFIX + name.getMethodName() + ".expected.txt";
        Program original = ListingParseUtils.parseFromResource(originalPath);
        Program expected = ListingParseUtils.parseFromResource(expectedPath);

        split(original);

        String originalText = new ListingBuilder().buildListing(original, "");
        String expectedText = new ListingBuilder().buildListing(expected, "");
        Assert.assertEquals(expectedText, originalText);

        new PhiUpdater().updatePhis(original, 0);
        String updatedText = new ListingBuilder().buildListing(original, "");
        Assert.assertEquals("Phi update", originalText, updatedText);
    }

    private void split(Program program) {
        List<Instruction> splitPoints = new ArrayList<>();
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                if (instruction instanceof InvokeInstruction
                        && ((InvokeInstruction) instruction).getMethod().equals(SPLIT_MARKER)) {
                    splitPoints.add(instruction);
                }
            }
        }

        BasicBlockSplitter splitter = new BasicBlockSplitter(program);
        for (Instruction splitPoint : splitPoints) {
            BasicBlock block = splitPoint.getBasicBlock();
            BasicBlock next = splitter.split(block, splitPoint);
            JumpInstruction jump = new JumpInstruction();
            jump.setTarget(next);
            block.add(jump);
        }
        splitter.fixProgram();
    }
}
