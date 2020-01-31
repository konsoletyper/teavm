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
package org.threeten.bp.chrono;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.threeten.bp.Duration;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.ResolverStyle;
import org.threeten.bp.temporal.ChronoUnit;
import org.threeten.bp.temporal.Temporal;
import org.threeten.bp.temporal.TemporalAccessor;
import org.threeten.bp.temporal.TemporalAdjuster;
import org.threeten.bp.temporal.TemporalAmount;
import org.threeten.bp.temporal.TemporalField;
import org.threeten.bp.temporal.TemporalUnit;
import org.threeten.bp.temporal.ValueRange;

/**
 * Test assertions that must be true for all built-in chronologies.
 */
@SuppressWarnings("rawtypes")
@Test
public class TestChronoZonedDateTime {
    //-----------------------------------------------------------------------
    // regular data factory for names and descriptions of available calendars
    //-----------------------------------------------------------------------
    @DataProvider(name = "calendars")
    Chronology[][] data_of_calendars() {
        return new Chronology[][]{
                    {HijrahChronology.INSTANCE},
                    {IsoChronology.INSTANCE},
                    {JapaneseChronology.INSTANCE},
                    {MinguoChronology.INSTANCE},
                    {ThaiBuddhistChronology.INSTANCE},
        };
    }

    @Test(dataProvider="calendars")
    public void test_badWithAdjusterChrono(Chronology chrono) {
        LocalDate refDate = LocalDate.of(1900, 1, 1);
        ChronoZonedDateTime czdt = chrono.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
        for (Chronology[] clist : data_of_calendars()) {
            Chronology chrono2 = clist[0];
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

    @Test(dataProvider="calendars")
    public void test_badPlusAdjusterChrono(Chronology chrono) {
        LocalDate refDate = LocalDate.of(1900, 1, 1);
        ChronoZonedDateTime czdt = chrono.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
        for (Chronology[] clist : data_of_calendars()) {
            Chronology chrono2 = clist[0];
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

    @Test(dataProvider="calendars")
    public void test_badMinusAdjusterChrono(Chronology chrono) {
        LocalDate refDate = LocalDate.of(1900, 1, 1);
        ChronoZonedDateTime czdt = chrono.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
        for (Chronology[] clist : data_of_calendars()) {
            Chronology chrono2 = clist[0];
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

    @Test(dataProvider="calendars")
    public void test_badPlusPeriodUnitChrono(Chronology chrono) {
        LocalDate refDate = LocalDate.of(1900, 1, 1);
        ChronoZonedDateTime czdt = chrono.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
        for (Chronology[] clist : data_of_calendars()) {
            Chronology chrono2 = clist[0];
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

    @Test(dataProvider="calendars")
    public void test_badMinusPeriodUnitChrono(Chronology chrono) {
        LocalDate refDate = LocalDate.of(1900, 1, 1);
        ChronoZonedDateTime czdt = chrono.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
        for (Chronology[] clist : data_of_calendars()) {
            Chronology chrono2 = clist[0];
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

    @Test(dataProvider="calendars")
    public void test_badDateTimeFieldChrono(Chronology chrono) {
        LocalDate refDate = LocalDate.of(1900, 1, 1);
        ChronoZonedDateTime czdt = chrono.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
        for (Chronology[] clist : data_of_calendars()) {
            Chronology chrono2 = clist[0];
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
    @Test(dataProvider="calendars")
    public void test_zonedDateTime_comparisons(Chronology chrono) {
        List<ChronoZonedDateTime<?>> dates = new ArrayList<ChronoZonedDateTime<?>>();

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
        for (Chronology[] clist : data_of_calendars()) {
            List<ChronoZonedDateTime<?>> otherDates = new ArrayList<ChronoZonedDateTime<?>>();
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

    //-----------------------------------------------------------------------
    // Test Serialization of ISO via chrono API
    //-----------------------------------------------------------------------
    @Test( dataProvider="calendars")
    public void test_ChronoZonedDateTimeSerialization(Chronology chrono) throws Exception {
        ZonedDateTime ref = LocalDate.of(2000, 1, 5).atTime(12, 1, 2, 3).atZone(ZoneId.of("GMT+01:23"));
        ChronoZonedDateTime<?> orginal = chrono.date(ref).atTime(ref.toLocalTime()).atZone(ref.getZone());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(orginal);
        out.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bais);
        ChronoZonedDateTime<?> ser = (ChronoZonedDateTime<?>) in.readObject();
        assertEquals(ser, orginal, "deserialized date is wrong");
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
