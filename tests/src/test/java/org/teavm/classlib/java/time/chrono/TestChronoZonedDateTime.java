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
package org.teavm.classlib.java.time.chrono;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.chrono.ChronoZonedDateTime;
import java.time.chrono.Chronology;
import java.time.chrono.HijrahChronology;
import java.time.chrono.IsoChronology;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.MinguoChronology;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ValueRange;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test assertions that must be true for all built-in chronologies.
 */
@Test
@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class TestChronoZonedDateTime {
    //-----------------------------------------------------------------------
    // regular data factory for names and descriptions of available calendars
    //-----------------------------------------------------------------------
    @DataProvider(name = "calendars")
    Object[][] data_of_calendars() {
        return new Chronology[][]{
                    {HijrahChronology.INSTANCE},
                    {IsoChronology.INSTANCE},
                    {JapaneseChronology.INSTANCE},
                    {MinguoChronology.INSTANCE},
                    {ThaiBuddhistChronology.INSTANCE},
        };
    }

    @DataProvider(name = "calendars2")
    Object[][] data_of_calendars2() {
        return new Chronology[][]{
                {IsoChronology.INSTANCE},
                {JapaneseChronology.INSTANCE},
                {MinguoChronology.INSTANCE},
                {ThaiBuddhistChronology.INSTANCE},
        };
    }

    @Test(dataProvider = "calendars")
    public void test_badWithAdjusterChrono(Chronology chrono) {
        LocalDate refDate = LocalDate.of(1900, 1, 1);
        ChronoZonedDateTime<?> czdt = chrono.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
        for (Object[] clist : data_of_calendars()) {
            Chronology chrono2 = (Chronology) clist[0];
            ChronoZonedDateTime<?> czdt2 = chrono2.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
            TemporalAdjuster adjuster = new FixedAdjuster(czdt2);
            if (chrono != chrono2) {
                try {
                    czdt.with(adjuster);
                    Assert.fail("WithAdjuster should have thrown a ClassCastException, "
                            + "required: " + czdt + ", supplied: " + czdt2);
                } catch (ClassCastException cce) {
                    // Expected exception; not an error
                }
            } else {
                ChronoZonedDateTime<?> result = czdt.with(adjuster);
                assertEquals(result, czdt2, "WithAdjuster failed to replace date");
            }
        }
    }

    @Test(dataProvider = "calendars")
    public void test_badPlusAdjusterChrono(Chronology chrono) {
        LocalDate refDate = LocalDate.of(1900, 1, 1);
        ChronoZonedDateTime<?> czdt = chrono.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
        for (Object[] clist : data_of_calendars()) {
            Chronology chrono2 = (Chronology) clist[0];
            ChronoZonedDateTime<?> czdt2 = chrono2.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
            TemporalAmount adjuster = new FixedAdjuster(czdt2);
            if (chrono != chrono2) {
                try {
                    czdt.plus(adjuster);
                    Assert.fail("WithAdjuster should have thrown a ClassCastException, "
                            + "required: " + czdt + ", supplied: " + czdt2);
                } catch (ClassCastException cce) {
                    // Expected exception; not an error
                }
            } else {
                // Same chronology,
                ChronoZonedDateTime<?> result = czdt.plus(adjuster);
                assertEquals(result, czdt2, "WithAdjuster failed to replace date time");
            }
        }
    }

    @Test(dataProvider = "calendars")
    public void test_badMinusAdjusterChrono(Chronology chrono) {
        LocalDate refDate = LocalDate.of(1900, 1, 1);
        ChronoZonedDateTime<?> czdt = chrono.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
        for (Object[] clist : data_of_calendars()) {
            Chronology chrono2 = (Chronology) clist[0];
            ChronoZonedDateTime<?> czdt2 = chrono2.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
            TemporalAmount adjuster = new FixedAdjuster(czdt2);
            if (chrono != chrono2) {
                try {
                    czdt.minus(adjuster);
                    Assert.fail("WithAdjuster should have thrown a ClassCastException, "
                            + "required: " + czdt + ", supplied: " + czdt2);
                } catch (ClassCastException cce) {
                    // Expected exception; not an error
                }
            } else {
                // Same chronology,
                ChronoZonedDateTime<?> result = czdt.minus(adjuster);
                assertEquals(result, czdt2, "WithAdjuster failed to replace date");
            }
        }
    }

    @Test(dataProvider = "calendars")
    public void test_badPlusPeriodUnitChrono(Chronology chrono) {
        LocalDate refDate = LocalDate.of(1900, 1, 1);
        ChronoZonedDateTime<?> czdt = chrono.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
        for (Object[] clist : data_of_calendars()) {
            Chronology chrono2 = (Chronology) clist[0];
            ChronoZonedDateTime<?> czdt2 = chrono2.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
            TemporalUnit adjuster = new FixedPeriodUnit(czdt2);
            if (chrono != chrono2) {
                try {
                    czdt.plus(1, adjuster);
                    Assert.fail("PeriodUnit.doPlus plus should have thrown a ClassCastException, " + czdt
                            + " can not be cast to " + czdt2);
                } catch (ClassCastException cce) {
                    // Expected exception; not an error
                }
            } else {
                // Same chronology,
                ChronoZonedDateTime<?> result = czdt.plus(1, adjuster);
                assertEquals(result, czdt2, "WithAdjuster failed to replace date");
            }
        }
    }

    @Test(dataProvider = "calendars")
    public void test_badMinusPeriodUnitChrono(Chronology chrono) {
        LocalDate refDate = LocalDate.of(1900, 1, 1);
        ChronoZonedDateTime<?> czdt = chrono.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
        for (Object[] clist : data_of_calendars()) {
            Chronology chrono2 = (Chronology) clist[0];
            ChronoZonedDateTime<?> czdt2 = chrono2.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
            TemporalUnit adjuster = new FixedPeriodUnit(czdt2);
            if (chrono != chrono2) {
                try {
                    czdt.minus(1, adjuster);
                    Assert.fail("PeriodUnit.doPlus minus should have thrown a ClassCastException, " + czdt.getClass()
                            + " can not be cast to " + czdt2.getClass());
                } catch (ClassCastException cce) {
                    // Expected exception; not an error
                }
            } else {
                // Same chronology,
                ChronoZonedDateTime<?> result = czdt.minus(1, adjuster);
                assertEquals(result, czdt2, "WithAdjuster failed to replace date");
            }
        }
    }

    @Test(dataProvider = "calendars")
    public void test_badDateTimeFieldChrono(Chronology chrono) {
        LocalDate refDate = LocalDate.of(1900, 1, 1);
        ChronoZonedDateTime<?> czdt = chrono.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
        for (Object[] clist : data_of_calendars()) {
            Chronology chrono2 = (Chronology) clist[0];
            ChronoZonedDateTime<?> czdt2 = chrono2.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
            TemporalField adjuster = new FixedDateTimeField(czdt2);
            if (chrono != chrono2) {
                try {
                    czdt.with(adjuster, 1);
                    Assert.fail("DateTimeField adjustInto() should have thrown a ClassCastException, " + czdt.getClass()
                            + " can not be cast to " + czdt2.getClass());
                } catch (ClassCastException cce) {
                    // Expected exception; not an error
                }
            } else {
                // Same chronology,
                ChronoZonedDateTime<?> result = czdt.with(adjuster, 1);
                assertEquals(result, czdt2, "DateTimeField adjustInto() failed to replace date");
            }
        }
    }

    //-----------------------------------------------------------------------
    // isBefore, isAfter, isEqual, INSTANT_COMPARATOR  test a Chrono against the other Chronos
    //-----------------------------------------------------------------------
    @SuppressWarnings("unused")
    @Test(dataProvider = "calendars2")
    // TODO: excluded HijrahChronology
    // it produces 'Invalid Hijrah date' error on JVM
    public void test_zonedDateTime_comparisons(Chronology chrono) {
        List<ChronoZonedDateTime<?>> dates = new ArrayList<>();

        ChronoZonedDateTime<?> date = chrono.date(LocalDate.of(1900, 1, 1))
                .atTime(LocalTime.MIN)
                .atZone(ZoneOffset.UTC);

        // Insert dates in order, no duplicates
        if (chrono != JapaneseChronology.INSTANCE) {
            dates.add(date.minus(100, ChronoUnit.YEARS));
        }
        dates.add(date.minus(1, ChronoUnit.YEARS));
        dates.add(date.minus(1, ChronoUnit.MONTHS));
        dates.add(date.minus(1, ChronoUnit.WEEKS));
        dates.add(date.minus(1, ChronoUnit.DAYS));
        dates.add(date.minus(1, ChronoUnit.HOURS));
        dates.add(date.minus(1, ChronoUnit.MINUTES));
        dates.add(date.minus(1, ChronoUnit.SECONDS));
        dates.add(date.minus(1, ChronoUnit.NANOS));
        dates.add(date);
        dates.add(date.plus(1, ChronoUnit.NANOS));
        dates.add(date.plus(1, ChronoUnit.SECONDS));
        dates.add(date.plus(1, ChronoUnit.MINUTES));
        dates.add(date.plus(1, ChronoUnit.HOURS));
        dates.add(date.plus(1, ChronoUnit.DAYS));
        dates.add(date.plus(1, ChronoUnit.WEEKS));
        dates.add(date.plus(1, ChronoUnit.MONTHS));
        dates.add(date.plus(1, ChronoUnit.YEARS));
        dates.add(date.plus(100, ChronoUnit.YEARS));

        // Check these dates against the corresponding dates for every calendar
        for (Object[] clist : data_of_calendars2()) {
            List<ChronoZonedDateTime<?>> otherDates = new ArrayList<>();
            Chronology chrono2 = IsoChronology.INSTANCE; //clist[0];
            for (ChronoZonedDateTime<?> d : dates) {
                otherDates.add(chrono2.date(d).atTime(d.toLocalTime()).atZone(d.getZone()));
            }

            // Now compare  the sequence of original dates with the sequence of converted dates
            for (int i = 0; i < dates.size(); i++) {
                ChronoZonedDateTime<?> a = dates.get(i);
                for (int j = 0; j < otherDates.size(); j++) {
                    ChronoZonedDateTime<?> b = otherDates.get(j);
                    int cmp = ChronoZonedDateTime.timeLineOrder().compare(a, b);
                    if (i < j) {
                        assertTrue(cmp < 0, a + " compare " + b);
                        assertEquals(a.isBefore(b), true, a + " isBefore " + b);
                        assertEquals(a.isAfter(b), false, a + " ifAfter " + b);
                        assertEquals(a.isEqual(b), false, a + " isEqual " + b);
                    } else if (i > j) {
                        assertTrue(cmp > 0, a + " compare " + b);
                        assertEquals(a.isBefore(b), false, a + " isBefore " + b);
                        assertEquals(a.isAfter(b), true, a + " ifAfter " + b);
                        assertEquals(a.isEqual(b), false, a + " isEqual " + b);
                    } else {
                        assertTrue(cmp == 0, a + " compare " + b);
                        assertEquals(a.isBefore(b), false, a + " isBefore " + b);
                        assertEquals(a.isAfter(b), false, a + " ifAfter " + b);
                        assertEquals(a.isEqual(b), true, a + " isEqual " + b);
                    }
                }
            }
        }
    }

    /**
     * FixedAdjusted returns a fixed DateTime in all adjustments.
     * Construct an adjuster with the DateTime that should be returned from adjustIntoAdjustment.
     */
    static class FixedAdjuster implements TemporalAdjuster, TemporalAmount {
        private Temporal datetime;

        FixedAdjuster(Temporal datetime) {
            this.datetime = datetime;
        }

        @Override
        public Temporal adjustInto(Temporal ignore) {
            return datetime;
        }

        @Override
        public Temporal addTo(Temporal ignore) {
            return datetime;
        }

        @Override
        public Temporal subtractFrom(Temporal ignore) {
            return datetime;
        }

        @Override
        public List<TemporalUnit> getUnits() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public long get(TemporalUnit unit) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    /**
     * FixedPeriodUnit returns a fixed DateTime in all adjustments.
     * Construct an FixedPeriodUnit with the DateTime that should be returned from doPlus.
     */
    static class FixedPeriodUnit implements TemporalUnit {
        private Temporal dateTime;

        FixedPeriodUnit(Temporal dateTime) {
            this.dateTime = dateTime;
        }

        @Override
        public String toString() {
            return "FixedPeriodUnit";
        }

        @Override
        public Duration getDuration() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isDurationEstimated() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isDateBased() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isTimeBased() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isSupportedBy(Temporal dateTime) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R extends Temporal> R addTo(R dateTime, long periodToAdd) {
            return (R) this.dateTime;
        }

        @Override
        public long between(Temporal temporal1, Temporal temporal2) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    /**
     * FixedDateTimeField returns a fixed DateTime in all adjustments.
     * Construct an FixedDateTimeField with the DateTime that should be returned from adjustInto.
     */
    static class FixedDateTimeField implements TemporalField {
        private Temporal dateTime;
        FixedDateTimeField(Temporal dateTime) {
            this.dateTime = dateTime;
        }

        @Override
        public String toString() {
            return "FixedDateTimeField";
        }

        @Override
        public TemporalUnit getBaseUnit() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public TemporalUnit getRangeUnit() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ValueRange range() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isDateBased() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isTimeBased() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isSupportedBy(TemporalAccessor dateTime) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ValueRange rangeRefinedBy(TemporalAccessor dateTime) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public long getFrom(TemporalAccessor dateTime) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R extends Temporal> R adjustInto(R dateTime, long newValue) {
            return (R) this.dateTime;
        }

        @Override
        public String getDisplayName(Locale locale) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public TemporalAccessor resolve(Map<TemporalField, Long> fieldValues,
                        TemporalAccessor partialTemporal, ResolverStyle resolverStyle) {
            return null;
        }
    }
}
