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
package org.teavm.classlib.java.time.format;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static org.testng.Assert.assertEquals;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.TemporalField;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMProperties;
import org.teavm.junit.TeaVMProperty;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test text printing.
 */
@Test
@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
@TeaVMProperties(@TeaVMProperty(key = "java.util.Locale.available", value = "en, en_US, fr_FR"))
public class TestDateTimeTextPrinting {

    private DateTimeFormatterBuilder builder;

    @BeforeMethod
    public void setUp() {
        builder = new DateTimeFormatterBuilder();
    }

    //-----------------------------------------------------------------------
    @DataProvider(name = "printText")
    Object[][] data_text() {
        return new Object[][] {
            {DAY_OF_WEEK, TextStyle.FULL, 1, "Monday"},
            {DAY_OF_WEEK, TextStyle.FULL, 2, "Tuesday"},
            {DAY_OF_WEEK, TextStyle.FULL, 3, "Wednesday"},
            {DAY_OF_WEEK, TextStyle.FULL, 4, "Thursday"},
            {DAY_OF_WEEK, TextStyle.FULL, 5, "Friday"},
            {DAY_OF_WEEK, TextStyle.FULL, 6, "Saturday"},
            {DAY_OF_WEEK, TextStyle.FULL, 7, "Sunday"},

            {DAY_OF_WEEK, TextStyle.SHORT, 1, "Mon"},
            {DAY_OF_WEEK, TextStyle.SHORT, 2, "Tue"},
            {DAY_OF_WEEK, TextStyle.SHORT, 3, "Wed"},
            {DAY_OF_WEEK, TextStyle.SHORT, 4, "Thu"},
            {DAY_OF_WEEK, TextStyle.SHORT, 5, "Fri"},
            {DAY_OF_WEEK, TextStyle.SHORT, 6, "Sat"},
            {DAY_OF_WEEK, TextStyle.SHORT, 7, "Sun"},

            {DAY_OF_MONTH, TextStyle.FULL, 1, "1"},
            {DAY_OF_MONTH, TextStyle.FULL, 2, "2"},
            {DAY_OF_MONTH, TextStyle.FULL, 3, "3"},
            {DAY_OF_MONTH, TextStyle.FULL, 28, "28"},
            {DAY_OF_MONTH, TextStyle.FULL, 29, "29"},
            {DAY_OF_MONTH, TextStyle.FULL, 30, "30"},
            {DAY_OF_MONTH, TextStyle.FULL, 31, "31"},

            {DAY_OF_MONTH, TextStyle.SHORT, 1, "1"},
            {DAY_OF_MONTH, TextStyle.SHORT, 2, "2"},
            {DAY_OF_MONTH, TextStyle.SHORT, 3, "3"},
            {DAY_OF_MONTH, TextStyle.SHORT, 28, "28"},
            {DAY_OF_MONTH, TextStyle.SHORT, 29, "29"},
            {DAY_OF_MONTH, TextStyle.SHORT, 30, "30"},
            {DAY_OF_MONTH, TextStyle.SHORT, 31, "31"},

            {MONTH_OF_YEAR, TextStyle.FULL, 1, "January"},
            {MONTH_OF_YEAR, TextStyle.FULL, 12, "December"},

            {MONTH_OF_YEAR, TextStyle.SHORT, 1, "Jan"},
            {MONTH_OF_YEAR, TextStyle.SHORT, 12, "Dec"},
       };
    }

    @Test(dataProvider = "printText")
    public void test_appendText2arg_print(TemporalField field, TextStyle style, int value, String expected) {
        DateTimeFormatter f = builder.appendText(field, style).toFormatter(Locale.ENGLISH);
        LocalDateTime dt = LocalDateTime.of(2010, 1, 1, 0, 0);
        dt = dt.with(field, value);
        String text = f.format(dt);
        assertEquals(text, expected);
    }

    @Test(dataProvider = "printText")
    public void test_appendText1arg_print(TemporalField field, TextStyle style, int value, String expected) {
        if (style == TextStyle.FULL) {
            DateTimeFormatter f = builder.appendText(field).toFormatter(Locale.ENGLISH);
            LocalDateTime dt = LocalDateTime.of(2010, 1, 1, 0, 0);
            dt = dt.with(field, value);
            String text = f.format(dt);
            assertEquals(text, expected);
        }
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_print_appendText2arg_french_long() throws Exception {
        DateTimeFormatter f = builder.appendText(MONTH_OF_YEAR, TextStyle.FULL).toFormatter(Locale.FRENCH);
        LocalDateTime dt = LocalDateTime.of(2010, 1, 1, 0, 0);
        String text = f.format(dt);
        assertEquals(text, "janvier");
    }

    @Test
    public void test_print_appendText2arg_french_short() throws Exception {
        DateTimeFormatter f = builder.appendText(MONTH_OF_YEAR, TextStyle.SHORT).toFormatter(Locale.FRENCH);
        LocalDateTime dt = LocalDateTime.of(2010, 1, 1, 0, 0);
        String text = f.format(dt);
        assertEquals(text, "janv.");
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_appendTextMap() throws Exception {
        Map<Long, String> map = new HashMap<Long, String>();
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
        builder.appendText(MONTH_OF_YEAR, map);
        DateTimeFormatter f = builder.toFormatter();
        LocalDateTime dt = LocalDateTime.of(2010, 1, 1, 0, 0);
        for (Month month : Month.values()) {
            assertEquals(f.format(dt.with(month)), map.get((long) month.getValue()));
        }
    }

    @Test
    public void test_appendTextMap_DOM() throws Exception {
        Map<Long, String> map = new HashMap<Long, String>();
        map.put(1L, "1st");
        map.put(2L, "2nd");
        map.put(3L, "3rd");
        builder.appendText(DAY_OF_MONTH, map);
        DateTimeFormatter f = builder.toFormatter();
        LocalDateTime dt = LocalDateTime.of(2010, 1, 1, 0, 0);
        assertEquals(f.format(dt.withDayOfMonth(1)), "1st");
        assertEquals(f.format(dt.withDayOfMonth(2)), "2nd");
        assertEquals(f.format(dt.withDayOfMonth(3)), "3rd");
    }

    @Test
    public void test_appendTextMapIncomplete() throws Exception {
        Map<Long, String> map = new HashMap<Long, String>();
        map.put(1L, "JNY");
        builder.appendText(MONTH_OF_YEAR, map);
        DateTimeFormatter f = builder.toFormatter();
        LocalDateTime dt = LocalDateTime.of(2010, 2, 1, 0, 0);
        assertEquals(f.format(dt), "2");
    }

}
