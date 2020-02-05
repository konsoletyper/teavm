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
package org.teavm.classlib.java.time.temporal;

import static org.junit.Assert.assertEquals;
import static org.teavm.classlib.java.time.TMonth.AUGUST;
import static org.teavm.classlib.java.time.TMonth.FEBRUARY;
import static org.teavm.classlib.java.time.TMonth.JULY;
import static org.teavm.classlib.java.time.TMonth.JUNE;
import static org.teavm.classlib.java.time.TMonth.MARCH;
import static org.teavm.classlib.java.time.TMonth.OCTOBER;
import static org.teavm.classlib.java.time.TMonth.SEPTEMBER;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.FOREVER;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MONTHS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.WEEKS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.YEARS;

import org.junit.Test;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TMonth;
import org.teavm.classlib.java.time.TZoneOffset;

public class TestChronoUnit {

    Object[][] data_yearsBetween() {

        return new Object[][] { { date(1939, SEPTEMBER, 2), date(1939, SEPTEMBER, 1), 0 },
        { date(1939, SEPTEMBER, 2), date(1939, SEPTEMBER, 2), 0 },
        { date(1939, SEPTEMBER, 2), date(1939, SEPTEMBER, 3), 0 },

        { date(1939, SEPTEMBER, 2), date(1940, SEPTEMBER, 1), 0 },
        { date(1939, SEPTEMBER, 2), date(1940, SEPTEMBER, 2), 1 },
        { date(1939, SEPTEMBER, 2), date(1940, SEPTEMBER, 3), 1 },

        { date(1939, SEPTEMBER, 2), date(1938, SEPTEMBER, 1), -1 },
        { date(1939, SEPTEMBER, 2), date(1938, SEPTEMBER, 2), -1 },
        { date(1939, SEPTEMBER, 2), date(1938, SEPTEMBER, 3), 0 },

        { date(1939, SEPTEMBER, 2), date(1945, SEPTEMBER, 3), 6 },
        { date(1939, SEPTEMBER, 2), date(1945, OCTOBER, 3), 6 },
        { date(1939, SEPTEMBER, 2), date(1945, AUGUST, 3), 5 }, };
    }

    @Test
    public void test_yearsBetween() {

        for (Object[] data : data_yearsBetween()) {
            TLocalDate start = (TLocalDate) data[0];
            TLocalDate end = (TLocalDate) data[1];
            long expected = ((Number) data[2]).longValue();

            assertEquals(YEARS.between(start, end), expected);
        }
    }

    @Test
    public void test_yearsBetweenReversed() {

        for (Object[] data : data_yearsBetween()) {
            TLocalDate start = (TLocalDate) data[0];
            TLocalDate end = (TLocalDate) data[1];
            long expected = ((Number) data[2]).longValue();

            assertEquals(YEARS.between(end, start), -expected);
        }
    }

    @Test
    public void test_yearsBetween_LocalDateTimeSameTime() {

        for (Object[] data : data_yearsBetween()) {
            TLocalDate start = (TLocalDate) data[0];
            TLocalDate end = (TLocalDate) data[1];
            long expected = ((Number) data[2]).longValue();

            assertEquals(YEARS.between(start.atTime(12, 30), end.atTime(12, 30)), expected);
        }
    }

    @Test
    public void test_yearsBetween_LocalDateTimeLaterTime() {

        for (Object[] data : data_yearsBetween()) {
            TLocalDate start = (TLocalDate) data[0];
            TLocalDate end = (TLocalDate) data[1];
            long expected = ((Number) data[2]).longValue();

            if (end.isAfter(start)) {
                assertEquals(YEARS.between(start.atTime(12, 30), end.atTime(12, 31)), expected);
            } else {
                assertEquals(YEARS.between(start.atTime(12, 31), end.atTime(12, 30)), expected);
            }
        }
    }

    @Test
    public void test_yearsBetween_ZonedDateSameOffset() {

        for (Object[] data : data_yearsBetween()) {
            TLocalDate start = (TLocalDate) data[0];
            TLocalDate end = (TLocalDate) data[1];
            long expected = ((Number) data[2]).longValue();

            assertEquals(
                    YEARS.between(start.atStartOfDay(TZoneOffset.ofHours(2)), end.atStartOfDay(TZoneOffset.ofHours(2))),
                    expected);
        }
    }

