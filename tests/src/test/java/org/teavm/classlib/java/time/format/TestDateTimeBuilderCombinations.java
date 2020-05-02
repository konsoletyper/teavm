/*
 *  Copyright 2020 adopted to TeaVM by Joerg Hohwiller
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

import static org.junit.Assert.assertEquals;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_WEEK_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_WEEK_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.EPOCH_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.PROLEPTIC_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZonedDateTime;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class TestDateTimeBuilderCombinations {

    private static final TZoneId PARIS = TZoneId.of("Europe/Paris");

    Object[][] data_combine() {

        return new Object[][] {
        { YEAR, 2012, MONTH_OF_YEAR, 6, DAY_OF_MONTH, 3, null, null, TLocalDate.FROM, TLocalDate.of(2012, 6, 3) },
        { PROLEPTIC_MONTH, 2012 * 12 + 6 - 1, DAY_OF_MONTH, 3, null, null, null, null, TLocalDate.FROM,
        TLocalDate.of(2012, 6, 3) },
        { YEAR, 2012, ALIGNED_WEEK_OF_YEAR, 6, DAY_OF_WEEK, 3, null, null, TLocalDate.FROM, TLocalDate.of(2012, 2, 8) },
        { YEAR, 2012, DAY_OF_YEAR, 155, null, null, null, null, TLocalDate.FROM, TLocalDate.of(2012, 6, 3) },
        // {ERA, 1, YEAR_OF_ERA, 2012, DAY_OF_YEAR, 155, null, null, TLocalDate.FROM, TLocalDate.of(2012, 6, 3)},
        // {YEAR, 2012, MONTH_OF_YEAR, 6, null, null, null, null, TLocalDate.FROM, null},
        { EPOCH_DAY, 12, null, null, null, null, null, null, TLocalDate.FROM, TLocalDate.of(1970, 1, 13) }, };
    }

    @Test
    public void test_derive() {

        for (Object[] data : data_combine()) {
            TTemporalField field1 = (TTemporalField) data[0];
            Number value1 = (Number) data[1];
            TTemporalField field2 = (TTemporalField) data[2];
            Number value2 = (Number) data[3];
            TTemporalField field3 = (TTemporalField) data[4];
            Number value3 = (Number) data[5];
            TTemporalField field4 = (TTemporalField) data[6];
            Number value4 = (Number) data[7];
            TTemporalQuery<?> query = (TTemporalQuery<?>) data[8];
            Object expectedVal = data[9];

            TDateTimeBuilder builder = new TDateTimeBuilder(field1, value1.longValue());
            builder.chrono = TIsoChronology.INSTANCE;
            if (field2 != null) {
                builder.addFieldValue(field2, value2.longValue());
            }
            if (field3 != null) {
                builder.addFieldValue(field3, value3.longValue());
            }
            if (field4 != null) {
                builder.addFieldValue(field4, value4.longValue());
            }
            builder.resolve(TResolverStyle.SMART, null);
            assertEquals(builder.build(query), expectedVal);
        }
    }

    Object[][] data_normalized() {

        return new Object[][] { { YEAR, 2127, null, null, null, null, YEAR, 2127 },
        { MONTH_OF_YEAR, 12, null, null, null, null, MONTH_OF_YEAR, 12 },
        { DAY_OF_YEAR, 127, null, null, null, null, DAY_OF_YEAR, 127 },
        { DAY_OF_MONTH, 23, null, null, null, null, DAY_OF_MONTH, 23 },
        { DAY_OF_WEEK, 127, null, null, null, null, DAY_OF_WEEK, 127L },
        { ALIGNED_WEEK_OF_YEAR, 23, null, null, null, null, ALIGNED_WEEK_OF_YEAR, 23 },
        { ALIGNED_DAY_OF_WEEK_IN_YEAR, 4, null, null, null, null, ALIGNED_DAY_OF_WEEK_IN_YEAR, 4L },
        { ALIGNED_WEEK_OF_MONTH, 4, null, null, null, null, ALIGNED_WEEK_OF_MONTH, 4 },
        { ALIGNED_DAY_OF_WEEK_IN_MONTH, 3, null, null, null, null, ALIGNED_DAY_OF_WEEK_IN_MONTH, 3 },
        { PROLEPTIC_MONTH, 15, null, null, null, null, PROLEPTIC_MONTH, null },
        { PROLEPTIC_MONTH, 1971 * 12 + 4 - 1, null, null, null, null, YEAR, 1971 },
        { PROLEPTIC_MONTH, 1971 * 12 + 4 - 1, null, null, null, null, MONTH_OF_YEAR, 4 }, };
    }

    @Test
    public void test_normalized() {

        for (Object[] data : data_combine()) {
            TTemporalField field1 = (TTemporalField) data[0];
            Number value1 = (Number) data[1];
            TTemporalField field2 = (TTemporalField) data[2];
            Number value2 = (Number) data[3];
            TTemporalField field3 = (TTemporalField) data[4];
            Number value3 = (Number) data[5];
            TTemporalField query = (TTemporalField) data[6];
            Number expectedVal = (Number) data[7];

            TDateTimeBuilder builder = new TDateTimeBuilder(field1, value1.longValue());
            builder.chrono = TIsoChronology.INSTANCE;
            if (field2 != null) {
                builder.addFieldValue(field2, value2.longValue());
            }
            if (field3 != null) {
                builder.addFieldValue(field3, value3.longValue());
            }
            builder.resolve(TResolverStyle.SMART, null);
            if (expectedVal != null) {
                assertEquals(builder.getLong(query), expectedVal.longValue());
            } else {
                assertEquals(builder.isSupported(query), false);
            }
        }
    }

    @Test
    public void test_parse_ZDT_withZone() {

        TDateTimeFormatter fmt = TDateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(PARIS);
        TTemporalAccessor acc = fmt.parse("2014-06-30 01:02:03");
        assertEquals(TZonedDateTime.from(acc), TZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, PARIS));
    }

    @Test
    public void test_parse_Instant_withZone() {

        TDateTimeFormatter fmt = TDateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(PARIS);
        TTemporalAccessor acc = fmt.parse("2014-06-30 01:02:03");
        assertEquals(TInstant.from(acc), TZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, PARIS).toInstant());
    }

}
