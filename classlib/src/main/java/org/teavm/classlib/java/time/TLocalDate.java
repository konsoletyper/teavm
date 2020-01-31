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

import static org.teavm.classlib.java.time.TLocalTime.SECONDS_PER_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_WEEK_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_WEEK_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.EPOCH_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.ERA;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.PROLEPTIC_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;

import org.teavm.classlib.java.time.chrono.TChronoLocalDate;
import org.teavm.classlib.java.time.chrono.TEra;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.format.TDateTimeParseException;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalAdjusters;
import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.time.temporal.TValueRange;
import org.teavm.classlib.java.time.zone.TZoneOffsetTransition;
import org.teavm.classlib.java.time.zone.TZoneRules;

public final class TLocalDate
        extends TChronoLocalDate
        implements TTemporal, TTemporalAdjuster, Serializable {

    public static final TLocalDate MIN = TLocalDate.of(TYear.MIN_VALUE, 1, 1);
    public static final TLocalDate MAX = TLocalDate.of(TYear.MAX_VALUE, 12, 31);
    public static final TTemporalQuery<TLocalDate> FROM = new TTemporalQuery<TLocalDate>() {
        @Override
        public TLocalDate queryFrom(TTemporalAccessor temporal) {
            return TLocalDate.from(temporal);
        }
    };

    private static final long serialVersionUID = 2942565459149668126L;
    private static final int DAYS_PER_CYCLE = 146097;
    static final long DAYS_0000_TO_1970 = (DAYS_PER_CYCLE * 5L) - (30L * 365L + 7L);

    private final int year;
    private final short month;
    private final short day;

    //-----------------------------------------------------------------------
    public static TLocalDate now() {
        return now(TClock.systemDefaultZone());
    }

    public static TLocalDate now(TZoneId zone) {
        return now(TClock.system(zone));
    }

    public static TLocalDate now(TClock clock) {
        TJdk8Methods.requireNonNull(clock, "clock");
        final TInstant now = clock.instant();  // called once
        TZoneOffset offset = clock.getZone().getRules().getOffset(now);
        long epochSec = now.getEpochSecond() + offset.getTotalSeconds();  // overflow caught later
        long epochDay = TJdk8Methods.floorDiv(epochSec, SECONDS_PER_DAY);
        return TLocalDate.ofEpochDay(epochDay);
    }

    //-----------------------------------------------------------------------
    public static TLocalDate of(int year, TMonth month, int dayOfMonth) {
        YEAR.checkValidValue(year);
        TJdk8Methods.requireNonNull(month, "month");
        DAY_OF_MONTH.checkValidValue(dayOfMonth);
        return create(year, month, dayOfMonth);
    }

    public static TLocalDate of(int year, int month, int dayOfMonth) {
        YEAR.checkValidValue(year);
        MONTH_OF_YEAR.checkValidValue(month);
        DAY_OF_MONTH.checkValidValue(dayOfMonth);
        return create(year, TMonth.of(month), dayOfMonth);
    }

    //-----------------------------------------------------------------------
    public static TLocalDate ofYearDay(int year, int dayOfYear) {
        YEAR.checkValidValue(year);
        DAY_OF_YEAR.checkValidValue(dayOfYear);
        boolean leap = TIsoChronology.INSTANCE.isLeapYear(year);
        if (dayOfYear == 366 && leap == false) {
            throw new TDateTimeException("Invalid date 'DayOfYear 366' as '" + year + "' is not a leap year");
        }
        TMonth moy = TMonth.of((dayOfYear - 1) / 31 + 1);
        int monthEnd = moy.firstDayOfYear(leap) + moy.length(leap) - 1;
        if (dayOfYear > monthEnd) {
            moy = moy.plus(1);
        }
        int dom = dayOfYear - moy.firstDayOfYear(leap) + 1;
        return create(year, moy, dom);
    }

    //-----------------------------------------------------------------------
    public static TLocalDate ofEpochDay(long epochDay) {
        EPOCH_DAY.checkValidValue(epochDay);
        long zeroDay = epochDay + DAYS_0000_TO_1970;
        // find the march-based year
        zeroDay -= 60;  // adjust to 0000-03-01 so leap day is at end of four year cycle
        long adjust = 0;
        if (zeroDay < 0) {
            // adjust negative years to positive for calculation
            long adjustCycles = (zeroDay + 1) / DAYS_PER_CYCLE - 1;
            adjust = adjustCycles * 400;
            zeroDay += -adjustCycles * DAYS_PER_CYCLE;
        }
        long yearEst = (400 * zeroDay + 591) / DAYS_PER_CYCLE;
        long doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        if (doyEst < 0) {
            // fix estimate
            yearEst--;
            doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        }
        yearEst += adjust;  // reset any negative year
        int marchDoy0 = (int) doyEst;

        // convert march-based values back to january-based
        int marchMonth0 = (marchDoy0 * 5 + 2) / 153;
        int month = (marchMonth0 + 2) % 12 + 1;
        int dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1;
        yearEst += marchMonth0 / 10;

        // check year now we are certain it is correct
        int year = YEAR.checkValidIntValue(yearEst);
        return new TLocalDate(year, month, dom);
    }

    //-----------------------------------------------------------------------
    public static TLocalDate from(TTemporalAccessor temporal) {
        TLocalDate date = temporal.query(TTemporalQueries.localDate());
        if (date == null) {
            throw new TDateTimeException("Unable to obtain TLocalDate from TTemporalAccessor: " +
                    temporal + ", type " + temporal.getClass().getName());
        }
        return date;
    }

    //-----------------------------------------------------------------------
    public static TLocalDate parse(CharSequence text) {
        return parse(text, TDateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static TLocalDate parse(CharSequence text, TDateTimeFormatter formatter) {
        TJdk8Methods.requireNonNull(formatter, "formatter");
        return formatter.parse(text, TLocalDate.FROM);
    }

    //-----------------------------------------------------------------------
    private static TLocalDate create(int year, TMonth month, int dayOfMonth) {
        if (dayOfMonth > 28 && dayOfMonth > month.length(TIsoChronology.INSTANCE.isLeapYear(year))) {
            if (dayOfMonth == 29) {
                throw new TDateTimeException("Invalid date 'February 29' as '" + year + "' is not a leap year");
            } else {
                throw new TDateTimeException("Invalid date '" + month.name() + " " + dayOfMonth + "'");
            }
        }
        return new TLocalDate(year, month.getValue(), dayOfMonth);
    }

    private static TLocalDate resolvePreviousValid(int year, int month, int day) {
        switch (month) {
            case 2:
                day = Math.min(day, TIsoChronology.INSTANCE.isLeapYear(year) ? 29 : 28);
                break;
            case 4:
            case 6:
            case 9:
            case 11:
                day = Math.min(day, 30);
                break;
        }
        return TLocalDate.of(year, month, day);
    }

    private TLocalDate(int year, int month, int dayOfMonth) {
        this.year = year;
        this.month = (short) month;
        this.day = (short) dayOfMonth;
    }

    //-----------------------------------------------------------------------
    @Override  // override for Javadoc
    public boolean isSupported(TTemporalField field) {
        return super.isSupported(field);
    }

    @Override
    public TValueRange range(TTemporalField field) {
        if (field instanceof TChronoField) {
            TChronoField f = (TChronoField) field;
            if (f.isDateBased()) {
                switch (f) {
                    case DAY_OF_MONTH: return TValueRange.of(1, lengthOfMonth());
                    case DAY_OF_YEAR: return TValueRange.of(1, lengthOfYear());
                    case ALIGNED_WEEK_OF_MONTH: return TValueRange.of(1, getMonth() == TMonth.FEBRUARY && isLeapYear() == false ? 4 : 5);
                    case YEAR_OF_ERA:
                        return (getYear() <= 0 ? TValueRange.of(1, TYear.MAX_VALUE + 1) : TValueRange.of(1, TYear.MAX_VALUE));
                }
                return field.range();
            }
            throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.rangeRefinedBy(this);
    }

    @Override  // override for Javadoc and performance
    public int get(TTemporalField field) {
        if (field instanceof TChronoField) {
            return get0(field);
        }
        return super.get(field);
    }

    @Override
    public long getLong(TTemporalField field) {
        if (field instanceof TChronoField) {
            if (field == EPOCH_DAY) {
                return toEpochDay();
            }
            if (field == PROLEPTIC_MONTH) {
                return getProlepticMonth();
            }
            return get0(field);
        }
        return field.getFrom(this);
    }

    private int get0(TTemporalField field) {
        switch ((TChronoField) field) {
            case DAY_OF_WEEK: return getDayOfWeek().getValue();
            case ALIGNED_DAY_OF_WEEK_IN_MONTH: return ((day - 1) % 7) + 1;
            case ALIGNED_DAY_OF_WEEK_IN_YEAR: return ((getDayOfYear() - 1) % 7) + 1;
            case DAY_OF_MONTH: return day;
            case DAY_OF_YEAR: return getDayOfYear();
            case EPOCH_DAY: throw new TDateTimeException("Field too large for an int: " + field);
            case ALIGNED_WEEK_OF_MONTH: return ((day - 1) / 7) + 1;
            case ALIGNED_WEEK_OF_YEAR: return ((getDayOfYear() - 1) / 7) + 1;
            case MONTH_OF_YEAR: return month;
            case PROLEPTIC_MONTH: throw new TDateTimeException("Field too large for an int: " + field);
            case YEAR_OF_ERA: return (year >= 1 ? year : 1 - year);
            case YEAR: return year;
            case ERA: return (year >= 1 ? 1 : 0);
        }
        throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
    }

    private long getProlepticMonth() {
        return (year * 12L) + (month - 1);
    }

    //-----------------------------------------------------------------------
    @Override
    public TIsoChronology getChronology() {
        return TIsoChronology.INSTANCE;
    }

    @Override // override for Javadoc
    public TEra getEra() {
        return super.getEra();
    }

    public int getYear() {
        return year;
    }

    public int getMonthValue() {
        return month;
    }

    public TMonth getMonth() {
        return TMonth.of(month);
    }

    public int getDayOfMonth() {
        return day;
    }

    public int getDayOfYear() {
        return getMonth().firstDayOfYear(isLeapYear()) + day - 1;
    }

    public TDayOfWeek getDayOfWeek() {
        int dow0 = TJdk8Methods.floorMod(toEpochDay() + 3, 7);
        return TDayOfWeek.of(dow0 + 1);
    }

    //-----------------------------------------------------------------------
    @Override // override for Javadoc and performance
    public boolean isLeapYear() {
        return TIsoChronology.INSTANCE.isLeapYear(year);
    }

    @Override
    public int lengthOfMonth() {
        switch (month) {
            case 2:
                return (isLeapYear() ? 29 : 28);
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
            default:
                return 31;
        }
    }

    @Override // override for Javadoc and performance
    public int lengthOfYear() {
        return (isLeapYear() ? 366 : 365);
    }

    //-----------------------------------------------------------------------
    @Override
    public TLocalDate with(TTemporalAdjuster adjuster) {
        // optimizations
        if (adjuster instanceof TLocalDate) {
            return (TLocalDate) adjuster;
        }
        return (TLocalDate) adjuster.adjustInto(this);
    }

    @Override
    public TLocalDate with(TTemporalField field, long newValue) {
        if (field instanceof TChronoField) {
            TChronoField f = (TChronoField) field;
            f.checkValidValue(newValue);
            switch (f) {
                case DAY_OF_WEEK: return plusDays(newValue - getDayOfWeek().getValue());
                case ALIGNED_DAY_OF_WEEK_IN_MONTH: return plusDays(newValue - getLong(ALIGNED_DAY_OF_WEEK_IN_MONTH));
                case ALIGNED_DAY_OF_WEEK_IN_YEAR: return plusDays(newValue - getLong(ALIGNED_DAY_OF_WEEK_IN_YEAR));
                case DAY_OF_MONTH: return withDayOfMonth((int) newValue);
                case DAY_OF_YEAR: return withDayOfYear((int) newValue);
                case EPOCH_DAY: return TLocalDate.ofEpochDay(newValue);
                case ALIGNED_WEEK_OF_MONTH: return plusWeeks(newValue - getLong(ALIGNED_WEEK_OF_MONTH));
                case ALIGNED_WEEK_OF_YEAR: return plusWeeks(newValue - getLong(ALIGNED_WEEK_OF_YEAR));
                case MONTH_OF_YEAR: return withMonth((int) newValue);
                case PROLEPTIC_MONTH: return plusMonths(newValue - getLong(PROLEPTIC_MONTH));
                case YEAR_OF_ERA: return withYear((int) (year >= 1 ? newValue : 1 - newValue));
                case YEAR: return withYear((int) newValue);
                case ERA: return (getLong(ERA) == newValue ? this : withYear(1 - year));
            }
            throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.adjustInto(this, newValue);
    }

    //-----------------------------------------------------------------------
    public TLocalDate withYear(int year) {
        if (this.year == year) {
            return this;
        }
        YEAR.checkValidValue(year);
        return resolvePreviousValid(year, month, day);
    }

    public TLocalDate withMonth(int month) {
        if (this.month == month) {
            return this;
        }
        MONTH_OF_YEAR.checkValidValue(month);
        return resolvePreviousValid(year, month, day);
    }

    public TLocalDate withDayOfMonth(int dayOfMonth) {
        if (this.day == dayOfMonth) {
            return this;
        }
        return of(year, month, dayOfMonth);
    }

    public TLocalDate withDayOfYear(int dayOfYear) {
        if (this.getDayOfYear() == dayOfYear) {
            return this;
        }
        return ofYearDay(year, dayOfYear);
    }

    //-----------------------------------------------------------------------
    @Override
    public TLocalDate plus(TTemporalAmount amount) {
        return (TLocalDate) amount.addTo(this);
    }

    @Override
    public TLocalDate plus(long amountToAdd, TTemporalUnit unit) {
        if (unit instanceof TChronoUnit) {
            TChronoUnit f = (TChronoUnit) unit;
            switch (f) {
                case DAYS: return plusDays(amountToAdd);
                case WEEKS: return plusWeeks(amountToAdd);
                case MONTHS: return plusMonths(amountToAdd);
                case YEARS: return plusYears(amountToAdd);
                case DECADES: return plusYears(TJdk8Methods.safeMultiply(amountToAdd, 10));
                case CENTURIES: return plusYears(TJdk8Methods.safeMultiply(amountToAdd, 100));
                case MILLENNIA: return plusYears(TJdk8Methods.safeMultiply(amountToAdd, 1000));
                case ERAS: return with(ERA, TJdk8Methods.safeAdd(getLong(ERA), amountToAdd));
            }
            throw new TUnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
        return unit.addTo(this, amountToAdd);
    }

    //-----------------------------------------------------------------------
    public TLocalDate plusYears(long yearsToAdd) {
        if (yearsToAdd == 0) {
            return this;
        }
        int newYear = YEAR.checkValidIntValue(year + yearsToAdd);  // safe overflow
        return resolvePreviousValid(newYear, month, day);
    }

    public TLocalDate plusMonths(long monthsToAdd) {
        if (monthsToAdd == 0) {
            return this;
        }
        long monthCount = year * 12L + (month - 1);
        long calcMonths = monthCount + monthsToAdd;  // safe overflow
        int newYear = YEAR.checkValidIntValue(TJdk8Methods.floorDiv(calcMonths, 12));
        int newMonth = TJdk8Methods.floorMod(calcMonths, 12) + 1;
        return resolvePreviousValid(newYear, newMonth, day);
    }

    public TLocalDate plusWeeks(long weeksToAdd) {
        return plusDays(TJdk8Methods.safeMultiply(weeksToAdd, 7));
    }

    public TLocalDate plusDays(long daysToAdd) {
        if (daysToAdd == 0) {
            return this;
        }
        long mjDay = TJdk8Methods.safeAdd(toEpochDay(), daysToAdd);
        return TLocalDate.ofEpochDay(mjDay);
    }

    //-----------------------------------------------------------------------
    @Override
    public TLocalDate minus(TTemporalAmount amount) {
        return (TLocalDate) amount.subtractFrom(this);
    }

    @Override
    public TLocalDate minus(long amountToSubtract, TTemporalUnit unit) {
        return (amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit) : plus(-amountToSubtract, unit));
    }

    //-----------------------------------------------------------------------
    public TLocalDate minusYears(long yearsToSubtract) {
        return (yearsToSubtract == Long.MIN_VALUE ? plusYears(Long.MAX_VALUE).plusYears(1) : plusYears(-yearsToSubtract));
    }

    public TLocalDate minusMonths(long monthsToSubtract) {
        return (monthsToSubtract == Long.MIN_VALUE ? plusMonths(Long.MAX_VALUE).plusMonths(1) : plusMonths(-monthsToSubtract));
    }

    public TLocalDate minusWeeks(long weeksToSubtract) {
        return (weeksToSubtract == Long.MIN_VALUE ? plusWeeks(Long.MAX_VALUE).plusWeeks(1) : plusWeeks(-weeksToSubtract));
    }

    public TLocalDate minusDays(long daysToSubtract) {
        return (daysToSubtract == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1) : plusDays(-daysToSubtract));
    }

    //-----------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TTemporalQuery<R> query) {
        if (query == TTemporalQueries.localDate()) {
            return (R) this;
        }
        return super.query(query);
    }

    @Override  // override for Javadoc
    public TTemporal adjustInto(TTemporal temporal) {
        return super.adjustInto(temporal);
    }

    @Override
    public long until(TTemporal endExclusive, TTemporalUnit unit) {
        TLocalDate end = TLocalDate.from(endExclusive);
        if (unit instanceof TChronoUnit) {
            switch ((TChronoUnit) unit) {
                case DAYS: return daysUntil(end);
                case WEEKS: return daysUntil(end) / 7;
                case MONTHS: return monthsUntil(end);
                case YEARS: return monthsUntil(end) / 12;
                case DECADES: return monthsUntil(end) / 120;
                case CENTURIES: return monthsUntil(end) / 1200;
                case MILLENNIA: return monthsUntil(end) / 12000;
                case ERAS: return end.getLong(ERA) - getLong(ERA);
            }
            throw new TUnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
        return unit.between(this, end);
    }

    long daysUntil(TLocalDate end) {
        return end.toEpochDay() - toEpochDay();  // no overflow
    }

    private long monthsUntil(TLocalDate end) {
        long packed1 = getProlepticMonth() * 32L + getDayOfMonth();  // no overflow
        long packed2 = end.getProlepticMonth() * 32L + end.getDayOfMonth();  // no overflow
        return (packed2 - packed1) / 32;
    }

    @Override
    public TPeriod until(TChronoLocalDate endDate) {
        TLocalDate end = TLocalDate.from(endDate);
        long totalMonths = end.getProlepticMonth() - this.getProlepticMonth();  // safe
        int days = end.day - this.day;
        if (totalMonths > 0 && days < 0) {
            totalMonths--;
            TLocalDate calcDate = this.plusMonths(totalMonths);
            days = (int) (end.toEpochDay() - calcDate.toEpochDay());  // safe
        } else if (totalMonths < 0 && days > 0) {
            totalMonths++;
            days -= end.lengthOfMonth();
        }
        long years = totalMonths / 12;  // safe
        int months = (int) (totalMonths % 12);  // safe
        return TPeriod.of(TJdk8Methods.safeToInt(years), months, days);
    }

    //-----------------------------------------------------------------------
    @Override
    public TLocalDateTime atTime(TLocalTime time) {
        return TLocalDateTime.of(this, time);
    }

    public TLocalDateTime atTime(int hour, int minute) {
        return atTime(TLocalTime.of(hour, minute));
    }

    public TLocalDateTime atTime(int hour, int minute, int second) {
        return atTime(TLocalTime.of(hour, minute, second));
    }

    public TLocalDateTime atTime(int hour, int minute, int second, int nanoOfSecond) {
        return atTime(TLocalTime.of(hour, minute, second, nanoOfSecond));
    }

    public TOffsetDateTime atTime(TOffsetTime time) {
        return TOffsetDateTime.of(TLocalDateTime.of(this, time.toLocalTime()), time.getOffset());
    }

    public TLocalDateTime atStartOfDay() {
        return TLocalDateTime.of(this, TLocalTime.MIDNIGHT);
    }

    public TZonedDateTime atStartOfDay(TZoneId zone) {
        TJdk8Methods.requireNonNull(zone, "zone");
        // need to handle case where there is a gap from 11:30 to 00:30
        // standard ZDT factory would result in 01:00 rather than 00:30
        TLocalDateTime ldt = atTime(TLocalTime.MIDNIGHT);
        if (zone instanceof TZoneOffset == false) {
            TZoneRules rules = zone.getRules();
            TZoneOffsetTransition trans = rules.getTransition(ldt);
            if (trans != null && trans.isGap()) {
                ldt = trans.getDateTimeAfter();
            }
        }
        return TZonedDateTime.of(ldt, zone);
    }

    //-----------------------------------------------------------------------
    @Override
    public long toEpochDay() {
        long y = year;
        long m = month;
        long total = 0;
        total += 365 * y;
        if (y >= 0) {
            total += (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400;
        } else {
            total -= y / -4 - y / -100 + y / -400;
        }
        total += ((367 * m - 362) / 12);
        total += day - 1;
        if (m > 2) {
            total--;
            if (isLeapYear() == false) {
                total--;
            }
        }
        return total - DAYS_0000_TO_1970;
    }

    //-----------------------------------------------------------------------
    @Override  // override for Javadoc and performance
    public int compareTo(TChronoLocalDate other) {
        if (other instanceof TLocalDate) {
            return compareTo0((TLocalDate) other);
        }
        return super.compareTo(other);
    }

    int compareTo0(TLocalDate otherDate) {
        int cmp = (year - otherDate.year);
        if (cmp == 0) {
            cmp = (month - otherDate.month);
            if (cmp == 0) {
                cmp = (day - otherDate.day);
            }
        }
        return cmp;
    }

    @Override  // override for Javadoc and performance
    public boolean isAfter(TChronoLocalDate other) {
        if (other instanceof TLocalDate) {
            return compareTo0((TLocalDate) other) > 0;
        }
        return super.isAfter(other);
    }

    @Override  // override for Javadoc and performance
    public boolean isBefore(TChronoLocalDate other) {
        if (other instanceof TLocalDate) {
            return compareTo0((TLocalDate) other) < 0;
        }
        return super.isBefore(other);
    }

    @Override  // override for Javadoc and performance
    public boolean isEqual(TChronoLocalDate other) {
        if (other instanceof TLocalDate) {
            return compareTo0((TLocalDate) other) == 0;
        }
        return super.isEqual(other);
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TLocalDate) {
            return compareTo0((TLocalDate) obj) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int yearValue = year;
        int monthValue = month;
        int dayValue = day;
        return (yearValue & 0xFFFFF800) ^ ((yearValue << 11) + (monthValue << 6) + (dayValue));
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
        int yearValue = year;
        int monthValue = month;
        int dayValue = day;
        int absYear = Math.abs(yearValue);
        StringBuilder buf = new StringBuilder(10);
        if (absYear < 1000) {
            if (yearValue < 0) {
                buf.append(yearValue - 10000).deleteCharAt(1);
            } else {
                buf.append(yearValue + 10000).deleteCharAt(0);
            }
        } else {
            if (yearValue > 9999) {
                buf.append('+');
            }
            buf.append(yearValue);
        }
        return buf.append(monthValue < 10 ? "-0" : "-")
            .append(monthValue)
            .append(dayValue < 10 ? "-0" : "-")
            .append(dayValue)
            .toString();
    }

    @Override  // override for Javadoc
    public String format(TDateTimeFormatter formatter) {
        return super.format(formatter);
    }

    //-----------------------------------------------------------------------
    private Object writeReplace() {
        return new Ser(Ser.LOCAL_DATE_TYPE, this);
    }

    private Object readResolve() throws ObjectStreamException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    void writeExternal(DataOutput out) throws IOException {
        out.writeInt(year);
        out.writeByte(month);
        out.writeByte(day);
    }

    static TLocalDate readExternal(DataInput in) throws IOException {
        int year = in.readInt();
        int month = in.readByte();
        int dayOfMonth = in.readByte();
        return TLocalDate.of(year, month, dayOfMonth);
    }

}
