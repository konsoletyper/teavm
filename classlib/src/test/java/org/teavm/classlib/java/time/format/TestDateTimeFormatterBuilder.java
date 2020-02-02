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
import static org.junit.Assert.fail;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;
import static org.teavm.classlib.java.time.temporal.TChronoField.MINUTE_OF_HOUR;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;

import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;

public class TestDateTimeFormatterBuilder {

    private TDateTimeFormatterBuilder builder;

    @Before
    public void setUp() {

        this.builder = new TDateTimeFormatterBuilder();
    }

    @Test
    public void test_toFormatter_empty() {

        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "");
    }

    @Test
    public void test_parseCaseSensitive() {

        this.builder.parseCaseSensitive();
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "ParseCaseSensitive(true)");
    }

    @Test
    public void test_parseCaseInsensitive() {

        this.builder.parseCaseInsensitive();
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "ParseCaseSensitive(false)");
    }

    @Test
    public void test_parseStrict() {

        this.builder.parseStrict();
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "ParseStrict(true)");
    }

    @Test
    public void test_parseLenient() {

        this.builder.parseLenient();
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "ParseStrict(false)");
    }

    @Test
    public void test_appendValue_1arg() {

        this.builder.appendValue(DAY_OF_MONTH);
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Value(DayOfMonth)");
    }

    @Test(expected = NullPointerException.class)
    public void test_appendValue_1arg_null() {

        this.builder.appendValue(null);
    }

    @Test
    public void test_appendValue_2arg() {

        this.builder.appendValue(DAY_OF_MONTH, 3);
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Value(DayOfMonth,3)");
    }

    @Test(expected = NullPointerException.class)
    public void test_appendValue_2arg_null() {

        this.builder.appendValue(null, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_appendValue_2arg_widthTooSmall() {

        this.builder.appendValue(DAY_OF_MONTH, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_appendValue_2arg_widthTooBig() {

        this.builder.appendValue(DAY_OF_MONTH, 20);
    }

    @Test
    public void test_appendValue_3arg() {

        this.builder.appendValue(DAY_OF_MONTH, 2, 3, TSignStyle.NORMAL);
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Value(DayOfMonth,2,3,NORMAL)");
    }

    @Test(expected = NullPointerException.class)
    public void test_appendValue_3arg_nullField() {

        this.builder.appendValue(null, 2, 3, TSignStyle.NORMAL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_appendValue_3arg_minWidthTooSmall() {

        this.builder.appendValue(DAY_OF_MONTH, 0, 2, TSignStyle.NORMAL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_appendValue_3arg_minWidthTooBig() {

        this.builder.appendValue(DAY_OF_MONTH, 20, 2, TSignStyle.NORMAL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_appendValue_3arg_maxWidthTooSmall() {

        this.builder.appendValue(DAY_OF_MONTH, 2, 0, TSignStyle.NORMAL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_appendValue_3arg_maxWidthTooBig() {

        this.builder.appendValue(DAY_OF_MONTH, 2, 20, TSignStyle.NORMAL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_appendValue_3arg_maxWidthMinWidth() {

        this.builder.appendValue(DAY_OF_MONTH, 4, 2, TSignStyle.NORMAL);
    }

    @Test(expected = NullPointerException.class)
    public void test_appendValue_3arg_nullSignStyle() {

        this.builder.appendValue(DAY_OF_MONTH, 2, 3, null);
    }

    @Test
    public void test_appendValue_subsequent2_parse3() {

        this.builder.appendValue(MONTH_OF_YEAR, 1, 2, TSignStyle.NORMAL).appendValue(DAY_OF_MONTH, 2);
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Value(MonthOfYear,1,2,NORMAL)Value(DayOfMonth,2)");
        TTemporalAccessor cal = f.parseUnresolved("123", new ParsePosition(0));
        assertEquals(cal.get(MONTH_OF_YEAR), 1);
        assertEquals(cal.get(DAY_OF_MONTH), 23);
    }

    @Test
    public void test_appendValue_subsequent2_parse4() {

        this.builder.appendValue(MONTH_OF_YEAR, 1, 2, TSignStyle.NORMAL).appendValue(DAY_OF_MONTH, 2);
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Value(MonthOfYear,1,2,NORMAL)Value(DayOfMonth,2)");
        TTemporalAccessor cal = f.parseUnresolved("0123", new ParsePosition(0));
        assertEquals(cal.get(MONTH_OF_YEAR), 1);
        assertEquals(cal.get(DAY_OF_MONTH), 23);
    }

    @Test
    public void test_appendValue_subsequent2_parse5() {

        this.builder.appendValue(MONTH_OF_YEAR, 1, 2, TSignStyle.NORMAL).appendValue(DAY_OF_MONTH, 2)
                .appendLiteral('4');
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Value(MonthOfYear,1,2,NORMAL)Value(DayOfMonth,2)'4'");
        TTemporalAccessor cal = f.parseUnresolved("01234", new ParsePosition(0));
        assertEquals(cal.get(MONTH_OF_YEAR), 1);
        assertEquals(cal.get(DAY_OF_MONTH), 23);
    }

    @Test
    public void test_appendValue_subsequent3_parse6() {

        this.builder.appendValue(YEAR, 4, 10, TSignStyle.EXCEEDS_PAD).appendValue(MONTH_OF_YEAR, 2)
                .appendValue(DAY_OF_MONTH, 2);
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Value(TYear,4,10,EXCEEDS_PAD)Value(MonthOfYear,2)Value(DayOfMonth,2)");
        TTemporalAccessor cal = f.parseUnresolved("20090630", new ParsePosition(0));
        assertEquals(cal.get(YEAR), 2009);
        assertEquals(cal.get(MONTH_OF_YEAR), 6);
        assertEquals(cal.get(DAY_OF_MONTH), 30);
    }

    @Test(expected = NullPointerException.class)
    public void test_appendValueReduced_null() {

        this.builder.appendValueReduced(null, 2, 2, 2000);
    }

    @Test
    public void test_appendValueReduced() {

        this.builder.appendValueReduced(YEAR, 2, 2, 2000);
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "ReducedValue(TYear,2,2,2000)");
        TTemporalAccessor cal = f.parseUnresolved("12", new ParsePosition(0));
        assertEquals(cal.get(YEAR), 2012);
    }

    @Test
    public void test_appendValueReduced_subsequent_parse() {

        this.builder.appendValue(MONTH_OF_YEAR, 1, 2, TSignStyle.NORMAL).appendValueReduced(YEAR, 2, 2, 2000);
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Value(MonthOfYear,1,2,NORMAL)ReducedValue(TYear,2,2,2000)");
        TTemporalAccessor cal = f.parseUnresolved("123", new ParsePosition(0));
        assertEquals(cal.get(MONTH_OF_YEAR), 1);
        assertEquals(cal.get(YEAR), 2023);
    }

    @Test
    public void test_appendFraction_4arg() {

        this.builder.appendFraction(MINUTE_OF_HOUR, 1, 9, false);
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Fraction(MinuteOfHour,1,9)");
    }

    @Test(expected = NullPointerException.class)
    public void test_appendFraction_4arg_nullRule() {

        this.builder.appendFraction(null, 1, 9, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_appendFraction_4arg_invalidRuleNotFixedSet() {

        this.builder.appendFraction(DAY_OF_MONTH, 1, 9, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_appendFraction_4arg_minTooSmall() {

        this.builder.appendFraction(MINUTE_OF_HOUR, -1, 9, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_appendFraction_4arg_minTooBig() {

        this.builder.appendFraction(MINUTE_OF_HOUR, 10, 9, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_appendFraction_4arg_maxTooSmall() {

        this.builder.appendFraction(MINUTE_OF_HOUR, 0, -1, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_appendFraction_4arg_maxTooBig() {

        this.builder.appendFraction(MINUTE_OF_HOUR, 1, 10, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_appendFraction_4arg_maxWidthMinWidth() {

        this.builder.appendFraction(MINUTE_OF_HOUR, 9, 3, false);
    }

    @Test
    public void test_appendText_1arg() {

        this.builder.appendText(MONTH_OF_YEAR);
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Text(MonthOfYear)");
    }

    @Test(expected = NullPointerException.class)
    public void test_appendText_1arg_null() {

        this.builder.appendText(null);
    }

    @Test
    public void test_appendText_2arg() {

        this.builder.appendText(MONTH_OF_YEAR, TTextStyle.SHORT);
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Text(MonthOfYear,SHORT)");
    }

    @Test(expected = NullPointerException.class)
    public void test_appendText_2arg_nullRule() {

        this.builder.appendText(null, TTextStyle.SHORT);
    }

    @Test(expected = NullPointerException.class)
    public void test_appendText_2arg_nullStyle() {

        this.builder.appendText(MONTH_OF_YEAR, (TTextStyle) null);
    }

    @Test
    public void test_appendTextMap() {

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
        this.builder.appendText(MONTH_OF_YEAR, map);
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Text(MonthOfYear)"); // TODO: toString should be different?
    }

    @Test(expected = NullPointerException.class)
    public void test_appendTextMap_nullRule() {

        this.builder.appendText(null, new HashMap<Long, String>());
    }

    @Test(expected = NullPointerException.class)
    public void test_appendTextMap_nullStyle() {

        this.builder.appendText(MONTH_OF_YEAR, (Map<Long, String>) null);
    }

    @Test
    public void test_appendOffsetId() {

        this.builder.appendOffsetId();
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Offset(+HH:MM:ss,'Z')");
    }

    Object[][] data_offsetPatterns() {

        return new Object[][] { { "+HH" }, { "+HHMM" }, { "+HH:MM" }, { "+HHMMss" }, { "+HH:MM:ss" }, { "+HHMMSS" },
        { "+HH:MM:SS" }, };
    }

    @Test
    public void test_appendOffset() {

        for (Object[] data : data_offsetPatterns()) {
            String pattern = (String) data[0];

            TDateTimeFormatterBuilder formatterBuilder = new TDateTimeFormatterBuilder();
            formatterBuilder.appendOffset(pattern, "Z");
            TDateTimeFormatter f = formatterBuilder.toFormatter();
            assertEquals(f.toString(), "Offset(" + pattern + ",'Z')");
        }
    }

    Object[][] data_badOffsetPatterns() {

        return new Object[][] { { "HH" }, { "HHMM" }, { "HH:MM" }, { "HHMMss" }, { "HH:MM:ss" }, { "HHMMSS" },
        { "HH:MM:SS" }, { "+H" }, { "+HMM" }, { "+HHM" }, { "+A" }, };
    }

    @Test
    public void test_appendOffset_badPattern() {

        for (Object[] data : data_badOffsetPatterns()) {
            String pattern = (String) data[0];

            try {
                this.builder.appendOffset(pattern, "Z");
                fail("Expected IllegalArgumentException");
            } catch (IllegalArgumentException e) {
                // expected
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void test_appendOffset_3arg_nullText() {

        this.builder.appendOffset("+HH:MM", null);
    }

    @Test(expected = NullPointerException.class)
    public void test_appendOffset_3arg_nullPattern() {

        this.builder.appendOffset(null, "Z");
    }

    @Test
    public void test_appendZoneId() {

        this.builder.appendZoneId();
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "TZoneId()");
    }

    @Test
    public void test_appendZoneText_1arg() {

        this.builder.appendZoneText(TTextStyle.FULL);
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "ZoneText(FULL)");
    }

    @Test(expected = NullPointerException.class)
    public void test_appendZoneText_1arg_nullText() {

        this.builder.appendZoneText(null);
    }

    @Test
    public void test_padNext_1arg() {

        this.builder.appendValue(MONTH_OF_YEAR).padNext(2).appendValue(DAY_OF_MONTH).appendValue(DAY_OF_WEEK);
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Value(MonthOfYear)Pad(Value(DayOfMonth),2)Value(TDayOfWeek)");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_padNext_1arg_invalidWidth() {

        this.builder.padNext(0);
    }

    @Test
    public void test_padNext_2arg_dash() {

        this.builder.appendValue(MONTH_OF_YEAR).padNext(2, '-').appendValue(DAY_OF_MONTH).appendValue(DAY_OF_WEEK);
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Value(MonthOfYear)Pad(Value(DayOfMonth),2,'-')Value(TDayOfWeek)");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_padNext_2arg_invalidWidth() {

        this.builder.padNext(0, '-');
    }

    @Test
    public void test_padOptional() {

        this.builder.appendValue(MONTH_OF_YEAR).padNext(5).optionalStart().appendValue(DAY_OF_MONTH).optionalEnd()
                .appendValue(DAY_OF_WEEK);
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Value(MonthOfYear)Pad([Value(DayOfMonth)],5)Value(TDayOfWeek)");
    }

    @Test
    public void test_optionalStart_noEnd() {

        this.builder.appendValue(MONTH_OF_YEAR).optionalStart().appendValue(DAY_OF_MONTH).appendValue(DAY_OF_WEEK);
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Value(MonthOfYear)[Value(DayOfMonth)Value(TDayOfWeek)]");
    }

    @Test
    public void test_optionalStart2_noEnd() {

        this.builder.appendValue(MONTH_OF_YEAR).optionalStart().appendValue(DAY_OF_MONTH).optionalStart()
                .appendValue(DAY_OF_WEEK);
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Value(MonthOfYear)[Value(DayOfMonth)[Value(TDayOfWeek)]]");
    }

    @Test
    public void test_optionalStart_doubleStart() {

        this.builder.appendValue(MONTH_OF_YEAR).optionalStart().optionalStart().appendValue(DAY_OF_MONTH);
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Value(MonthOfYear)[[Value(DayOfMonth)]]");
    }

    @Test
    public void test_optionalEnd() {

        this.builder.appendValue(MONTH_OF_YEAR).optionalStart().appendValue(DAY_OF_MONTH).optionalEnd()
                .appendValue(DAY_OF_WEEK);
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Value(MonthOfYear)[Value(DayOfMonth)]Value(TDayOfWeek)");
    }

    @Test
    public void test_optionalEnd2() {

        this.builder.appendValue(MONTH_OF_YEAR).optionalStart().appendValue(DAY_OF_MONTH).optionalStart()
                .appendValue(DAY_OF_WEEK).optionalEnd().appendValue(DAY_OF_MONTH).optionalEnd();
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Value(MonthOfYear)[Value(DayOfMonth)[Value(TDayOfWeek)]Value(DayOfMonth)]");
    }

    @Test
    public void test_optionalEnd_doubleStartSingleEnd() {

        this.builder.appendValue(MONTH_OF_YEAR).optionalStart().optionalStart().appendValue(DAY_OF_MONTH).optionalEnd();
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Value(MonthOfYear)[[Value(DayOfMonth)]]");
    }

    @Test
    public void test_optionalEnd_doubleStartDoubleEnd() {

        this.builder.appendValue(MONTH_OF_YEAR).optionalStart().optionalStart().appendValue(DAY_OF_MONTH).optionalEnd()
                .optionalEnd();
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Value(MonthOfYear)[[Value(DayOfMonth)]]");
    }

    @Test
    public void test_optionalStartEnd_immediateStartEnd() {

        this.builder.appendValue(MONTH_OF_YEAR).optionalStart().optionalEnd().appendValue(DAY_OF_MONTH);
        TDateTimeFormatter f = this.builder.toFormatter();
        assertEquals(f.toString(), "Value(MonthOfYear)Value(DayOfMonth)");
    }

    @Test(expected = IllegalStateException.class)
    public void test_optionalEnd_noStart() {

        this.builder.optionalEnd();
    }

    Object[][] dataValid() {

        return new Object[][] { { "'a'", "'a'" }, { "''", "''" }, { "'!'", "'!'" }, { "!", "'!'" },

        { "'hello_people,][)('", "'hello_people,][)('" }, { "'hi'", "'hi'" }, { "'yyyy'", "'yyyy'" }, { "''''", "''" },
        { "'o''clock'", "'o''clock'" },

        { "G", "Text(TEra,SHORT)" }, { "GG", "Text(TEra,SHORT)" }, { "GGG", "Text(TEra,SHORT)" },
        { "GGGG", "Text(TEra)" }, { "GGGGG", "Text(TEra,NARROW)" },

        { "u", "Value(TYear)" }, { "uu", "ReducedValue(TYear,2,2,2000-01-01)" }, { "uuu", "Value(TYear,3,19,NORMAL)" },
        { "uuuu", "Value(TYear,4,19,EXCEEDS_PAD)" }, { "uuuuu", "Value(TYear,5,19,EXCEEDS_PAD)" },

        { "y", "Value(YearOfEra)" }, { "yy", "ReducedValue(YearOfEra,2,2,2000-01-01)" },
        { "yyy", "Value(YearOfEra,3,19,NORMAL)" }, { "yyyy", "Value(YearOfEra,4,19,EXCEEDS_PAD)" },
        { "yyyyy", "Value(YearOfEra,5,19,EXCEEDS_PAD)" },

        // {"Y", "Value(WeekBasedYear)"},
        // {"YY", "ReducedValue(WeekBasedYear,2,2000)"},
        // {"YYY", "Value(WeekBasedYear,3,19,NORMAL)"},
        // {"YYYY", "Value(WeekBasedYear,4,19,EXCEEDS_PAD)"},
        // {"YYYYY", "Value(WeekBasedYear,5,19,EXCEEDS_PAD)"},

        { "M", "Value(MonthOfYear)" }, { "MM", "Value(MonthOfYear,2)" }, { "MMM", "Text(MonthOfYear,SHORT)" },
        { "MMMM", "Text(MonthOfYear)" }, { "MMMMM", "Text(MonthOfYear,NARROW)" },

        // {"w", "Value(WeekOfWeekBasedYear)"},
        // {"ww", "Value(WeekOfWeekBasedYear,2)"},
        // {"www", "Value(WeekOfWeekBasedYear,3)"},

        { "D", "Value(DayOfYear)" }, { "DD", "Value(DayOfYear,2)" }, { "DDD", "Value(DayOfYear,3)" },

        { "d", "Value(DayOfMonth)" }, { "dd", "Value(DayOfMonth,2)" },

        { "F", "Value(AlignedDayOfWeekInMonth)" },

        { "E", "Text(TDayOfWeek,SHORT)" }, { "EE", "Text(TDayOfWeek,SHORT)" }, { "EEE", "Text(TDayOfWeek,SHORT)" },
        { "EEEE", "Text(TDayOfWeek)" }, { "EEEEE", "Text(TDayOfWeek,NARROW)" },

        { "a", "Text(AmPmOfDay,SHORT)" },

        { "H", "Value(HourOfDay)" }, { "HH", "Value(HourOfDay,2)" },

        { "K", "Value(HourOfAmPm)" }, { "KK", "Value(HourOfAmPm,2)" },

        { "k", "Value(ClockHourOfDay)" }, { "kk", "Value(ClockHourOfDay,2)" },

        { "h", "Value(ClockHourOfAmPm)" }, { "hh", "Value(ClockHourOfAmPm,2)" },

        { "m", "Value(MinuteOfHour)" }, { "mm", "Value(MinuteOfHour,2)" },

        { "s", "Value(SecondOfMinute)" }, { "ss", "Value(SecondOfMinute,2)" },

        { "S", "Fraction(NanoOfSecond,1,1)" }, { "SS", "Fraction(NanoOfSecond,2,2)" },
        { "SSS", "Fraction(NanoOfSecond,3,3)" }, { "SSSSSSSSS", "Fraction(NanoOfSecond,9,9)" },

        { "A", "Value(MilliOfDay)" }, { "AA", "Value(MilliOfDay,2)" }, { "AAA", "Value(MilliOfDay,3)" },

        { "n", "Value(NanoOfSecond)" }, { "nn", "Value(NanoOfSecond,2)" }, { "nnn", "Value(NanoOfSecond,3)" },

        { "N", "Value(NanoOfDay)" }, { "NN", "Value(NanoOfDay,2)" }, { "NNN", "Value(NanoOfDay,3)" },

        { "z", "ZoneText(SHORT)" }, { "zz", "ZoneText(SHORT)" }, { "zzz", "ZoneText(SHORT)" },
        { "zzzz", "ZoneText(FULL)" },

        { "VV", "TZoneId()" },

        { "Z", "Offset(+HHMM,'+0000')" }, // SimpleDateFormat compatible
        { "ZZ", "Offset(+HHMM,'+0000')" }, { "ZZZ", "Offset(+HHMM,'+0000')" },

        { "X", "Offset(+HHmm,'Z')" }, { "XX", "Offset(+HHMM,'Z')" }, { "XXX", "Offset(+HH:MM,'Z')" },
        { "XXXX", "Offset(+HHMMss,'Z')" }, { "XXXXX", "Offset(+HH:MM:ss,'Z')" },

        { "x", "Offset(+HHmm,'+00')" }, { "xx", "Offset(+HHMM,'+0000')" }, { "xxx", "Offset(+HH:MM,'+00:00')" },
        { "xxxx", "Offset(+HHMMss,'+0000')" }, { "xxxxx", "Offset(+HH:MM:ss,'+00:00')" },

        { "ppH", "Pad(Value(HourOfDay),2)" }, { "pppDD", "Pad(Value(DayOfYear,2),3)" },

        { "uuuu[-MM[-dd", "Value(TYear,4,19,EXCEEDS_PAD)['-'Value(MonthOfYear,2)['-'Value(DayOfMonth,2)]]" },
        { "uuuu[-MM[-dd]]", "Value(TYear,4,19,EXCEEDS_PAD)['-'Value(MonthOfYear,2)['-'Value(DayOfMonth,2)]]" },
        { "uuuu[-MM[]-dd]", "Value(TYear,4,19,EXCEEDS_PAD)['-'Value(MonthOfYear,2)'-'Value(DayOfMonth,2)]" },

        { "uuuu-MM-dd'T'HH:mm:ss.SSS", "Value(TYear,4,19,EXCEEDS_PAD)'-'Value(MonthOfYear,2)'-'Value(DayOfMonth,2)"
                + "'T'Value(HourOfDay,2)':'Value(MinuteOfHour,2)':'Value(SecondOfMinute,2)'.'Fraction(NanoOfSecond,3,3)" }, };
    }

    @Test
    public void test_appendPattern_valid() {

        for (Object[] data : dataValid()) {
            String input = (String) data[0];
            String expected = (String) data[1];

            TDateTimeFormatterBuilder formatterBuilder = new TDateTimeFormatterBuilder();
            formatterBuilder.appendPattern(input);
            TDateTimeFormatter f = formatterBuilder.toFormatter();
            assertEquals(f.toString(), expected);
        }
    }

    Object[][] dataInvalid() {

        return new Object[][] { { "'" }, { "'hello" }, { "'hel''lo" }, { "'hello''" }, { "]" }, { "{" }, { "}" },
        { "#" },

        { "yyyy]" }, { "yyyy]MM" }, { "yyyy[MM]]" },

        { "MMMMMM" }, { "QQQQQQ" }, { "EEEEEE" }, { "aaaaaa" }, { "XXXXXX" },

        { "RO" },

        { "p" }, { "pp" }, { "p:" },

        { "f" }, { "ff" }, { "f:" }, { "fy" }, { "fa" }, { "fM" },

        { "ddd" }, { "FF" }, { "FFF" }, { "aa" }, { "aaa" }, { "aaaa" }, { "aaaaa" }, { "HHH" }, { "KKK" }, { "kkk" },
        { "hhh" }, { "mmm" }, { "sss" }, };
    }

    @Test
    public void test_appendPattern_invalid() {

        for (Object[] data : dataInvalid()) {
            String input = (String) data[0];

            try {
                this.builder.appendPattern(input);
                fail("Expected IllegalArgumentException");
            } catch (IllegalArgumentException ex) {
                // exepcted
            }
        }
    }

}
