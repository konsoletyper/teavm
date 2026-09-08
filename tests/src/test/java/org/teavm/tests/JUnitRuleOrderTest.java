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
 * The {@code order} attribute, which JUnit documents as "rules with a higher value are inner".
 *
 * <p>Declaration order here is the reverse of application order, so only {@code order} can be
 * deciding the outcome.
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM // The JVM comparison runner invokes methods directly, without applying JUnit rules.
public class JUnitRuleOrderTest {

    static final List<String> events = new ArrayList<>();

    @Rule(order = 10)
    public TestRule declaredFirstWithHighOrder = new RecordingRule("high");

    @Rule(order = 1)
    public TestRule declaredSecondWithLowOrder = new RecordingRule("low");

    @Test
    public void theHigherOrderRuleIsTheInnerOne() {
        events.add("test");

        assertEquals(Arrays.asList("low before", "high before", "test"), events);
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
                    if (name.equals("low")) {
                        events.clear();
                    }
                    events.add(name + " before");
                    base.evaluate();
                }
            };
        }
    }
}
