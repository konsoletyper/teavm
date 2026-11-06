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
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.text.ParsePosition;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTest;

/**
 * Test DateTimeFormatterBuilder.
 */
@TeaVMTest
public class TestDateTimeFormatterBuilder {

    private DateTimeFormatterBuilder builder;

    @BeforeEach
    public void setUp() {
        builder = new DateTimeFormatterBuilder();
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_toFormatter_empty() throws Exception {
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("", f.toString());
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parseCaseSensitive() throws Exception {
        builder.parseCaseSensitive();
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("ParseCaseSensitive(true)", f.toString());
    }

    @Test
    public void test_parseCaseInsensitive() throws Exception {
        builder.parseCaseInsensitive();
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("ParseCaseSensitive(false)", f.toString());
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parseStrict() throws Exception {
        builder.parseStrict();
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("ParseStrict(true)", f.toString());
    }

    @Test
    public void test_parseLenient() throws Exception {
        builder.parseLenient();
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("ParseStrict(false)", f.toString());
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_appendValue_1arg() throws Exception {
        builder.appendValue(DAY_OF_MONTH);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Value(DayOfMonth)", f.toString());
    }

    @Test
    public void test_appendValue_1arg_null() throws Exception {
        assertThrows(NullPointerException.class, () -> builder.appendValue(null));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_appendValue_2arg() throws Exception {
        builder.appendValue(DAY_OF_MONTH, 3);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Value(DayOfMonth,3)", f.toString());
    }

    @Test
    public void test_appendValue_2arg_null() throws Exception {
        assertThrows(NullPointerException.class, () -> builder.appendValue(null, 3));
    }

    @Test
    public void test_appendValue_2arg_widthTooSmall() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> builder.appendValue(DAY_OF_MONTH, 0));
    }

    @Test
    public void test_appendValue_2arg_widthTooBig() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> builder.appendValue(DAY_OF_MONTH, 20));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_appendValue_3arg() throws Exception {
        builder.appendValue(DAY_OF_MONTH, 2, 3, SignStyle.NORMAL);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Value(DayOfMonth,2,3,NORMAL)", f.toString());
    }

    @Test
    public void test_appendValue_3arg_nullField() throws Exception {
        assertThrows(NullPointerException.class, () -> builder.appendValue(null, 2, 3, SignStyle.NORMAL));
    }

    @Test
    public void test_appendValue_3arg_minWidthTooSmall() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> builder.appendValue(DAY_OF_MONTH, 0, 2, SignStyle.NORMAL));
    }

