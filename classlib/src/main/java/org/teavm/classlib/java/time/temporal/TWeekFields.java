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

import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MONTHS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.WEEKS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.YEARS;

import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TDayOfWeek;
import org.teavm.classlib.java.time.TYear;
import org.teavm.classlib.java.time.chrono.TChronoLocalDate;
import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.format.TResolverStyle;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;
import org.teavm.classlib.java.util.TLocale;

public final class TWeekFields implements TSerializable {
    // implementation notes
    // querying week-of-month or week-of-year should return the week value bound within the month/year
    // however, setting the week value should be lenient (use plus/minus weeks)
    // allow week-of-month outer range [0 to 5]
    // allow week-of-year outer range [0 to 53]
    // this is because callers shouldn't be expected to know the details of validity

    private static final ConcurrentMap<String, TWeekFields> CACHE = new ConcurrentHashMap<String, TWeekFields>(4, 0.75f,
            2);

    public static final TWeekFields ISO = new TWeekFields(TDayOfWeek.MONDAY, 4);

    public static final TWeekFields SUNDAY_START = TWeekFields.of(TDayOfWeek.SUNDAY, 1);

    private final TDayOfWeek firstDayOfWeek;

    private final int minimalDays;

    private transient final TTemporalField dayOfWeek = ComputedDayOfField.ofDayOfWeekField(this);

    private transient final TTemporalField weekOfMonth = ComputedDayOfField.ofWeekOfMonthField(this);

    private transient final TTemporalField weekOfYear = ComputedDayOfField.ofWeekOfYearField(this);

    private transient final TTemporalField weekOfWeekBasedYear = ComputedDayOfField.ofWeekOfWeekBasedYearField(this);

    private transient final TTemporalField weekBasedYear = ComputedDayOfField.ofWeekBasedYearField(this);

    public static TWeekFields of(Locale locale) {

        TJdk8Methods.requireNonNull(locale, "locale");
        locale = new Locale(locale.getLanguage(), locale.getCountry()); // elminate variants

        // obtain these from TGregorianCalendar for now
        GregorianCalendar gcal = new GregorianCalendar(locale);
        int calDow = gcal.getFirstDayOfWeek();
        TDayOfWeek dow = TDayOfWeek.SUNDAY.plus(calDow - 1);
        int minDays = gcal.getMinimalDaysInFirstWeek();
        return TWeekFields.of(dow, minDays);
    }

    public static TWeekFields of(TDayOfWeek firstDayOfWeek, int minimalDaysInFirstWeek) {

        String key = firstDayOfWeek.toString() + minimalDaysInFirstWeek;
        TWeekFields rules = CACHE.get(key);
        if (rules == null) {
            rules = new TWeekFields(firstDayOfWeek, minimalDaysInFirstWeek);
            CACHE.putIfAbsent(key, rules);
            rules = CACHE.get(key);
        }
        return rules;
    }

    private TWeekFields(TDayOfWeek firstDayOfWeek, int minimalDaysInFirstWeek) {

        TJdk8Methods.requireNonNull(firstDayOfWeek, "firstDayOfWeek");
        if (minimalDaysInFirstWeek < 1 || minimalDaysInFirstWeek > 7) {
            throw new IllegalArgumentException("Minimal number of days is invalid");
        }
        this.firstDayOfWeek = firstDayOfWeek;
        this.minimalDays = minimalDaysInFirstWeek;
    }

    public TDayOfWeek getFirstDayOfWeek() {

        return this.firstDayOfWeek;
    }

    public int getMinimalDaysInFirstWeek() {

        return this.minimalDays;
    }

    public TTemporalField dayOfWeek() {

        return this.dayOfWeek;
    }

    public TTemporalField weekOfMonth() {

        return this.weekOfMonth;
    }

    public TTemporalField weekOfYear() {

        return this.weekOfYear;
    }

    public TTemporalField weekOfWeekBasedYear() {

        return this.weekOfWeekBasedYear;
    }

