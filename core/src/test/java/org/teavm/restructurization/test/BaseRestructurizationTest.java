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
package org.teavm.restructurization.test;

import static org.junit.Assert.assertEquals;
import static org.teavm.model.builder.ProgramBuilder.build;
import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.text.ListingBuilder;
import org.teavm.restructurization.Block;
import org.teavm.restructurization.BlockBuilder;
import org.teavm.restructurization.Restructurization;

public abstract class BaseRestructurizationTest {
    private Restructurization restructurization = new Restructurization();
    private ListingBuilder listingBuilder = new ListingBuilder();
    protected Program program;
    private Block block;

    protected void restructurize(Runnable r) {
        program = build(r);
        block = restructurization.apply(program);
    }

    protected void check(Runnable r) {
        var expected = BlockBuilder.build(r);
        assertEquals(
                "Wrong result for program:\n" + listingBuilder.buildListing(program, "  "),
                expected.toString(),
                block.toString()
        );
    }

    protected Instruction terminator(int index) {
        return program.basicBlockAt(index).getLastInstruction();
    }

    protected BasicBlock block(int index) {
        return program.basicBlockAt(index);
    }

    protected Variable variable(int index) {
        return program.variableAt(index);
    }
}
