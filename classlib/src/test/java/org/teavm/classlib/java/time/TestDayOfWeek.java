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
import static org.junit.Assert.assertSame;
import static org.teavm.classlib.java.time.TDayOfWeek.MONDAY;
import static org.teavm.classlib.java.time.TDayOfWeek.SUNDAY;
import static org.teavm.classlib.java.time.TDayOfWeek.WEDNESDAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.teavm.classlib.java.util.TLocale;

import org.junit.Before;
import org.testng.annotations.DataProvider;
import org.junit.Test;
import org.teavm.classlib.java.time.format.TTextStyle;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TJulianFields;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;

@Test
public class TestDayOfWeek extends AbstractDateTimeTest {

    @Before
    public void setUp() {
    }

    //-----------------------------------------------------------------------
    @Override
    protected List<TTemporalAccessor> samples() {
        TTemporalAccessor[] array = {MONDAY, WEDNESDAY, SUNDAY, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TTemporalField> validFields() {
        TTemporalField[] array = {
            DAY_OF_WEEK,
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
        for (int i = 1; i <= 7; i++) {
            TDayOfWeek test = TDayOfWeek.of(i);
            assertEquals(test.getValue(), i);
            assertSame(TDayOfWeek.of(i), test);
        }
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_factory_int_valueTooLow() {
        TDayOfWeek.of(0);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_factory_int_valueTooHigh() {
        TDayOfWeek.of(8);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_CalendricalObject() {
        assertEquals(TDayOfWeek.from(TLocalDate.of(2011, 6, 6)), TDayOfWeek.MONDAY);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_factory_CalendricalObject_invalid_noDerive() {
        TDayOfWeek.from(TLocalTime.of(12, 30));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_factory_CalendricalObject_null() {
        TDayOfWeek.from((TTemporalAccessor) null);
    }

    //-----------------------------------------------------------------------
    // get(TTemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_TemporalField() {
        assertEquals(TDayOfWeek.WEDNESDAY.getLong(TChronoField.DAY_OF_WEEK), 3);
    }

    @Test
    public void test_getLong_TemporalField() {
        assertEquals(TDayOfWeek.WEDNESDAY.getLong(TChronoField.DAY_OF_WEEK), 3);
    }

    //-----------------------------------------------------------------------
    // query(TTemporalQuery)
    //-----------------------------------------------------------------------
    @Test
    public void test_query() {
        assertEquals(TDayOfWeek.FRIDAY.query(TTemporalQueries.chronology()), null);
        assertEquals(TDayOfWeek.FRIDAY.query(TTemporalQueries.localDate()), null);
        assertEquals(TDayOfWeek.FRIDAY.query(TTemporalQueries.localTime()), null);
        assertEquals(TDayOfWeek.FRIDAY.query(TTemporalQueries.offset()), null);
        assertEquals(TDayOfWeek.FRIDAY.query(TTemporalQueries.precision()), TChronoUnit.DAYS);
        assertEquals(TDayOfWeek.FRIDAY.query(TTemporalQueries.zone()), null);
        assertEquals(TDayOfWeek.FRIDAY.query(TTemporalQueries.zoneId()), null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_query_null() {
        TDayOfWeek.FRIDAY.query(null);
    }

    //-----------------------------------------------------------------------
    // getDisplayName()
    //-----------------------------------------------------------------------
    @Test
    public void test_getDisplayName() {
        assertEquals(TDayOfWeek.MONDAY.getDisplayName(TTextStyle.SHORT, TLocale.US), "Mon");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_getDisplayName_nullStyle() {
        TDayOfWeek.MONDAY.getDisplayName(null, TLocale.US);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_getDisplayName_nullLocale() {
        TDayOfWeek.MONDAY.getDisplayName(TTextStyle.FULL, null);
    }

    //-----------------------------------------------------------------------
    // plus(long), plus(long,unit)
    //-----------------------------------------------------------------------
    @DataProvider(name="plus")
    Object[][] data_plus() {
        return new Object[][] {
            {1, -8, 7},
            {1, -7, 1},
            {1, -6, 2},
            {1, -5, 3},
            {1, -4, 4},
            {1, -3, 5},
            {1, -2, 6},
            {1, -1, 7},
            {1, 0, 1},
            {1, 1, 2},
            {1, 2, 3},
            {1, 3, 4},
            {1, 4, 5},
            {1, 5, 6},
            {1, 6, 7},
            {1, 7, 1},
            {1, 8, 2},

            {1, 1, 2},
            {2, 1, 3},
            {3, 1, 4},
            {4, 1, 5},
            {5, 1, 6},
            {6, 1, 7},
            {7, 1, 1},

            {1, -1, 7},
            {2, -1, 1},
            {3, -1, 2},
            {4, -1, 3},
            {5, -1, 4},
            {6, -1, 5},
            {7, -1, 6},
        };
    }

    @Test(dataProvider="plus")
    public void test_plus_long(int base, long amount, int expected) {
        assertEquals(TDayOfWeek.of(base).plus(amount), TDayOfWeek.of(expected));
    }

    //-----------------------------------------------------------------------
    // minus(long), minus(long,unit)
    //-----------------------------------------------------------------------
    @DataProvider(name="minus")
    Object[][] data_minus() {
        return new Object[][] {
            {1, -8, 2},
            {1, -7, 1},
            {1, -6, 7},
            {1, -5, 6},
            {1, -4, 5},
            {1, -3, 4},
            {1, -2, 3},
            {1, -1, 2},
            {1, 0, 1},
            {1, 1, 7},
            {1, 2, 6},
            {1, 3, 5},
            {1, 4, 4},
            {1, 5, 3},
            {1, 6, 2},
            {1, 7, 1},
            {1, 8, 7},
        };
    }

    @Test(dataProvider="minus")
    public void test_minus_long(int base, long amount, int expected) {
        assertEquals(TDayOfWeek.of(base).minus(amount), TDayOfWeek.of(expected));
    }

    //-----------------------------------------------------------------------
    // adjustInto()
    //-----------------------------------------------------------------------
    @Test
    public void test_adjustInto() {
        assertEquals(TDayOfWeek.MONDAY.adjustInto(TLocalDate.of(2012, 9, 2)), TLocalDate.of(2012, 8, 27));
        assertEquals(TDayOfWeek.MONDAY.adjustInto(TLocalDate.of(2012, 9, 3)), TLocalDate.of(2012, 9, 3));
        assertEquals(TDayOfWeek.MONDAY.adjustInto(TLocalDate.of(2012, 9, 4)), TLocalDate.of(2012, 9, 3));
        assertEquals(TDayOfWeek.MONDAY.adjustInto(TLocalDate.of(2012, 9, 10)), TLocalDate.of(2012, 9, 10));
        assertEquals(TDayOfWeek.MONDAY.adjustInto(TLocalDate.of(2012, 9, 11)), TLocalDate.of(2012, 9, 10));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_adjustInto_null() {
        TDayOfWeek.MONDAY.adjustInto((TTemporal) null);
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @Test
    public void test_toString() {
        assertEquals(TDayOfWeek.MONDAY.toString(), "MONDAY");
        assertEquals(TDayOfWeek.TUESDAY.toString(), "TUESDAY");
        assertEquals(TDayOfWeek.WEDNESDAY.toString(), "WEDNESDAY");
        assertEquals(TDayOfWeek.THURSDAY.toString(), "THURSDAY");
        assertEquals(TDayOfWeek.FRIDAY.toString(), "FRIDAY");
        assertEquals(TDayOfWeek.SATURDAY.toString(), "SATURDAY");
        assertEquals(TDayOfWeek.SUNDAY.toString(), "SUNDAY");
    }

    //-----------------------------------------------------------------------
    // generated methods
    //-----------------------------------------------------------------------
    @Test
    public void test_enum() {
        assertEquals(TDayOfWeek.valueOf("MONDAY"), TDayOfWeek.MONDAY);
        assertEquals(TDayOfWeek.values()[0], TDayOfWeek.MONDAY);
    }

}
