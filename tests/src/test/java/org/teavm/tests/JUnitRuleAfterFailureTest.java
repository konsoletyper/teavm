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

import static org.junit.Assert.assertSame;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** Checks that a teardown failure reaches the rule wrapping the compiled test. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM // The JVM comparison runner invokes methods directly, without applying JUnit rules.
public class JUnitRuleAfterFailureTest {
    private static final AssertionError AFTER_FAILURE = new AssertionError("after failed");

    @Rule
    public TestRule expectAfterFailure = (base, description) -> new Statement() {
        @Override
        public void evaluate() throws Throwable {
            try {
                base.evaluate();
            } catch (AssertionError failure) {
                assertSame(AFTER_FAILURE, failure);
                return;
            }
            throw new AssertionError("The @After failure was swallowed");
        }
    };

    @After
    public void failAfterwards() {
        throw AFTER_FAILURE;
    }

    @Test
    public void bodyPassesBeforeTeardownFails() {
    }
}