    public TTemporalField weekBasedYear() {

        return this.weekBasedYear;
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }
        if (object instanceof TWeekFields) {
            return hashCode() == object.hashCode();
        }
        return false;
    }

    @Override
    public int hashCode() {

        return this.firstDayOfWeek.ordinal() * 7 + this.minimalDays;
    }

    @Override
    public String toString() {

        return "TWeekFields[" + this.firstDayOfWeek + ',' + this.minimalDays + ']';
    }

    static class ComputedDayOfField implements TTemporalField {

        static ComputedDayOfField ofDayOfWeekField(TWeekFields weekDef) {

            return new ComputedDayOfField("TDayOfWeek", weekDef, TChronoUnit.DAYS, TChronoUnit.WEEKS,
                    DAY_OF_WEEK_RANGE);
        }

        static ComputedDayOfField ofWeekOfMonthField(TWeekFields weekDef) {

            return new ComputedDayOfField("WeekOfMonth", weekDef, TChronoUnit.WEEKS, TChronoUnit.MONTHS,
                    WEEK_OF_MONTH_RANGE);
        }

        static ComputedDayOfField ofWeekOfYearField(TWeekFields weekDef) {

            return new ComputedDayOfField("WeekOfYear", weekDef, TChronoUnit.WEEKS, TChronoUnit.YEARS,
                    WEEK_OF_YEAR_RANGE);
        }

        static ComputedDayOfField ofWeekOfWeekBasedYearField(TWeekFields weekDef) {

            return new ComputedDayOfField("WeekOfWeekBasedYear", weekDef, TChronoUnit.WEEKS,
                    TIsoFields.WEEK_BASED_YEARS, WEEK_OF_WEEK_BASED_YEAR_RANGE);
        }

        static ComputedDayOfField ofWeekBasedYearField(TWeekFields weekDef) {

            return new ComputedDayOfField("WeekBasedYear", weekDef, TIsoFields.WEEK_BASED_YEARS, TChronoUnit.FOREVER,
                    WEEK_BASED_YEAR_RANGE);
        }

        private final String name;

        private final TWeekFields weekDef;

        private final TTemporalUnit baseUnit;

        private final TTemporalUnit rangeUnit;

        private final TValueRange range;

        private ComputedDayOfField(String name, TWeekFields weekDef, TTemporalUnit baseUnit, TTemporalUnit rangeUnit,
                TValueRange range) {

            this.name = name;
            this.weekDef = weekDef;
            this.baseUnit = baseUnit;
            this.rangeUnit = rangeUnit;
            this.range = range;
        }

        private static final TValueRange DAY_OF_WEEK_RANGE = TValueRange.of(1, 7);

        private static final TValueRange WEEK_OF_MONTH_RANGE = TValueRange.of(0, 1, 4, 6);

        private static final TValueRange WEEK_OF_YEAR_RANGE = TValueRange.of(0, 1, 52, 54);

        private static final TValueRange WEEK_OF_WEEK_BASED_YEAR_RANGE = TValueRange.of(1, 52, 53);

        private static final TValueRange WEEK_BASED_YEAR_RANGE = YEAR.range();

        @Override
        public long getFrom(TTemporalAccessor temporal) {

            // Offset the ISO DOW by the start of this week
            int sow = this.weekDef.getFirstDayOfWeek().getValue();
            int isoDow = temporal.get(TChronoField.DAY_OF_WEEK);
            int dow = TJdk8Methods.floorMod(isoDow - sow, 7) + 1;

            if (this.rangeUnit == TChronoUnit.WEEKS) {
                return dow;
            } else if (this.rangeUnit == TChronoUnit.MONTHS) {
                int dom = temporal.get(TChronoField.DAY_OF_MONTH);
                int offset = startOfWeekOffset(dom, dow);
                return computeWeek(offset, dom);
            } else if (this.rangeUnit == TChronoUnit.YEARS) {
                int doy = temporal.get(TChronoField.DAY_OF_YEAR);
                int offset = startOfWeekOffset(doy, dow);
                return computeWeek(offset, doy);
            } else if (this.rangeUnit == TIsoFields.WEEK_BASED_YEARS) {
                return localizedWOWBY(temporal);
            } else if (this.rangeUnit == TChronoUnit.FOREVER) {
                return localizedWBY(temporal);
            } else {
                throw new IllegalStateException("unreachable");
            }
        }

        private int localizedDayOfWeek(TTemporalAccessor temporal, int sow) {

            int isoDow = temporal.get(DAY_OF_WEEK);
            return TJdk8Methods.floorMod(isoDow - sow, 7) + 1;
        }

        private long localizedWeekOfMonth(TTemporalAccessor temporal, int dow) {

            int dom = temporal.get(DAY_OF_MONTH);
            int offset = startOfWeekOffset(dom, dow);
            return computeWeek(offset, dom);
        }

        private long localizedWeekOfYear(TTemporalAccessor temporal, int dow) {

            int doy = temporal.get(DAY_OF_YEAR);
            int offset = startOfWeekOffset(doy, dow);
            return computeWeek(offset, doy);
        }

        private int localizedWOWBY(TTemporalAccessor temporal) {

            int sow = this.weekDef.getFirstDayOfWeek().getValue();
            int isoDow = temporal.get(DAY_OF_WEEK);
            int dow = TJdk8Methods.floorMod(isoDow - sow, 7) + 1;
            long woy = localizedWeekOfYear(temporal, dow);
            if (woy == 0) {
                TChronoLocalDate previous = TChronology.from(temporal).date(temporal).minus(1, TChronoUnit.WEEKS);
                return (int) localizedWeekOfYear(previous, dow) + 1;
            } else if (woy >= 53) {
                int offset = startOfWeekOffset(temporal.get(DAY_OF_YEAR), dow);
                int year = temporal.get(YEAR);
                int yearLen = TYear.isLeap(year) ? 366 : 365;
                int weekIndexOfFirstWeekNextYear = computeWeek(offset,
                        yearLen + this.weekDef.getMinimalDaysInFirstWeek());
                if (woy >= weekIndexOfFirstWeekNextYear) {
                    return (int) (woy - (weekIndexOfFirstWeekNextYear - 1));
                }
            }
            return (int) woy;
        }

        private int localizedWBY(TTemporalAccessor temporal) {

            int sow = this.weekDef.getFirstDayOfWeek().getValue();
            int isoDow = temporal.get(DAY_OF_WEEK);
            int dow = TJdk8Methods.floorMod(isoDow - sow, 7) + 1;
            int year = temporal.get(YEAR);
            long woy = localizedWeekOfYear(temporal, dow);
            if (woy == 0) {
                return year - 1;
            } else if (woy < 53) {
                return year;
            }
            int offset = startOfWeekOffset(temporal.get(DAY_OF_YEAR), dow);
            int yearLen = TYear.isLeap(year) ? 366 : 365;
            int weekIndexOfFirstWeekNextYear = computeWeek(offset, yearLen + this.weekDef.getMinimalDaysInFirstWeek());
            if (woy >= weekIndexOfFirstWeekNextYear) {
                return year + 1;
            }
            return year;
        }

        private int startOfWeekOffset(int day, int dow) {

            // offset of first day corresponding to the day of week in first 7 days (zero origin)
            int weekStart = TJdk8Methods.floorMod(day - dow, 7);
            int offset = -weekStart;
            if (weekStart + 1 > this.weekDef.getMinimalDaysInFirstWeek()) {
                // The previous week has the minimum days in the current month to be a 'week'
                offset = 7 - weekStart;
            }
            return offset;
        }

        private int computeWeek(int offset, int day) {

            return ((7 + offset + (day - 1)) / 7);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R extends TTemporal> R adjustInto(R temporal, long newValue) {

            // Check the new value and get the old value of the field
            int newVal = this.range.checkValidIntValue(newValue, this);
            int currentVal = temporal.get(this);
            if (newVal == currentVal) {
                return temporal;
            }
            if (this.rangeUnit == TChronoUnit.FOREVER) {
                // adjust in whole weeks so dow never changes
                int baseWowby = temporal.get(this.weekDef.weekOfWeekBasedYear);
                long diffWeeks = (long) ((newValue - currentVal) * 52.1775);
                TTemporal result = temporal.plus(diffWeeks, TChronoUnit.WEEKS);
                if (result.get(this) > newVal) {
                    // ended up in later week-based-year
                    // move to last week of previous year
                    int newWowby = result.get(this.weekDef.weekOfWeekBasedYear);
                    result = result.minus(newWowby, TChronoUnit.WEEKS);
                } else {
                    if (result.get(this) < newVal) {
                        // ended up in earlier week-based-year
                        result = result.plus(2, TChronoUnit.WEEKS);
                    }
                    // reset the week-of-week-based-year
                    int newWowby = result.get(this.weekDef.weekOfWeekBasedYear);
                    result = result.plus(baseWowby - newWowby, TChronoUnit.WEEKS);
                    if (result.get(this) > newVal) {
                        result = result.minus(1, TChronoUnit.WEEKS);
                    }
                }
                return (R) result;
            }
            // Compute the difference and add that using the base using of the field
            int delta = newVal - currentVal;
            return (R) temporal.plus(delta, this.baseUnit);
        }

        @Override
        public TTemporalAccessor resolve(Map<TTemporalField, Long> fieldValues, TTemporalAccessor partialTemporal,
                TResolverStyle resolverStyle) {

            int sow = this.weekDef.getFirstDayOfWeek().getValue();
            if (this.rangeUnit == WEEKS) { // day-of-week
                final long value = fieldValues.remove(this);
                int localDow = this.range.checkValidIntValue(value, this);
                int isoDow = TJdk8Methods.floorMod((sow - 1) + (localDow - 1), 7) + 1;
                fieldValues.put(DAY_OF_WEEK, (long) isoDow);
                return null;
            }
            if (fieldValues.containsKey(DAY_OF_WEEK) == false) {
                return null;
            }

            // week-based-year
            if (this.rangeUnit == TChronoUnit.FOREVER) {
                if (fieldValues.containsKey(this.weekDef.weekOfWeekBasedYear) == false) {
                    return null;
                }
                TChronology chrono = TChronology.from(partialTemporal); // defaults to ISO
                int isoDow = DAY_OF_WEEK.checkValidIntValue(fieldValues.get(DAY_OF_WEEK));
                int dow = TJdk8Methods.floorMod(isoDow - sow, 7) + 1;
                final int wby = range().checkValidIntValue(fieldValues.get(this), this);
                TChronoLocalDate date;
                long days;
                if (resolverStyle == TResolverStyle.LENIENT) {
                    date = chrono.date(wby, 1, this.weekDef.getMinimalDaysInFirstWeek());
                    long wowby = fieldValues.get(this.weekDef.weekOfWeekBasedYear);
                    int dateDow = localizedDayOfWeek(date, sow);
                    long weeks = wowby - localizedWeekOfYear(date, dateDow);
                    days = weeks * 7 + (dow - dateDow);
                } else {
                    date = chrono.date(wby, 1, this.weekDef.getMinimalDaysInFirstWeek());
                    long wowby = this.weekDef.weekOfWeekBasedYear.range().checkValidIntValue(
                            fieldValues.get(this.weekDef.weekOfWeekBasedYear), this.weekDef.weekOfWeekBasedYear);
                    int dateDow = localizedDayOfWeek(date, sow);
                    long weeks = wowby - localizedWeekOfYear(date, dateDow);
                    days = weeks * 7 + (dow - dateDow);
                }
                date = date.plus(days, DAYS);
                if (resolverStyle == TResolverStyle.STRICT) {
                    if (date.getLong(this) != fieldValues.get(this)) {
                        throw new TDateTimeException("Strict mode rejected date parsed to a different year");
                    }
                }
                fieldValues.remove(this);
                fieldValues.remove(this.weekDef.weekOfWeekBasedYear);
                fieldValues.remove(DAY_OF_WEEK);
                return date;
            }

            if (fieldValues.containsKey(YEAR) == false) {
                return null;
            }
            int isoDow = DAY_OF_WEEK.checkValidIntValue(fieldValues.get(DAY_OF_WEEK));
            int dow = TJdk8Methods.floorMod(isoDow - sow, 7) + 1;
            int year = YEAR.checkValidIntValue(fieldValues.get(YEAR));
            TChronology chrono = TChronology.from(partialTemporal); // defaults to ISO
            if (this.rangeUnit == MONTHS) { // week-of-month
                if (fieldValues.containsKey(MONTH_OF_YEAR) == false) {
                    return null;
                }
                final long value = fieldValues.remove(this);
                TChronoLocalDate date;
                long days;
                if (resolverStyle == TResolverStyle.LENIENT) {
                    long month = fieldValues.get(MONTH_OF_YEAR);
                    date = chrono.date(year, 1, 1);
                    date = date.plus(month - 1, MONTHS);
                    int dateDow = localizedDayOfWeek(date, sow);
                    long weeks = value - localizedWeekOfMonth(date, dateDow);
                    days = weeks * 7 + (dow - dateDow);
                } else {
                    int month = MONTH_OF_YEAR.checkValidIntValue(fieldValues.get(MONTH_OF_YEAR));
                    date = chrono.date(year, month, 8);
                    int dateDow = localizedDayOfWeek(date, sow);
                    int wom = this.range.checkValidIntValue(value, this);
                    long weeks = wom - localizedWeekOfMonth(date, dateDow);
                    days = weeks * 7 + (dow - dateDow);
                }
                date = date.plus(days, DAYS);
                if (resolverStyle == TResolverStyle.STRICT) {
                    if (date.getLong(MONTH_OF_YEAR) != fieldValues.get(MONTH_OF_YEAR)) {
                        throw new TDateTimeException("Strict mode rejected date parsed to a different month");
                    }
                }
                fieldValues.remove(this);
                fieldValues.remove(YEAR);
                fieldValues.remove(MONTH_OF_YEAR);
                fieldValues.remove(DAY_OF_WEEK);
                return date;
            } else if (this.rangeUnit == YEARS) { // week-of-year
                final long value = fieldValues.remove(this);
                TChronoLocalDate date = chrono.date(year, 1, 1);
                long days;
                if (resolverStyle == TResolverStyle.LENIENT) {
                    int dateDow = localizedDayOfWeek(date, sow);
                    long weeks = value - localizedWeekOfYear(date, dateDow);
                    days = weeks * 7 + (dow - dateDow);
                } else {
                    int dateDow = localizedDayOfWeek(date, sow);
                    int woy = this.range.checkValidIntValue(value, this);
                    long weeks = woy - localizedWeekOfYear(date, dateDow);
                    days = weeks * 7 + (dow - dateDow);
                }
                date = date.plus(days, DAYS);
                if (resolverStyle == TResolverStyle.STRICT) {
                    if (date.getLong(YEAR) != fieldValues.get(YEAR)) {
                        throw new TDateTimeException("Strict mode rejected date parsed to a different year");
                    }
                }
                fieldValues.remove(this);
                fieldValues.remove(YEAR);
                fieldValues.remove(DAY_OF_WEEK);
                return date;
            } else {
                throw new IllegalStateException("unreachable");
            }
        }

        @Override
        public TTemporalUnit getBaseUnit() {

            return this.baseUnit;
        }

        @Override
        public TTemporalUnit getRangeUnit() {

            return this.rangeUnit;
        }

        @Override
        public TValueRange range() {

            return this.range;
        }

        @Override
        public boolean isDateBased() {

            return true;
        }

        @Override
        public boolean isTimeBased() {

            return false;
        }

        @Override
        public boolean isSupportedBy(TTemporalAccessor temporal) {

            if (temporal.isSupported(TChronoField.DAY_OF_WEEK)) {
                if (this.rangeUnit == TChronoUnit.WEEKS) {
                    return true;
                } else if (this.rangeUnit == TChronoUnit.MONTHS) {
                    return temporal.isSupported(TChronoField.DAY_OF_MONTH);
                } else if (this.rangeUnit == TChronoUnit.YEARS) {
                    return temporal.isSupported(TChronoField.DAY_OF_YEAR);
                } else if (this.rangeUnit == TIsoFields.WEEK_BASED_YEARS) {
                    return temporal.isSupported(TChronoField.EPOCH_DAY);
                } else if (this.rangeUnit == TChronoUnit.FOREVER) {
                    return temporal.isSupported(TChronoField.EPOCH_DAY);
                }
            }
            return false;
        }

        @Override
        public TValueRange rangeRefinedBy(TTemporalAccessor temporal) {

            if (this.rangeUnit == TChronoUnit.WEEKS) {
                return this.range;
            }

            TTemporalField field = null;
            if (this.rangeUnit == TChronoUnit.MONTHS) {
                field = TChronoField.DAY_OF_MONTH;
            } else if (this.rangeUnit == TChronoUnit.YEARS) {
                field = TChronoField.DAY_OF_YEAR;
            } else if (this.rangeUnit == TIsoFields.WEEK_BASED_YEARS) {
                return rangeWOWBY(temporal);
            } else if (this.rangeUnit == TChronoUnit.FOREVER) {
                return temporal.range(YEAR);
            } else {
                throw new IllegalStateException("unreachable");
            }

            // Offset the ISO DOW by the start of this week
            int sow = this.weekDef.getFirstDayOfWeek().getValue();
            int isoDow = temporal.get(TChronoField.DAY_OF_WEEK);
            int dow = TJdk8Methods.floorMod(isoDow - sow, 7) + 1;

            int offset = startOfWeekOffset(temporal.get(field), dow);
            TValueRange fieldRange = temporal.range(field);
            return TValueRange.of(computeWeek(offset, (int) fieldRange.getMinimum()),
                    computeWeek(offset, (int) fieldRange.getMaximum()));
        }

        private TValueRange rangeWOWBY(TTemporalAccessor temporal) {

            int sow = this.weekDef.getFirstDayOfWeek().getValue();
            int isoDow = temporal.get(DAY_OF_WEEK);
            int dow = TJdk8Methods.floorMod(isoDow - sow, 7) + 1;
            long woy = localizedWeekOfYear(temporal, dow);
            if (woy == 0) {
                return rangeWOWBY(TChronology.from(temporal).date(temporal).minus(2, TChronoUnit.WEEKS));
            }
            int offset = startOfWeekOffset(temporal.get(DAY_OF_YEAR), dow);
            int year = temporal.get(YEAR);
            int yearLen = TYear.isLeap(year) ? 366 : 365;
            int weekIndexOfFirstWeekNextYear = computeWeek(offset, yearLen + this.weekDef.getMinimalDaysInFirstWeek());
            if (woy >= weekIndexOfFirstWeekNextYear) {
                return rangeWOWBY(TChronology.from(temporal).date(temporal).plus(2, TChronoUnit.WEEKS));
            }
            return TValueRange.of(1, weekIndexOfFirstWeekNextYear - 1);
        }

        @Override
        public String getDisplayName(TLocale locale) {

            TJdk8Methods.requireNonNull(locale, "locale");
            if (this.rangeUnit == YEARS) { // week-of-year
                return "Week";
            }
            return toString();
        }

        @Override
        public String toString() {

            return this.name + "[" + this.weekDef.toString() + "]";
        }
    }

}
