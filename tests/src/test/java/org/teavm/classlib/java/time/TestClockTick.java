/*
 *  Copyright 2020 Alexey Andreev.
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
/*
 * Copyright (c) 2007-present Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.teavm.classlib.java.time;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;
import org.testng.annotations.Test;

/**
 * Test tick clock.
 */
@Test
@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class TestClockTick extends AbstractTest {

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final Duration AMOUNT = Duration.ofSeconds(2);
    private static final ZonedDateTime ZDT = LocalDateTime.of(2008, 6, 30, 11, 30, 10, 500)
            .atZone(ZoneOffset.ofHours(2));
    private static final Instant INSTANT = ZDT.toInstant();

    //-----------------------------------------------------------------------
    public void test_tick_ClockDuration_250millis() {
        for (int i = 0; i < 1000; i++) {
            Clock test = Clock.tick(Clock.fixed(ZDT.withNano(i * 1000000).toInstant(), PARIS), Duration.ofMillis(250));
            assertEquals(test.instant(), ZDT.withNano((i / 250) * 250000000).toInstant());
            assertEquals(test.getZone(), PARIS);
        }
    }

    public void test_tick_ClockDuration_250micros() {
        for (int i = 0; i < 1000; i++) {
            Clock test = Clock.tick(Clock.fixed(ZDT.withNano(i * 1000).toInstant(), PARIS), Duration.ofNanos(250000));
            assertEquals(test.instant(), ZDT.withNano((i / 250) * 250000).toInstant());
            assertEquals(test.getZone(), PARIS);
        }
    }

    public void test_tick_ClockDuration_20nanos() {
        for (int i = 0; i < 1000; i++) {
            Clock test = Clock.tick(Clock.fixed(ZDT.withNano(i).toInstant(), PARIS), Duration.ofNanos(20));
            assertEquals(test.instant(), ZDT.withNano((i / 20) * 20).toInstant());
            assertEquals(test.getZone(), PARIS);
        }
    }

    public void test_tick_ClockDuration_zeroDuration() {
        Clock underlying = Clock.system(PARIS);
        Clock test = Clock.tick(underlying, Duration.ZERO);
        assertSame(test, underlying);  // spec says same
    }

    public void test_tick_ClockDuration_1nsDuration() {
        Clock underlying = Clock.system(PARIS);
        Clock test = Clock.tick(underlying, Duration.ofNanos(1));
        assertSame(test, underlying);  // spec says same
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void test_tick_ClockDuration_maxDuration() {
        Clock.tick(Clock.systemUTC(), Duration.ofSeconds(Long.MAX_VALUE));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_tick_ClockDuration_subMilliNotDivisible_123ns() {
        Clock.tick(Clock.systemUTC(), Duration.ofSeconds(0, 123));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_tick_ClockDuration_subMilliNotDivisible_999ns() {
        Clock.tick(Clock.systemUTC(), Duration.ofSeconds(0, 999));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_tick_ClockDuration_subMilliNotDivisible_999999999ns() {
        Clock.tick(Clock.systemUTC(), Duration.ofSeconds(0, 999999999));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_tick_ClockDuration_negative1ns() {
        Clock.tick(Clock.systemUTC(), Duration.ofSeconds(0, -1));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_tick_ClockDuration_negative1s() {
        Clock.tick(Clock.systemUTC(), Duration.ofSeconds(-1));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_tick_ClockDuration_nullClock() {
        Clock.tick(null, Duration.ZERO);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_tick_ClockDuration_nullDuration() {
        Clock.tick(Clock.systemUTC(), null);
    }

    //-----------------------------------------------------------------------
    public void test_tickSeconds_ZoneId() throws Exception {
        Clock test = Clock.tickSeconds(PARIS);
        assertEquals(test.getZone(), PARIS);
        assertEquals(test.instant().getNano(), 0);
        Thread.sleep(100);
        assertEquals(test.instant().getNano(), 0);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_tickSeconds_ZoneId_nullZoneId() {
        Clock.tickSeconds(null);
    }

    //-----------------------------------------------------------------------
    public void test_tickMinutes_ZoneId() {
        Clock test = Clock.tickMinutes(PARIS);
        assertEquals(test.getZone(), PARIS);
        Instant instant = test.instant();
        assertEquals(instant.getEpochSecond() % 60, 0);
        assertEquals(instant.getNano(), 0);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_tickMinutes_ZoneId_nullZoneId() {
        Clock.tickMinutes(null);
    }

    //-------------------------------------------------------------------------
    public void test_withZone() {
        Clock test = Clock.tick(Clock.system(PARIS), Duration.ofMillis(500));
        Clock changed = test.withZone(MOSCOW);
        assertEquals(test.getZone(), PARIS);
        assertEquals(changed.getZone(), MOSCOW);
    }

    public void test_withZone_same() {
        Clock test = Clock.tick(Clock.system(PARIS), Duration.ofMillis(500));
        Clock changed = test.withZone(PARIS);
        assertSame(test, changed);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_withZone_null() {
        Clock.tick(Clock.system(PARIS), Duration.ofMillis(500)).withZone(null);
    }

    //-----------------------------------------------------------------------
    public void test__equals() {
        Clock a = Clock.tick(Clock.system(PARIS), Duration.ofMillis(500));
        Clock b = Clock.tick(Clock.system(PARIS), Duration.ofMillis(500));
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), true);
        assertEquals(b.equals(a), true);
        assertEquals(b.equals(b), true);

        Clock c = Clock.tick(Clock.system(MOSCOW), Duration.ofMillis(500));
        assertEquals(a.equals(c), false);

        Clock d = Clock.tick(Clock.system(PARIS), Duration.ofMillis(499));
        assertEquals(a.equals(d), false);

        assertEquals(a.equals(null), false);
        assertEquals(a.equals("other type"), false);
        assertEquals(a.equals(Clock.systemUTC()), false);
    }

    public void test_hashCode() {
        Clock a = Clock.tick(Clock.system(PARIS), Duration.ofMillis(500));
        Clock b = Clock.tick(Clock.system(PARIS), Duration.ofMillis(500));
        assertEquals(a.hashCode(), a.hashCode());
        assertEquals(a.hashCode(), b.hashCode());

        Clock c = Clock.tick(Clock.system(MOSCOW), Duration.ofMillis(500));
        assertEquals(a.hashCode() == c.hashCode(), false);

        Clock d = Clock.tick(Clock.system(PARIS), Duration.ofMillis(499));
        assertEquals(a.hashCode() == d.hashCode(), false);
    }

    //-----------------------------------------------------------------------
    public void test_toString() {
        Clock test = Clock.tick(Clock.systemUTC(), Duration.ofMillis(500));
        assertEquals(test.toString(), "TickClock[SystemClock[Z],PT0.5S]");
    }

}
