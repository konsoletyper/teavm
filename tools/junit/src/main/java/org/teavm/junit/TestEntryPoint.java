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
package org.teavm.junit;

import java.util.ArrayList;
import java.util.List;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

final class TestEntryPoint {
    private static Object testCase;

    private TestEntryPoint() {
    }

    public static void run(String name) throws Throwable {
        List<Launcher> launchers = new ArrayList<>();
        testCase = createTestCase();
        launchers(name, launchers);
        for (Launcher launcher : launchers) {
            applyRules(new LaunchStatement(launcher), name).evaluate();
        }
    }

    private static native Object createTestCase();

    private static native void before();

    private static native void launchers(String name, List<Launcher> result) throws Throwable;

    private static native void after();

    /**
     * Wraps the statement in the test case's rules, and returns it unchanged when there are none.
     */
    private static native Statement applyRules(Statement base, String name);

    static Description describe(String className, String name) {
        return Description.createTestDescription(className, name != null ? name : className);
    }

    public static void main(String[] args) throws Throwable {
        run(args.length == 1 ? args[0] : null);
    }

    interface Launcher {
        void launch(Object testCase) throws Throwable;
    }

    private static final class LaunchStatement extends Statement {
        private final Launcher launcher;

        LaunchStatement(Launcher launcher) {
            this.launcher = launcher;
        }

        @Override
        public void evaluate() throws Throwable {
            before();
            Throwable failure = null;
            try {
                launcher.launch(testCase);
            } catch (Throwable e) {
                failure = e;
            }
            try {
                after();
            } catch (Throwable e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
            if (failure != null) {
                throw failure;
            }
        }
    }
}
