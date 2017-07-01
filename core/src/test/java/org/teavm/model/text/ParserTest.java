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
package org.teavm.model.text;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import org.junit.Test;
import org.teavm.model.BasicBlock;
import org.teavm.model.ListingParseUtils;
import org.teavm.model.Program;

public class ParserTest {
    @Test
    public void simple() throws Exception {
        Program program = runTest("simple");
        assertEquals(2, program.basicBlockCount());
        assertEquals(4, program.variableCount());
        assertEquals(4, program.basicBlockAt(0).instructionCount());
        assertEquals(1, program.basicBlockAt(1).instructionCount());
    }

    @Test
    public void conditional() throws Exception {
        Program program = runTest("conditional");
        assertEquals(7, program.basicBlockCount());
        for (int i = 0; i < 7; ++i) {
            assertEquals(1, program.basicBlockAt(i).instructionCount());
        }
    }

    @Test
    public void phi() throws Exception {
        Program program = runTest("phi");
        assertEquals(4, program.basicBlockCount());
        assertEquals(2, program.basicBlockAt(3).getPhis().size());
    }

    @Test
    public void constant() throws Exception {
        Program program = runTest("constant");
        assertEquals(1, program.basicBlockCount());

        BasicBlock block = program.basicBlockAt(0);
        assertEquals(7, block.instructionCount());
    }

    @Test
    public void invocation() throws Exception {
        Program program = runTest("invocation");
        assertEquals(1, program.basicBlockCount());
    }

    @Test
    public void casting() throws Exception {
        Program program = runTest("casting");
        assertEquals(1, program.basicBlockCount());
    }

    @Test
    public void operations() throws Exception {
        runTest("operations");
    }

    @Test
    public void create() throws Exception {
        Program program = runTest("create");
        assertEquals(1, program.basicBlockCount());
    }

    @Test
    public void fields() throws Exception {
        runTest("fields");
    }

    @Test
    public void switchInsn() throws Exception {
        runTest("switchInsn");
    }

    @Test
    public void exceptions() throws Exception {
        runTest("exceptions");
    }

    private Program runTest(String name) throws IOException {
        return ListingParseUtils.parseFromResource("model/text/" + name + ".txt");
    }
}
