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
package org.teavm.classlib.java.time.format;

import static org.junit.Assert.assertEquals;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TMonth;
import org.teavm.classlib.java.time.temporal.TTemporalField;

public class TestDateTimeTextPrinting {

    private TDateTimeFormatterBuilder builder;

    @Before
    public void setUp() {

        this.builder = new TDateTimeFormatterBuilder();
    }

    Object[][] data_text() {

        return new Object[][] { { DAY_OF_WEEK, TTextStyle.FULL, 1, "Monday" },
        { DAY_OF_WEEK, TTextStyle.FULL, 2, "Tuesday" }, { DAY_OF_WEEK, TTextStyle.FULL, 3, "Wednesday" },
        { DAY_OF_WEEK, TTextStyle.FULL, 4, "Thursday" }, { DAY_OF_WEEK, TTextStyle.FULL, 5, "Friday" },
        { DAY_OF_WEEK, TTextStyle.FULL, 6, "Saturday" }, { DAY_OF_WEEK, TTextStyle.FULL, 7, "Sunday" },

        { DAY_OF_WEEK, TTextStyle.SHORT, 1, "Mon" }, { DAY_OF_WEEK, TTextStyle.SHORT, 2, "Tue" },
        { DAY_OF_WEEK, TTextStyle.SHORT, 3, "Wed" }, { DAY_OF_WEEK, TTextStyle.SHORT, 4, "Thu" },
        { DAY_OF_WEEK, TTextStyle.SHORT, 5, "Fri" }, { DAY_OF_WEEK, TTextStyle.SHORT, 6, "Sat" },
        { DAY_OF_WEEK, TTextStyle.SHORT, 7, "Sun" },

        { DAY_OF_MONTH, TTextStyle.FULL, 1, "1" }, { DAY_OF_MONTH, TTextStyle.FULL, 2, "2" },
        { DAY_OF_MONTH, TTextStyle.FULL, 3, "3" }, { DAY_OF_MONTH, TTextStyle.FULL, 28, "28" },
        { DAY_OF_MONTH, TTextStyle.FULL, 29, "29" }, { DAY_OF_MONTH, TTextStyle.FULL, 30, "30" },
        { DAY_OF_MONTH, TTextStyle.FULL, 31, "31" },

        { DAY_OF_MONTH, TTextStyle.SHORT, 1, "1" }, { DAY_OF_MONTH, TTextStyle.SHORT, 2, "2" },
        { DAY_OF_MONTH, TTextStyle.SHORT, 3, "3" }, { DAY_OF_MONTH, TTextStyle.SHORT, 28, "28" },
        { DAY_OF_MONTH, TTextStyle.SHORT, 29, "29" }, { DAY_OF_MONTH, TTextStyle.SHORT, 30, "30" },
        { DAY_OF_MONTH, TTextStyle.SHORT, 31, "31" },

        { MONTH_OF_YEAR, TTextStyle.FULL, 1, "January" }, { MONTH_OF_YEAR, TTextStyle.FULL, 12, "December" },

        { MONTH_OF_YEAR, TTextStyle.SHORT, 1, "Jan" }, { MONTH_OF_YEAR, TTextStyle.SHORT, 12, "Dec" }, };
    }

    @Test
    public void test_appendText2arg_print() {

        for (Object[] data : data_text()) {
            TTemporalField field = (TTemporalField) data[0];
            TTextStyle style = (TTextStyle) data[1];
            int value = (int) data[2];
            String expected = (String) data[3];

            TDateTimeFormatter f = new TDateTimeFormatterBuilder().appendText(field, style).toFormatter(Locale.ENGLISH);
            TLocalDateTime dt = TLocalDateTime.of(2010, 1, 1, 0, 0);
            dt = dt.with(field, value);
            String text = f.format(dt);
            assertEquals(text, expected);
        }
    }

    @Test
    public void test_appendText1arg_print() {

        for (Object[] data : data_text()) {
            TTemporalField field = (TTemporalField) data[0];
            TTextStyle style = (TTextStyle) data[1];
            int value = (int) data[2];
            String expected = (String) data[3];

            if (style == TTextStyle.FULL) {
                TDateTimeFormatter f = new TDateTimeFormatterBuilder().appendText(field).toFormatter(Locale.ENGLISH);
                TLocalDateTime dt = TLocalDateTime.of(2010, 1, 1, 0, 0);
                dt = dt.with(field, value);
                String text = f.format(dt);
                assertEquals(text, expected);
            }
        }
    }

    @Test
    public void test_print_appendText2arg_french_long() {

        TDateTimeFormatter f = this.builder.appendText(MONTH_OF_YEAR, TTextStyle.FULL).toFormatter(Locale.FRENCH);
        TLocalDateTime dt = TLocalDateTime.of(2010, 1, 1, 0, 0);
        String text = f.format(dt);
        assertEquals(text, "janvier");
    }

    @Test
    public void test_print_appendText2arg_french_short() {

        TDateTimeFormatter f = this.builder.appendText(MONTH_OF_YEAR, TTextStyle.SHORT).toFormatter(Locale.FRENCH);
        TLocalDateTime dt = TLocalDateTime.of(2010, 1, 1, 0, 0);
        String text = f.format(dt);
        assertEquals(text, "janv.");
    }

    @Test
    public void test_appendTextMap() {

        Map<Long, String> map = new HashMap<>();
        map.put(1L, "JNY");
        map.put(2L, "FBY");
        map.put(3L, "MCH");
        map.put(4L, "APL");
        map.put(5L, "MAY");
        map.put(6L, "JUN");
        map.put(7L, "JLY");
        map.put(8L, "AGT");
        map.put(9L, "SPT");
        map.put(10L, "OBR");
        map.put(11L, "NVR");
        map.put(12L, "DBR");
        this.builder.appendText(MONTH_OF_YEAR, map);
        TDateTimeFormatter f = this.builder.toFormatter();
        TLocalDateTime dt = TLocalDateTime.of(2010, 1, 1, 0, 0);
        for (TMonth month : TMonth.values()) {
            assertEquals(f.format(dt.with(month)), map.get((long) month.getValue()));
        }
    }

    @Test
    public void test_appendTextMap_DOM() {

        Map<Long, String> map = new HashMap<Long, String>();
        map.put(1L, "1st");
        map.put(2L, "2nd");
        map.put(3L, "3rd");
        this.builder.appendText(DAY_OF_MONTH, map);
        TDateTimeFormatter f = this.builder.toFormatter();
        TLocalDateTime dt = TLocalDateTime.of(2010, 1, 1, 0, 0);
        assertEquals(f.format(dt.withDayOfMonth(1)), "1st");
        assertEquals(f.format(dt.withDayOfMonth(2)), "2nd");
        assertEquals(f.format(dt.withDayOfMonth(3)), "3rd");
    }

    @Test
    public void test_appendTextMapIncomplete() {

        Map<Long, String> map = new HashMap<Long, String>();
        map.put(1L, "JNY");
        this.builder.appendText(MONTH_OF_YEAR, map);
        TDateTimeFormatter f = this.builder.toFormatter();
        TLocalDateTime dt = TLocalDateTime.of(2010, 2, 1, 0, 0);
        assertEquals(f.format(dt), "2");
    }

}
