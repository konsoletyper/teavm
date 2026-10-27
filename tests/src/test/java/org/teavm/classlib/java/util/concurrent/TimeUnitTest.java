/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.classlib.java.util.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigInteger;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class TimeUnitTest {
    @Test
    public void convert() {
        assertEquals(60, TimeUnit.MINUTES.toSeconds(1));
        assertEquals(30000, TimeUnit.SECONDS.toMillis(30));
        assertEquals(2, TimeUnit.DAYS.convert(48, TimeUnit.HOURS));
        assertEquals(180, TimeUnit.MINUTES.convert(3, TimeUnit.HOURS));
    }

    @Test
    public void conversionSaturates() {
        assertEquals(Long.MAX_VALUE, TimeUnit.SECONDS.toMillis(Long.MAX_VALUE));
        assertEquals(Long.MIN_VALUE, TimeUnit.SECONDS.toMillis(Long.MIN_VALUE));
        assertEquals(Long.MAX_VALUE, TimeUnit.DAYS.toNanos(Long.MAX_VALUE / 1000));
        assertEquals(Long.MIN_VALUE, TimeUnit.HOURS.toMicros(-Long.MAX_VALUE / 1000));
        assertEquals(Long.MAX_VALUE, TimeUnit.NANOSECONDS.convert(Long.MAX_VALUE, TimeUnit.SECONDS));
        assertEquals(Long.MAX_VALUE / 1000 * 1000, TimeUnit.SECONDS.toMillis(Long.MAX_VALUE / 1000));
        assertEquals(Long.MAX_VALUE, TimeUnit.NANOSECONDS.toNanos(Long.MAX_VALUE));
    }

    @Test
    public void sleepWithNonPositiveTimeoutReturns() throws InterruptedException {
        TimeUnit.SECONDS.sleep(0);
        TimeUnit.SECONDS.sleep(-1);
    }

    @Test
    public void timedWaitWithZeroTimeoutReturns() throws InterruptedException {
        Object lock = new Object();
        // Wakes the wait below if it waits forever, so the test fails instead of hanging.
        Thread notifier = new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return;
            }
            synchronized (lock) {
                lock.notifyAll();
            }
        });
        notifier.start();
        long start = System.currentTimeMillis();
        synchronized (lock) {
            TimeUnit.SECONDS.timedWait(lock, 0);
        }
        long elapsed = System.currentTimeMillis() - start;
        notifier.interrupt();
        assertTrue(elapsed < 1000, "timedWait(0) waited " + elapsed + " ms");
    }

    // Javadoc of convert(Duration): "Returns: the converted duration in this unit, or Long.MIN_VALUE if
    // conversion would negatively overflow, or Long.MAX_VALUE if it would positively overflow."
    @Test
    public void convertDuration() {
        assertEquals(3004005, TimeUnit.MICROSECONDS.convert(Duration.ofSeconds(3, 4_005_006)));
        assertEquals(2, TimeUnit.DAYS.convert(Duration.ofHours(49)));
        assertEquals(0, TimeUnit.NANOSECONDS.convert(Duration.ZERO));
        // A negative Duration rounds toward zero, as the other conversions do.
        assertEquals(-999, TimeUnit.MILLISECONDS.convert(Duration.ofSeconds(-1, 1)));
        assertEquals(-1, TimeUnit.SECONDS.convert(Duration.ofMillis(-1500)));
        assertEquals(0, TimeUnit.SECONDS.convert(Duration.ofNanos(-1)));
        assertEquals(-1, TimeUnit.DAYS.convert(Duration.ofHours(-47)));
        assertEquals(Long.MAX_VALUE, TimeUnit.NANOSECONDS.convert(Duration.ofSeconds(Long.MAX_VALUE)));
        assertEquals(Long.MIN_VALUE, TimeUnit.NANOSECONDS.convert(Duration.ofSeconds(Long.MIN_VALUE)));
        assertEquals(Long.MAX_VALUE, TimeUnit.SECONDS.convert(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999)));
        assertEquals(Long.MIN_VALUE, TimeUnit.SECONDS.convert(Duration.ofSeconds(Long.MIN_VALUE)));
        assertEquals(Long.MIN_VALUE + 1, TimeUnit.SECONDS.convert(Duration.ofSeconds(Long.MIN_VALUE, 1)));
        // The seconds fit, and the overflow comes from adding the nanoseconds.
        assertEquals(Long.MAX_VALUE, TimeUnit.NANOSECONDS.convert(Duration.ofSeconds(9223372036L, 854775807)));
        assertEquals(Long.MAX_VALUE, TimeUnit.NANOSECONDS.convert(Duration.ofSeconds(9223372036L, 854775808)));
        assertEquals(Long.MIN_VALUE, TimeUnit.NANOSECONDS.convert(Duration.ofSeconds(-9223372037L, 145224192)));
        assertEquals(Long.MIN_VALUE, TimeUnit.NANOSECONDS.convert(Duration.ofSeconds(-9223372037L, 145224191)));
        assertEquals(Long.MIN_VALUE + 1, TimeUnit.NANOSECONDS.convert(Duration.ofSeconds(-9223372037L, 145224193)));
        assertEquals(Long.MAX_VALUE - 1, TimeUnit.NANOSECONDS.convert(Duration.ofSeconds(9223372036L, 854775806)));
        assertEquals(Long.MAX_VALUE - 1, TimeUnit.MICROSECONDS.convert(Duration.ofSeconds(9223372036854L,
                775806999)));
        assertEquals(Long.MAX_VALUE, TimeUnit.MICROSECONDS.convert(Duration.ofSeconds(9223372036854L, 775807999)));
        assertEquals(Long.MAX_VALUE, TimeUnit.MICROSECONDS.convert(Duration.ofSeconds(9223372036854L, 775808000)));
        // Javadoc: "Throws: NullPointerException - if duration is null".
        assertThrows(NullPointerException.class, () -> TimeUnit.SECONDS.convert((Duration) null));
    }

    // Checks convert(Duration) against the exact quotient, rounded toward zero and clamped to the long range,
    // for every unit and for seconds near zero and near each overflow boundary.
    @Test
    public void convertDurationMatchesExactQuotient() {
        var random = new Random(73);
        var nanosPerSecond = BigInteger.valueOf(1_000_000_000);
        var max = BigInteger.valueOf(Long.MAX_VALUE);
        var min = BigInteger.valueOf(Long.MIN_VALUE);
        var secondsSamples = new ArrayList<Long>();
        // For a unit finer than a second, the largest number of seconds that converts without overflow.
        for (var unit : new TimeUnit[] { TimeUnit.NANOSECONDS, TimeUnit.MICROSECONDS, TimeUnit.MILLISECONDS }) {
            var boundary = Long.MAX_VALUE / unit.convert(1, TimeUnit.SECONDS);
            for (var delta = -2; delta <= 2; ++delta) {
                secondsSamples.add(boundary + delta);
                secondsSamples.add(-boundary - 1 + delta);
            }
        }
        for (var delta = 0; delta <= 2; ++delta) {
            secondsSamples.add(Long.MAX_VALUE - delta);
            secondsSamples.add(Long.MIN_VALUE + delta);
        }
        for (var s = -3L; s <= 3; ++s) {
            secondsSamples.add(s);
        }
        for (var i = 0; i < 100; ++i) {
            secondsSamples.add(random.nextLong());
            secondsSamples.add((long) random.nextInt());
        }
        var nanosSamples = new int[] { 0, 1, 999, 1000, 999_999, 1_000_000, 145224191, 145224192, 854775807,
                854775808, 999_999_999, 0, 0, 0, 0 };
        for (var i = 11; i < nanosSamples.length; ++i) {
            nanosSamples[i] = random.nextInt(1_000_000_000);
        }
        for (var seconds : secondsSamples) {
            for (var nanos : nanosSamples) {
                var duration = Duration.ofSeconds(seconds, nanos);
                var total = BigInteger.valueOf(duration.getSeconds()).multiply(nanosPerSecond)
                        .add(BigInteger.valueOf(duration.getNano()));
                for (var unit : TimeUnit.values()) {
                    var expected = total.divide(BigInteger.valueOf(unit.toNanos(1))).max(min).min(max).longValue();
                    assertEquals(expected, unit.convert(duration), () -> unit + ".convert(" + duration + ")");
                }
            }
        }
    }

    // Javadoc of convert(Duration): "For any TimeUnit unit, unit.convert(Duration.ofNanos(n)) is equivalent to
    // unit.convert(n, NANOSECONDS), and unit.convert(Duration.of(n, unit.toChronoUnit())) is equivalent to n (in
    // the absence of overflow)."
    @Test
    public void convertDurationSpecEquivalences() {
        var random = new Random(37);
        var samples = new ArrayList<Long>(List.of(0L, 1L, -1L, 999L, -999L, 1000L, -1000L, 999_999_999L,
                -999_999_999L, 1_000_000_000L, -1_000_000_000L, Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE - 1,
                Long.MIN_VALUE + 1));
        for (var i = 0; i < 300; ++i) {
            samples.add(random.nextLong());
            samples.add((long) random.nextInt());
        }
        for (var unit : TimeUnit.values()) {
            var limit = Long.MAX_VALUE / (unit.toSeconds(1) > 0 ? unit.toSeconds(1) : 1);
            for (var n : samples) {
                assertEquals(unit.convert(n, TimeUnit.NANOSECONDS), unit.convert(Duration.ofNanos(n)),
                        () -> unit + ", ofNanos(" + n + ")");
                if (n >= -limit && n <= limit) {
                    assertEquals(n, unit.convert(Duration.of(n, unit.toChronoUnit())),
                            () -> unit + ", of(" + n + ")");
                }
            }
        }
    }

    // Javadoc of toChronoUnit: "Converts this TimeUnit to the equivalent ChronoUnit." Javadoc of of: "Converts a
    // ChronoUnit to the equivalent TimeUnit. Throws: IllegalArgumentException - if chronoUnit has no equivalent
    // TimeUnit; NullPointerException - if chronoUnit is null".
    @Test
    public void chronoUnitMapping() {
        var units = TimeUnit.values();
        var chronoUnits = new ChronoUnit[] { ChronoUnit.NANOS, ChronoUnit.MICROS, ChronoUnit.MILLIS,
                ChronoUnit.SECONDS, ChronoUnit.MINUTES, ChronoUnit.HOURS, ChronoUnit.DAYS };
        assertEquals(chronoUnits.length, units.length);
        for (var i = 0; i < units.length; ++i) {
            assertEquals(chronoUnits[i], units[i].toChronoUnit());
            assertEquals(units[i], TimeUnit.of(chronoUnits[i]));
            assertEquals(units[i].toNanos(1), chronoUnits[i].getDuration().toNanos());
        }
        var mapped = List.of(chronoUnits);
        for (var chronoUnit : ChronoUnit.values()) {
            if (!mapped.contains(chronoUnit)) {
                assertThrows(IllegalArgumentException.class, () -> TimeUnit.of(chronoUnit), chronoUnit::toString);
            }
        }
        assertThrows(NullPointerException.class, () -> TimeUnit.of(null));
    }
}
