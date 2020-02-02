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

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;
import org.teavm.classlib.java.time.temporal.TValueRange;

final class TChronoLocalDateTimeImpl<D extends TChronoLocalDate> extends TChronoLocalDateTime<D>
        implements TTemporal, TTemporalAdjuster, TSerializable {

    private static final int HOURS_PER_DAY = 24;

    private static final int MINUTES_PER_HOUR = 60;

    private static final int MINUTES_PER_DAY = MINUTES_PER_HOUR * HOURS_PER_DAY;

    private static final int SECONDS_PER_MINUTE = 60;

    private static final int SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR;

    private static final int SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY;

    private static final long MILLIS_PER_DAY = SECONDS_PER_DAY * 1000L;

    private static final long MICROS_PER_DAY = SECONDS_PER_DAY * 1000000L;

    private static final long NANOS_PER_SECOND = 1000000000L;

    private static final long NANOS_PER_MINUTE = NANOS_PER_SECOND * SECONDS_PER_MINUTE;

    private static final long NANOS_PER_HOUR = NANOS_PER_MINUTE * MINUTES_PER_HOUR;

    private static final long NANOS_PER_DAY = NANOS_PER_HOUR * HOURS_PER_DAY;

    private final D date;

    private final TLocalTime time;

    static <R extends TChronoLocalDate> TChronoLocalDateTimeImpl<R> of(R date, TLocalTime time) {

        return new TChronoLocalDateTimeImpl<>(date, time);
    }

    private TChronoLocalDateTimeImpl(D date, TLocalTime time) {

        TJdk8Methods.requireNonNull(date, "date");
        TJdk8Methods.requireNonNull(time, "time");
        this.date = date;
        this.time = time;
    }

    private TChronoLocalDateTimeImpl<D> with(TTemporal newDate, TLocalTime newTime) {

        if (this.date == newDate && this.time == newTime) {
            return this;
        }
        // Validate that the new DateTime is a TChronoLocalDate (and not something else)
        D cd = this.date.getChronology().ensureChronoLocalDate(newDate);
        return new TChronoLocalDateTimeImpl<D>(cd, newTime);
    }

    @Override
    public D toLocalDate() {

        return this.date;
    }

    @Override
    public TLocalTime toLocalTime() {

        return this.time;
    }

    @Override
    public boolean isSupported(TTemporalField field) {

        if (field instanceof TChronoField) {
            return field.isDateBased() || field.isTimeBased();
        }
        return field != null && field.isSupportedBy(this);
    }

    @Override
    public boolean isSupported(TTemporalUnit unit) {

        if (unit instanceof TChronoUnit) {
            return unit.isDateBased() || unit.isTimeBased();
        }
        return unit != null && unit.isSupportedBy(this);
    }

    @Override
    public TValueRange range(TTemporalField field) {

        if (field instanceof TChronoField) {
            return (field.isTimeBased() ? this.time.range(field) : this.date.range(field));
        }
        return field.rangeRefinedBy(this);
    }

    @Override
    public int get(TTemporalField field) {

        if (field instanceof TChronoField) {
            return (field.isTimeBased() ? this.time.get(field) : this.date.get(field));
        }
        return range(field).checkValidIntValue(getLong(field), field);
    }

    @Override
    public long getLong(TTemporalField field) {

        if (field instanceof TChronoField) {
            return (field.isTimeBased() ? this.time.getLong(field) : this.date.getLong(field));
        }
        return field.getFrom(this);
    }

    @Override
    public TChronoLocalDateTimeImpl<D> with(TTemporalAdjuster adjuster) {

        if (adjuster instanceof TChronoLocalDate) {
            // The Chrono is checked in with(date,time)
            return with((TChronoLocalDate) adjuster, this.time);
        } else if (adjuster instanceof TLocalTime) {
            return with(this.date, (TLocalTime) adjuster);
        } else if (adjuster instanceof TChronoLocalDateTimeImpl) {
            return this.date.getChronology().ensureChronoLocalDateTime((TChronoLocalDateTimeImpl<?>) adjuster);
        }
        return this.date.getChronology()
                .ensureChronoLocalDateTime((TChronoLocalDateTimeImpl<?>) adjuster.adjustInto(this));
    }

    @Override
    public TChronoLocalDateTimeImpl<D> with(TTemporalField field, long newValue) {

        if (field instanceof TChronoField) {
            if (field.isTimeBased()) {
                return with(this.date, this.time.with(field, newValue));
            } else {
                return with(this.date.with(field, newValue), this.time);
            }
        }
        return this.date.getChronology().ensureChronoLocalDateTime(field.adjustInto(this, newValue));
    }

    @Override
    public TChronoLocalDateTimeImpl<D> plus(long amountToAdd, TTemporalUnit unit) {

        if (unit instanceof TChronoUnit) {
            TChronoUnit f = (TChronoUnit) unit;
            switch (f) {
                case NANOS:
                    return plusNanos(amountToAdd);
                case MICROS:
                    return plusDays(amountToAdd / MICROS_PER_DAY).plusNanos((amountToAdd % MICROS_PER_DAY) * 1000);
                case MILLIS:
                    return plusDays(amountToAdd / MILLIS_PER_DAY).plusNanos((amountToAdd % MILLIS_PER_DAY) * 1000000);
                case SECONDS:
                    return plusSeconds(amountToAdd);
                case MINUTES:
                    return plusMinutes(amountToAdd);
                case HOURS:
                    return plusHours(amountToAdd);
                case HALF_DAYS:
                    return plusDays(amountToAdd / 256).plusHours((amountToAdd % 256) * 12); // no overflow (256 is
                                                                                            // multiple of 2)
            }
            return with(this.date.plus(amountToAdd, unit), this.time);
        }
        return this.date.getChronology().ensureChronoLocalDateTime(unit.addTo(this, amountToAdd));
    }

    private TChronoLocalDateTimeImpl<D> plusDays(long days) {

        return with(this.date.plus(days, TChronoUnit.DAYS), this.time);
    }

    private TChronoLocalDateTimeImpl<D> plusHours(long hours) {

        return plusWithOverflow(this.date, hours, 0, 0, 0);
    }

    private TChronoLocalDateTimeImpl<D> plusMinutes(long minutes) {

        return plusWithOverflow(this.date, 0, minutes, 0, 0);
    }

    TChronoLocalDateTimeImpl<D> plusSeconds(long seconds) {

        return plusWithOverflow(this.date, 0, 0, seconds, 0);
    }

    private TChronoLocalDateTimeImpl<D> plusNanos(long nanos) {

        return plusWithOverflow(this.date, 0, 0, 0, nanos);
    }

    private TChronoLocalDateTimeImpl<D> plusWithOverflow(D newDate, long hours, long minutes, long seconds,
            long nanos) {

        // 9223372036854775808 long, 2147483648 int
        if ((hours | minutes | seconds | nanos) == 0) {
            return with(newDate, this.time);
        }
        long totDays = nanos / NANOS_PER_DAY + // max/24*60*60*1B
                seconds / SECONDS_PER_DAY + // max/24*60*60
                minutes / MINUTES_PER_DAY + // max/24*60
                hours / HOURS_PER_DAY; // max/24
        long totNanos = nanos % NANOS_PER_DAY + // max 86400000000000
                (seconds % SECONDS_PER_DAY) * NANOS_PER_SECOND + // max 86400000000000
                (minutes % MINUTES_PER_DAY) * NANOS_PER_MINUTE + // max 86400000000000
                (hours % HOURS_PER_DAY) * NANOS_PER_HOUR; // max 86400000000000
        long curNoD = this.time.toNanoOfDay(); // max 86400000000000
        totNanos = totNanos + curNoD; // total 432000000000000
        totDays += TJdk8Methods.floorDiv(totNanos, NANOS_PER_DAY);
        long newNoD = TJdk8Methods.floorMod(totNanos, NANOS_PER_DAY);
        TLocalTime newTime = (newNoD == curNoD ? this.time : TLocalTime.ofNanoOfDay(newNoD));
        return with(newDate.plus(totDays, TChronoUnit.DAYS), newTime);
    }

    @Override
    public TChronoZonedDateTime<D> atZone(TZoneId zoneId) {

        return ChronoZonedDateTimeImpl.ofBest(this, zoneId, null);
    }

    @Override
    public long until(TTemporal endExclusive, TTemporalUnit unit) {

        @SuppressWarnings("unchecked")
        TChronoLocalDateTime<D> end = (TChronoLocalDateTime<D>) toLocalDate().getChronology()
                .localDateTime(endExclusive);
        if (unit instanceof TChronoUnit) {
            TChronoUnit f = (TChronoUnit) unit;
            if (f.isTimeBased()) {
                long amount = end.getLong(EPOCH_DAY) - this.date.getLong(EPOCH_DAY);
                switch (f) {
                    case NANOS:
                        amount = TJdk8Methods.safeMultiply(amount, NANOS_PER_DAY);
                        break;
                    case MICROS:
                        amount = TJdk8Methods.safeMultiply(amount, MICROS_PER_DAY);
                        break;
                    case MILLIS:
                        amount = TJdk8Methods.safeMultiply(amount, MILLIS_PER_DAY);
                        break;
                    case SECONDS:
                        amount = TJdk8Methods.safeMultiply(amount, SECONDS_PER_DAY);
                        break;
                    case MINUTES:
                        amount = TJdk8Methods.safeMultiply(amount, MINUTES_PER_DAY);
                        break;
                    case HOURS:
                        amount = TJdk8Methods.safeMultiply(amount, HOURS_PER_DAY);
                        break;
                    case HALF_DAYS:
                        amount = TJdk8Methods.safeMultiply(amount, 2);
                        break;
                }
                return TJdk8Methods.safeAdd(amount, this.time.until(end.toLocalTime(), unit));
            }
            TChronoLocalDate endDate = end.toLocalDate();
            if (end.toLocalTime().isBefore(this.time)) {
                endDate = endDate.minus(1, TChronoUnit.DAYS);
            }
            return this.date.until(endDate, unit);
        }
        return unit.between(this, end);
    }

}
