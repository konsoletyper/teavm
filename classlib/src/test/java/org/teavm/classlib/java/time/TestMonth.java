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

import static org.junit.Assert.assertEquals;
import static org.teavm.classlib.java.time.TMonth.DECEMBER;
import static org.teavm.classlib.java.time.TMonth.JANUARY;
import static org.teavm.classlib.java.time.TMonth.JUNE;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.teavm.classlib.java.util.TLocale;

import org.testng.annotations.DataProvider;
import org.junit.Test;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TTextStyle;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TJulianFields;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;

@Test
public class TestMonth extends AbstractDateTimeTest {

    private static final int MAX_LENGTH = 12;

    //-----------------------------------------------------------------------
    @Override
    protected List<TTemporalAccessor> samples() {
        TTemporalAccessor[] array = {JANUARY, JUNE, DECEMBER, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TTemporalField> validFields() {
        TTemporalField[] array = {
            MONTH_OF_YEAR,
        };
        return Arrays.asList(array);
    }

    @Override
    protected List<TTemporalField> invalidFields() {
        List<TTemporalField> list = new ArrayList<TTemporalField>(Arrays.<TTemporalField>asList(TChronoField.values()));
        list.removeAll(validFields());
        list.add(TJulianFields.JULIAN_DAY);
        list.add(TJulianFields.MODIFIED_JULIAN_DAY);
        list.add(TJulianFields.RATA_DIE);
        return list;
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_int_singleton() {
        for (int i = 1; i <= MAX_LENGTH; i++) {
            TMonth test = TMonth.of(i);
            assertEquals(test.getValue(), i);
        }
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_factory_int_tooLow() {
        TMonth.of(0);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_factory_int_tooHigh() {
        TMonth.of(13);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_CalendricalObject() {
        assertEquals(TMonth.from(TLocalDate.of(2011, 6, 6)), JUNE);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_factory_CalendricalObject_invalid_noDerive() {
        TMonth.from(TLocalTime.of(12, 30));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_factory_CalendricalObject_null() {
        TMonth.from((TTemporalAccessor) null);
    }

    //-----------------------------------------------------------------------
    // get(TTemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_TemporalField() {
        assertEquals(TMonth.JULY.get(TChronoField.MONTH_OF_YEAR), 7);
    }

    @Test
    public void test_getLong_TemporalField() {
        assertEquals(TMonth.JULY.getLong(TChronoField.MONTH_OF_YEAR), 7);
    }

    //-----------------------------------------------------------------------
    // query(TTemporalQuery)
    //-----------------------------------------------------------------------
    @Test
    public void test_query() {
        assertEquals(TMonth.JUNE.query(TTemporalQueries.chronology()), TIsoChronology.INSTANCE);
        assertEquals(TMonth.JUNE.query(TTemporalQueries.localDate()), null);
        assertEquals(TMonth.JUNE.query(TTemporalQueries.localTime()), null);
        assertEquals(TMonth.JUNE.query(TTemporalQueries.offset()), null);
        assertEquals(TMonth.JUNE.query(TTemporalQueries.precision()), TChronoUnit.MONTHS);
        assertEquals(TMonth.JUNE.query(TTemporalQueries.zone()), null);
        assertEquals(TMonth.JUNE.query(TTemporalQueries.zoneId()), null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_query_null() {
        TMonth.JUNE.query(null);
    }

    //-----------------------------------------------------------------------
    // getDisplayName()
    //-----------------------------------------------------------------------
    @Test
    public void test_getDisplayName() {
        assertEquals(TMonth.JANUARY.getDisplayName(TTextStyle.SHORT, TLocale.US), "Jan");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_getDisplayName_nullStyle() {
        TMonth.JANUARY.getDisplayName(null, TLocale.US);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_getDisplayName_nullLocale() {
        TMonth.JANUARY.getDisplayName(TTextStyle.FULL, null);
    }

    //-----------------------------------------------------------------------
    // plus(long), plus(long,unit)
    //-----------------------------------------------------------------------
    @DataProvider(name="plus")
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

    @Test(dataProvider="plus")
    public void test_plus_long(int base, long amount, int expected) {
        assertEquals(TMonth.of(base).plus(amount), TMonth.of(expected));
    }

    //-----------------------------------------------------------------------
    // minus(long), minus(long,unit)
    //-----------------------------------------------------------------------
    @DataProvider(name="minus")
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

    @Test(dataProvider="minus")
    public void test_minus_long(int base, long amount, int expected) {
        assertEquals(TMonth.of(base).minus(amount), TMonth.of(expected));
    }

    //-----------------------------------------------------------------------
    // length(boolean)
    //-----------------------------------------------------------------------
    @Test
    public void test_length_boolean_notLeapYear() {
        assertEquals(TMonth.JANUARY.length(false), 31);
        assertEquals(TMonth.FEBRUARY.length(false), 28);
        assertEquals(TMonth.MARCH.length(false), 31);
        assertEquals(TMonth.APRIL.length(false), 30);
        assertEquals(TMonth.MAY.length(false), 31);
        assertEquals(TMonth.JUNE.length(false), 30);
        assertEquals(TMonth.JULY.length(false), 31);
        assertEquals(TMonth.AUGUST.length(false), 31);
        assertEquals(TMonth.SEPTEMBER.length(false), 30);
        assertEquals(TMonth.OCTOBER.length(false), 31);
        assertEquals(TMonth.NOVEMBER.length(false), 30);
        assertEquals(TMonth.DECEMBER.length(false), 31);
    }

    @Test
    public void test_length_boolean_leapYear() {
        assertEquals(TMonth.JANUARY.length(true), 31);
        assertEquals(TMonth.FEBRUARY.length(true), 29);
        assertEquals(TMonth.MARCH.length(true), 31);
        assertEquals(TMonth.APRIL.length(true), 30);
        assertEquals(TMonth.MAY.length(true), 31);
        assertEquals(TMonth.JUNE.length(true), 30);
        assertEquals(TMonth.JULY.length(true), 31);
        assertEquals(TMonth.AUGUST.length(true), 31);
        assertEquals(TMonth.SEPTEMBER.length(true), 30);
        assertEquals(TMonth.OCTOBER.length(true), 31);
        assertEquals(TMonth.NOVEMBER.length(true), 30);
        assertEquals(TMonth.DECEMBER.length(true), 31);
    }

    //-----------------------------------------------------------------------
    // minLength()
    //-----------------------------------------------------------------------
    @Test
    public void test_minLength() {
        assertEquals(TMonth.JANUARY.minLength(), 31);
        assertEquals(TMonth.FEBRUARY.minLength(), 28);
        assertEquals(TMonth.MARCH.minLength(), 31);
        assertEquals(TMonth.APRIL.minLength(), 30);
        assertEquals(TMonth.MAY.minLength(), 31);
        assertEquals(TMonth.JUNE.minLength(), 30);
        assertEquals(TMonth.JULY.minLength(), 31);
        assertEquals(TMonth.AUGUST.minLength(), 31);
        assertEquals(TMonth.SEPTEMBER.minLength(), 30);
        assertEquals(TMonth.OCTOBER.minLength(), 31);
        assertEquals(TMonth.NOVEMBER.minLength(), 30);
        assertEquals(TMonth.DECEMBER.minLength(), 31);
    }

    //-----------------------------------------------------------------------
    // maxLength()
    //-----------------------------------------------------------------------
    @Test
    public void test_maxLength() {
        assertEquals(TMonth.JANUARY.maxLength(), 31);
        assertEquals(TMonth.FEBRUARY.maxLength(), 29);
        assertEquals(TMonth.MARCH.maxLength(), 31);
        assertEquals(TMonth.APRIL.maxLength(), 30);
        assertEquals(TMonth.MAY.maxLength(), 31);
        assertEquals(TMonth.JUNE.maxLength(), 30);
        assertEquals(TMonth.JULY.maxLength(), 31);
        assertEquals(TMonth.AUGUST.maxLength(), 31);
        assertEquals(TMonth.SEPTEMBER.maxLength(), 30);
        assertEquals(TMonth.OCTOBER.maxLength(), 31);
        assertEquals(TMonth.NOVEMBER.maxLength(), 30);
        assertEquals(TMonth.DECEMBER.maxLength(), 31);
    }

    //-----------------------------------------------------------------------
    // firstDayOfYear(boolean)
    //-----------------------------------------------------------------------
    @Test
    public void test_firstDayOfYear_notLeapYear() {
        assertEquals(TMonth.JANUARY.firstDayOfYear(false), 1);
        assertEquals(TMonth.FEBRUARY.firstDayOfYear(false), 1 + 31);
        assertEquals(TMonth.MARCH.firstDayOfYear(false), 1 + 31 + 28);
        assertEquals(TMonth.APRIL.firstDayOfYear(false), 1 + 31 + 28 + 31);
        assertEquals(TMonth.MAY.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30);
        assertEquals(TMonth.JUNE.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30 + 31);
        assertEquals(TMonth.JULY.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30 + 31 + 30);
        assertEquals(TMonth.AUGUST.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30 + 31 + 30 + 31);
        assertEquals(TMonth.SEPTEMBER.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30 + 31 + 30 + 31 + 31);
        assertEquals(TMonth.OCTOBER.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30 + 31 + 30 + 31 + 31 + 30);
        assertEquals(TMonth.NOVEMBER.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31);
        assertEquals(TMonth.DECEMBER.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31 + 30);
    }

    @Test
    public void test_firstDayOfYear_leapYear() {
        assertEquals(TMonth.JANUARY.firstDayOfYear(true), 1);
        assertEquals(TMonth.FEBRUARY.firstDayOfYear(true), 1 + 31);
        assertEquals(TMonth.MARCH.firstDayOfYear(true), 1 + 31 + 29);
        assertEquals(TMonth.APRIL.firstDayOfYear(true), 1 + 31 + 29 + 31);
        assertEquals(TMonth.MAY.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30);
        assertEquals(TMonth.JUNE.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30 + 31);
        assertEquals(TMonth.JULY.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30 + 31 + 30);
        assertEquals(TMonth.AUGUST.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30 + 31 + 30 + 31);
        assertEquals(TMonth.SEPTEMBER.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30 + 31 + 30 + 31 + 31);
        assertEquals(TMonth.OCTOBER.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30 + 31 + 30 + 31 + 31 + 30);
        assertEquals(TMonth.NOVEMBER.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31);
        assertEquals(TMonth.DECEMBER.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31 + 30);
    }

    //-----------------------------------------------------------------------
    // firstMonthOfQuarter()
    //-----------------------------------------------------------------------
    @Test
    public void test_firstMonthOfQuarter() {
        assertEquals(TMonth.JANUARY.firstMonthOfQuarter(), TMonth.JANUARY);
        assertEquals(TMonth.FEBRUARY.firstMonthOfQuarter(), TMonth.JANUARY);
        assertEquals(TMonth.MARCH.firstMonthOfQuarter(), TMonth.JANUARY);
        assertEquals(TMonth.APRIL.firstMonthOfQuarter(), TMonth.APRIL);
        assertEquals(TMonth.MAY.firstMonthOfQuarter(), TMonth.APRIL);
        assertEquals(TMonth.JUNE.firstMonthOfQuarter(), TMonth.APRIL);
        assertEquals(TMonth.JULY.firstMonthOfQuarter(), TMonth.JULY);
        assertEquals(TMonth.AUGUST.firstMonthOfQuarter(), TMonth.JULY);
        assertEquals(TMonth.SEPTEMBER.firstMonthOfQuarter(), TMonth.JULY);
        assertEquals(TMonth.OCTOBER.firstMonthOfQuarter(), TMonth.OCTOBER);
        assertEquals(TMonth.NOVEMBER.firstMonthOfQuarter(), TMonth.OCTOBER);
        assertEquals(TMonth.DECEMBER.firstMonthOfQuarter(), TMonth.OCTOBER);
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @Test
    public void test_toString() {
        assertEquals(TMonth.JANUARY.toString(), "JANUARY");
        assertEquals(TMonth.FEBRUARY.toString(), "FEBRUARY");
        assertEquals(TMonth.MARCH.toString(), "MARCH");
        assertEquals(TMonth.APRIL.toString(), "APRIL");
        assertEquals(TMonth.MAY.toString(), "MAY");
        assertEquals(TMonth.JUNE.toString(), "JUNE");
        assertEquals(TMonth.JULY.toString(), "JULY");
        assertEquals(TMonth.AUGUST.toString(), "AUGUST");
        assertEquals(TMonth.SEPTEMBER.toString(), "SEPTEMBER");
        assertEquals(TMonth.OCTOBER.toString(), "OCTOBER");
        assertEquals(TMonth.NOVEMBER.toString(), "NOVEMBER");
        assertEquals(TMonth.DECEMBER.toString(), "DECEMBER");
    }

    //-----------------------------------------------------------------------
    // generated methods
    //-----------------------------------------------------------------------
    @Test
    public void test_enum() {
        assertEquals(TMonth.valueOf("JANUARY"), TMonth.JANUARY);
        assertEquals(TMonth.values()[0], TMonth.JANUARY);
    }

}
