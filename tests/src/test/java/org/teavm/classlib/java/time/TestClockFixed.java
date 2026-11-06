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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

/**
 * Test fixed clock.
 */
@TeaVMTest
public class TestClockFixed extends AbstractTest {

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final Instant INSTANT = LocalDateTime.of(2008, 6, 30, 11, 30, 10, 500)
            .atZone(ZoneOffset.ofHours(2)).toInstant();

    //-------------------------------------------------------------------------
    @Test
    public void test_fixed_InstantZoneId() {
        Clock test = Clock.fixed(INSTANT, PARIS);
        assertEquals(INSTANT, test.instant());
        assertEquals(PARIS, test.getZone());
    }

    @Test
    public void test_fixed_InstantZoneId_nullInstant() {
        assertThrows(NullPointerException.class, () -> Clock.fixed(null, PARIS));
    }

    @Test
    public void test_fixed_InstantZoneId_nullZoneId() {
        assertThrows(NullPointerException.class, () -> Clock.fixed(INSTANT, null));
    }

    //-------------------------------------------------------------------------
    @Test
    public void test_withZone() {
        Clock test = Clock.fixed(INSTANT, PARIS);
        Clock changed = test.withZone(MOSCOW);
        assertEquals(PARIS, test.getZone());
        assertEquals(MOSCOW, changed.getZone());
    }

    @Test
    public void test_withZone_same() {
        Clock test = Clock.fixed(INSTANT, PARIS);
        Clock changed = test.withZone(PARIS);
        assertSame(changed, test);
    }

    @Test
    public void test_withZone_null() {
        assertThrows(NullPointerException.class, () -> Clock.fixed(INSTANT, PARIS).withZone(null));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_equals() {
        Clock a = Clock.fixed(INSTANT, ZoneOffset.UTC);
        Clock b = Clock.fixed(INSTANT, ZoneOffset.UTC);
        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertTrue(b.equals(b));

        Clock c = Clock.fixed(INSTANT, PARIS);
        assertFalse(a.equals(c));

        Clock d = Clock.fixed(INSTANT.minusNanos(1), ZoneOffset.UTC);
        assertFalse(a.equals(d));

        assertFalse(a.equals(null));
        assertFalse(a.equals("other type"));
        assertFalse(a.equals(Clock.systemUTC()));
    }

    @Test
    public void test_hashCode() {
        Clock a = Clock.fixed(INSTANT, ZoneOffset.UTC);
        Clock b = Clock.fixed(INSTANT, ZoneOffset.UTC);
        assertEquals(a.hashCode(), a.hashCode());
        assertEquals(b.hashCode(), a.hashCode());

        Clock c = Clock.fixed(INSTANT, PARIS);
        assertFalse(a.hashCode() == c.hashCode());

        Clock d = Clock.fixed(INSTANT.minusNanos(1), ZoneOffset.UTC);
        assertFalse(a.hashCode() == d.hashCode());
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_toString() {
        Clock test = Clock.fixed(INSTANT, PARIS);
        assertEquals("FixedClock[2008-06-30T09:30:10.000000500Z,Europe/Paris]", test.toString());
    }

}
