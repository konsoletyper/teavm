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

import org.junit.Test;

public class TestClock {

    static class MockInstantClock extends TClock {
        final long millis;

        final TZoneId zone;

        MockInstantClock(long millis, TZoneId zone) {

            this.millis = millis;
            this.zone = zone;
        }

        @Override
        public TInstant instant() {

            return TInstant.ofEpochMilli(this.millis);
        }

        @Override
        public TZoneId getZone() {

            return this.zone;
        }

        @Override
        public TClock withZone(TZoneId timeZone) {

            return new MockInstantClock(this.millis, timeZone);
        }

        @Override
        public boolean equals(Object obj) {

            return false;
        }

        @Override
        public int hashCode() {

            return 0;
        }

        @Override
        public String toString() {

            return "Mock";
        }
    }

    private static final TInstant INSTANT = TInstant.ofEpochSecond(1873687, 357000000);

    private static final TZoneId ZONE = TZoneId.of("Europe/Paris");

    private static final TClock MOCK_INSTANT = new MockInstantClock(INSTANT.toEpochMilli(), ZONE);

    @Test
    public void test_mockInstantClock_get() {

        assertEquals(MOCK_INSTANT.instant(), INSTANT);
        assertEquals(MOCK_INSTANT.millis(), INSTANT.toEpochMilli());
        assertEquals(MOCK_INSTANT.getZone(), ZONE);
    }

    @Test
    public void test_mockInstantClock_withZone() {

        TZoneId london = TZoneId.of("Europe/London");
        TClock changed = MOCK_INSTANT.withZone(london);
        assertEquals(MOCK_INSTANT.instant(), INSTANT);
        assertEquals(MOCK_INSTANT.millis(), INSTANT.toEpochMilli());
        assertEquals(changed.getZone(), london);
    }

}
