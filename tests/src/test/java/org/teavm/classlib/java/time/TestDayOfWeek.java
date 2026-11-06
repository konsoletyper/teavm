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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.teavm.junit.TeaVMTest;

/**
 * Test DayOfWeek.
 */
@TeaVMTest
public class TestDayOfWeek extends AbstractDateTimeTest {

    @BeforeEach
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
            assertEquals(i, test.getValue());
            assertSame(test, DayOfWeek.of(i));
        }
    }

    @Test
    public void test_factory_int_valueTooLow() {
        assertThrows(DateTimeException.class, () -> DayOfWeek.of(0));
    }

    @Test
    public void test_factory_int_valueTooHigh() {
        assertThrows(DateTimeException.class, () -> DayOfWeek.of(8));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_CalendricalObject() {
        assertEquals(DayOfWeek.MONDAY, DayOfWeek.from(LocalDate.of(2011, 6, 6)));
    }

    @Test
    public void test_factory_CalendricalObject_invalid_noDerive() {
        assertThrows(DateTimeException.class, () -> DayOfWeek.from(LocalTime.of(12, 30)));
    }

    @Test
    public void test_factory_CalendricalObject_null() {
        assertThrows(NullPointerException.class, () -> DayOfWeek.from((TemporalAccessor) null));
    }

    //-----------------------------------------------------------------------
    // get(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_TemporalField() {
        assertEquals(3, DayOfWeek.WEDNESDAY.getLong(ChronoField.DAY_OF_WEEK));
    }

    @Test
    public void test_getLong_TemporalField() {
        assertEquals(3, DayOfWeek.WEDNESDAY.getLong(ChronoField.DAY_OF_WEEK));
    }

    //-----------------------------------------------------------------------
    // query(TemporalQuery)
    //-----------------------------------------------------------------------
    @Test
    public void test_query() {
        assertEquals(null, DayOfWeek.FRIDAY.query(TemporalQueries.chronology()));
        assertEquals(null, DayOfWeek.FRIDAY.query(TemporalQueries.localDate()));
        assertEquals(null, DayOfWeek.FRIDAY.query(TemporalQueries.localTime()));
        assertEquals(null, DayOfWeek.FRIDAY.query(TemporalQueries.offset()));
        assertEquals(ChronoUnit.DAYS, DayOfWeek.FRIDAY.query(TemporalQueries.precision()));
        assertEquals(null, DayOfWeek.FRIDAY.query(TemporalQueries.zone()));
        assertEquals(null, DayOfWeek.FRIDAY.query(TemporalQueries.zoneId()));
    }

    @Test
    public void test_query_null() {
        assertThrows(NullPointerException.class, () -> DayOfWeek.FRIDAY.query(null));
    }

    //-----------------------------------------------------------------------
    // getDisplayName()
    //-----------------------------------------------------------------------
    @Test
    public void test_getDisplayName() {
        assertEquals("Mon", DayOfWeek.MONDAY.getDisplayName(TextStyle.SHORT, Locale.US));
    }

    @Test
    public void test_getDisplayName_nullStyle() {
        assertThrows(NullPointerException.class, () -> DayOfWeek.MONDAY.getDisplayName(null, Locale.US));
    }

    @Test
    public void test_getDisplayName_nullLocale() {
        assertThrows(NullPointerException.class, () -> DayOfWeek.MONDAY.getDisplayName(TextStyle.FULL, null));
    }

    //-----------------------------------------------------------------------
    // plus(long), plus(long,unit)
    //-----------------------------------------------------------------------
    static Object[][] data_plus() {
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

    @ParameterizedTest
    @MethodSource("data_plus")
    public void test_plus_long(int base, long amount, int expected) {
        assertEquals(DayOfWeek.of(expected), DayOfWeek.of(base).plus(amount));
    }

    //-----------------------------------------------------------------------
    // minus(long), minus(long,unit)
    //-----------------------------------------------------------------------
    static Object[][] data_minus() {
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

    @ParameterizedTest
    @MethodSource("data_minus")
    public void test_minus_long(int base, long amount, int expected) {
        assertEquals(DayOfWeek.of(expected), DayOfWeek.of(base).minus(amount));
    }

    //-----------------------------------------------------------------------
    // adjustInto()
    //-----------------------------------------------------------------------
    @Test
    public void test_adjustInto() {
        assertEquals(LocalDate.of(2012, 8, 27), DayOfWeek.MONDAY.adjustInto(LocalDate.of(2012, 9, 2)));
        assertEquals(LocalDate.of(2012, 9, 3), DayOfWeek.MONDAY.adjustInto(LocalDate.of(2012, 9, 3)));
        assertEquals(LocalDate.of(2012, 9, 3), DayOfWeek.MONDAY.adjustInto(LocalDate.of(2012, 9, 4)));
        assertEquals(LocalDate.of(2012, 9, 10), DayOfWeek.MONDAY.adjustInto(LocalDate.of(2012, 9, 10)));
        assertEquals(LocalDate.of(2012, 9, 10), DayOfWeek.MONDAY.adjustInto(LocalDate.of(2012, 9, 11)));
    }

    @Test
    public void test_adjustInto_null() {
        assertThrows(NullPointerException.class, () -> DayOfWeek.MONDAY.adjustInto((Temporal) null));
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @Test
    public void test_toString() {
        assertEquals("MONDAY", DayOfWeek.MONDAY.toString());
        assertEquals("TUESDAY", DayOfWeek.TUESDAY.toString());
        assertEquals("WEDNESDAY", DayOfWeek.WEDNESDAY.toString());
        assertEquals("THURSDAY", DayOfWeek.THURSDAY.toString());
        assertEquals("FRIDAY", DayOfWeek.FRIDAY.toString());
        assertEquals("SATURDAY", DayOfWeek.SATURDAY.toString());
        assertEquals("SUNDAY", DayOfWeek.SUNDAY.toString());
    }

    //-----------------------------------------------------------------------
    // generated methods
    //-----------------------------------------------------------------------
    @Test
    public void test_enum() {
        assertEquals(DayOfWeek.MONDAY, DayOfWeek.valueOf("MONDAY"));
        assertEquals(DayOfWeek.MONDAY, DayOfWeek.values()[0]);
    }

}
