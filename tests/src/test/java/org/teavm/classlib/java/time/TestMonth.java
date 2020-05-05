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
package org.teavm.classlib.java.time;

import static java.time.Month.DECEMBER;
import static java.time.Month.JANUARY;
import static java.time.Month.JUNE;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static org.testng.Assert.assertEquals;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.chrono.IsoChronology;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.JulianFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test Month.
 */
@Test
@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class TestMonth extends AbstractDateTimeTest {

    private static final int MAX_LENGTH = 12;

    //-----------------------------------------------------------------------
    @Override
    protected List<TemporalAccessor> samples() {
        TemporalAccessor[] array = {JANUARY, JUNE, DECEMBER, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> validFields() {
        TemporalField[] array = {
            MONTH_OF_YEAR,
        };
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> invalidFields() {
        List<TemporalField> list = new ArrayList<TemporalField>(Arrays.<TemporalField>asList(ChronoField.values()));
        list.removeAll(validFields());
        list.add(JulianFields.JULIAN_DAY);
        list.add(JulianFields.MODIFIED_JULIAN_DAY);
        list.add(JulianFields.RATA_DIE);
        return list;
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_int_singleton() {
        for (int i = 1; i <= MAX_LENGTH; i++) {
            Month test = Month.of(i);
            assertEquals(test.getValue(), i);
        }
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void test_factory_int_tooLow() {
        Month.of(0);
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void test_factory_int_tooHigh() {
        Month.of(13);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_CalendricalObject() {
        assertEquals(Month.from(LocalDate.of(2011, 6, 6)), JUNE);
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void test_factory_CalendricalObject_invalid_noDerive() {
        Month.from(LocalTime.of(12, 30));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_factory_CalendricalObject_null() {
        Month.from((TemporalAccessor) null);
    }

    //-----------------------------------------------------------------------
    // get(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_TemporalField() {
        assertEquals(Month.JULY.get(ChronoField.MONTH_OF_YEAR), 7);
    }

    @Test
    public void test_getLong_TemporalField() {
        assertEquals(Month.JULY.getLong(ChronoField.MONTH_OF_YEAR), 7);
    }

    //-----------------------------------------------------------------------
    // query(TemporalQuery)
    //-----------------------------------------------------------------------
    @Test
    public void test_query() {
        assertEquals(Month.JUNE.query(TemporalQueries.chronology()), IsoChronology.INSTANCE);
        assertEquals(Month.JUNE.query(TemporalQueries.localDate()), null);
        assertEquals(Month.JUNE.query(TemporalQueries.localTime()), null);
        assertEquals(Month.JUNE.query(TemporalQueries.offset()), null);
        assertEquals(Month.JUNE.query(TemporalQueries.precision()), ChronoUnit.MONTHS);
        assertEquals(Month.JUNE.query(TemporalQueries.zone()), null);
        assertEquals(Month.JUNE.query(TemporalQueries.zoneId()), null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_query_null() {
        Month.JUNE.query(null);
    }

    //-----------------------------------------------------------------------
    // getDisplayName()
    //-----------------------------------------------------------------------
    @Test
    public void test_getDisplayName() {
        assertEquals(Month.JANUARY.getDisplayName(TextStyle.SHORT, Locale.US), "Jan");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_getDisplayName_nullStyle() {
        Month.JANUARY.getDisplayName(null, Locale.US);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_getDisplayName_nullLocale() {
        Month.JANUARY.getDisplayName(TextStyle.FULL, null);
    }

    //-----------------------------------------------------------------------
    // plus(long), plus(long,unit)
    //-----------------------------------------------------------------------
    @DataProvider(name = "plus")
    Object[][] data_plus() {
        return new Object[][] {
            {1, -13, 12},
            {1, -12, 1},
            {1, -11, 2},
            {1, -10, 3},
            {1, -9, 4},
            {1, -8, 5},
            {1, -7, 6},
            {1, -6, 7},
            {1, -5, 8},
            {1, -4, 9},
            {1, -3, 10},
            {1, -2, 11},
            {1, -1, 12},
            {1, 0, 1},
            {1, 1, 2},
            {1, 2, 3},
            {1, 3, 4},
            {1, 4, 5},
            {1, 5, 6},
            {1, 6, 7},
            {1, 7, 8},
            {1, 8, 9},
            {1, 9, 10},
            {1, 10, 11},
            {1, 11, 12},
            {1, 12, 1},
            {1, 13, 2},

            {1, 1, 2},
            {2, 1, 3},
            {3, 1, 4},
            {4, 1, 5},
            {5, 1, 6},
            {6, 1, 7},
            {7, 1, 8},
            {8, 1, 9},
            {9, 1, 10},
            {10, 1, 11},
            {11, 1, 12},
            {12, 1, 1},

            {1, -1, 12},
            {2, -1, 1},
            {3, -1, 2},
            {4, -1, 3},
            {5, -1, 4},
            {6, -1, 5},
            {7, -1, 6},
            {8, -1, 7},
            {9, -1, 8},
            {10, -1, 9},
            {11, -1, 10},
            {12, -1, 11},
        };
    }

    @Test(dataProvider = "plus")
    public void test_plus_long(int base, long amount, int expected) {
        assertEquals(Month.of(base).plus(amount), Month.of(expected));
    }

    //-----------------------------------------------------------------------
    // minus(long), minus(long,unit)
    //-----------------------------------------------------------------------
    @DataProvider(name = "minus")
    Object[][] data_minus() {
        return new Object[][] {
            {1, -13, 2},
            {1, -12, 1},
            {1, -11, 12},
            {1, -10, 11},
            {1, -9, 10},
            {1, -8, 9},
            {1, -7, 8},
            {1, -6, 7},
            {1, -5, 6},
            {1, -4, 5},
            {1, -3, 4},
            {1, -2, 3},
            {1, -1, 2},
            {1, 0, 1},
            {1, 1, 12},
            {1, 2, 11},
            {1, 3, 10},
            {1, 4, 9},
            {1, 5, 8},
            {1, 6, 7},
            {1, 7, 6},
            {1, 8, 5},
            {1, 9, 4},
            {1, 10, 3},
            {1, 11, 2},
            {1, 12, 1},
            {1, 13, 12},
        };
    }

    @Test(dataProvider = "minus")
    public void test_minus_long(int base, long amount, int expected) {
        assertEquals(Month.of(base).minus(amount), Month.of(expected));
    }

    //-----------------------------------------------------------------------
    // length(boolean)
    //-----------------------------------------------------------------------
    @Test
    public void test_length_boolean_notLeapYear() {
        assertEquals(Month.JANUARY.length(false), 31);
        assertEquals(Month.FEBRUARY.length(false), 28);
        assertEquals(Month.MARCH.length(false), 31);
        assertEquals(Month.APRIL.length(false), 30);
        assertEquals(Month.MAY.length(false), 31);
        assertEquals(Month.JUNE.length(false), 30);
        assertEquals(Month.JULY.length(false), 31);
        assertEquals(Month.AUGUST.length(false), 31);
        assertEquals(Month.SEPTEMBER.length(false), 30);
        assertEquals(Month.OCTOBER.length(false), 31);
        assertEquals(Month.NOVEMBER.length(false), 30);
        assertEquals(Month.DECEMBER.length(false), 31);
    }

    @Test
    public void test_length_boolean_leapYear() {
        assertEquals(Month.JANUARY.length(true), 31);
        assertEquals(Month.FEBRUARY.length(true), 29);
        assertEquals(Month.MARCH.length(true), 31);
        assertEquals(Month.APRIL.length(true), 30);
        assertEquals(Month.MAY.length(true), 31);
        assertEquals(Month.JUNE.length(true), 30);
        assertEquals(Month.JULY.length(true), 31);
        assertEquals(Month.AUGUST.length(true), 31);
        assertEquals(Month.SEPTEMBER.length(true), 30);
        assertEquals(Month.OCTOBER.length(true), 31);
        assertEquals(Month.NOVEMBER.length(true), 30);
        assertEquals(Month.DECEMBER.length(true), 31);
    }

    //-----------------------------------------------------------------------
    // minLength()
    //-----------------------------------------------------------------------
    @Test
    public void test_minLength() {
        assertEquals(Month.JANUARY.minLength(), 31);
        assertEquals(Month.FEBRUARY.minLength(), 28);
        assertEquals(Month.MARCH.minLength(), 31);
        assertEquals(Month.APRIL.minLength(), 30);
        assertEquals(Month.MAY.minLength(), 31);
        assertEquals(Month.JUNE.minLength(), 30);
        assertEquals(Month.JULY.minLength(), 31);
        assertEquals(Month.AUGUST.minLength(), 31);
        assertEquals(Month.SEPTEMBER.minLength(), 30);
        assertEquals(Month.OCTOBER.minLength(), 31);
        assertEquals(Month.NOVEMBER.minLength(), 30);
        assertEquals(Month.DECEMBER.minLength(), 31);
    }

    //-----------------------------------------------------------------------
    // maxLength()
    //-----------------------------------------------------------------------
    @Test
    public void test_maxLength() {
        assertEquals(Month.JANUARY.maxLength(), 31);
        assertEquals(Month.FEBRUARY.maxLength(), 29);
        assertEquals(Month.MARCH.maxLength(), 31);
        assertEquals(Month.APRIL.maxLength(), 30);
        assertEquals(Month.MAY.maxLength(), 31);
        assertEquals(Month.JUNE.maxLength(), 30);
        assertEquals(Month.JULY.maxLength(), 31);
        assertEquals(Month.AUGUST.maxLength(), 31);
        assertEquals(Month.SEPTEMBER.maxLength(), 30);
        assertEquals(Month.OCTOBER.maxLength(), 31);
        assertEquals(Month.NOVEMBER.maxLength(), 30);
        assertEquals(Month.DECEMBER.maxLength(), 31);
    }

    //-----------------------------------------------------------------------
    // firstDayOfYear(boolean)
    //-----------------------------------------------------------------------
    @Test
    public void test_firstDayOfYear_notLeapYear() {
        assertEquals(Month.JANUARY.firstDayOfYear(false), 1);
        assertEquals(Month.FEBRUARY.firstDayOfYear(false), 1 + 31);
        assertEquals(Month.MARCH.firstDayOfYear(false), 1 + 31 + 28);
        assertEquals(Month.APRIL.firstDayOfYear(false), 1 + 31 + 28 + 31);
        assertEquals(Month.MAY.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30);
        assertEquals(Month.JUNE.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30 + 31);
        assertEquals(Month.JULY.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30 + 31 + 30);
        assertEquals(Month.AUGUST.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30 + 31 + 30 + 31);
        assertEquals(Month.SEPTEMBER.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30 + 31 + 30 + 31 + 31);
        assertEquals(Month.OCTOBER.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30 + 31 + 30 + 31 + 31 + 30);
        assertEquals(Month.NOVEMBER.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31);
        assertEquals(Month.DECEMBER.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31 + 30);
    }

    @Test
    public void test_firstDayOfYear_leapYear() {
        assertEquals(Month.JANUARY.firstDayOfYear(true), 1);
        assertEquals(Month.FEBRUARY.firstDayOfYear(true), 1 + 31);
        assertEquals(Month.MARCH.firstDayOfYear(true), 1 + 31 + 29);
        assertEquals(Month.APRIL.firstDayOfYear(true), 1 + 31 + 29 + 31);
        assertEquals(Month.MAY.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30);
        assertEquals(Month.JUNE.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30 + 31);
        assertEquals(Month.JULY.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30 + 31 + 30);
        assertEquals(Month.AUGUST.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30 + 31 + 30 + 31);
        assertEquals(Month.SEPTEMBER.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30 + 31 + 30 + 31 + 31);
        assertEquals(Month.OCTOBER.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30 + 31 + 30 + 31 + 31 + 30);
        assertEquals(Month.NOVEMBER.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31);
        assertEquals(Month.DECEMBER.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31 + 30);
    }

    //-----------------------------------------------------------------------
    // firstMonthOfQuarter()
    //-----------------------------------------------------------------------
    @Test
    public void test_firstMonthOfQuarter() {
        assertEquals(Month.JANUARY.firstMonthOfQuarter(), Month.JANUARY);
        assertEquals(Month.FEBRUARY.firstMonthOfQuarter(), Month.JANUARY);
        assertEquals(Month.MARCH.firstMonthOfQuarter(), Month.JANUARY);
        assertEquals(Month.APRIL.firstMonthOfQuarter(), Month.APRIL);
        assertEquals(Month.MAY.firstMonthOfQuarter(), Month.APRIL);
        assertEquals(Month.JUNE.firstMonthOfQuarter(), Month.APRIL);
        assertEquals(Month.JULY.firstMonthOfQuarter(), Month.JULY);
        assertEquals(Month.AUGUST.firstMonthOfQuarter(), Month.JULY);
        assertEquals(Month.SEPTEMBER.firstMonthOfQuarter(), Month.JULY);
        assertEquals(Month.OCTOBER.firstMonthOfQuarter(), Month.OCTOBER);
        assertEquals(Month.NOVEMBER.firstMonthOfQuarter(), Month.OCTOBER);
        assertEquals(Month.DECEMBER.firstMonthOfQuarter(), Month.OCTOBER);
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @Test
    public void test_toString() {
        assertEquals(Month.JANUARY.toString(), "JANUARY");
        assertEquals(Month.FEBRUARY.toString(), "FEBRUARY");
        assertEquals(Month.MARCH.toString(), "MARCH");
        assertEquals(Month.APRIL.toString(), "APRIL");
        assertEquals(Month.MAY.toString(), "MAY");
        assertEquals(Month.JUNE.toString(), "JUNE");
        assertEquals(Month.JULY.toString(), "JULY");
        assertEquals(Month.AUGUST.toString(), "AUGUST");
        assertEquals(Month.SEPTEMBER.toString(), "SEPTEMBER");
        assertEquals(Month.OCTOBER.toString(), "OCTOBER");
        assertEquals(Month.NOVEMBER.toString(), "NOVEMBER");
        assertEquals(Month.DECEMBER.toString(), "DECEMBER");
    }

    //-----------------------------------------------------------------------
    // generated methods
    //-----------------------------------------------------------------------
    @Test
    public void test_enum() {
        assertEquals(Month.valueOf("JANUARY"), Month.JANUARY);
        assertEquals(Month.values()[0], Month.JANUARY);
    }

}