    @Test
    public void test_appendValue_3arg_minWidthTooBig() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> builder.appendValue(DAY_OF_MONTH, 20, 2, SignStyle.NORMAL));
    }

    @Test
    public void test_appendValue_3arg_maxWidthTooSmall() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> builder.appendValue(DAY_OF_MONTH, 2, 0, SignStyle.NORMAL));
    }

    @Test
    public void test_appendValue_3arg_maxWidthTooBig() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> builder.appendValue(DAY_OF_MONTH, 2, 20, SignStyle.NORMAL));
    }

    @Test
    public void test_appendValue_3arg_maxWidthMinWidth() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> builder.appendValue(DAY_OF_MONTH, 4, 2, SignStyle.NORMAL));
    }

    @Test
    public void test_appendValue_3arg_nullSignStyle() throws Exception {
        assertThrows(NullPointerException.class, () -> builder.appendValue(DAY_OF_MONTH, 2, 3, null));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_appendValue_subsequent2_parse3() throws Exception {
        builder.appendValue(MONTH_OF_YEAR, 1, 2, SignStyle.NORMAL).appendValue(DAY_OF_MONTH, 2);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Value(MonthOfYear,1,2,NORMAL)Value(DayOfMonth,2)", f.toString());
        TemporalAccessor cal = f.parseUnresolved("123", new ParsePosition(0));
        assertEquals(1, cal.get(MONTH_OF_YEAR));
        assertEquals(23, cal.get(DAY_OF_MONTH));
    }

    @Test
    public void test_appendValue_subsequent2_parse4() throws Exception {
        builder.appendValue(MONTH_OF_YEAR, 1, 2, SignStyle.NORMAL).appendValue(DAY_OF_MONTH, 2);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Value(MonthOfYear,1,2,NORMAL)Value(DayOfMonth,2)", f.toString());
        TemporalAccessor cal = f.parseUnresolved("0123", new ParsePosition(0));
        assertEquals(1, cal.get(MONTH_OF_YEAR));
        assertEquals(23, cal.get(DAY_OF_MONTH));
    }

    @Test
    public void test_appendValue_subsequent2_parse5() throws Exception {
        builder.appendValue(MONTH_OF_YEAR, 1, 2, SignStyle.NORMAL).appendValue(DAY_OF_MONTH, 2).appendLiteral('4');
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Value(MonthOfYear,1,2,NORMAL)Value(DayOfMonth,2)'4'", f.toString());
        TemporalAccessor cal = f.parseUnresolved("01234", new ParsePosition(0));
        assertEquals(1, cal.get(MONTH_OF_YEAR));
        assertEquals(23, cal.get(DAY_OF_MONTH));
    }

    @Test
    public void test_appendValue_subsequent3_parse6() throws Exception {
        builder
            .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
            .appendValue(MONTH_OF_YEAR, 2)
            .appendValue(DAY_OF_MONTH, 2);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Value(Year,4,10,EXCEEDS_PAD)Value(MonthOfYear,2)Value(DayOfMonth,2)", f.toString());
        TemporalAccessor cal = f.parseUnresolved("20090630", new ParsePosition(0));
        assertEquals(2009, cal.get(YEAR));
        assertEquals(6, cal.get(MONTH_OF_YEAR));
        assertEquals(30, cal.get(DAY_OF_MONTH));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_appendValueReduced_null() throws Exception {
        assertThrows(NullPointerException.class, () -> builder.appendValueReduced(null, 2, 2, 2000));
    }

    @Test
    public void test_appendValueReduced() throws Exception {
        builder.appendValueReduced(YEAR, 2, 2, 2000);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("ReducedValue(Year,2,2,2000)", f.toString());
        TemporalAccessor cal = f.parseUnresolved("12", new ParsePosition(0));
        assertEquals(2012, cal.get(YEAR));
    }

    @Test
    public void test_appendValueReduced_subsequent_parse() throws Exception {
        builder.appendValue(MONTH_OF_YEAR, 1, 2, SignStyle.NORMAL).appendValueReduced(YEAR, 2, 2, 2000);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Value(MonthOfYear,1,2,NORMAL)ReducedValue(Year,2,2,2000)", f.toString());
        TemporalAccessor cal = f.parseUnresolved("123", new ParsePosition(0));
        assertEquals(1, cal.get(MONTH_OF_YEAR));
        assertEquals(2023, cal.get(YEAR));
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @Test
    public void test_appendFraction_4arg() throws Exception {
        builder.appendFraction(MINUTE_OF_HOUR, 1, 9, false);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Fraction(MinuteOfHour,1,9)", f.toString());
    }

    @Test
    public void test_appendFraction_4arg_nullRule() throws Exception {
        assertThrows(NullPointerException.class, () -> builder.appendFraction(null, 1, 9, false));
    }

    @Test
    public void test_appendFraction_4arg_invalidRuleNotFixedSet() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> builder.appendFraction(DAY_OF_MONTH, 1, 9, false));
    }

    @Test
    public void test_appendFraction_4arg_minTooSmall() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> builder.appendFraction(MINUTE_OF_HOUR, -1, 9, false));
    }

    @Test
    public void test_appendFraction_4arg_minTooBig() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> builder.appendFraction(MINUTE_OF_HOUR, 10, 9, false));
    }

    @Test
    public void test_appendFraction_4arg_maxTooSmall() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> builder.appendFraction(MINUTE_OF_HOUR, 0, -1, false));
    }

    @Test
    public void test_appendFraction_4arg_maxTooBig() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> builder.appendFraction(MINUTE_OF_HOUR, 1, 10, false));
    }

    @Test
    public void test_appendFraction_4arg_maxWidthMinWidth() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> builder.appendFraction(MINUTE_OF_HOUR, 9, 3, false));
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @Test
    public void test_appendText_1arg() throws Exception {
        builder.appendText(MONTH_OF_YEAR);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Text(MonthOfYear)", f.toString());
    }

    @Test
    public void test_appendText_1arg_null() throws Exception {
        assertThrows(NullPointerException.class, () -> builder.appendText(null));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_appendText_2arg() throws Exception {
        builder.appendText(MONTH_OF_YEAR, TextStyle.SHORT);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Text(MonthOfYear,SHORT)", f.toString());
    }

    @Test
    public void test_appendText_2arg_nullRule() throws Exception {
        assertThrows(NullPointerException.class, () -> builder.appendText(null, TextStyle.SHORT));
    }

    @Test
    public void test_appendText_2arg_nullStyle() throws Exception {
        assertThrows(NullPointerException.class, () -> builder.appendText(MONTH_OF_YEAR, (TextStyle) null));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_appendTextMap() throws Exception {
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
        builder.appendText(MONTH_OF_YEAR, map);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Text(MonthOfYear)", f.toString());  // TODO: toString should be different?
    }

    @Test
    public void test_appendTextMap_nullRule() throws Exception {
        assertThrows(NullPointerException.class, () -> builder.appendText(null, new HashMap<Long, String>()));
    }

    @Test
    public void test_appendTextMap_nullStyle() {
        assertThrows(NullPointerException.class, () -> builder.appendText(MONTH_OF_YEAR, (Map<Long, String>) null));
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @Test
    public void test_appendOffsetId() throws Exception {
        builder.appendOffsetId();
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Offset(+HH:MM:ss,'Z')", f.toString());
    }

    static Object[][] data_offsetPatterns() {
        return new Object[][] {
            {"+HH"},
            {"+HHMM"},
            {"+HH:MM"},
            {"+HHMMss"},
            {"+HH:MM:ss"},
            {"+HHMMSS"},
            {"+HH:MM:SS"},
        };
    }

    @ParameterizedTest
    @MethodSource("data_offsetPatterns")
    public void test_appendOffset(String pattern) throws Exception {
        builder.appendOffset(pattern, "Z");
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Offset(" + pattern + ",'Z')", f.toString());
    }

    static Object[][] data_badOffsetPatterns() {
        return new Object[][] {
            {"HH"},
            {"HHMM"},
            {"HH:MM"},
            {"HHMMss"},
            {"HH:MM:ss"},
            {"HHMMSS"},
            {"HH:MM:SS"},
            {"+HHM"},
            {"+A"},
        };
    }

    @ParameterizedTest
    @MethodSource("data_badOffsetPatterns")
    public void test_appendOffset_badPattern(String pattern) throws Exception {
        assertThrows(IllegalArgumentException.class, () -> builder.appendOffset(pattern, "Z"));
    }

    @Test
    public void test_appendOffset_3arg_nullText() throws Exception {
        assertThrows(NullPointerException.class, () -> builder.appendOffset("+HH:MM", null));
    }

    @Test
    public void test_appendOffset_3arg_nullPattern() throws Exception {
        assertThrows(NullPointerException.class, () -> builder.appendOffset(null, "Z"));
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @Test
    public void test_appendZoneId() throws Exception {
        builder.appendZoneId();
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("ZoneId()", f.toString());
    }

    @Test
    public void test_appendZoneText_1arg() throws Exception {
        builder.appendZoneText(TextStyle.FULL);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("ZoneText(FULL)", f.toString());
    }

    @Test
    public void test_appendZoneText_1arg_nullText() throws Exception {
        assertThrows(NullPointerException.class, () -> builder.appendZoneText(null));
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @Test
    @SkipJVM
    // TODO: crashes on JVM due (possible) bug
    public void test_padNext_1arg() throws Exception {
        builder.appendValue(MONTH_OF_YEAR).padNext(2).appendValue(DAY_OF_MONTH).appendValue(DAY_OF_WEEK);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Value(MonthOfYear)Pad(Value(DayOfMonth),2)Value(DayOfWeek)", f.toString());
    }

    @Test
    public void test_padNext_1arg_invalidWidth() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> builder.padNext(0));
    }

    //-----------------------------------------------------------------------
    @Test
    @SkipJVM
    // TODO: crashes on JVM due (possible) bug
    public void test_padNext_2arg_dash() throws Exception {
        builder.appendValue(MONTH_OF_YEAR).padNext(2, '-').appendValue(DAY_OF_MONTH).appendValue(DAY_OF_WEEK);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Value(MonthOfYear)Pad(Value(DayOfMonth),2,'-')Value(DayOfWeek)", f.toString());
    }

    @Test
    public void test_padNext_2arg_invalidWidth() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> builder.padNext(0, '-'));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_padOptional() throws Exception {
        builder.appendValue(MONTH_OF_YEAR).padNext(5).optionalStart().appendValue(DAY_OF_MONTH)
                .optionalEnd().appendValue(DAY_OF_WEEK);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Value(MonthOfYear)Pad([Value(DayOfMonth)],5)Value(DayOfWeek)", f.toString());
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @Test
    public void test_optionalStart_noEnd() throws Exception {
        builder.appendValue(MONTH_OF_YEAR).optionalStart().appendValue(DAY_OF_MONTH).appendValue(DAY_OF_WEEK);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Value(MonthOfYear)[Value(DayOfMonth)Value(DayOfWeek)]", f.toString());
    }

    @Test
    public void test_optionalStart2_noEnd() throws Exception {
        builder.appendValue(MONTH_OF_YEAR).optionalStart().appendValue(DAY_OF_MONTH).optionalStart()
                .appendValue(DAY_OF_WEEK);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Value(MonthOfYear)[Value(DayOfMonth)[Value(DayOfWeek)]]", f.toString());
    }

    @Test
    public void test_optionalStart_doubleStart() throws Exception {
        builder.appendValue(MONTH_OF_YEAR).optionalStart().optionalStart().appendValue(DAY_OF_MONTH);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Value(MonthOfYear)[[Value(DayOfMonth)]]", f.toString());
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_optionalEnd() throws Exception {
        builder.appendValue(MONTH_OF_YEAR).optionalStart().appendValue(DAY_OF_MONTH).optionalEnd()
                .appendValue(DAY_OF_WEEK);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Value(MonthOfYear)[Value(DayOfMonth)]Value(DayOfWeek)", f.toString());
    }

    @Test
    public void test_optionalEnd2() throws Exception {
        builder.appendValue(MONTH_OF_YEAR).optionalStart().appendValue(DAY_OF_MONTH)
            .optionalStart().appendValue(DAY_OF_WEEK).optionalEnd().appendValue(DAY_OF_MONTH).optionalEnd();
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Value(MonthOfYear)[Value(DayOfMonth)[Value(DayOfWeek)]Value(DayOfMonth)]", f.toString());
    }

    @Test
    public void test_optionalEnd_doubleStartSingleEnd() throws Exception {
        builder.appendValue(MONTH_OF_YEAR).optionalStart().optionalStart().appendValue(DAY_OF_MONTH).optionalEnd();
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Value(MonthOfYear)[[Value(DayOfMonth)]]", f.toString());
    }

    @Test
    public void test_optionalEnd_doubleStartDoubleEnd() throws Exception {
        builder.appendValue(MONTH_OF_YEAR).optionalStart().optionalStart().appendValue(DAY_OF_MONTH)
                .optionalEnd().optionalEnd();
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Value(MonthOfYear)[[Value(DayOfMonth)]]", f.toString());
    }

    @Test
    public void test_optionalStartEnd_immediateStartEnd() throws Exception {
        builder.appendValue(MONTH_OF_YEAR).optionalStart().optionalEnd().appendValue(DAY_OF_MONTH);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals("Value(MonthOfYear)Value(DayOfMonth)", f.toString());
    }

    @Test
    public void test_optionalEnd_noStart() throws Exception {
        assertThrows(IllegalStateException.class, () -> builder.optionalEnd());
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    static Object[][] dataValid() {
        return new Object[][] {
            {"'a'", "'a'"},
            {"''", "''"},
            {"'!'", "'!'"},
            {"!", "'!'"},

            {"'hello_people,][)('", "'hello_people,][)('"},
            {"'hi'", "'hi'"},
            {"'yyyy'", "'yyyy'"},
            {"''''", "''"},
            {"'o''clock'", "'o''clock'"},

            {"G", "Text(Era,SHORT)"},
            {"GG", "Text(Era,SHORT)"},
            {"GGG", "Text(Era,SHORT)"},
            {"GGGG", "Text(Era)"},
            {"GGGGG", "Text(Era,NARROW)"},

            {"u", "Value(Year)"},
            {"uu", "ReducedValue(Year,2,2,2000-01-01)"},
            {"uuu", "Value(Year,3,19,NORMAL)"},
            {"uuuu", "Value(Year,4,19,EXCEEDS_PAD)"},
            {"uuuuu", "Value(Year,5,19,EXCEEDS_PAD)"},

            {"y", "Value(YearOfEra)"},
            {"yy", "ReducedValue(YearOfEra,2,2,2000-01-01)"},
            {"yyy", "Value(YearOfEra,3,19,NORMAL)"},
            {"yyyy", "Value(YearOfEra,4,19,EXCEEDS_PAD)"},
            {"yyyyy", "Value(YearOfEra,5,19,EXCEEDS_PAD)"},

//            {"Y", "Value(WeekBasedYear)"},
//            {"YY", "ReducedValue(WeekBasedYear,2,2000)"},
//            {"YYY", "Value(WeekBasedYear,3,19,NORMAL)"},
//            {"YYYY", "Value(WeekBasedYear,4,19,EXCEEDS_PAD)"},
//            {"YYYYY", "Value(WeekBasedYear,5,19,EXCEEDS_PAD)"},

            {"M", "Value(MonthOfYear)"},
            {"MM", "Value(MonthOfYear,2)"},
            {"MMM", "Text(MonthOfYear,SHORT)"},
            {"MMMM", "Text(MonthOfYear)"},
            {"MMMMM", "Text(MonthOfYear,NARROW)"},

//            {"w", "Value(WeekOfWeekBasedYear)"},
//            {"ww", "Value(WeekOfWeekBasedYear,2)"},
//            {"www", "Value(WeekOfWeekBasedYear,3)"},

            {"D", "Value(DayOfYear)"},
            {"DD", "Value(DayOfYear,2,3,NOT_NEGATIVE)"},
            {"DDD", "Value(DayOfYear,3)"},

            {"d", "Value(DayOfMonth)"},
            {"dd", "Value(DayOfMonth,2)"},

                // TODO: looks like JDK 21 has bug here
            //{"F", "Value(AlignedDayOfWeekInMonth)"},

            {"E", "Text(DayOfWeek,SHORT)"},
            {"EE", "Text(DayOfWeek,SHORT)"},
            {"EEE", "Text(DayOfWeek,SHORT)"},
            {"EEEE", "Text(DayOfWeek)"},
            {"EEEEE", "Text(DayOfWeek,NARROW)"},

            {"a", "Text(AmPmOfDay,SHORT)"},

            {"H", "Value(HourOfDay)"},
            {"HH", "Value(HourOfDay,2)"},

            {"K", "Value(HourOfAmPm)"},
            {"KK", "Value(HourOfAmPm,2)"},

            {"k", "Value(ClockHourOfDay)"},
            {"kk", "Value(ClockHourOfDay,2)"},

            {"h", "Value(ClockHourOfAmPm)"},
            {"hh", "Value(ClockHourOfAmPm,2)"},

            {"m", "Value(MinuteOfHour)"},
            {"mm", "Value(MinuteOfHour,2)"},

            {"s", "Value(SecondOfMinute)"},
            {"ss", "Value(SecondOfMinute,2)"},

            {"S", "Fraction(NanoOfSecond,1,1)"},
            {"SS", "Fraction(NanoOfSecond,2,2)"},
            {"SSS", "Fraction(NanoOfSecond,3,3)"},
            {"SSSSSSSSS", "Fraction(NanoOfSecond,9,9)"},

            {"A", "Value(MilliOfDay,1,19,NOT_NEGATIVE)"},
            {"AA", "Value(MilliOfDay,2,19,NOT_NEGATIVE)"},
            {"AAA", "Value(MilliOfDay,3,19,NOT_NEGATIVE)"},

            {"n", "Value(NanoOfSecond,1,19,NOT_NEGATIVE)"},
            {"nn", "Value(NanoOfSecond,2,19,NOT_NEGATIVE)"},
            {"nnn", "Value(NanoOfSecond,3,19,NOT_NEGATIVE)"},

            {"N", "Value(NanoOfDay,1,19,NOT_NEGATIVE)"},
            {"NN", "Value(NanoOfDay,2,19,NOT_NEGATIVE)"},
            {"NNN", "Value(NanoOfDay,3,19,NOT_NEGATIVE)"},

            {"z", "ZoneText(SHORT)"},
            {"zz", "ZoneText(SHORT)"},
            {"zzz", "ZoneText(SHORT)"},
            {"zzzz", "ZoneText(FULL)"},

            {"VV", "ZoneId()"},

            {"Z", "Offset(+HHMM,'+0000')"},  // SimpleDateFormat compatible
            {"ZZ", "Offset(+HHMM,'+0000')"},
            {"ZZZ", "Offset(+HHMM,'+0000')"},

            {"X", "Offset(+HHmm,'Z')"},
            {"XX", "Offset(+HHMM,'Z')"},
            {"XXX", "Offset(+HH:MM,'Z')"},
            {"XXXX", "Offset(+HHMMss,'Z')"},
            {"XXXXX", "Offset(+HH:MM:ss,'Z')"},

            {"x", "Offset(+HHmm,'+00')"},
            {"xx", "Offset(+HHMM,'+0000')"},
            {"xxx", "Offset(+HH:MM,'+00:00')"},
            {"xxxx", "Offset(+HHMMss,'+0000')"},
            {"xxxxx", "Offset(+HH:MM:ss,'+00:00')"},

            {"ppH", "Pad(Value(HourOfDay),2)"},
            {"pppDD", "Pad(Value(DayOfYear,2,3,NOT_NEGATIVE),3)"},

            {"uuuu[-MM[-dd", "Value(Year,4,19,EXCEEDS_PAD)['-'Value(MonthOfYear,2)['-'Value(DayOfMonth,2)]]"},
            {"uuuu[-MM[-dd]]", "Value(Year,4,19,EXCEEDS_PAD)['-'Value(MonthOfYear,2)['-'Value(DayOfMonth,2)]]"},
            {"uuuu[-MM[]-dd]", "Value(Year,4,19,EXCEEDS_PAD)['-'Value(MonthOfYear,2)'-'Value(DayOfMonth,2)]"},

            {"uuuu-MM-dd'T'HH:mm:ss.SSS", "Value(Year,4,19,EXCEEDS_PAD)'-'Value(MonthOfYear,2)'-'Value(DayOfMonth,2)"
                    +  "'T'Value(HourOfDay,2)':'Value(MinuteOfHour,2)':'Value(SecondOfMinute,2)'."
                    + "'Fraction(NanoOfSecond,3,3)"},
        };
    }

    @ParameterizedTest
    @MethodSource("dataValid")
    public void test_appendPattern_valid(String input, String expected) throws Exception {
        builder.appendPattern(input);
        DateTimeFormatter f = builder.toFormatter();
        assertEquals(expected, f.toString());
    }

    //-----------------------------------------------------------------------
    static Object[][] dataInvalid() {
        return new Object[][] {
            {"'"},
            {"'hello"},
            {"'hel''lo"},
            {"'hello''"},
            {"]"},
            {"{"},
            {"}"},
            {"#"},

            {"yyyy]"},
            {"yyyy]MM"},
            {"yyyy[MM]]"},

            {"MMMMMM"},
            {"QQQQQQ"},
            {"EEEEEE"},
            {"aaaaaa"},
            {"XXXXXX"},

            {"RO"},

            {"p"},
            {"pp"},
            {"p:"},

            {"f"},
            {"ff"},
            {"f:"},
            {"fy"},
            {"fa"},
            {"fM"},
            
            {"ddd"},
            {"FF"},
            {"FFF"},
            {"aa"},
            {"aaa"},
            {"aaaa"},
            {"aaaaa"},
            {"HHH"},
            {"KKK"},
            {"kkk"},
            {"hhh"},
            {"mmm"},
            {"sss"},
        };
    }

    @ParameterizedTest
    @MethodSource("dataInvalid")
    public void test_appendPattern_invalid(String input) throws Exception {
        assertThrows(IllegalArgumentException.class, () -> {
            try {
                builder.appendPattern(input);
            } catch (IllegalArgumentException ex) {
                throw ex;
            }
        });
    }

}
