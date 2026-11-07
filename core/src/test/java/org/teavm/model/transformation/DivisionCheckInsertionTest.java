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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.teavm.model.ListingParseUtils;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.text.ListingBuilder;

public class DivisionCheckInsertionTest {
    private static final String PREFIX = "model/transformation/division-check-insertion/";
    private static final MethodReference METHOD = new MethodReference("Test", "test", ValueType.INTEGER);
    @Rule
    public TestName name = new TestName();

    @Test
    public void unknownDivisor() {
        doTest();
    }

    @Test
    public void constantDivisor() {
        doTest();
    }

    @Test
    public void zeroDivisor() {
        doTest();
    }

    @Test
    public void minusOneDivisor() {
        doTest();
    }

    @Test
    public void branchNotZero() {
        doTest();
    }

    @Test
    public void branchComparison() {
        doTest();
    }

    @Test
    public void branchLong() {
        doTest();
    }

    @Test
    public void branchBypassed() {
        doTest();
    }

    @Test
    public void joinNonZero() {
        doTest();
    }

    @Test
    public void joinWithZero() {
        doTest();
    }

    @Test
    public void joinEdgeRefinement() {
        doTest();
    }

    @Test
    public void loopCounterNotSupported() {
        doTest();
    }

    @Test
    public void loopCounterOverflow() {
        doTest();
    }

    @Test
    public void repeatedDivision() {
        doTest();
    }

    @Test
    public void divisionInHandler() {
        doTest();
    }

    @Test
    public void switchCases() {
        doTest();
    }

    @Test
    public void bitMask() {
        doTest();
    }

    @Test
    public void charDivisor() {
        doTest();
    }

    private void doTest() {
        String originalPath = PREFIX + name.getMethodName() + ".original.txt";
        String expectedPath = PREFIX + name.getMethodName() + ".expected.txt";
        Program original = ListingParseUtils.parseFromResource(originalPath);
        Program expected = ListingParseUtils.parseFromResource(expectedPath);

        new DivisionCheckInsertion().transformProgram(original, METHOD);

        String originalText = new ListingBuilder().buildListing(original, "");
        String expectedText = new ListingBuilder().buildListing(expected, "");
        Assert.assertEquals(expectedText, originalText);
    }
}
