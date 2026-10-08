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
package org.teavm.model.transformation;

import static org.junit.Assert.assertTrue;
import java.io.IOException;
import org.junit.Test;
import org.teavm.model.Instruction;
import org.teavm.model.ListingParseUtils;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BinaryOperation;

public class BoundCheckInsertionTest {
    private static final MethodReference METHOD =
            new MethodReference("Test", "test", ValueType.object("java.lang.Object"));

    /**
     * A constant division by zero must not be folded. Folding evaluates the division inside the
     * compiler, so the pass threw ArithmeticException instead of leaving the division for the
     * backend to emit, and the C target failed to compile legal Java.
     */
    @Test
    public void constantDivisionByZeroIsNotFolded() throws IOException {
        Program program =
                ListingParseUtils.parseFromResource("model/text/constantDivisionByZero.txt");
        new BoundCheckInsertion().transformProgram(program, METHOD);
    }

    @Test
    public void constantModuloByZeroIsNotFolded() throws IOException {
        Program program = ListingParseUtils.parseFromResource("model/text/constantModuloByZero.txt");
        new BoundCheckInsertion().transformProgram(program, METHOD);
    }

    /**
     * The guard must reject only a zero divisor. An ordinary constant division still has to reach the
     * folding arithmetic, and the pass must leave the program's instructions alone: it records
     * constants for its own bound-check decisions and never rewrites them, so a reader expecting the
     * division to disappear would be wrong.
     */
    @Test
    public void constantDivisionByNonZeroIsStillHandled() throws IOException {
        Program program = ListingParseUtils.parseFromResource("model/text/constantDivisionFolds.txt");
        new BoundCheckInsertion().transformProgram(program, METHOD);

        boolean divisionPreserved = false;
        for (Instruction instruction : program.basicBlockAt(0)) {
            if (instruction instanceof BinaryInstruction
                    && ((BinaryInstruction) instruction).getOperation() == BinaryOperation.DIVIDE) {
                divisionPreserved = true;
            }
        }
        assertTrue("this pass records constants, it does not rewrite them", divisionPreserved);
    }

    /**
     * The modulo guard has the same two sides as the division one, so it is pinned separately rather
     * than assumed to follow: a non-zero constant remainder must still be handled.
     */
    @Test
    public void constantModuloByNonZeroIsHandled() throws IOException {
        Program program = ListingParseUtils.parseFromResource("model/text/constantModuloFolds.txt");
        new BoundCheckInsertion().transformProgram(program, METHOD);
    }
}
