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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.teavm.model.util.ProgramViolation.Kind.HANDLER_PHI_ENTRY_VALUE;
import static org.teavm.model.util.ProgramViolation.Kind.HANDLER_PHI_ORDER;
import static org.teavm.model.util.ProgramViolation.Kind.INCOMING_FROM_NON_PREDECESSOR;
import static org.teavm.model.util.ProgramViolation.Kind.MISSING_INCOMING;
import static org.teavm.model.util.ProgramViolation.Kind.MULTIPLE_DEFINITION;
import static org.teavm.model.util.ProgramViolation.Kind.MULTIPLE_INCOMINGS;
import static org.teavm.model.util.ProgramViolation.Kind.NORMAL_AND_EXCEPTIONAL_EDGE;
import static org.teavm.model.util.ProgramViolation.Kind.NOT_DOMINATED;
import static org.teavm.model.util.ProgramViolation.Kind.PHI_IN_ENTRY_BLOCK;
import static org.teavm.model.util.ProgramViolation.Kind.UNDEFINED_VARIABLE;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.teavm.model.ListingParseUtils;
import org.teavm.model.Program;
import org.teavm.model.util.ProgramChecker;
import org.teavm.model.util.ProgramInvariantException;
import org.teavm.model.util.ProgramViolation;

public class ProgramCheckerTest {
    private static final String PREFIX = "model/util/program-checker/";
    @Rule
    public TestName name = new TestName();

    @Test
    public void valid() {
        doTest();
    }

    @Test
    public void handlerPhiWithoutEntryValue() {
        doTest();
    }

    @Test
    public void handlerPhiDuplicates() {
        doTest();
    }

    @Test
    public void unreachableBlocksIgnored() {
        doTest();
    }

    @Test
    public void parameters() {
        doTest(2, MULTIPLE_DEFINITION, UNDEFINED_VARIABLE);
    }

    @Test
    public void multipleDefinition() {
        doTest(MULTIPLE_DEFINITION);
    }

    @Test
    public void undefinedVariable() {
        doTest(UNDEFINED_VARIABLE);
    }

    @Test
    public void notDominated() {
        doTest(NOT_DOMINATED);
    }

    @Test
    public void notDominatedInBlock() {
        doTest(NOT_DOMINATED);
    }

    @Test
    public void notDominatedIncoming() {
        doTest(NOT_DOMINATED);
    }

    @Test
    public void phiInEntryBlock() {
        doTest(PHI_IN_ENTRY_BLOCK);
    }

    @Test
    public void missingIncoming() {
        doTest(MISSING_INCOMING);
    }

    @Test
    public void missingExceptionIncoming() {
        doTest(MISSING_INCOMING);
    }

    @Test
    public void incomingFromNonPredecessor() {
        doTest(INCOMING_FROM_NON_PREDECESSOR);
    }

    @Test
    public void multipleIncomings() {
        doTest(MULTIPLE_INCOMINGS);
    }

    @Test
    public void normalAndExceptionalEdge() {
        doTest(NORMAL_AND_EXCEPTIONAL_EDGE);
    }

    @Test
    public void handlerPhiTwoEntryValues() {
        doTest(HANDLER_PHI_ENTRY_VALUE);
    }

    @Test
    public void handlerPhiEntryValueNotFirst() {
        doTest(HANDLER_PHI_ENTRY_VALUE);
    }

    @Test
    public void handlerPhiOrder() {
        doTest(HANDLER_PHI_ORDER);
    }

    @Test
    public void handlerPhiEntryValueNotDominated() {
        doTest(NOT_DOMINATED);
    }

    @Test
    public void verifyThrows() {
        Program program = ListingParseUtils.parseFromResource(PREFIX + "handlerPhiOrder.txt");
        try {
            ProgramChecker.verify(program, 0, "in test");
            fail("Exception expected");
        } catch (ProgramInvariantException e) {
            assertEquals(1, e.getViolations().size());
            assertTrue(e.getMessage(), e.getMessage().contains("HANDLER_PHI_ORDER"));
            assertTrue(e.getMessage(), e.getMessage().contains("in test"));
        }
    }

    @Test
    public void verifyPasses() {
        Program program = ListingParseUtils.parseFromResource(PREFIX + "valid.txt");
        ProgramChecker.verify(program, 0, "in test");
    }

    private void doTest(ProgramViolation.Kind... expected) {
        doTest(0, expected);
    }

    private void doTest(int implicitVariableCount, ProgramViolation.Kind... expected) {
        Program program = ListingParseUtils.parseFromResource(PREFIX + name.getMethodName() + ".txt");
        List<ProgramViolation> violations = ProgramChecker.check(program, implicitVariableCount);
        List<ProgramViolation.Kind> actual = violations.stream()
                .map(ProgramViolation::getKind)
                .sorted()
                .collect(Collectors.toList());
        List<ProgramViolation.Kind> expectedList = Arrays.stream(expected).sorted().collect(Collectors.toList());
        assertEquals(violations.toString(), expectedList, actual);
    }
}
