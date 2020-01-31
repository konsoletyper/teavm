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

import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.EPOCH_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.ERA;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR_OF_ERA;

import java.util.Comparator;

import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.jdk8.TDefaultInterfaceTemporal;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;
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

public abstract class TChronoLocalDate
        extends TDefaultInterfaceTemporal
        implements TTemporal, TTemporalAdjuster, Comparable<TChronoLocalDate> {

    public static Comparator<TChronoLocalDate> timeLineOrder() {
        return DATE_COMPARATOR;
    }
    private static final Comparator<TChronoLocalDate> DATE_COMPARATOR =
            new Comparator<TChronoLocalDate>() {
        @Override
        public int compare(TChronoLocalDate date1, TChronoLocalDate date2) {
            return TJdk8Methods.compareLongs(date1.toEpochDay(), date2.toEpochDay());
        }
    };

    //-----------------------------------------------------------------------
    public static TChronoLocalDate from(TTemporalAccessor temporal) {
        TJdk8Methods.requireNonNull(temporal, "temporal");
        if (temporal instanceof TChronoLocalDate) {
            return (TChronoLocalDate) temporal;
        }
        TChronology chrono = temporal.query(TTemporalQueries.chronology());
        if (chrono == null) {
            throw new TDateTimeException("No TChronology found to create TChronoLocalDate: " + temporal.getClass());
        }
        return chrono.date(temporal);
    }

    //-----------------------------------------------------------------------
    public abstract TChronology getChronology();

    public TEra getEra() {
        return getChronology().eraOf(get(ERA));
    }

    //-----------------------------------------------------------------------
    public boolean isLeapYear() {
        return getChronology().isLeapYear(getLong(YEAR));
    }

    public abstract int lengthOfMonth();

    public int lengthOfYear() {
        return (isLeapYear() ? 366 : 365);
    }

    @Override
    public boolean isSupported(TTemporalField field) {
        if (field instanceof TChronoField) {
            return field.isDateBased();
        }
        return field != null && field.isSupportedBy(this);
    }

    @Override
    public boolean isSupported(TTemporalUnit unit) {
        if (unit instanceof TChronoUnit) {
            return unit.isDateBased();
        }
        return unit != null && unit.isSupportedBy(this);
    }

    //-------------------------------------------------------------------------
    // override for covariant return type
    @Override
    public TChronoLocalDate with(TTemporalAdjuster adjuster) {
        return getChronology().ensureChronoLocalDate(super.with(adjuster));
    }

    @Override
    public abstract TChronoLocalDate with(TTemporalField field, long newValue);

    @Override
    public TChronoLocalDate plus(TTemporalAmount amount) {
        return getChronology().ensureChronoLocalDate(super.plus(amount));
    }

    @Override
    public abstract TChronoLocalDate plus(long amountToAdd, TTemporalUnit unit);

    @Override
    public TChronoLocalDate minus(TTemporalAmount amount) {
        return getChronology().ensureChronoLocalDate(super.minus(amount));
    }

    @Override
    public TChronoLocalDate minus(long amountToSubtract, TTemporalUnit unit) {
        return getChronology().ensureChronoLocalDate(super.minus(amountToSubtract, unit));
    }

    //-----------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TTemporalQuery<R> query) {
        if (query == TTemporalQueries.chronology()) {
            return (R) getChronology();
        } else if (query == TTemporalQueries.precision()) {
            return (R) TChronoUnit.DAYS;
        } else if (query == TTemporalQueries.localDate()) {
            return (R) TLocalDate.ofEpochDay(toEpochDay());
        } else if (query == TTemporalQueries.localTime() || query == TTemporalQueries.zone() ||
                query == TTemporalQueries.zoneId() || query == TTemporalQueries.offset()) {
            return null;
        }
        return super.query(query);
    }

    @Override
    public TTemporal adjustInto(TTemporal temporal) {
        return temporal.with(EPOCH_DAY, toEpochDay());
    }

    //-----------------------------------------------------------------------
    public abstract TChronoPeriod until(TChronoLocalDate endDateExclusive);

    public String format(TDateTimeFormatter formatter) {
        TJdk8Methods.requireNonNull(formatter, "formatter");
        return formatter.format(this);
    }

    //-----------------------------------------------------------------------
    public TChronoLocalDateTime<?> atTime(TLocalTime localTime) {
        return TChronoLocalDateTimeImpl.of(this, localTime);
    }

    //-----------------------------------------------------------------------
    public long toEpochDay() {
        return getLong(EPOCH_DAY);
    }

    //-----------------------------------------------------------------------
    @Override
    public int compareTo(TChronoLocalDate other) {
        int cmp = TJdk8Methods.compareLongs(toEpochDay(), other.toEpochDay());
        if (cmp == 0) {
            cmp = getChronology().compareTo(other.getChronology());
        }
        return cmp;
    }

    //-----------------------------------------------------------------------
    public boolean isAfter(TChronoLocalDate other) {
        return this.toEpochDay() > other.toEpochDay();
    }

    public boolean isBefore(TChronoLocalDate other) {
        return this.toEpochDay() < other.toEpochDay();
    }

    public boolean isEqual(TChronoLocalDate other) {
        return this.toEpochDay() == other.toEpochDay();
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TChronoLocalDate) {
            return compareTo((TChronoLocalDate) obj) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        long epDay = toEpochDay();
        return getChronology().hashCode() ^ ((int) (epDay ^ (epDay >>> 32)));
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
        // getLong() reduces chances of exceptions in toString()
        long yoe = getLong(YEAR_OF_ERA);
        long moy = getLong(MONTH_OF_YEAR);
        long dom = getLong(DAY_OF_MONTH);
        StringBuilder buf = new StringBuilder(30);
        buf.append(getChronology().toString())
                .append(" ")
                .append(getEra())
                .append(" ")
                .append(yoe)
                .append(moy < 10 ? "-0" : "-").append(moy)
                .append(dom < 10 ? "-0" : "-").append(dom);
        return buf.toString();
    }

}
