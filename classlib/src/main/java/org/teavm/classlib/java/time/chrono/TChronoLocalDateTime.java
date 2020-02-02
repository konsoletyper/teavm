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

import static org.teavm.classlib.java.time.temporal.TChronoField.EPOCH_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.NANOS;

import java.util.Comparator;
import java.util.Objects;

import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;

public abstract class TChronoLocalDateTime<D extends TChronoLocalDate>
        implements TTemporal, TTemporalAdjuster, Comparable<TChronoLocalDateTime<?>> {

    public static Comparator<TChronoLocalDateTime<?>> timeLineOrder() {

        return DATE_TIME_COMPARATOR;
    }

    private static final Comparator<TChronoLocalDateTime<?>> DATE_TIME_COMPARATOR = new Comparator<TChronoLocalDateTime<?>>() {
        @Override
        public int compare(TChronoLocalDateTime<?> datetime1, TChronoLocalDateTime<?> datetime2) {

            int cmp = Long.compare(datetime1.toLocalDate().toEpochDay(), datetime2.toLocalDate().toEpochDay());
            if (cmp == 0) {
                cmp = Long.compare(datetime1.toLocalTime().toNanoOfDay(), datetime2.toLocalTime().toNanoOfDay());
            }
            return cmp;
        }
    };

    public static TChronoLocalDateTime<?> from(TTemporalAccessor temporal) {

        Objects.requireNonNull(temporal, "temporal");
        if (temporal instanceof TChronoLocalDateTime) {
            return (TChronoLocalDateTime<?>) temporal;
        }
        TChronology chrono = temporal.query(TTemporalQueries.chronology());
        if (chrono == null) {
            throw new TDateTimeException("No TChronology found to create TChronoLocalDateTime: " + temporal.getClass());
        }
        return chrono.localDateTime(temporal);
    }

    public TChronology getChronology() {

        return toLocalDate().getChronology();
    }

    public abstract D toLocalDate();

    public abstract TLocalTime toLocalTime();

    @Override
    public TChronoLocalDateTime<D> with(TTemporalAdjuster adjuster) {

        return ((TAbstractChronology) getChronology()).ensureChronoLocalDateTime(TTemporal.super.with(adjuster));
    }

    @Override
    public abstract TChronoLocalDateTime<D> with(TTemporalField field, long newValue);

    @Override
    public TChronoLocalDateTime<D> plus(TTemporalAmount amount) {

        return ((TAbstractChronology) getChronology()).ensureChronoLocalDateTime(TTemporal.super.plus(amount));
    }

    @Override
    public abstract TChronoLocalDateTime<D> plus(long amountToAdd, TTemporalUnit unit);

    @Override
    public TChronoLocalDateTime<D> minus(TTemporalAmount amount) {

        return ((TAbstractChronology) getChronology()).ensureChronoLocalDateTime(TTemporal.super.minus(amount));
    }

    @Override
    public TChronoLocalDateTime<D> minus(long amountToSubtract, TTemporalUnit unit) {

        return ((TAbstractChronology) getChronology())
                .ensureChronoLocalDateTime(TTemporal.super.minus(amountToSubtract, unit));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TTemporalQuery<R> query) {

        if (query == TTemporalQueries.chronology()) {
            return (R) getChronology();
        } else if (query == TTemporalQueries.precision()) {
            return (R) NANOS;
        } else if (query == TTemporalQueries.localDate()) {
            return (R) TLocalDate.ofEpochDay(toLocalDate().toEpochDay());
        } else if (query == TTemporalQueries.localTime()) {
            return (R) toLocalTime();
        } else if (query == TTemporalQueries.zone() || query == TTemporalQueries.zoneId()
                || query == TTemporalQueries.offset()) {
            return null;
        }
        return TTemporal.super.query(query);
    }

    @Override
    public TTemporal adjustInto(TTemporal temporal) {

        return temporal.with(EPOCH_DAY, toLocalDate().toEpochDay()).with(NANO_OF_DAY, toLocalTime().toNanoOfDay());
    }

    public String format(TDateTimeFormatter formatter) {

        Objects.requireNonNull(formatter, "formatter");
        return formatter.format(this);
    }

    public abstract TChronoZonedDateTime<D> atZone(TZoneId zone);

    public TInstant toInstant(TZoneOffset offset) {

        return TInstant.ofEpochSecond(toEpochSecond(offset), toLocalTime().getNano());
    }

    public long toEpochSecond(TZoneOffset offset) {

        Objects.requireNonNull(offset, "offset");
        long epochDay = toLocalDate().toEpochDay();
        long secs = epochDay * 86400 + toLocalTime().toSecondOfDay();
        secs -= offset.getTotalSeconds();
        return secs;
    }

    @Override
    public int compareTo(TChronoLocalDateTime<?> other) {

        int cmp = toLocalDate().compareTo(other.toLocalDate());
        if (cmp == 0) {
            cmp = toLocalTime().compareTo(other.toLocalTime());
            if (cmp == 0) {
                cmp = getChronology().compareTo(other.getChronology());
            }
        }
        return cmp;
    }

    public boolean isAfter(TChronoLocalDateTime<?> other) {

        long thisEpDay = this.toLocalDate().toEpochDay();
        long otherEpDay = other.toLocalDate().toEpochDay();
        return thisEpDay > otherEpDay
                || (thisEpDay == otherEpDay && this.toLocalTime().toNanoOfDay() > other.toLocalTime().toNanoOfDay());
    }

    public boolean isBefore(TChronoLocalDateTime<?> other) {

        long thisEpDay = this.toLocalDate().toEpochDay();
        long otherEpDay = other.toLocalDate().toEpochDay();
        return thisEpDay < otherEpDay
                || (thisEpDay == otherEpDay && this.toLocalTime().toNanoOfDay() < other.toLocalTime().toNanoOfDay());
    }

    public boolean isEqual(TChronoLocalDateTime<?> other) {

        // Do the time check first, it is cheaper than computing EPOCH day.
        return this.toLocalTime().toNanoOfDay() == other.toLocalTime().toNanoOfDay()
                && this.toLocalDate().toEpochDay() == other.toLocalDate().toEpochDay();
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj instanceof TChronoLocalDateTime) {
            return compareTo((TChronoLocalDateTime<?>) obj) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {

        return toLocalDate().hashCode() ^ toLocalTime().hashCode();
    }

    @Override
    public String toString() {

        return toLocalDate().toString() + 'T' + toLocalTime().toString();
    }

}
