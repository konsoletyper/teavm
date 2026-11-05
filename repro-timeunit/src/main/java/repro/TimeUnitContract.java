/*
 *  Copyright 2026 TeaVM contributors.
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
package repro;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeoutException;
import org.teavm.classlib.java.util.concurrent.TTimeUnit;

public final class TimeUnitContract {
    private TimeUnitContract() {
    }

    public static void main(String[] args) {
        for (TTimeUnit unit : TTimeUnit.values()) {
            check(unit.toNanos(Long.MAX_VALUE) == Long.MAX_VALUE, "TTimeUnit positive saturation " + unit);
            check(unit.toNanos(Long.MIN_VALUE) == Long.MIN_VALUE, "TTimeUnit negative saturation " + unit);
            check(unit.convert(Long.MIN_VALUE, unit) == Long.MIN_VALUE, "TTimeUnit identity " + unit);
        }
        check(TTimeUnit.SECONDS.toNanos(18_446_744_074L) == Long.MAX_VALUE, "TTimeUnit positive wraparound");
        check(TTimeUnit.SECONDS.toNanos(-18_446_744_074L) == Long.MIN_VALUE, "TTimeUnit negative wraparound");

        for (TimeUnit unit : TimeUnit.values()) {
            check(unit.toNanos(Long.MAX_VALUE) == Long.MAX_VALUE, "positive saturation " + unit);
            check(unit.toNanos(Long.MIN_VALUE) == Long.MIN_VALUE, "negative saturation " + unit);
            check(unit.convert(Long.MIN_VALUE, unit) == Long.MIN_VALUE, "identity " + unit);
            check(unit.toNanos(0) == 0, "zero " + unit);
        }
        check(TimeUnit.SECONDS.toNanos(18_446_744_074L) == Long.MAX_VALUE, "positive wraparound");
        check(TimeUnit.SECONDS.toNanos(-18_446_744_074L) == Long.MIN_VALUE, "negative wraparound");
        check(TimeUnit.SECONDS.toNanos(9_223_372_036L) == 9_223_372_036_000_000_000L, "last exact seconds");
        check(TimeUnit.SECONDS.toNanos(9_223_372_037L) == Long.MAX_VALUE, "first saturated seconds");
        check(TimeUnit.SECONDS.toNanos(-9_223_372_036L) == -9_223_372_036_000_000_000L, "negative exact seconds");
        check(TimeUnit.SECONDS.toNanos(-9_223_372_037L) == Long.MIN_VALUE, "negative saturated seconds");
        check(TimeUnit.MICROSECONDS.convert(Long.MAX_VALUE, TimeUnit.DAYS) == Long.MAX_VALUE, "convert saturation");
        check(TimeUnit.HOURS.convert(Long.MIN_VALUE, TimeUnit.DAYS) == Long.MIN_VALUE, "negative convert saturation");
        check(TimeUnit.DAYS.convert(Long.MAX_VALUE, TimeUnit.NANOSECONDS) == 106_751, "narrowing positive");
        check(TimeUnit.DAYS.convert(Long.MIN_VALUE, TimeUnit.NANOSECONDS) == -106_751, "narrowing negative");
        check(TimeUnit.MINUTES.toHours(-119) == -1, "truncation toward zero");
        try {
            TimeUnit.SECONDS.convert(1, null);
            throw new AssertionError("Expected null source unit failure");
        } catch (NullPointerException expected) {
            // Expected.
        }

        FutureTask<Integer> completed = new FutureTask<>(() -> 42);
        completed.run();
        try {
            check(completed.get(-1, TimeUnit.SECONDS) == 42, "completed future with negative timeout");
        } catch (Exception e) {
            throw new AssertionError("Completed future should not time out", e);
        }
        FutureTask<Integer> incomplete = new FutureTask<>(() -> 7);
        try {
            incomplete.get(-1, TimeUnit.SECONDS);
            throw new AssertionError("Incomplete future with negative timeout should time out");
        } catch (TimeoutException expected) {
            // Expected: nonpositive timeout does not wait.
        } catch (Exception e) {
            throw new AssertionError("Unexpected exception from timed get", e);
        }
        System.out.println("TimeUnit contract passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