    @Test
    public void test_yearsBetween_ZonedDateLaterOffset() {

        for (Object[] data : data_yearsBetween()) {
            TLocalDate start = (TLocalDate) data[0];
            TLocalDate end = (TLocalDate) data[1];
            long expected = ((Number) data[2]).longValue();

            // +01:00 is later than +02:00
            if (end.isAfter(start)) {
                assertEquals(YEARS.between(start.atStartOfDay(TZoneOffset.ofHours(2)),
                        end.atStartOfDay(TZoneOffset.ofHours(1))), expected);
            } else {
                assertEquals(YEARS.between(start.atStartOfDay(TZoneOffset.ofHours(1)),
                        end.atStartOfDay(TZoneOffset.ofHours(2))), expected);
            }
        }
    }

    Object[][] data_monthsBetween() {

        return new Object[][] { { date(2012, JULY, 2), date(2012, JULY, 1), 0 },
        { date(2012, JULY, 2), date(2012, JULY, 2), 0 }, { date(2012, JULY, 2), date(2012, JULY, 3), 0 },

        { date(2012, JULY, 2), date(2012, AUGUST, 1), 0 }, { date(2012, JULY, 2), date(2012, AUGUST, 2), 1 },
        { date(2012, JULY, 2), date(2012, AUGUST, 3), 1 },

        { date(2012, JULY, 2), date(2012, SEPTEMBER, 1), 1 }, { date(2012, JULY, 2), date(2012, SEPTEMBER, 2), 2 },
        { date(2012, JULY, 2), date(2012, SEPTEMBER, 3), 2 },

        { date(2012, JULY, 2), date(2012, JUNE, 1), -1 }, { date(2012, JULY, 2), date(2012, JUNE, 2), -1 },
        { date(2012, JULY, 2), date(2012, JUNE, 3), 0 },

        { date(2012, FEBRUARY, 27), date(2012, MARCH, 26), 0 }, { date(2012, FEBRUARY, 27), date(2012, MARCH, 27), 1 },
        { date(2012, FEBRUARY, 27), date(2012, MARCH, 28), 1 },

        { date(2012, FEBRUARY, 28), date(2012, MARCH, 27), 0 }, { date(2012, FEBRUARY, 28), date(2012, MARCH, 28), 1 },
        { date(2012, FEBRUARY, 28), date(2012, MARCH, 29), 1 },

        { date(2012, FEBRUARY, 29), date(2012, MARCH, 28), 0 }, { date(2012, FEBRUARY, 29), date(2012, MARCH, 29), 1 },
        { date(2012, FEBRUARY, 29), date(2012, MARCH, 30), 1 }, };
    }

    @Test
    public void test_monthsBetween() {

        for (Object[] data : data_monthsBetween()) {
            TLocalDate start = (TLocalDate) data[0];
            TLocalDate end = (TLocalDate) data[1];
            long expected = ((Number) data[2]).longValue();

            assertEquals(MONTHS.between(start, end), expected);
        }
    }

    @Test
    public void test_monthsBetweenReversed() {

        for (Object[] data : data_monthsBetween()) {
            TLocalDate start = (TLocalDate) data[0];
            TLocalDate end = (TLocalDate) data[1];
            long expected = ((Number) data[2]).longValue();

            assertEquals(MONTHS.between(end, start), -expected);
        }
    }

    @Test
    public void test_monthsBetween_LocalDateTimeSameTime() {

        for (Object[] data : data_monthsBetween()) {
            TLocalDate start = (TLocalDate) data[0];
            TLocalDate end = (TLocalDate) data[1];
            long expected = ((Number) data[2]).longValue();

            assertEquals(MONTHS.between(start.atTime(12, 30), end.atTime(12, 30)), expected);
        }
    }

    @Test
    public void test_monthsBetween_LocalDateTimeLaterTime() {

        for (Object[] data : data_monthsBetween()) {
            TLocalDate start = (TLocalDate) data[0];
            TLocalDate end = (TLocalDate) data[1];
            long expected = ((Number) data[2]).longValue();

            if (end.isAfter(start)) {
                assertEquals(MONTHS.between(start.atTime(12, 30), end.atTime(12, 31)), expected);
            } else {
                assertEquals(MONTHS.between(start.atTime(12, 31), end.atTime(12, 30)), expected);
            }
        }
    }

    @Test
    public void test_monthsBetween_ZonedDateSameOffset() {

        for (Object[] data : data_monthsBetween()) {
            TLocalDate start = (TLocalDate) data[0];
            TLocalDate end = (TLocalDate) data[1];
            long expected = ((Number) data[2]).longValue();

            assertEquals(MONTHS.between(start.atStartOfDay(TZoneOffset.ofHours(2)),
                    end.atStartOfDay(TZoneOffset.ofHours(2))), expected);
        }
    }

