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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class TestClock_Tick extends AbstractTest {

    private static final TZoneId MOSCOW = TZoneId.of("Europe/Moscow");

    private static final TZoneId PARIS = TZoneId.of("Europe/Paris");

    private static final TDuration AMOUNT = TDuration.ofSeconds(2);

    private static final TZonedDateTime ZDT = TLocalDateTime.of(2008, 6, 30, 11, 30, 10, 500)
            .atZone(TZoneOffset.ofHours(2));

    private static final TInstant INSTANT = ZDT.toInstant();

    @Test
    public void test_tick_ClockDuration_250millis() {

        for (int i = 0; i < 1000; i++) {
            TClock test = TClock.tick(TClock.fixed(ZDT.withNano(i * 1000000).toInstant(), PARIS),
                    TDuration.ofMillis(250));
            assertEquals(test.instant(), ZDT.withNano((i / 250) * 250000000).toInstant());
            assertEquals(test.getZone(), PARIS);
        }
    }

    @Test
    public void test_tick_ClockDuration_250micros() {

        for (int i = 0; i < 1000; i++) {
            TClock test = TClock.tick(TClock.fixed(ZDT.withNano(i * 1000).toInstant(), PARIS),
                    TDuration.ofNanos(250000));
            assertEquals(test.instant(), ZDT.withNano((i / 250) * 250000).toInstant());
            assertEquals(test.getZone(), PARIS);
        }
    }

    @Test
    public void test_tick_ClockDuration_20nanos() {

        for (int i = 0; i < 1000; i++) {
            TClock test = TClock.tick(TClock.fixed(ZDT.withNano(i).toInstant(), PARIS), TDuration.ofNanos(20));
            assertEquals(test.instant(), ZDT.withNano((i / 20) * 20).toInstant());
            assertEquals(test.getZone(), PARIS);
        }
    }

    @Test
    public void test_tick_ClockDuration_zeroDuration() {

        TClock underlying = TClock.system(PARIS);
        TClock test = TClock.tick(underlying, TDuration.ZERO);
        assertSame(test, underlying); // spec says same
    }

    @Test
    public void test_tick_ClockDuration_1nsDuration() {

        TClock underlying = TClock.system(PARIS);
        TClock test = TClock.tick(underlying, TDuration.ofNanos(1));
        assertSame(test, underlying); // spec says same
    }

    @Test(expected = ArithmeticException.class)
    public void test_tick_ClockDuration_maxDuration() {

        TClock.tick(TClock.systemUTC(), TDuration.ofSeconds(Long.MAX_VALUE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_tick_ClockDuration_subMilliNotDivisible_123ns() {

        TClock.tick(TClock.systemUTC(), TDuration.ofSeconds(0, 123));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_tick_ClockDuration_subMilliNotDivisible_999ns() {

        TClock.tick(TClock.systemUTC(), TDuration.ofSeconds(0, 999));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_tick_ClockDuration_subMilliNotDivisible_999999999ns() {

        TClock.tick(TClock.systemUTC(), TDuration.ofSeconds(0, 999999999));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_tick_ClockDuration_negative1ns() {

        TClock.tick(TClock.systemUTC(), TDuration.ofSeconds(0, -1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_tick_ClockDuration_negative1s() {

        TClock.tick(TClock.systemUTC(), TDuration.ofSeconds(-1));
    }

    @Test(expected = NullPointerException.class)
    public void test_tick_ClockDuration_nullClock() {

        TClock.tick(null, TDuration.ZERO);
    }

    @Test(expected = NullPointerException.class)
    public void test_tick_ClockDuration_nullDuration() {

        TClock.tick(TClock.systemUTC(), null);
    }

    @Test
    public void test_tickSeconds_ZoneId() throws Exception {

        TClock test = TClock.tickSeconds(PARIS);
        assertEquals(test.getZone(), PARIS);
        assertEquals(test.instant().getNano(), 0);
        Thread.sleep(100);
        assertEquals(test.instant().getNano(), 0);
    }

    @Test(expected = NullPointerException.class)
    public void test_tickSeconds_ZoneId_nullZoneId() {

        TClock.tickSeconds(null);
    }

    @Test
    public void test_tickMinutes_ZoneId() {

        TClock test = TClock.tickMinutes(PARIS);
        assertEquals(test.getZone(), PARIS);
        TInstant instant = test.instant();
        assertEquals(instant.getEpochSecond() % 60, 0);
        assertEquals(instant.getNano(), 0);
    }

    @Test(expected = NullPointerException.class)
    public void test_tickMinutes_ZoneId_nullZoneId() {

        TClock.tickMinutes(null);
    }

    @Test
    public void test_withZone() {

        TClock test = TClock.tick(TClock.system(PARIS), TDuration.ofMillis(500));
        TClock changed = test.withZone(MOSCOW);
        assertEquals(test.getZone(), PARIS);
        assertEquals(changed.getZone(), MOSCOW);
    }

    @Test
    public void test_withZone_same() {

        TClock test = TClock.tick(TClock.system(PARIS), TDuration.ofMillis(500));
        TClock changed = test.withZone(PARIS);
        assertSame(test, changed);
    }

    @Test(expected = NullPointerException.class)
    public void test_withZone_null() {

        TClock.tick(TClock.system(PARIS), TDuration.ofMillis(500)).withZone(null);
    }

    @Test
    public void test__equals() {

        TClock a = TClock.tick(TClock.system(PARIS), TDuration.ofMillis(500));
        TClock b = TClock.tick(TClock.system(PARIS), TDuration.ofMillis(500));
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), true);
        assertEquals(b.equals(a), true);
        assertEquals(b.equals(b), true);

        TClock c = TClock.tick(TClock.system(MOSCOW), TDuration.ofMillis(500));
        assertEquals(a.equals(c), false);

        TClock d = TClock.tick(TClock.system(PARIS), TDuration.ofMillis(499));
        assertEquals(a.equals(d), false);

        assertEquals(a.equals(null), false);
        assertEquals(a.equals("other type"), false);
        assertEquals(a.equals(TClock.systemUTC()), false);
    }

    @Test
    public void test_hashCode() {

        TClock a = TClock.tick(TClock.system(PARIS), TDuration.ofMillis(500));
        TClock b = TClock.tick(TClock.system(PARIS), TDuration.ofMillis(500));
        assertEquals(a.hashCode(), a.hashCode());
        assertEquals(a.hashCode(), b.hashCode());

        TClock c = TClock.tick(TClock.system(MOSCOW), TDuration.ofMillis(500));
        assertEquals(a.hashCode() == c.hashCode(), false);

        TClock d = TClock.tick(TClock.system(PARIS), TDuration.ofMillis(499));
        assertEquals(a.hashCode() == d.hashCode(), false);
    }

    @Test
    public void test_toString() {

        TClock test = TClock.tick(TClock.systemUTC(), TDuration.ofMillis(500));
        assertEquals(test.toString(), "TickClock[SystemClock[Z],PT0.5S]");
    }

}
