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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.teavm.junit.TeaVMTest;

/**
 * Test Month.
 */
@TeaVMTest
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
            assertEquals(i, test.getValue());
        }
    }

    @Test
    public void test_factory_int_tooLow() {
        assertThrows(DateTimeException.class, () -> Month.of(0));
    }

    @Test
    public void test_factory_int_tooHigh() {
        assertThrows(DateTimeException.class, () -> Month.of(13));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_CalendricalObject() {
        assertEquals(JUNE, Month.from(LocalDate.of(2011, 6, 6)));
    }

    @Test
    public void test_factory_CalendricalObject_invalid_noDerive() {
        assertThrows(DateTimeException.class, () -> Month.from(LocalTime.of(12, 30)));
    }

    @Test
    public void test_factory_CalendricalObject_null() {
        assertThrows(NullPointerException.class, () -> Month.from((TemporalAccessor) null));
    }

    //-----------------------------------------------------------------------
    // get(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_TemporalField() {
        assertEquals(7, Month.JULY.get(ChronoField.MONTH_OF_YEAR));
    }

    @Test
    public void test_getLong_TemporalField() {
        assertEquals(7, Month.JULY.getLong(ChronoField.MONTH_OF_YEAR));
    }

    //-----------------------------------------------------------------------
    // query(TemporalQuery)
    //-----------------------------------------------------------------------
    @Test
    public void test_query() {
        assertEquals(IsoChronology.INSTANCE, Month.JUNE.query(TemporalQueries.chronology()));
        assertEquals(null, Month.JUNE.query(TemporalQueries.localDate()));
        assertEquals(null, Month.JUNE.query(TemporalQueries.localTime()));
        assertEquals(null, Month.JUNE.query(TemporalQueries.offset()));
        assertEquals(ChronoUnit.MONTHS, Month.JUNE.query(TemporalQueries.precision()));
        assertEquals(null, Month.JUNE.query(TemporalQueries.zone()));
        assertEquals(null, Month.JUNE.query(TemporalQueries.zoneId()));
    }

    @Test
    public void test_query_null() {
        assertThrows(NullPointerException.class, () -> Month.JUNE.query(null));
    }

    //-----------------------------------------------------------------------
    // getDisplayName()
    //-----------------------------------------------------------------------
    @Test
    public void test_getDisplayName() {
        assertEquals("Jan", Month.JANUARY.getDisplayName(TextStyle.SHORT, Locale.US));
    }

    @Test
    public void test_getDisplayName_nullStyle() {
        assertThrows(NullPointerException.class, () -> Month.JANUARY.getDisplayName(null, Locale.US));
    }

    @Test
    public void test_getDisplayName_nullLocale() {
        assertThrows(NullPointerException.class, () -> Month.JANUARY.getDisplayName(TextStyle.FULL, null));
    }

    //-----------------------------------------------------------------------
    // plus(long), plus(long,unit)
    //-----------------------------------------------------------------------
    static Object[][] data_plus() {
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

    @ParameterizedTest
    @MethodSource("data_plus")
    public void test_plus_long(int base, long amount, int expected) {
        assertEquals(Month.of(expected), Month.of(base).plus(amount));
    }

    //-----------------------------------------------------------------------
    // minus(long), minus(long,unit)
    //-----------------------------------------------------------------------
    static Object[][] data_minus() {
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

    @ParameterizedTest
    @MethodSource("data_minus")
    public void test_minus_long(int base, long amount, int expected) {
        assertEquals(Month.of(expected), Month.of(base).minus(amount));
    }

    //-----------------------------------------------------------------------
    // length(boolean)
    //-----------------------------------------------------------------------
    @Test
    public void test_length_boolean_notLeapYear() {
        assertEquals(31, Month.JANUARY.length(false));
        assertEquals(28, Month.FEBRUARY.length(false));
        assertEquals(31, Month.MARCH.length(false));
        assertEquals(30, Month.APRIL.length(false));
        assertEquals(31, Month.MAY.length(false));
        assertEquals(30, Month.JUNE.length(false));
        assertEquals(31, Month.JULY.length(false));
        assertEquals(31, Month.AUGUST.length(false));
        assertEquals(30, Month.SEPTEMBER.length(false));
        assertEquals(31, Month.OCTOBER.length(false));
        assertEquals(30, Month.NOVEMBER.length(false));
        assertEquals(31, Month.DECEMBER.length(false));
    }

    @Test
    public void test_length_boolean_leapYear() {
        assertEquals(31, Month.JANUARY.length(true));
        assertEquals(29, Month.FEBRUARY.length(true));
        assertEquals(31, Month.MARCH.length(true));
        assertEquals(30, Month.APRIL.length(true));
        assertEquals(31, Month.MAY.length(true));
        assertEquals(30, Month.JUNE.length(true));
        assertEquals(31, Month.JULY.length(true));
        assertEquals(31, Month.AUGUST.length(true));
        assertEquals(30, Month.SEPTEMBER.length(true));
        assertEquals(31, Month.OCTOBER.length(true));
        assertEquals(30, Month.NOVEMBER.length(true));
        assertEquals(31, Month.DECEMBER.length(true));
    }

    //-----------------------------------------------------------------------
    // minLength()
    //-----------------------------------------------------------------------
    @Test
    public void test_minLength() {
        assertEquals(31, Month.JANUARY.minLength());
        assertEquals(28, Month.FEBRUARY.minLength());
        assertEquals(31, Month.MARCH.minLength());
        assertEquals(30, Month.APRIL.minLength());
        assertEquals(31, Month.MAY.minLength());
        assertEquals(30, Month.JUNE.minLength());
        assertEquals(31, Month.JULY.minLength());
        assertEquals(31, Month.AUGUST.minLength());
        assertEquals(30, Month.SEPTEMBER.minLength());
        assertEquals(31, Month.OCTOBER.minLength());
        assertEquals(30, Month.NOVEMBER.minLength());
        assertEquals(31, Month.DECEMBER.minLength());
    }

    //-----------------------------------------------------------------------
    // maxLength()
    //-----------------------------------------------------------------------
    @Test
    public void test_maxLength() {
        assertEquals(31, Month.JANUARY.maxLength());
        assertEquals(29, Month.FEBRUARY.maxLength());
        assertEquals(31, Month.MARCH.maxLength());
        assertEquals(30, Month.APRIL.maxLength());
        assertEquals(31, Month.MAY.maxLength());
        assertEquals(30, Month.JUNE.maxLength());
        assertEquals(31, Month.JULY.maxLength());
        assertEquals(31, Month.AUGUST.maxLength());
        assertEquals(30, Month.SEPTEMBER.maxLength());
        assertEquals(31, Month.OCTOBER.maxLength());
        assertEquals(30, Month.NOVEMBER.maxLength());
        assertEquals(31, Month.DECEMBER.maxLength());
    }

    //-----------------------------------------------------------------------
    // firstDayOfYear(boolean)
    //-----------------------------------------------------------------------
    @Test
    public void test_firstDayOfYear_notLeapYear() {
        assertEquals(1, Month.JANUARY.firstDayOfYear(false));
        assertEquals(1 + 31, Month.FEBRUARY.firstDayOfYear(false));
        assertEquals(1 + 31 + 28, Month.MARCH.firstDayOfYear(false));
        assertEquals(1 + 31 + 28 + 31, Month.APRIL.firstDayOfYear(false));
        assertEquals(1 + 31 + 28 + 31 + 30, Month.MAY.firstDayOfYear(false));
        assertEquals(1 + 31 + 28 + 31 + 30 + 31, Month.JUNE.firstDayOfYear(false));
        assertEquals(1 + 31 + 28 + 31 + 30 + 31 + 30, Month.JULY.firstDayOfYear(false));
        assertEquals(1 + 31 + 28 + 31 + 30 + 31 + 30 + 31, Month.AUGUST.firstDayOfYear(false));
        assertEquals(1 + 31 + 28 + 31 + 30 + 31 + 30 + 31 + 31, Month.SEPTEMBER.firstDayOfYear(false));
        assertEquals(1 + 31 + 28 + 31 + 30 + 31 + 30 + 31 + 31 + 30, Month.OCTOBER.firstDayOfYear(false));
        assertEquals(1 + 31 + 28 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31, Month.NOVEMBER.firstDayOfYear(false));
        assertEquals(1 + 31 + 28 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31 + 30, Month.DECEMBER.firstDayOfYear(false));
    }

    @Test
    public void test_firstDayOfYear_leapYear() {
        assertEquals(1, Month.JANUARY.firstDayOfYear(true));
        assertEquals(1 + 31, Month.FEBRUARY.firstDayOfYear(true));
        assertEquals(1 + 31 + 29, Month.MARCH.firstDayOfYear(true));
        assertEquals(1 + 31 + 29 + 31, Month.APRIL.firstDayOfYear(true));
        assertEquals(1 + 31 + 29 + 31 + 30, Month.MAY.firstDayOfYear(true));
        assertEquals(1 + 31 + 29 + 31 + 30 + 31, Month.JUNE.firstDayOfYear(true));
        assertEquals(1 + 31 + 29 + 31 + 30 + 31 + 30, Month.JULY.firstDayOfYear(true));
        assertEquals(1 + 31 + 29 + 31 + 30 + 31 + 30 + 31, Month.AUGUST.firstDayOfYear(true));
        assertEquals(1 + 31 + 29 + 31 + 30 + 31 + 30 + 31 + 31, Month.SEPTEMBER.firstDayOfYear(true));
        assertEquals(1 + 31 + 29 + 31 + 30 + 31 + 30 + 31 + 31 + 30, Month.OCTOBER.firstDayOfYear(true));
        assertEquals(1 + 31 + 29 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31, Month.NOVEMBER.firstDayOfYear(true));
        assertEquals(1 + 31 + 29 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31 + 30, Month.DECEMBER.firstDayOfYear(true));
    }

    //-----------------------------------------------------------------------
    // firstMonthOfQuarter()
    //-----------------------------------------------------------------------
    @Test
    public void test_firstMonthOfQuarter() {
        assertEquals(Month.JANUARY, Month.JANUARY.firstMonthOfQuarter());
        assertEquals(Month.JANUARY, Month.FEBRUARY.firstMonthOfQuarter());
        assertEquals(Month.JANUARY, Month.MARCH.firstMonthOfQuarter());
        assertEquals(Month.APRIL, Month.APRIL.firstMonthOfQuarter());
        assertEquals(Month.APRIL, Month.MAY.firstMonthOfQuarter());
        assertEquals(Month.APRIL, Month.JUNE.firstMonthOfQuarter());
        assertEquals(Month.JULY, Month.JULY.firstMonthOfQuarter());
        assertEquals(Month.JULY, Month.AUGUST.firstMonthOfQuarter());
        assertEquals(Month.JULY, Month.SEPTEMBER.firstMonthOfQuarter());
        assertEquals(Month.OCTOBER, Month.OCTOBER.firstMonthOfQuarter());
        assertEquals(Month.OCTOBER, Month.NOVEMBER.firstMonthOfQuarter());
        assertEquals(Month.OCTOBER, Month.DECEMBER.firstMonthOfQuarter());
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @Test
    public void test_toString() {
        assertEquals("JANUARY", Month.JANUARY.toString());
        assertEquals("FEBRUARY", Month.FEBRUARY.toString());
        assertEquals("MARCH", Month.MARCH.toString());
        assertEquals("APRIL", Month.APRIL.toString());
        assertEquals("MAY", Month.MAY.toString());
        assertEquals("JUNE", Month.JUNE.toString());
        assertEquals("JULY", Month.JULY.toString());
        assertEquals("AUGUST", Month.AUGUST.toString());
        assertEquals("SEPTEMBER", Month.SEPTEMBER.toString());
        assertEquals("OCTOBER", Month.OCTOBER.toString());
        assertEquals("NOVEMBER", Month.NOVEMBER.toString());
        assertEquals("DECEMBER", Month.DECEMBER.toString());
    }

    //-----------------------------------------------------------------------
    // generated methods
    //-----------------------------------------------------------------------
    @Test
    public void test_enum() {
        assertEquals(Month.JANUARY, Month.valueOf("JANUARY"));
        assertEquals(Month.JANUARY, Month.values()[0]);
    }

}
