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
package org.teavm.model.optimization.test;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.teavm.model.ListingParseUtils;
import org.teavm.model.Program;
import org.teavm.model.optimization.UnreachableBasicBlockEliminator;
import org.teavm.model.text.ListingBuilder;

public class UnreachableBasicBlockEliminationTest {
    private static final String PREFIX = "model/optimization/unreachable-basic-block-elimination/";
    @Rule
    public TestName name = new TestName();

    @Test
    public void parameterIncomingKept() {
        doTest();
    }

    @Test
    public void phiIncomingKept() {
        doTest();
    }

    @Test
    public void staleIncomingWithDefinedValueRemoved() {
        doTest();
    }

    @Test
    public void handlerIncomingFromThrowingBlockKept() {
        doTest();
    }

    @Test
    public void handlerIncomingsOfTruncatedBlock() {
        doTest();
    }

    @Test
    public void handlerIncomingDefinedByPhiKept() {
        doTest();
    }

    private void doTest() {
        String originalPath = PREFIX + name.getMethodName() + ".original.txt";
        String expectedPath = PREFIX + name.getMethodName() + ".expected.txt";
        Program original = ListingParseUtils.parseFromResource(originalPath);
        Program expected = ListingParseUtils.parseFromResource(expectedPath);

        new UnreachableBasicBlockEliminator().optimize(original, 0);

        String originalText = new ListingBuilder().buildListing(original, "");
        String expectedText = new ListingBuilder().buildListing(expected, "");
        Assert.assertEquals(expectedText, originalText);
    }
}
