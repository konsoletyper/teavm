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

public class TestClock_Fixed extends AbstractTest {

    private static final TZoneId MOSCOW = TZoneId.of("Europe/Moscow");

    private static final TZoneId PARIS = TZoneId.of("Europe/Paris");

    private static final TInstant INSTANT = TLocalDateTime.of(2008, 6, 30, 11, 30, 10, 500)
            .atZone(TZoneOffset.ofHours(2)).toInstant();

    @Test
    public void test_fixed_InstantZoneId() {

        TClock test = TClock.fixed(INSTANT, PARIS);
        assertEquals(test.instant(), INSTANT);
        assertEquals(test.getZone(), PARIS);
    }

    @Test(expected = NullPointerException.class)
    public void test_fixed_InstantZoneId_nullInstant() {

        TClock.fixed(null, PARIS);
    }

    @Test(expected = NullPointerException.class)
    public void test_fixed_InstantZoneId_nullZoneId() {

        TClock.fixed(INSTANT, null);
    }

    @Test
    public void test_withZone() {

        TClock test = TClock.fixed(INSTANT, PARIS);
        TClock changed = test.withZone(MOSCOW);
        assertEquals(test.getZone(), PARIS);
        assertEquals(changed.getZone(), MOSCOW);
    }

    @Test
    public void test_withZone_same() {

        TClock test = TClock.fixed(INSTANT, PARIS);
        TClock changed = test.withZone(PARIS);
        assertSame(test, changed);
    }

    @Test(expected = NullPointerException.class)
    public void test_withZone_null() {

        TClock.fixed(INSTANT, PARIS).withZone(null);
    }

    @Test
    public void test_equals() {

        TClock a = TClock.fixed(INSTANT, TZoneOffset.UTC);
        TClock b = TClock.fixed(INSTANT, TZoneOffset.UTC);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), true);
        assertEquals(b.equals(a), true);
        assertEquals(b.equals(b), true);

        TClock c = TClock.fixed(INSTANT, PARIS);
        assertEquals(a.equals(c), false);

        TClock d = TClock.fixed(INSTANT.minusNanos(1), TZoneOffset.UTC);
        assertEquals(a.equals(d), false);

        assertEquals(a.equals(null), false);
        assertEquals(a.equals("other type"), false);
        assertEquals(a.equals(TClock.systemUTC()), false);
    }

    @Test
    public void test_hashCode() {

        TClock a = TClock.fixed(INSTANT, TZoneOffset.UTC);
        TClock b = TClock.fixed(INSTANT, TZoneOffset.UTC);
        assertEquals(a.hashCode(), a.hashCode());
        assertEquals(a.hashCode(), b.hashCode());

        TClock c = TClock.fixed(INSTANT, PARIS);
        assertEquals(a.hashCode() == c.hashCode(), false);

        TClock d = TClock.fixed(INSTANT.minusNanos(1), TZoneOffset.UTC);
        assertEquals(a.hashCode() == d.hashCode(), false);
    }

    @Test
    public void test_toString() {

        TClock test = TClock.fixed(INSTANT, PARIS);
        assertEquals(test.toString(), "FixedClock[2008-06-30T09:30:10.000000500Z,Europe/Paris]");
    }

}
