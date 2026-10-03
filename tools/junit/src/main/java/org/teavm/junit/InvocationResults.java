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
package org.teavm.junit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Results of individual invocations of a Jupiter parameterized test. All invocations run at once, within a
 * single run of the compiled test (see {@code TestEntryPoint.collectInvocationFailures}), and Jupiter
 * invocations on the JVM pick their results from here by index.
 */
final class InvocationResults {
    private final Map<Integer, Throwable> failures = new HashMap<>();
    private Throwable commonFailure;

    /** Runs every run of a test (one per platform/configuration), collecting failures of each invocation. */
    static InvocationResults collect(List<TestRun> runs, boolean cleanup) {
        var results = new InvocationResults();
        for (var run : runs) {
            var strategy = TeaVMTestInfrastructure.runners.get(run.getGroup().getKind());
            if (strategy == null) {
                continue;
            }
            try {
                strategy.runTest(run);
            } catch (Throwable e) {
                results.add(run, e);
            } finally {
                if (cleanup) {
                    strategy.cleanup();
                }
            }
        }
        return results;
    }

    /** Throws failure of the invocation with the given zero-based index, if any. */
    void check(int index) throws Throwable {
        if (commonFailure != null) {
            throw commonFailure;
        }
        var failure = failures.get(index);
        if (failure != null) {
            throw failure;
        }
    }

    private void add(TestRun run, Throwable e) {
        var text = findEncodedFailures(e);
        if (text == null) {
            // Failed not in a particular invocation, but e.g. while producing arguments, or crashed
            commonFailure = merge(commonFailure, e);
            return;
        }

        var start = text.indexOf(TestEntryPoint.INVOCATION_FAILURES_START)
                + TestEntryPoint.INVOCATION_FAILURES_START.length();
        var end = text.indexOf(TestEntryPoint.INVOCATION_FAILURES_END, start);
        var body = text.substring(start, end >= 0 ? end : text.length());
        var prefix = TestEntryPoint.INVOCATION_FAILURE_PREFIX;
        var index = body.indexOf(prefix);
        while (index >= 0) {
            var numberStart = index + prefix.length();
            var numberEnd = body.indexOf("@@", numberStart);
            var invocation = Integer.parseInt(body.substring(numberStart, numberEnd));
            var next = body.indexOf(prefix, numberEnd);
            var trace = body.substring(numberEnd + 2, next >= 0 ? next : body.length()).trim();
            var failure = new AssertionError(run.getName() + " failed:\n" + trace);
            failures.put(invocation, merge(failures.get(invocation), failure));
            index = next;
        }
    }

    private static String findEncodedFailures(Throwable e) {
        for (var t = e; t != null; t = t.getCause() != t ? t.getCause() : null) {
            var message = t.getMessage();
            if (message != null && message.contains(TestEntryPoint.INVOCATION_FAILURES_START)) {
                return message;
            }
        }
        return null;
    }

    private static Throwable merge(Throwable existing, Throwable failure) {
        if (existing == null) {
            return failure;
        }
        existing.addSuppressed(failure);
        return existing;
    }
}
