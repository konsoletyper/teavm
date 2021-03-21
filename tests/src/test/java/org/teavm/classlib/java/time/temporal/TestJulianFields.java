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

import static org.testng.Assert.assertEquals;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.JulianFields;
import java.time.temporal.TemporalField;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class TestJulianFields {

    private static final LocalDate JAN01_1970 = LocalDate.of(1970, 1, 1);
    private static final LocalDate DEC31_1969 = LocalDate.of(1969, 12, 31);
    private static final LocalDate NOV12_1945 = LocalDate.of(1945, 11, 12);
    private static final LocalDate JAN01_0001 = LocalDate.of(1, 1, 1);

    @BeforeMethod
    public void setUp() {
    }

    //-----------------------------------------------------------------------
    @DataProvider(name = "samples")
    Object[][] data_samples() {
        return new Object[][] {
            {ChronoField.EPOCH_DAY, JAN01_1970, 0L},
            {JulianFields.JULIAN_DAY, JAN01_1970, 2400001L + 40587L},
            {JulianFields.MODIFIED_JULIAN_DAY, JAN01_1970, 40587L},
            {JulianFields.RATA_DIE, JAN01_1970, 710347L + (40587L - 31771L)},

            {ChronoField.EPOCH_DAY, DEC31_1969, -1L},
            {JulianFields.JULIAN_DAY, DEC31_1969, 2400001L + 40586L},
            {JulianFields.MODIFIED_JULIAN_DAY, DEC31_1969, 40586L},
            {JulianFields.RATA_DIE, DEC31_1969, 710347L + (40586L - 31771L)},

            {ChronoField.EPOCH_DAY, NOV12_1945, (-24 * 365 - 6) - 31 - 30 + 11},
            {JulianFields.JULIAN_DAY, NOV12_1945, 2431772L},
            {JulianFields.MODIFIED_JULIAN_DAY, NOV12_1945, 31771L},
            {JulianFields.RATA_DIE, NOV12_1945, 710347L},

            {ChronoField.EPOCH_DAY, JAN01_0001, (-24 * 365 - 6) - 31 - 30 + 11 - 710346L},
            {JulianFields.JULIAN_DAY, JAN01_0001, 2431772L - 710346L},
            {JulianFields.MODIFIED_JULIAN_DAY, JAN01_0001, 31771L - 710346L},
            {JulianFields.RATA_DIE, JAN01_0001, 1},
        };
    }

    @Test(dataProvider = "samples")
    public void test_samples_get(TemporalField field, LocalDate date, long expected) {
        assertEquals(date.getLong(field), expected);
    }

    @Test(dataProvider = "samples")
    public void test_samples_set(TemporalField field, LocalDate date, long value) {
        assertEquals(field.adjustInto(LocalDate.MAX, value), date);
        assertEquals(field.adjustInto(LocalDate.MIN, value), date);
        assertEquals(field.adjustInto(JAN01_1970, value), date);
        assertEquals(field.adjustInto(DEC31_1969, value), date);
        assertEquals(field.adjustInto(NOV12_1945, value), date);
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @Test
    public void test_toString() {
        assertEquals(JulianFields.JULIAN_DAY.toString(), "JulianDay");
        assertEquals(JulianFields.MODIFIED_JULIAN_DAY.toString(), "ModifiedJulianDay");
        assertEquals(JulianFields.RATA_DIE.toString(), "RataDie");
    }

}
