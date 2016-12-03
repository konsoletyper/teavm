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
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.junit.Assert;
import org.junit.Test;
import org.teavm.model.BasicBlock;
import org.teavm.model.Program;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.StringConstantInstruction;

public class ParserTest {
    @Test
    public void simple() throws Exception {
        Program program = runTest("simple");
        assertEquals(2, program.basicBlockCount());
        assertEquals(4, program.variableCount());
        assertEquals(4, program.basicBlockAt(0).getInstructions().size());
        assertEquals(1, program.basicBlockAt(1).getInstructions().size());
    }

    @Test
    public void conditional() throws Exception {
        Program program = runTest("conditional");
        assertEquals(7, program.basicBlockCount());
        for (int i = 0; i < 7; ++i) {
            assertEquals(1, program.basicBlockAt(i).getInstructions().size());
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
        assertEquals(7, block.getInstructions().size());
        assertTrue("IntConstant", block.getInstructions().get(0) instanceof IntegerConstantInstruction);
        assertTrue("LongConstant", block.getInstructions().get(1) instanceof LongConstantInstruction);
        assertTrue("FloatConstant", block.getInstructions().get(2) instanceof FloatConstantInstruction);
        assertTrue("DoubleConstant", block.getInstructions().get(3) instanceof DoubleConstantInstruction);
        assertTrue("StringConstant", block.getInstructions().get(4) instanceof StringConstantInstruction);
        assertTrue("ClassConstant", block.getInstructions().get(5) instanceof ClassConstantInstruction);
    }

    private Program runTest(String name) throws IOException {
        ClassLoader classLoader = ParserTest.class.getClassLoader();
        String path = "model/text/" + name + ".txt";

        try (InputStream input = classLoader.getResourceAsStream(path);
                InputStreamReader reader = new InputStreamReader(input, "UTF-8")) {
            return new ListingParser().parse(reader);
        } catch (ListingParseException e) {
            Location location;
            try (InputStream input = classLoader.getResourceAsStream(path)) {
                location = offsetToLocation(e.getIndex(), input);
            }
            Assert.fail("Parse error at [" + (location.row + 1) + "; " + (location.column + 1)
                    + "]: " + e.getMessage());
            return null;
        }
    }

    private Location offsetToLocation(int offset, InputStream input) throws IOException {
        int row = 0;
        int column = 0;
        try (InputStreamReader reader = new InputStreamReader(input, "UTF-8")) {
            for (int i = 0; i < offset; ++i) {
                int c = reader.read();
                if (c == '\n') {
                    row++;
                    column = 0;
                } else {
                    column++;
                }
            }
        }
        return new Location(row, column);
    }

    static class Location {
        int row;
        int column;

        public Location(int row, int column) {
            this.row = row;
            this.column = column;
        }
    }
}
