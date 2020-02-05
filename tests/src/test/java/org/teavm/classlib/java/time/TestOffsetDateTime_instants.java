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

import org.junit.Test;

public class TestOffsetDateTime_instants {

    private static final TZoneOffset OFFSET_PONE = TZoneOffset.ofHours(1);

    private static final TZoneOffset OFFSET_MAX = TZoneOffset.ofHours(18);

    private static final TZoneOffset OFFSET_MIN = TZoneOffset.ofHours(-18);

    @Test(expected = NullPointerException.class)
    public void factory_ofInstant_nullInstant() {

        TOffsetDateTime.ofInstant((TInstant) null, OFFSET_PONE);
    }

    @Test(expected = NullPointerException.class)
    public void factory_ofInstant_nullOffset() {

        TInstant instant = TInstant.ofEpochSecond(0L);
        TOffsetDateTime.ofInstant(instant, (TZoneOffset) null);
    }

    @Test
    public void factory_ofInstant_allSecsInDay() {

        for (int i = 0; i < (24 * 60 * 60); i++) {
            TInstant instant = TInstant.ofEpochSecond(i);
            TOffsetDateTime test = TOffsetDateTime.ofInstant(instant, OFFSET_PONE);
            assertEquals(test.getYear(), 1970);
            assertEquals(test.getMonth(), TMonth.JANUARY);
            assertEquals(test.getDayOfMonth(), 1 + (i >= 23 * 60 * 60 ? 1 : 0));
            assertEquals(test.getHour(), ((i / (60 * 60)) + 1) % 24);
            assertEquals(test.getMinute(), (i / 60) % 60);
            assertEquals(test.getSecond(), i % 60);
        }
    }

    @Test
    public void factory_ofInstant_allDaysInCycle() {

        // sanity check using different algorithm
        TOffsetDateTime expected = TOffsetDateTime.of(TLocalDate.of(1970, 1, 1), TLocalTime.of(0, 0, 0, 0),
                TZoneOffset.UTC);
        for (long i = 0; i < 146097; i++) {
            TInstant instant = TInstant.ofEpochSecond(i * 24L * 60L * 60L);
            TOffsetDateTime test = TOffsetDateTime.ofInstant(instant, TZoneOffset.UTC);
            assertEquals(test, expected);
            expected = expected.plusDays(1);
        }
    }

    @Test
    public void factory_ofInstant_history() {

        doTest_factory_ofInstant_all(-2820, 2820);
    }

