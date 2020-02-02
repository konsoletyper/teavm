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
import static org.junit.Assert.fail;

import org.junit.Test;

public class TestClock_System extends AbstractTest {

    private static final TZoneId MOSCOW = TZoneId.of("Europe/Moscow");

    private static final TZoneId PARIS = TZoneId.of("Europe/Paris");

    @Test
    public void test_instant() {

        TClock system = TClock.systemUTC();
        assertEquals(system.getZone(), TZoneOffset.UTC);
        for (int i = 0; i < 10000; i++) {
            // assume can eventually get these within 10 milliseconds
            TInstant instant = system.instant();
            long systemMillis = System.currentTimeMillis();
            if (systemMillis - instant.toEpochMilli() < 10) {
                return; // success
            }
        }
        fail();
    }

    @Test
    public void test_millis() {

        TClock system = TClock.systemUTC();
        assertEquals(system.getZone(), TZoneOffset.UTC);
        for (int i = 0; i < 10000; i++) {
            // assume can eventually get these within 10 milliseconds
            long instant = system.millis();
            long systemMillis = System.currentTimeMillis();
            if (systemMillis - instant < 10) {
                return; // success
            }
        }
        fail();
    }

    @Test
    public void test_systemUTC() {

        TClock test = TClock.systemUTC();
        assertEquals(test.getZone(), TZoneOffset.UTC);
        assertEquals(test, TClock.system(TZoneOffset.UTC));
    }

    @Test
    public void test_systemDefaultZone() {

        TClock test = TClock.systemDefaultZone();
        assertEquals(test.getZone(), TZoneId.systemDefault());
        assertEquals(test, TClock.system(TZoneId.systemDefault()));
    }

    @Test
    public void test_system_ZoneId() {

        TClock test = TClock.system(PARIS);
        assertEquals(test.getZone(), PARIS);
    }

    @Test(expected = NullPointerException.class)
    public void test_zoneId_nullZoneId() {

        TClock.system(null);
    }

    @Test
    public void test_withZone() {

        TClock test = TClock.system(PARIS);
        TClock changed = test.withZone(MOSCOW);
        assertEquals(test.getZone(), PARIS);
        assertEquals(changed.getZone(), MOSCOW);
    }

    @Test
    public void test_withZone_same() {

        TClock test = TClock.system(PARIS);
        TClock changed = test.withZone(PARIS);
        assertSame(test, changed);
    }

    @Test
    public void test_withZone_fromUTC() {

        TClock test = TClock.systemUTC();
        TClock changed = test.withZone(PARIS);
        assertEquals(changed.getZone(), PARIS);
    }

    @Test(expected = NullPointerException.class)
    public void test_withZone_null() {

        TClock.systemUTC().withZone(null);
    }

    @Test
    public void test_equals() {

        TClock a = TClock.systemUTC();
        TClock b = TClock.systemUTC();
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), true);
        assertEquals(b.equals(a), true);
        assertEquals(b.equals(b), true);

        TClock c = TClock.system(PARIS);
        TClock d = TClock.system(PARIS);
        assertEquals(c.equals(c), true);
        assertEquals(c.equals(d), true);
        assertEquals(d.equals(c), true);
        assertEquals(d.equals(d), true);

        assertEquals(a.equals(c), false);
        assertEquals(c.equals(a), false);

        assertEquals(a.equals(null), false);
        assertEquals(a.equals("other type"), false);
        assertEquals(a.equals(TClock.fixed(TInstant.now(), TZoneOffset.UTC)), false);
    }

    @Test
    public void test_hashCode() {

        TClock a = TClock.system(TZoneOffset.UTC);
        TClock b = TClock.system(TZoneOffset.UTC);
        assertEquals(a.hashCode(), a.hashCode());
        assertEquals(a.hashCode(), b.hashCode());

        TClock c = TClock.system(PARIS);
        assertEquals(a.hashCode() == c.hashCode(), false);
    }

    @Test
    public void test_toString() {

        TClock test = TClock.system(PARIS);
        assertEquals(test.toString(), "SystemClock[Europe/Paris]");
    }

}
