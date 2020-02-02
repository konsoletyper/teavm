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

import static org.teavm.classlib.java.time.temporal.TChronoField.ERA;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR_OF_ERA;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.CENTURIES;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DECADES;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.ERAS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MILLENNIA;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.YEARS;

import java.util.Objects;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TComparable;
import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder;
import org.teavm.classlib.java.time.format.TSignStyle;
import org.teavm.classlib.java.time.jdk8.TDefaultInterfaceTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.time.temporal.TValueRange;

public final class TYear extends TDefaultInterfaceTemporalAccessor
        implements TTemporal, TTemporalAdjuster, TComparable<TYear>, TSerializable {

    public static final int MIN_VALUE = -999999999;

    public static final int MAX_VALUE = 999999999;

    public static final TTemporalQuery<TYear> FROM = new TTemporalQuery<TYear>() {
        @Override
        public TYear queryFrom(TTemporalAccessor temporal) {

            return TYear.from(temporal);
        }
    };

    private static final TDateTimeFormatter PARSER = new TDateTimeFormatterBuilder()
            .appendValue(YEAR, 4, 10, TSignStyle.EXCEEDS_PAD).toFormatter();

    private final int year;

    public static TYear now() {

        return now(TClock.systemDefaultZone());
    }

    public static TYear now(TZoneId zone) {

        return now(TClock.system(zone));
    }

    public static TYear now(TClock clock) {

        final TLocalDate now = TLocalDate.now(clock); // called once
        return TYear.of(now.getYear());
    }

    public static TYear of(int isoYear) {

        YEAR.checkValidValue(isoYear);
        return new TYear(isoYear);
    }

    public static TYear from(TTemporalAccessor temporal) {

        if (temporal instanceof TYear) {
            return (TYear) temporal;
        }
        try {
            if (TIsoChronology.INSTANCE.equals(TChronology.from(temporal)) == false) {
                temporal = TLocalDate.from(temporal);
            }
            return of(temporal.get(YEAR));
        } catch (TDateTimeException ex) {
            throw new TDateTimeException("Unable to obtain TYear from TTemporalAccessor: " + temporal + ", type "
                    + temporal.getClass().getName());
        }
    }

    public static TYear parse(CharSequence text) {

        return parse(text, PARSER);
    }

    public static TYear parse(CharSequence text, TDateTimeFormatter formatter) {

        Objects.requireNonNull(formatter, "formatter");
        return formatter.parse(text, TYear.FROM);
    }

    public static boolean isLeap(long year) {

        return ((year & 3) == 0) && ((year % 100) != 0 || (year % 400) == 0);
    }

    private TYear(int year) {

        this.year = year;
    }

    public int getValue() {

        return this.year;
    }

    @Override
    public boolean isSupported(TTemporalField field) {

        if (field instanceof TChronoField) {
            return field == YEAR || field == YEAR_OF_ERA || field == ERA;
        }
        return field != null && field.isSupportedBy(this);
    }

    @Override
    public boolean isSupported(TTemporalUnit unit) {

        if (unit instanceof TChronoUnit) {
            return unit == YEARS || unit == DECADES || unit == CENTURIES || unit == MILLENNIA || unit == ERAS;
        }
        return unit != null && unit.isSupportedBy(this);
    }

    @Override
    public TValueRange range(TTemporalField field) {

        if (field == YEAR_OF_ERA) {
            return (this.year <= 0 ? TValueRange.of(1, MAX_VALUE + 1) : TValueRange.of(1, MAX_VALUE));
        }
        return super.range(field);
    }

    @Override
    public int get(TTemporalField field) {

        return range(field).checkValidIntValue(getLong(field), field);
    }

    @Override
    public long getLong(TTemporalField field) {

        if (field instanceof TChronoField) {
            switch ((TChronoField) field) {
                case YEAR_OF_ERA:
                    return (this.year < 1 ? 1 - this.year : this.year);
                case YEAR:
                    return this.year;
                case ERA:
                    return (this.year < 1 ? 0 : 1);
            }
            throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.getFrom(this);
    }

    public boolean isLeap() {

        return TYear.isLeap(this.year);
    }

    public boolean isValidMonthDay(TMonthDay monthDay) {

        return monthDay != null && monthDay.isValidYear(this.year);
    }

    public int length() {

        return isLeap() ? 366 : 365;
    }

    @Override
    public TYear with(TTemporalAdjuster adjuster) {

        return (TYear) adjuster.adjustInto(this);
    }

    @Override
    public TYear with(TTemporalField field, long newValue) {

        if (field instanceof TChronoField) {
            TChronoField f = (TChronoField) field;
            f.checkValidValue(newValue);
            switch (f) {
                case YEAR_OF_ERA:
                    return TYear.of((int) (this.year < 1 ? 1 - newValue : newValue));
                case YEAR:
                    return TYear.of((int) newValue);
                case ERA:
                    return (getLong(ERA) == newValue ? this : TYear.of(1 - this.year));
            }
            throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.adjustInto(this, newValue);
    }

    @Override
    public TYear plus(TTemporalAmount amount) {

        return (TYear) amount.addTo(this);
    }

    @Override
    public TYear plus(long amountToAdd, TTemporalUnit unit) {

        if (unit instanceof TChronoUnit) {
            switch ((TChronoUnit) unit) {
                case YEARS:
                    return plusYears(amountToAdd);
                case DECADES:
                    return plusYears(Math.multiplyExact(amountToAdd, 10));
                case CENTURIES:
                    return plusYears(Math.multiplyExact(amountToAdd, 100));
                case MILLENNIA:
                    return plusYears(Math.multiplyExact(amountToAdd, 1000));
                case ERAS:
                    return with(ERA, Math.addExact(getLong(ERA), amountToAdd));
            }
            throw new TUnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
        return unit.addTo(this, amountToAdd);
    }

    public TYear plusYears(long yearsToAdd) {

        if (yearsToAdd == 0) {
            return this;
        }
        return of(YEAR.checkValidIntValue(this.year + yearsToAdd)); // overflow safe
    }

    @Override
    public TYear minus(TTemporalAmount amount) {

        return (TYear) amount.subtractFrom(this);
    }

    @Override
    public TYear minus(long amountToSubtract, TTemporalUnit unit) {

        return (amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit)
                : plus(-amountToSubtract, unit));
    }

    public TYear minusYears(long yearsToSubtract) {

        return (yearsToSubtract == Long.MIN_VALUE ? plusYears(Long.MAX_VALUE).plusYears(1)
                : plusYears(-yearsToSubtract));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TTemporalQuery<R> query) {

        if (query == TTemporalQueries.chronology()) {
            return (R) TIsoChronology.INSTANCE;
        } else if (query == TTemporalQueries.precision()) {
            return (R) YEARS;
        } else if (query == TTemporalQueries.localDate() || query == TTemporalQueries.localTime()
                || query == TTemporalQueries.zone() || query == TTemporalQueries.zoneId()
                || query == TTemporalQueries.offset()) {
            return null;
        }
        return super.query(query);
    }

    @Override
    public TTemporal adjustInto(TTemporal temporal) {

        if (TChronology.from(temporal).equals(TIsoChronology.INSTANCE) == false) {
            throw new TDateTimeException("Adjustment only supported on ISO date-time");
        }
        return temporal.with(YEAR, this.year);
    }

    @Override
    public long until(TTemporal endExclusive, TTemporalUnit unit) {

        TYear end = TYear.from(endExclusive);
        if (unit instanceof TChronoUnit) {
            long yearsUntil = ((long) end.year) - this.year; // no overflow
            switch ((TChronoUnit) unit) {
                case YEARS:
                    return yearsUntil;
                case DECADES:
                    return yearsUntil / 10;
                case CENTURIES:
                    return yearsUntil / 100;
                case MILLENNIA:
                    return yearsUntil / 1000;
                case ERAS:
                    return end.getLong(ERA) - getLong(ERA);
            }
            throw new TUnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
        return unit.between(this, end);
    }

    public TLocalDate atDay(int dayOfYear) {

        return TLocalDate.ofYearDay(this.year, dayOfYear);
    }

    public TYearMonth atMonth(TMonth month) {

        return TYearMonth.of(this.year, month);
    }

    public TYearMonth atMonth(int month) {

        return TYearMonth.of(this.year, month);
    }

    public TLocalDate atMonthDay(TMonthDay monthDay) {

        return monthDay.atYear(this.year);
    }

    @Override
    public int compareTo(TYear other) {

        return this.year - other.year;
    }

    public boolean isAfter(TYear other) {

        return this.year > other.year;
    }

    public boolean isBefore(TYear other) {

        return this.year < other.year;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj instanceof TYear) {
            return this.year == ((TYear) obj).year;
        }
        return false;
    }

    @Override
    public int hashCode() {

        return this.year;
    }

    @Override
    public String toString() {

        return Integer.toString(this.year);
    }

    public String format(TDateTimeFormatter formatter) {

        Objects.requireNonNull(formatter, "formatter");
        return formatter.format(this);
    }

}