    @Test
    public void factory_ofInstant_minYear() {

        doTest_factory_ofInstant_all(TYear.MIN_VALUE, TYear.MIN_VALUE + 420);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_ofInstant_tooLow() {

        long days_0000_to_1970 = (146097 * 5) - (30 * 365 + 7);
        int year = TYear.MIN_VALUE - 1;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days_0000_to_1970;
        TInstant instant = TInstant.ofEpochSecond(days * 24L * 60L * 60L);
        TOffsetDateTime.ofInstant(instant, TZoneOffset.UTC);
    }

    @Test
    public void factory_ofInstant_maxYear() {

        doTest_factory_ofInstant_all(TYear.MAX_VALUE - 420, TYear.MAX_VALUE);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_ofInstant_tooBig() {

        long days_0000_to_1970 = (146097 * 5) - (30 * 365 + 7);
        long year = TYear.MAX_VALUE + 1L;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days_0000_to_1970;
        TInstant instant = TInstant.ofEpochSecond(days * 24L * 60L * 60L);
        TOffsetDateTime.ofInstant(instant, TZoneOffset.UTC);
    }

    @Test
    public void factory_ofInstant_minWithMinOffset() {

        long days_0000_to_1970 = (146097 * 5) - (30 * 365 + 7);
        int year = TYear.MIN_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days_0000_to_1970;
        TInstant instant = TInstant.ofEpochSecond(days * 24L * 60L * 60L - OFFSET_MIN.getTotalSeconds());
        TOffsetDateTime test = TOffsetDateTime.ofInstant(instant, OFFSET_MIN);
        assertEquals(test.getYear(), TYear.MIN_VALUE);
        assertEquals(test.getMonth().getValue(), 1);
        assertEquals(test.getDayOfMonth(), 1);
        assertEquals(test.getOffset(), OFFSET_MIN);
        assertEquals(test.getHour(), 0);
        assertEquals(test.getMinute(), 0);
        assertEquals(test.getSecond(), 0);
        assertEquals(test.getNano(), 0);
    }

    @Test
    public void factory_ofInstant_minWithMaxOffset() {

        long days_0000_to_1970 = (146097 * 5) - (30 * 365 + 7);
        int year = TYear.MIN_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days_0000_to_1970;
        TInstant instant = TInstant.ofEpochSecond(days * 24L * 60L * 60L - OFFSET_MAX.getTotalSeconds());
        TOffsetDateTime test = TOffsetDateTime.ofInstant(instant, OFFSET_MAX);
        assertEquals(test.getYear(), TYear.MIN_VALUE);
        assertEquals(test.getMonth().getValue(), 1);
        assertEquals(test.getDayOfMonth(), 1);
        assertEquals(test.getOffset(), OFFSET_MAX);
        assertEquals(test.getHour(), 0);
        assertEquals(test.getMinute(), 0);
        assertEquals(test.getSecond(), 0);
        assertEquals(test.getNano(), 0);
    }

    @Test
    public void factory_ofInstant_maxWithMinOffset() {

        long days_0000_to_1970 = (146097 * 5) - (30 * 365 + 7);
        int year = TYear.MAX_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) + 365 - days_0000_to_1970;
        TInstant instant = TInstant.ofEpochSecond((days + 1) * 24L * 60L * 60L - 1 - OFFSET_MIN.getTotalSeconds());
        TOffsetDateTime test = TOffsetDateTime.ofInstant(instant, OFFSET_MIN);
        assertEquals(test.getYear(), TYear.MAX_VALUE);
        assertEquals(test.getMonth().getValue(), 12);
        assertEquals(test.getDayOfMonth(), 31);
        assertEquals(test.getOffset(), OFFSET_MIN);
        assertEquals(test.getHour(), 23);
        assertEquals(test.getMinute(), 59);
        assertEquals(test.getSecond(), 59);
        assertEquals(test.getNano(), 0);
    }

    @Test
    public void factory_ofInstant_maxWithMaxOffset() {

        long days_0000_to_1970 = (146097 * 5) - (30 * 365 + 7);
        int year = TYear.MAX_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) + 365 - days_0000_to_1970;
        TInstant instant = TInstant.ofEpochSecond((days + 1) * 24L * 60L * 60L - 1 - OFFSET_MAX.getTotalSeconds());
        TOffsetDateTime test = TOffsetDateTime.ofInstant(instant, OFFSET_MAX);
        assertEquals(test.getYear(), TYear.MAX_VALUE);
        assertEquals(test.getMonth().getValue(), 12);
        assertEquals(test.getDayOfMonth(), 31);
        assertEquals(test.getOffset(), OFFSET_MAX);
        assertEquals(test.getHour(), 23);
        assertEquals(test.getMinute(), 59);
        assertEquals(test.getSecond(), 59);
        assertEquals(test.getNano(), 0);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_ofInstant_maxInstantWithMaxOffset() {

        TInstant instant = TInstant.ofEpochSecond(Long.MAX_VALUE);
        TOffsetDateTime.ofInstant(instant, OFFSET_MAX);
    }

    @Test(expected = TDateTimeException.class)
    public void factory_ofInstant_maxInstantWithMinOffset() {

        TInstant instant = TInstant.ofEpochSecond(Long.MAX_VALUE);
        TOffsetDateTime.ofInstant(instant, OFFSET_MIN);
    }

    private void doTest_factory_ofInstant_all(long minYear, long maxYear) {

        long days_0000_to_1970 = (146097 * 5) - (30 * 365 + 7);
        int minOffset = (minYear <= 0 ? 0 : 3);
        int maxOffset = (maxYear <= 0 ? 0 : 3);
        long minDays = (minYear * 365L
                + ((minYear + minOffset) / 4L - (minYear + minOffset) / 100L + (minYear + minOffset) / 400L))
                - days_0000_to_1970;
        long maxDays = (maxYear * 365L
                + ((maxYear + maxOffset) / 4L - (maxYear + maxOffset) / 100L + (maxYear + maxOffset) / 400L)) + 365L
                - days_0000_to_1970;

        final TLocalDate maxDate = TLocalDate.of(TYear.MAX_VALUE, 12, 31);
        TOffsetDateTime expected = TOffsetDateTime.of(TLocalDate.of((int) minYear, 1, 1), TLocalTime.of(0, 0, 0, 0),
                TZoneOffset.UTC);
        for (long i = minDays; i < maxDays; i++) {
            TInstant instant = TInstant.ofEpochSecond(i * 24L * 60L * 60L);
            try {
                TOffsetDateTime test = TOffsetDateTime.ofInstant(instant, TZoneOffset.UTC);
                assertEquals(test, expected);
                if (expected.toLocalDate().equals(maxDate) == false) {
                    expected = expected.plusDays(1);
                }
            } catch (RuntimeException ex) {
                System.out.println("RuntimeException: " + i + " " + expected);
                throw ex;
            } catch (Error ex) {
                System.out.println("Error: " + i + " " + expected);
                throw ex;
            }
        }
    }

    @Test
    public void test_toInstant_19700101() {

        TOffsetDateTime dt = TOffsetDateTime.of(TLocalDate.of(1970, 1, 1), TLocalTime.of(0, 0, 0, 0), TZoneOffset.UTC);
        TInstant test = dt.toInstant();
        assertEquals(test.getEpochSecond(), 0);
        assertEquals(test.getNano(), 0);
    }

    @Test
    public void test_toInstant_19700101_oneNano() {

        TOffsetDateTime dt = TOffsetDateTime.of(TLocalDate.of(1970, 1, 1), TLocalTime.of(0, 0, 0, 1), TZoneOffset.UTC);
        TInstant test = dt.toInstant();
        assertEquals(test.getEpochSecond(), 0);
        assertEquals(test.getNano(), 1);
    }

    @Test
    public void test_toInstant_19700101_minusOneNano() {

        TOffsetDateTime dt = TOffsetDateTime.of(TLocalDate.of(1969, 12, 31), TLocalTime.of(23, 59, 59, 999999999),
                TZoneOffset.UTC);
        TInstant test = dt.toInstant();
        assertEquals(test.getEpochSecond(), -1);
        assertEquals(test.getNano(), 999999999);
    }

    @Test
    public void test_toInstant_19700102() {

        TOffsetDateTime dt = TOffsetDateTime.of(TLocalDate.of(1970, 1, 2), TLocalTime.of(0, 0, 0, 0), TZoneOffset.UTC);
        TInstant test = dt.toInstant();
        assertEquals(test.getEpochSecond(), 24L * 60L * 60L);
        assertEquals(test.getNano(), 0);
    }

    @Test
    public void test_toInstant_19691231() {

        TOffsetDateTime dt = TOffsetDateTime.of(TLocalDate.of(1969, 12, 31), TLocalTime.of(0, 0, 0, 0),
                TZoneOffset.UTC);
        TInstant test = dt.toInstant();
        assertEquals(test.getEpochSecond(), -24L * 60L * 60L);
        assertEquals(test.getNano(), 0);
    }

    @Test
    public void test_toEpochSecond_19700101() {

        TOffsetDateTime dt = TOffsetDateTime.of(TLocalDate.of(1970, 1, 1), TLocalTime.of(0, 0, 0, 0), TZoneOffset.UTC);
        assertEquals(dt.toEpochSecond(), 0);
    }

    @Test
    public void test_toEpochSecond_19700101_oneNano() {

        TOffsetDateTime dt = TOffsetDateTime.of(TLocalDate.of(1970, 1, 1), TLocalTime.of(0, 0, 0, 1), TZoneOffset.UTC);
        assertEquals(dt.toEpochSecond(), 0);
    }

    @Test
    public void test_toEpochSecond_19700101_minusOneNano() {

        TOffsetDateTime dt = TOffsetDateTime.of(TLocalDate.of(1969, 12, 31), TLocalTime.of(23, 59, 59, 999999999),
                TZoneOffset.UTC);
        assertEquals(dt.toEpochSecond(), -1);
    }

    @Test
    public void test_toEpochSecond_19700102() {

        TOffsetDateTime dt = TOffsetDateTime.of(TLocalDate.of(1970, 1, 2), TLocalTime.of(0, 0, 0, 0), TZoneOffset.UTC);
        assertEquals(dt.toEpochSecond(), 24L * 60L * 60L);
    }

    @Test
    public void test_toEpochSecond_19691231() {

        TOffsetDateTime dt = TOffsetDateTime.of(TLocalDate.of(1969, 12, 31), TLocalTime.of(0, 0, 0, 0),
                TZoneOffset.UTC);
        assertEquals(dt.toEpochSecond(), -24L * 60L * 60L);
    }

}
