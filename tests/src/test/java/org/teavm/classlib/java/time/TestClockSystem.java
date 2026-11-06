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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

/**
 * Test system clock.
 */
@TeaVMTest
public class TestClockSystem extends AbstractTest {

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    //-----------------------------------------------------------------------
    @Test
    public void test_instant() {
        Clock system = Clock.systemUTC();
        assertEquals(ZoneOffset.UTC, system.getZone());
        for (int i = 0; i < 10000; i++) {
            // assume can eventually get these within 10 milliseconds
            Instant instant = system.instant();
            long systemMillis = System.currentTimeMillis();
            if (systemMillis - instant.toEpochMilli() < 10) {
                return;  // success
            }
        }
        fail();
    }

    @Test
    public void test_millis() {
        Clock system = Clock.systemUTC();
        assertEquals(ZoneOffset.UTC, system.getZone());
        for (int i = 0; i < 10000; i++) {
            // assume can eventually get these within 10 milliseconds
            long instant = system.millis();
            long systemMillis = System.currentTimeMillis();
            if (systemMillis - instant < 10) {
                return;  // success
            }
        }
        fail();
    }

    //-------------------------------------------------------------------------
    @Test
    public void test_systemUTC() {
        Clock test = Clock.systemUTC();
        assertEquals(ZoneOffset.UTC, test.getZone());
        assertEquals(Clock.system(ZoneOffset.UTC), test);
    }

    @Test
    public void test_systemDefaultZone() {
        Clock test = Clock.systemDefaultZone();
        assertEquals(ZoneId.systemDefault(), test.getZone());
        assertEquals(Clock.system(ZoneId.systemDefault()), test);
    }

    @Test
    public void test_system_ZoneId() {
        Clock test = Clock.system(PARIS);
        assertEquals(PARIS, test.getZone());
    }

    @Test
    public void test_zoneId_nullZoneId() {
        assertThrows(NullPointerException.class, () -> Clock.system(null));
    }

    //-------------------------------------------------------------------------
    @Test
    public void test_withZone() {
        Clock test = Clock.system(PARIS);
        Clock changed = test.withZone(MOSCOW);
        assertEquals(PARIS, test.getZone());
        assertEquals(MOSCOW, changed.getZone());
    }

    @Test
    public void test_withZone_same() {
        Clock test = Clock.system(PARIS);
        Clock changed = test.withZone(PARIS);
        assertSame(changed, test);
    }

    @Test
    public void test_withZone_fromUTC() {
        Clock test = Clock.systemUTC();
        Clock changed = test.withZone(PARIS);
        assertEquals(PARIS, changed.getZone());
    }

    @Test
    public void test_withZone_null() {
        assertThrows(NullPointerException.class, () -> Clock.systemUTC().withZone(null));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_equals() {
        Clock a = Clock.systemUTC();
        Clock b = Clock.systemUTC();
        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertTrue(b.equals(b));

        Clock c = Clock.system(PARIS);
        Clock d = Clock.system(PARIS);
        assertTrue(c.equals(c));
        assertTrue(c.equals(d));
        assertTrue(d.equals(c));
        assertTrue(d.equals(d));

        assertFalse(a.equals(c));
        assertFalse(c.equals(a));

        assertFalse(a.equals(null));
        assertFalse(a.equals("other type"));
        assertFalse(a.equals(Clock.fixed(Instant.now(), ZoneOffset.UTC)));
    }

    @Test
    public void test_hashCode() {
        Clock a = Clock.system(ZoneOffset.UTC);
        Clock b = Clock.system(ZoneOffset.UTC);
        assertEquals(a.hashCode(), a.hashCode());
        assertEquals(b.hashCode(), a.hashCode());

        Clock c = Clock.system(PARIS);
        assertFalse(a.hashCode() == c.hashCode());
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_toString() {
        Clock test = Clock.system(PARIS);
        assertEquals("SystemClock[Europe/Paris]", test.toString());
    }

}
