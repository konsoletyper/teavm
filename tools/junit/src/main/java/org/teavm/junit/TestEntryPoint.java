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

final class TestEntryPoint {
    static final String INVOCATION_FAILURES_START = "@@teavm-invocation-failures@@";
    static final String INVOCATION_FAILURE_PREFIX = "@@teavm-invocation-failure#";
    static final String INVOCATION_FAILURES_END = "@@teavm-invocation-failures-end@@";

    private static Object testCase;

    /**
     * Set by generated {@link #launchers} code for tests where every invocation is reported separately
     * (Jupiter parameterized tests). In this case all invocations run even if some of them fail, and failures
     * are reported together, see {@link #reportInvocationFailures(List, List)}.
     */
    private static boolean collectInvocationFailures;

    /**
     * Set by generated {@link #launchers} code when every invocation must run against a fresh test instance,
     * like Jupiter does by default.
     */
    private static boolean instancePerInvocation;

    private TestEntryPoint() {
    }

    public static void run(String name) throws Throwable {
        List<Launcher> launchers = new ArrayList<>();
        testCase = createTestCase();
        launchers(name, launchers);
        if (!collectInvocationFailures) {
            for (Launcher launcher : launchers) {
                launch(launcher);
            }
            return;
        }

        List<Integer> failedIndexes = new ArrayList<>();
        List<Throwable> failures = new ArrayList<>();
        for (int i = 0; i < launchers.size(); ++i) {
            try {
                if (instancePerInvocation && i > 0) {
                    testCase = createTestCase();
                }
                launch(launchers.get(i));
            } catch (Throwable e) {
                failedIndexes.add(i);
                failures.add(e);
            }
        }
        if (!failures.isEmpty()) {
            reportInvocationFailures(failedIndexes, failures);
        }
    }

    private static void launch(Launcher launcher) throws Throwable {
        before();
        try {
            launcher.launch(testCase);
        } finally {
            try {
                after();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Encodes failures of individual invocations into a message of a single exception. Message is the only
     * part of an exception that reliably survives the trip from every backend back to the JVM, where
     * {@link InvocationResults} decodes it.
     */
    private static void reportInvocationFailures(List<Integer> indexes, List<Throwable> failures) {
        StringBuilder sb = new StringBuilder();
        sb.append(failures.size()).append(" invocation(s) failed\n").append(INVOCATION_FAILURES_START).append('\n');
        for (int i = 0; i < failures.size(); ++i) {
            sb.append(INVOCATION_FAILURE_PREFIX).append(indexes.get(i)).append("@@\n");
            printStackTrace(failures.get(i), sb);
        }
        sb.append(INVOCATION_FAILURES_END);
        throw new AssertionError(sb.toString());
    }

    static void printStackTrace(Throwable e, StringBuilder sb) {
        sb.append(e.getClass().getName());
        String message = e.getLocalizedMessage();
        if (message != null) {
            sb.append(": ").append(message);
        }
        sb.append("\n");
        StackTraceElement[] stackTrace = e.getStackTrace();
        if (stackTrace != null) {
            for (StackTraceElement element : stackTrace) {
                sb.append("\tat ").append(element).append("\n");
            }
        }
        if (e.getCause() != null && e.getCause() != e) {
            sb.append("Caused by: ");
            printStackTrace(e.getCause(), sb);
        }
    }

    private static native Object createTestCase();

    private static native void before();

    private static native void launchers(String name, List<Launcher> result) throws Throwable;

    private static native void after();

    public static void main(String[] args) throws Throwable {
        run(args.length == 1 ? args[0] : null);
    }

    interface Launcher {
        void launch(Object testCase) throws Throwable;
    }
}
