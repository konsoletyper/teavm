/*
 *  Copyright 2017 Alexey Andreev.
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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.teavm.model.ListingParseUtils;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.text.ListingBuilder;
import org.teavm.model.util.PhiUpdater;

public class PhiUpdaterTest {
    private static final String PREFIX = "model/util/phi-updater/";
    @Rule
    public TestName name = new TestName();

    @Test
    public void exceptionPhi() {
        doTest();
    }

    @Test
    public void exceptionPhiMultiple() {
        doTest();
    }

    @Test
    public void exceptionPhiFromSinglePhi() {
        doTest();
    }

    @Test
    public void existingExceptionPhi() {
        doTest();
    }

    @Test
    public void phiIncoming() {
        doTest();
    }

    private void doTest() {
        String originalPath = PREFIX + name.getMethodName() + ".original.txt";
        String expectedPath = PREFIX + name.getMethodName() + ".expected.txt";
        Program original = ListingParseUtils.parseFromResource(originalPath);
        Program expected = ListingParseUtils.parseFromResource(expectedPath);

        new PhiUpdater().updatePhis(original, new Variable[0]);

        String originalText = new ListingBuilder().buildListing(original, "");
        String expectedText = new ListingBuilder().buildListing(expected, "");
        Assert.assertEquals(expectedText, originalText);

        new PhiUpdater().updatePhis(original, new Variable[0]);
        String repeatedText = new ListingBuilder().buildListing(original, "");
        Assert.assertEquals("Repeated update", originalText, repeatedText);
    }
}