    @Test
    public void test_monthsBetween_ZonedDateLaterOffset() {

        for (Object[] data : data_monthsBetween()) {
            TLocalDate start = (TLocalDate) data[0];
            TLocalDate end = (TLocalDate) data[1];
            long expected = ((Number) data[2]).longValue();

            // +01:00 is later than +02:00
            if (end.isAfter(start)) {
                assertEquals(MONTHS.between(start.atStartOfDay(TZoneOffset.ofHours(2)),
                        end.atStartOfDay(TZoneOffset.ofHours(1))), expected);
            } else {
                assertEquals(MONTHS.between(start.atStartOfDay(TZoneOffset.ofHours(1)),
                        end.atStartOfDay(TZoneOffset.ofHours(2))), expected);
            }
        }
    }

    Object[][] data_weeksBetween() {

        return new Object[][] { { date(2012, JULY, 2), date(2012, JUNE, 25), -1 },
        { date(2012, JULY, 2), date(2012, JUNE, 26), 0 }, { date(2012, JULY, 2), date(2012, JULY, 2), 0 },
        { date(2012, JULY, 2), date(2012, JULY, 8), 0 }, { date(2012, JULY, 2), date(2012, JULY, 9), 1 },

        { date(2012, FEBRUARY, 28), date(2012, FEBRUARY, 21), -1 },
        { date(2012, FEBRUARY, 28), date(2012, FEBRUARY, 22), 0 },
        { date(2012, FEBRUARY, 28), date(2012, FEBRUARY, 28), 0 },
        { date(2012, FEBRUARY, 28), date(2012, FEBRUARY, 29), 0 },
        { date(2012, FEBRUARY, 28), date(2012, MARCH, 1), 0 }, { date(2012, FEBRUARY, 28), date(2012, MARCH, 5), 0 },
        { date(2012, FEBRUARY, 28), date(2012, MARCH, 6), 1 },

        { date(2012, FEBRUARY, 29), date(2012, FEBRUARY, 22), -1 },
        { date(2012, FEBRUARY, 29), date(2012, FEBRUARY, 23), 0 },
        { date(2012, FEBRUARY, 29), date(2012, FEBRUARY, 28), 0 },
        { date(2012, FEBRUARY, 29), date(2012, FEBRUARY, 29), 0 },
        { date(2012, FEBRUARY, 29), date(2012, MARCH, 1), 0 }, { date(2012, FEBRUARY, 29), date(2012, MARCH, 6), 0 },
        { date(2012, FEBRUARY, 29), date(2012, MARCH, 7), 1 }, };
    }

    @Test
    public void test_weeksBetween() {

        for (Object[] data : data_weeksBetween()) {
            TLocalDate start = (TLocalDate) data[0];
            TLocalDate end = (TLocalDate) data[1];
            long expected = ((Number) data[2]).longValue();

            assertEquals(WEEKS.between(start, end), expected);
        }
    }

    @Test
    public void test_weeksBetweenReversed() {

        for (Object[] data : data_weeksBetween()) {
            TLocalDate start = (TLocalDate) data[0];
            TLocalDate end = (TLocalDate) data[1];
            long expected = ((Number) data[2]).longValue();

            assertEquals(WEEKS.between(end, start), -expected);
        }
    }

    Object[][] data_daysBetween() {

        return new Object[][] { { date(2012, JULY, 2), date(2012, JULY, 1), -1 },
        { date(2012, JULY, 2), date(2012, JULY, 2), 0 }, { date(2012, JULY, 2), date(2012, JULY, 3), 1 },

        { date(2012, FEBRUARY, 28), date(2012, FEBRUARY, 27), -1 },
        { date(2012, FEBRUARY, 28), date(2012, FEBRUARY, 28), 0 },
        { date(2012, FEBRUARY, 28), date(2012, FEBRUARY, 29), 1 },
        { date(2012, FEBRUARY, 28), date(2012, MARCH, 1), 2 },

        { date(2012, FEBRUARY, 29), date(2012, FEBRUARY, 27), -2 },
        { date(2012, FEBRUARY, 29), date(2012, FEBRUARY, 28), -1 },
        { date(2012, FEBRUARY, 29), date(2012, FEBRUARY, 29), 0 },
        { date(2012, FEBRUARY, 29), date(2012, MARCH, 1), 1 },

        { date(2012, MARCH, 1), date(2012, FEBRUARY, 27), -3 }, { date(2012, MARCH, 1), date(2012, FEBRUARY, 28), -2 },
        { date(2012, MARCH, 1), date(2012, FEBRUARY, 29), -1 }, { date(2012, MARCH, 1), date(2012, MARCH, 1), 0 },
        { date(2012, MARCH, 1), date(2012, MARCH, 2), 1 },

        { date(2012, MARCH, 1), date(2013, FEBRUARY, 28), 364 }, { date(2012, MARCH, 1), date(2013, MARCH, 1), 365 },

        { date(2011, MARCH, 1), date(2012, FEBRUARY, 28), 364 },
        { date(2011, MARCH, 1), date(2012, FEBRUARY, 29), 365 }, { date(2011, MARCH, 1), date(2012, MARCH, 1), 366 }, };
    }

