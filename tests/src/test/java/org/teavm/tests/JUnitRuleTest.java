/*
 *  Copyright 2026 Carl Stainton.
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
package org.teavm.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/**
 * JUnit rules wrapping a test that TeaVM compiled and a browser ran.
 *
 * <p>The rules are applied in declaration order, each wrapping the one before, so the last field
 * declared is the outermost. That is what JUnit's own {@code RunRules} does with the list it is
 * given.
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM // The JVM comparison runner invokes methods directly, without applying JUnit rules.
public class JUnitRuleTest {

    static final List<String> events = new ArrayList<>();
    static String describedAs;
    static int depth;

    @Rule
    public TestRule first = new RecordingRule("first");

    @Rule
    public TestRule second = new RecordingRule("second");

    @Rule
    public TestRule fromMethod() {
        return new RecordingRule("fromMethod");
    }

    @Test
    public void bothRulesWrappedThisTest() {
        events.add("test");

        assertEquals(
                Arrays.asList("second before", "first before", "fromMethod before", "test"), events);
    }

    @Test
    public void theRuleSawTheTestDescription() {
        assertTrue(describedAs, describedAs.contains("JUnitRuleTest"));
        assertTrue(describedAs, describedAs.contains("theRuleSawTheTestDescription"));
    }

    @Test
    public void theTestBodyStillRuns() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void aRuleDeclaredAsAMethodIsAppliedInsideTheFields() {
        events.add("test");

        // JUnit applies methods before fields, so the method rule sits inside both fields.
        assertEquals(
                Arrays.asList("second before", "first before", "fromMethod before", "test"), events);
    }

    static final class RecordingRule implements TestRule {

        private final String name;

        RecordingRule(String name) {
            this.name = name;
        }

        @Override
        public Statement apply(Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    if (depth++ == 0) {
                        events.clear();
                    }
                    describedAs = description.getDisplayName();
                    events.add(name + " before");
                    try {
                        base.evaluate();
                    } finally {
                        depth--;
                        events.add(name + " after");
                        if (depth == 0) {
                            assertEquals(Arrays.asList("fromMethod after", "first after", "second after"),
                                    events.subList(events.size() - 3, events.size()));
                        }
                    }
                }
            };
        }
    }
}
