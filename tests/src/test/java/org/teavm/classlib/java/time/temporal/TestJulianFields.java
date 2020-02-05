/*
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
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
package org.teavm.classlib.java.time.temporal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.teavm.classlib.java.time.TLocalDate;

public class TestJulianFields {

    private static final TLocalDate JAN01_1970 = TLocalDate.of(1970, 1, 1);

    private static final TLocalDate DEC31_1969 = TLocalDate.of(1969, 12, 31);

    private static final TLocalDate NOV12_1945 = TLocalDate.of(1945, 11, 12);

    private static final TLocalDate JAN01_0001 = TLocalDate.of(1, 1, 1);

    Object[][] data_samples() {

        return new Object[][] { { TChronoField.EPOCH_DAY, JAN01_1970, 0L },
        { TJulianFields.JULIAN_DAY, JAN01_1970, 2400001L + 40587L },
        { TJulianFields.MODIFIED_JULIAN_DAY, JAN01_1970, 40587L },
        { TJulianFields.RATA_DIE, JAN01_1970, 710347L + (40587L - 31771L) },

        { TChronoField.EPOCH_DAY, DEC31_1969, -1L }, { TJulianFields.JULIAN_DAY, DEC31_1969, 2400001L + 40586L },
        { TJulianFields.MODIFIED_JULIAN_DAY, DEC31_1969, 40586L },
        { TJulianFields.RATA_DIE, DEC31_1969, 710347L + (40586L - 31771L) },

        { TChronoField.EPOCH_DAY, NOV12_1945, (-24 * 365 - 6) - 31 - 30 + 11 },
        { TJulianFields.JULIAN_DAY, NOV12_1945, 2431772L }, { TJulianFields.MODIFIED_JULIAN_DAY, NOV12_1945, 31771L },
        { TJulianFields.RATA_DIE, NOV12_1945, 710347L },

        { TChronoField.EPOCH_DAY, JAN01_0001, (-24 * 365 - 6) - 31 - 30 + 11 - 710346L },
        { TJulianFields.JULIAN_DAY, JAN01_0001, 2431772L - 710346L },
        { TJulianFields.MODIFIED_JULIAN_DAY, JAN01_0001, 31771L - 710346L },
        { TJulianFields.RATA_DIE, JAN01_0001, 1 }, };
    }

    @Test
    public void test_samples_get() {

        for (Object[] data : data_samples()) {
            TTemporalField field = (TTemporalField) data[0];
            TLocalDate date = (TLocalDate) data[1];
            long expected = ((Number) data[2]).longValue();

            assertEquals(date.getLong(field), expected);
        }
    }

    @Test
    public void test_samples_set() {

        for (Object[] data : data_samples()) {
            TTemporalField field = (TTemporalField) data[0];
            TLocalDate date = (TLocalDate) data[1];
            long value = ((Number) data[2]).longValue();

            assertEquals(field.adjustInto(TLocalDate.MAX, value), date);
            assertEquals(field.adjustInto(TLocalDate.MIN, value), date);
            assertEquals(field.adjustInto(JAN01_1970, value), date);
            assertEquals(field.adjustInto(DEC31_1969, value), date);
            assertEquals(field.adjustInto(NOV12_1945, value), date);
        }
    }

    @Test
    public void test_toString() {

        assertEquals(TJulianFields.JULIAN_DAY.toString(), "JulianDay");
        assertEquals(TJulianFields.MODIFIED_JULIAN_DAY.toString(), "ModifiedJulianDay");
        assertEquals(TJulianFields.RATA_DIE.toString(), "RataDie");
    }

}
