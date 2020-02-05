/*
 *  Copyright 2020, adopted to TeaVM by Joerg Hohwiller
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
package org.teavm.classlib.java.time;

import static org.teavm.classlib.java.time.temporal.TChronoField.ERA;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.PROLEPTIC_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR_OF_ERA;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.CENTURIES;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DECADES;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.ERAS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MILLENNIA;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MONTHS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.YEARS;

import java.util.Objects;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TComparable;
import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder;
import org.teavm.classlib.java.time.format.TSignStyle;
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

public final class TYearMonth implements TTemporal, TTemporalAdjuster, TComparable<TYearMonth>, TSerializable {

    public static final TTemporalQuery<TYearMonth> FROM = new TTemporalQuery<TYearMonth>() {
        @Override
        public TYearMonth queryFrom(TTemporalAccessor temporal) {

            return TYearMonth.from(temporal);
        }
    };

    private static final TDateTimeFormatter PARSER = new TDateTimeFormatterBuilder()
            .appendValue(YEAR, 4, 10, TSignStyle.EXCEEDS_PAD).appendLiteral('-').appendValue(MONTH_OF_YEAR, 2)
            .toFormatter();

    private final int year;

    private final int month;

    public static TYearMonth now() {

        return now(TClock.systemDefaultZone());
    }

    public static TYearMonth now(TZoneId zone) {

        return now(TClock.system(zone));
    }

    public static TYearMonth now(TClock clock) {

        final TLocalDate now = TLocalDate.now(clock); // called once
        return TYearMonth.of(now.getYear(), now.getMonth());
    }

    public static TYearMonth of(int year, TMonth month) {

        Objects.requireNonNull(month, "month");
        return of(year, month.getValue());
    }

    public static TYearMonth of(int year, int month) {

        YEAR.checkValidValue(year);
        MONTH_OF_YEAR.checkValidValue(month);
        return new TYearMonth(year, month);
    }

    public static TYearMonth from(TTemporalAccessor temporal) {

        if (temporal instanceof TYearMonth) {
            return (TYearMonth) temporal;
        }
        try {
            if (TIsoChronology.INSTANCE.equals(TChronology.from(temporal)) == false) {
                temporal = TLocalDate.from(temporal);
            }
            return of(temporal.get(YEAR), temporal.get(MONTH_OF_YEAR));
        } catch (TDateTimeException ex) {
            throw new TDateTimeException("Unable to obtain TYearMonth from TTemporalAccessor: " + temporal + ", type "
                    + temporal.getClass().getName());
        }
    }

    public static TYearMonth parse(CharSequence text) {

        return parse(text, PARSER);
    }

    public static TYearMonth parse(CharSequence text, TDateTimeFormatter formatter) {

        Objects.requireNonNull(formatter, "formatter");
        return formatter.parse(text, TYearMonth.FROM);
    }

    private TYearMonth(int year, int month) {

        this.year = year;
        this.month = month;
    }

    private TYearMonth with(int newYear, int newMonth) {

        if (this.year == newYear && this.month == newMonth) {
            return this;
        }
        return new TYearMonth(newYear, newMonth);
    }

    @Override
    public boolean isSupported(TTemporalField field) {

        if (field instanceof TChronoField) {
            return field == YEAR || field == MONTH_OF_YEAR || field == PROLEPTIC_MONTH || field == YEAR_OF_ERA
                    || field == ERA;
        }
        return field != null && field.isSupportedBy(this);
    }

    @Override
    public boolean isSupported(TTemporalUnit unit) {

        if (unit instanceof TChronoUnit) {
            return unit == MONTHS || unit == YEARS || unit == DECADES || unit == CENTURIES || unit == MILLENNIA
                    || unit == ERAS;
        }
        return unit != null && unit.isSupportedBy(this);
    }

    @Override
    public TValueRange range(TTemporalField field) {

        if (field == YEAR_OF_ERA) {
            return (getYear() <= 0 ? TValueRange.of(1, TYear.MAX_VALUE + 1) : TValueRange.of(1, TYear.MAX_VALUE));
        }
        return TTemporal.super.range(field);
    }

    @Override
    public int get(TTemporalField field) {

        return range(field).checkValidIntValue(getLong(field), field);
    }

    @Override
    public long getLong(TTemporalField field) {

        if (field instanceof TChronoField) {
            switch ((TChronoField) field) {
                case MONTH_OF_YEAR:
                    return this.month;
                case PROLEPTIC_MONTH:
                    return getProlepticMonth();
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

    private long getProlepticMonth() {

        return (this.year * 12L) + (this.month - 1);
    }

    public int getYear() {

        return this.year;
    }

    public int getMonthValue() {

        return this.month;
    }

    public TMonth getMonth() {

        return TMonth.of(this.month);
    }

    public boolean isLeapYear() {

        return TIsoChronology.INSTANCE.isLeapYear(this.year);
    }

    public boolean isValidDay(int dayOfMonth) {

        return dayOfMonth >= 1 && dayOfMonth <= lengthOfMonth();
    }

    public int lengthOfMonth() {

        return getMonth().length(isLeapYear());
    }

    public int lengthOfYear() {

        return (isLeapYear() ? 366 : 365);
    }

    @Override
    public TYearMonth with(TTemporalAdjuster adjuster) {

        return (TYearMonth) adjuster.adjustInto(this);
    }

    @Override
    public TYearMonth with(TTemporalField field, long newValue) {

        if (field instanceof TChronoField) {
            TChronoField f = (TChronoField) field;
            f.checkValidValue(newValue);
            switch (f) {
                case MONTH_OF_YEAR:
                    return withMonth((int) newValue);
                case PROLEPTIC_MONTH:
                    return plusMonths(newValue - getLong(PROLEPTIC_MONTH));
                case YEAR_OF_ERA:
                    return withYear((int) (this.year < 1 ? 1 - newValue : newValue));
                case YEAR:
                    return withYear((int) newValue);
                case ERA:
                    return (getLong(ERA) == newValue ? this : withYear(1 - this.year));
            }
            throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.adjustInto(this, newValue);
    }

    public TYearMonth withYear(int year) {

        YEAR.checkValidValue(year);
        return with(year, this.month);
    }

    public TYearMonth withMonth(int month) {

        MONTH_OF_YEAR.checkValidValue(month);
        return with(this.year, month);
    }

    @Override
    public TYearMonth plus(TTemporalAmount amount) {

        return (TYearMonth) amount.addTo(this);
    }

    @Override
    public TYearMonth plus(long amountToAdd, TTemporalUnit unit) {

        if (unit instanceof TChronoUnit) {
            switch ((TChronoUnit) unit) {
                case MONTHS:
                    return plusMonths(amountToAdd);
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

    public TYearMonth plusYears(long yearsToAdd) {

        if (yearsToAdd == 0) {
            return this;
        }
        int newYear = YEAR.checkValidIntValue(this.year + yearsToAdd); // safe overflow
        return with(newYear, this.month);
    }

    public TYearMonth plusMonths(long monthsToAdd) {

        if (monthsToAdd == 0) {
            return this;
        }
        long monthCount = this.year * 12L + (this.month - 1);
        long calcMonths = monthCount + monthsToAdd; // safe overflow
        int newYear = YEAR.checkValidIntValue(Math.floorDiv(calcMonths, 12));
        int newMonth = (int) Math.floorMod(calcMonths, 12) + 1;
        return with(newYear, newMonth);
    }

    @Override
    public TYearMonth minus(TTemporalAmount amount) {

        return (TYearMonth) amount.subtractFrom(this);
    }

    @Override
    public TYearMonth minus(long amountToSubtract, TTemporalUnit unit) {

        return (amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit)
                : plus(-amountToSubtract, unit));
    }

    public TYearMonth minusYears(long yearsToSubtract) {

        return (yearsToSubtract == Long.MIN_VALUE ? plusYears(Long.MAX_VALUE).plusYears(1)
                : plusYears(-yearsToSubtract));
    }

    public TYearMonth minusMonths(long monthsToSubtract) {

        return (monthsToSubtract == Long.MIN_VALUE ? plusMonths(Long.MAX_VALUE).plusMonths(1)
                : plusMonths(-monthsToSubtract));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TTemporalQuery<R> query) {

        if (query == TTemporalQueries.chronology()) {
            return (R) TIsoChronology.INSTANCE;
        } else if (query == TTemporalQueries.precision()) {
            return (R) MONTHS;
        } else if (query == TTemporalQueries.localDate() || query == TTemporalQueries.localTime()
                || query == TTemporalQueries.zone() || query == TTemporalQueries.zoneId()
                || query == TTemporalQueries.offset()) {
            return null;
        }
        return TTemporal.super.query(query);
    }

    @Override
    public TTemporal adjustInto(TTemporal temporal) {

        if (TChronology.from(temporal).equals(TIsoChronology.INSTANCE) == false) {
            throw new TDateTimeException("Adjustment only supported on ISO date-time");
        }
        return temporal.with(PROLEPTIC_MONTH, getProlepticMonth());
    }

    @Override
    public long until(TTemporal endExclusive, TTemporalUnit unit) {

        TYearMonth end = TYearMonth.from(endExclusive);
        if (unit instanceof TChronoUnit) {
            long monthsUntil = end.getProlepticMonth() - getProlepticMonth(); // no overflow
            switch ((TChronoUnit) unit) {
                case MONTHS:
                    return monthsUntil;
                case YEARS:
                    return monthsUntil / 12;
                case DECADES:
                    return monthsUntil / 120;
                case CENTURIES:
                    return monthsUntil / 1200;
                case MILLENNIA:
                    return monthsUntil / 12000;
                case ERAS:
                    return end.getLong(ERA) - getLong(ERA);
            }
            throw new TUnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
        return unit.between(this, end);
    }

    public TLocalDate atDay(int dayOfMonth) {

        return TLocalDate.of(this.year, this.month, dayOfMonth);
    }

    public TLocalDate atEndOfMonth() {

        return TLocalDate.of(this.year, this.month, lengthOfMonth());
    }

    @Override
    public int compareTo(TYearMonth other) {

        int cmp = (this.year - other.year);
        if (cmp == 0) {
            cmp = (this.month - other.month);
        }
        return cmp;
    }

    public boolean isAfter(TYearMonth other) {

        return compareTo(other) > 0;
    }

    public boolean isBefore(TYearMonth other) {

        return compareTo(other) < 0;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj instanceof TYearMonth) {
            TYearMonth other = (TYearMonth) obj;
            return this.year == other.year && this.month == other.month;
        }
        return false;
    }

    @Override
    public int hashCode() {

        return this.year ^ (this.month << 27);
    }

    @Override
    public String toString() {

        int absYear = Math.abs(this.year);
        StringBuilder buf = new StringBuilder(9);
        if (absYear < 1000) {
            if (this.year < 0) {
                buf.append(this.year - 10000).deleteCharAt(1);
            } else {
                buf.append(this.year + 10000).deleteCharAt(0);
            }
        } else {
            buf.append(this.year);
        }
        return buf.append(this.month < 10 ? "-0" : "-").append(this.month).toString();
    }

    public String format(TDateTimeFormatter formatter) {

        Objects.requireNonNull(formatter, "formatter");
        return formatter.format(this);
    }

}
