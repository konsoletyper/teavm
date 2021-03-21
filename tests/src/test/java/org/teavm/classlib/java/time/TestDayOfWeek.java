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

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.JulianFields;
import java.time.temporal.Temporal;
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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test DayOfWeek.
 */
@Test
@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class TestDayOfWeek extends AbstractDateTimeTest {

    @BeforeMethod
    public void setUp() {
    }

    //-----------------------------------------------------------------------
    @Override
    protected List<TemporalAccessor> samples() {
        TemporalAccessor[] array = { MONDAY, WEDNESDAY, SUNDAY, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> validFields() {
        TemporalField[] array = {
            DAY_OF_WEEK,
        };
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> invalidFields() {
        List<TemporalField> list = new ArrayList<>(Arrays.asList(ChronoField.values()));
        list.removeAll(validFields());
        list.add(JulianFields.JULIAN_DAY);
        list.add(JulianFields.MODIFIED_JULIAN_DAY);
        list.add(JulianFields.RATA_DIE);
        return list;
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_int_singleton() {
        for (int i = 1; i <= 7; i++) {
            DayOfWeek test = DayOfWeek.of(i);
            assertEquals(test.getValue(), i);
            assertSame(DayOfWeek.of(i), test);
        }
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void test_factory_int_valueTooLow() {
        DayOfWeek.of(0);
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void test_factory_int_valueTooHigh() {
        DayOfWeek.of(8);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_CalendricalObject() {
        assertEquals(DayOfWeek.from(LocalDate.of(2011, 6, 6)), DayOfWeek.MONDAY);
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void test_factory_CalendricalObject_invalid_noDerive() {
        DayOfWeek.from(LocalTime.of(12, 30));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_factory_CalendricalObject_null() {
        DayOfWeek.from((TemporalAccessor) null);
    }

    //-----------------------------------------------------------------------
    // get(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_TemporalField() {
        assertEquals(DayOfWeek.WEDNESDAY.getLong(ChronoField.DAY_OF_WEEK), 3);
    }

    @Test
    public void test_getLong_TemporalField() {
        assertEquals(DayOfWeek.WEDNESDAY.getLong(ChronoField.DAY_OF_WEEK), 3);
    }

    //-----------------------------------------------------------------------
    // query(TemporalQuery)
    //-----------------------------------------------------------------------
    @Test
    public void test_query() {
        assertEquals(DayOfWeek.FRIDAY.query(TemporalQueries.chronology()), null);
        assertEquals(DayOfWeek.FRIDAY.query(TemporalQueries.localDate()), null);
        assertEquals(DayOfWeek.FRIDAY.query(TemporalQueries.localTime()), null);
        assertEquals(DayOfWeek.FRIDAY.query(TemporalQueries.offset()), null);
        assertEquals(DayOfWeek.FRIDAY.query(TemporalQueries.precision()), ChronoUnit.DAYS);
        assertEquals(DayOfWeek.FRIDAY.query(TemporalQueries.zone()), null);
        assertEquals(DayOfWeek.FRIDAY.query(TemporalQueries.zoneId()), null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_query_null() {
        DayOfWeek.FRIDAY.query(null);
    }

    //-----------------------------------------------------------------------
    // getDisplayName()
    //-----------------------------------------------------------------------
    @Test
    public void test_getDisplayName() {
        assertEquals(DayOfWeek.MONDAY.getDisplayName(TextStyle.SHORT, Locale.US), "Mon");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_getDisplayName_nullStyle() {
        DayOfWeek.MONDAY.getDisplayName(null, Locale.US);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_getDisplayName_nullLocale() {
        DayOfWeek.MONDAY.getDisplayName(TextStyle.FULL, null);
    }

    //-----------------------------------------------------------------------
    // plus(long), plus(long,unit)
    //-----------------------------------------------------------------------
    @DataProvider(name = "plus")
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

    @Test(dataProvider = "plus")
    public void test_plus_long(int base, long amount, int expected) {
        assertEquals(DayOfWeek.of(base).plus(amount), DayOfWeek.of(expected));
    }

    //-----------------------------------------------------------------------
    // minus(long), minus(long,unit)
    //-----------------------------------------------------------------------
    @DataProvider(name = "minus")
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

    @Test(dataProvider = "minus")
    public void test_minus_long(int base, long amount, int expected) {
        assertEquals(DayOfWeek.of(base).minus(amount), DayOfWeek.of(expected));
    }

    //-----------------------------------------------------------------------
    // adjustInto()
    //-----------------------------------------------------------------------
    @Test
    public void test_adjustInto() {
        assertEquals(DayOfWeek.MONDAY.adjustInto(LocalDate.of(2012, 9, 2)), LocalDate.of(2012, 8, 27));
        assertEquals(DayOfWeek.MONDAY.adjustInto(LocalDate.of(2012, 9, 3)), LocalDate.of(2012, 9, 3));
        assertEquals(DayOfWeek.MONDAY.adjustInto(LocalDate.of(2012, 9, 4)), LocalDate.of(2012, 9, 3));
        assertEquals(DayOfWeek.MONDAY.adjustInto(LocalDate.of(2012, 9, 10)), LocalDate.of(2012, 9, 10));
        assertEquals(DayOfWeek.MONDAY.adjustInto(LocalDate.of(2012, 9, 11)), LocalDate.of(2012, 9, 10));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_adjustInto_null() {
        DayOfWeek.MONDAY.adjustInto((Temporal) null);
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @Test
    public void test_toString() {
        assertEquals(DayOfWeek.MONDAY.toString(), "MONDAY");
        assertEquals(DayOfWeek.TUESDAY.toString(), "TUESDAY");
        assertEquals(DayOfWeek.WEDNESDAY.toString(), "WEDNESDAY");
        assertEquals(DayOfWeek.THURSDAY.toString(), "THURSDAY");
        assertEquals(DayOfWeek.FRIDAY.toString(), "FRIDAY");
        assertEquals(DayOfWeek.SATURDAY.toString(), "SATURDAY");
        assertEquals(DayOfWeek.SUNDAY.toString(), "SUNDAY");
    }

    //-----------------------------------------------------------------------
    // generated methods
    //-----------------------------------------------------------------------
    @Test
    public void test_enum() {
        assertEquals(DayOfWeek.valueOf("MONDAY"), DayOfWeek.MONDAY);
        assertEquals(DayOfWeek.values()[0], DayOfWeek.MONDAY);
    }

}