    @Test
    public void test_daysBetween() {

        for (Object[] data : data_daysBetween()) {
            TLocalDate start = (TLocalDate) data[0];
            TLocalDate end = (TLocalDate) data[1];
            long expected = ((Number) data[2]).longValue();

            assertEquals(DAYS.between(start, end), expected);
        }
    }

    @Test
    public void test_daysBetweenReversed() {

        for (Object[] data : data_daysBetween()) {
            TLocalDate start = (TLocalDate) data[0];
            TLocalDate end = (TLocalDate) data[1];
            long expected = ((Number) data[2]).longValue();

            assertEquals(DAYS.between(end, start), -expected);
        }
    }

    @Test
    public void test_daysBetween_LocalDateTimeSameTime() {

        for (Object[] data : data_daysBetween()) {
            TLocalDate start = (TLocalDate) data[0];
            TLocalDate end = (TLocalDate) data[1];
            long expected = ((Number) data[2]).longValue();

            assertEquals(DAYS.between(start.atTime(12, 30), end.atTime(12, 30)), expected);
        }
    }

    @Test
    public void test_daysBetween_LocalDateTimeLaterTime() {

        for (Object[] data : data_daysBetween()) {
            TLocalDate start = (TLocalDate) data[0];
            TLocalDate end = (TLocalDate) data[1];
            long expected = ((Number) data[2]).longValue();

            if (end.isAfter(start)) {
                assertEquals(DAYS.between(start.atTime(12, 30), end.atTime(12, 31)), expected);
            } else {
                assertEquals(DAYS.between(start.atTime(12, 31), end.atTime(12, 30)), expected);
            }
        }
    }

    @Test
    public void test_daysBetween_ZonedDateSameOffset() {

        for (Object[] data : data_daysBetween()) {
            TLocalDate start = (TLocalDate) data[0];
            TLocalDate end = (TLocalDate) data[1];
            long expected = ((Number) data[2]).longValue();

            assertEquals(
                    DAYS.between(start.atStartOfDay(TZoneOffset.ofHours(2)), end.atStartOfDay(TZoneOffset.ofHours(2))),
                    expected);
        }
    }

    @Test
    public void test_daysBetween_ZonedDateLaterOffset() {

        for (Object[] data : data_daysBetween()) {
            TLocalDate start = (TLocalDate) data[0];
            TLocalDate end = (TLocalDate) data[1];
            long expected = ((Number) data[2]).longValue();

            // +01:00 is later than +02:00
            if (end.isAfter(start)) {
                assertEquals(DAYS.between(start.atStartOfDay(TZoneOffset.ofHours(2)),
                        end.atStartOfDay(TZoneOffset.ofHours(1))), expected);
            } else {
                assertEquals(DAYS.between(start.atStartOfDay(TZoneOffset.ofHours(1)),
                        end.atStartOfDay(TZoneOffset.ofHours(2))), expected);
            }
        }
    }

    @Test
    public void test_isDateBased() {

        for (TChronoUnit unit : TChronoUnit.values()) {
            if (unit.getDuration().getSeconds() < 86400) {
                assertEquals(unit.isDateBased(), false);
            } else if (unit == FOREVER) {
                assertEquals(unit.isDateBased(), false);
            } else {
                assertEquals(unit.isDateBased(), true);
            }
        }
    }

    @Test
    public void test_isTimeBased() {

        for (TChronoUnit unit : TChronoUnit.values()) {
            if (unit.getDuration().getSeconds() < 86400) {
                assertEquals(unit.isTimeBased(), true);
            } else if (unit == FOREVER) {
                assertEquals(unit.isTimeBased(), false);
            } else {
                assertEquals(unit.isTimeBased(), false);
            }
        }
    }

    private static TLocalDate date(int year, TMonth month, int dom) {

        return TLocalDate.of(year, month, dom);
    }

}