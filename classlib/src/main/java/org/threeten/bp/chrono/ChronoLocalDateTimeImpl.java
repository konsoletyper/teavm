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
package org.threeten.bp.chrono;

import static org.threeten.bp.temporal.ChronoField.EPOCH_DAY;
import java.io.Serializable;
import java.util.Objects;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.jdk8.Jdk8Methods;
import org.threeten.bp.temporal.ChronoField;
import org.threeten.bp.temporal.ChronoUnit;
import org.threeten.bp.temporal.Temporal;
import org.threeten.bp.temporal.TemporalAdjuster;
import org.threeten.bp.temporal.TemporalField;
import org.threeten.bp.temporal.TemporalUnit;
import org.threeten.bp.temporal.ValueRange;

/**
 * A date-time without a time-zone for the calendar neutral API.
 * <p>
 * {@code ChronoLocalDateTime} is an immutable date-time object that represents a date-time, often
 * viewed as year-month-day-hour-minute-second. This object can also access other
 * fields such as day-of-year, day-of-week and week-of-year.
 * <p>
 * This class stores all date and time fields, to a precision of nanoseconds.
 * It does not store or represent a time-zone. For example, the value
 * "2nd October 2007 at 13:45.30.123456789" can be stored in an {@code ChronoLocalDateTime}.
 *
 * <h3>Specification for implementors</h3>
 * This class is immutable and thread-safe.
 *
 * @param <D> the date type
 */
final class ChronoLocalDateTimeImpl<D extends ChronoLocalDate>
        extends ChronoLocalDateTime<D>
        implements Temporal, TemporalAdjuster, Serializable {

    /**
     * Hours per minute.
     */
    private static final int HOURS_PER_DAY = 24;
    /**
     * Minutes per hour.
     */
    private static final int MINUTES_PER_HOUR = 60;
    /**
     * Minutes per day.
     */
    private static final int MINUTES_PER_DAY = MINUTES_PER_HOUR * HOURS_PER_DAY;
    /**
     * Seconds per minute.
     */
    private static final int SECONDS_PER_MINUTE = 60;
    /**
     * Seconds per hour.
     */
    private static final int SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR;
    /**
     * Seconds per day.
     */
    private static final int SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY;
    /**
     * Milliseconds per day.
     */
    private static final long MILLIS_PER_DAY = SECONDS_PER_DAY * 1000L;
    /**
     * Microseconds per day.
     */
    private static final long MICROS_PER_DAY = SECONDS_PER_DAY * 1000000L;
    /**
     * Nanos per second.
     */
    private static final long NANOS_PER_SECOND = 1000000000L;
    /**
     * Nanos per minute.
     */
    private static final long NANOS_PER_MINUTE = NANOS_PER_SECOND * SECONDS_PER_MINUTE;
    /**
     * Nanos per hour.
     */
    private static final long NANOS_PER_HOUR = NANOS_PER_MINUTE * MINUTES_PER_HOUR;
    /**
     * Nanos per day.
     */
    private static final long NANOS_PER_DAY = NANOS_PER_HOUR * HOURS_PER_DAY;

    /**
     * The date part.
     */
    private final D date;
    /**
     * The time part.
     */
    private final LocalTime time;

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code ChronoLocalDateTime} from a date and time.
     *
     * @param date  the local date, not null
     * @param time  the local time, not null
     * @return the local date-time, not null
     */
    static <R extends ChronoLocalDate> ChronoLocalDateTimeImpl<R> of(R date, LocalTime time) {
        return new ChronoLocalDateTimeImpl<R>(date, time);
    }

    /**
     * Constructor.
     *
     * @param date  the date part of the date-time, not null
     * @param time  the time part of the date-time, not null
     */
    private ChronoLocalDateTimeImpl(D date, LocalTime time) {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(time, "time");
        this.date = date;
        this.time = time;
    }

    /**
     * Returns a copy of this date-time with the new date and time, checking
     * to see if a new object is in fact required.
     *
     * @param newDate  the date of the new date-time, not null
     * @param newTime  the time of the new date-time, not null
     * @return the date-time, not null
     */
    private ChronoLocalDateTimeImpl<D> with(Temporal newDate, LocalTime newTime) {
        if (date == newDate && time == newTime) {
            return this;
        }
        // Validate that the new DateTime is a ChronoLocalDate (and not something else)
        D cd = date.getChronology().ensureChronoLocalDate(newDate);
        return new ChronoLocalDateTimeImpl<D>(cd, newTime);
    }

    //-----------------------------------------------------------------------
    @Override
    public D toLocalDate() {
        return date;
    }

    @Override
    public LocalTime toLocalTime() {
        return time;
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return field.isDateBased() || field.isTimeBased();
        }
        return field != null && field.isSupportedBy(this);
    }

    @Override
    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit.isDateBased() || unit.isTimeBased();
        }
        return unit != null && unit.isSupportedBy(this);
    }

    @Override
    public ValueRange range(TemporalField field) {
        if (field instanceof ChronoField) {
            return field.isTimeBased() ? time.range(field) : date.range(field);
        }
        return field.rangeRefinedBy(this);
    }

    @Override
    public int get(TemporalField field) {
        if (field instanceof ChronoField) {
            return field.isTimeBased() ? time.get(field) : date.get(field);
        }
        return range(field).checkValidIntValue(getLong(field), field);
    }

    @Override
    public long getLong(TemporalField field) {
        if (field instanceof ChronoField) {
            return field.isTimeBased() ? time.getLong(field) : date.getLong(field);
        }
        return field.getFrom(this);
    }

    //-----------------------------------------------------------------------
    @Override
    public ChronoLocalDateTimeImpl<D> with(TemporalAdjuster adjuster) {
        if (adjuster instanceof ChronoLocalDate) {
            // The Chrono is checked in with(date,time)
            return with((ChronoLocalDate) adjuster, time);
        } else if (adjuster instanceof LocalTime) {
            return with(date, (LocalTime) adjuster);
        } else if (adjuster instanceof ChronoLocalDateTimeImpl) {
            return date.getChronology().ensureChronoLocalDateTime((ChronoLocalDateTimeImpl<?>) adjuster);
        }
        return date.getChronology().ensureChronoLocalDateTime((ChronoLocalDateTimeImpl<?>) adjuster.adjustInto(this));
    }

    @Override
    public ChronoLocalDateTimeImpl<D> with(TemporalField field, long newValue) {
        if (field instanceof ChronoField) {
            if (field.isTimeBased()) {
                return with(date, time.with(field, newValue));
            } else {
                return with(date.with(field, newValue), time);
            }
        }
        return date.getChronology().ensureChronoLocalDateTime(field.adjustInto(this, newValue));
    }

    //-----------------------------------------------------------------------
    @Override
    public ChronoLocalDateTimeImpl<D> plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            ChronoUnit f = (ChronoUnit) unit;
            switch (f) {
                case NANOS: return plusNanos(amountToAdd);
                case MICROS: return plusDays(amountToAdd / MICROS_PER_DAY)
                        .plusNanos((amountToAdd % MICROS_PER_DAY) * 1000);
                case MILLIS: return plusDays(amountToAdd / MILLIS_PER_DAY)
                        .plusNanos((amountToAdd % MILLIS_PER_DAY) * 1000000);
                case SECONDS: return plusSeconds(amountToAdd);
                case MINUTES: return plusMinutes(amountToAdd);
                case HOURS: return plusHours(amountToAdd);
                case HALF_DAYS:
                    // no overflow (256 is multiple of 2)
                    return plusDays(amountToAdd / 256).plusHours((amountToAdd % 256) * 12);
            }
            return with(date.plus(amountToAdd, unit), time);
        }
        return date.getChronology().ensureChronoLocalDateTime(unit.addTo(this, amountToAdd));
    }

    private ChronoLocalDateTimeImpl<D> plusDays(long days) {
        return with(date.plus(days, ChronoUnit.DAYS), time);
    }

    private ChronoLocalDateTimeImpl<D> plusHours(long hours) {
        return plusWithOverflow(date, hours, 0, 0, 0);
    }

    private ChronoLocalDateTimeImpl<D> plusMinutes(long minutes) {
        return plusWithOverflow(date, 0, minutes, 0, 0);
    }

    ChronoLocalDateTimeImpl<D> plusSeconds(long seconds) {
        return plusWithOverflow(date, 0, 0, seconds, 0);
    }

    private ChronoLocalDateTimeImpl<D> plusNanos(long nanos) {
        return plusWithOverflow(date, 0, 0, 0, nanos);
    }

    //-----------------------------------------------------------------------
    private ChronoLocalDateTimeImpl<D> plusWithOverflow(D newDate, long hours, long minutes, long seconds, long nanos) {
        // 9223372036854775808 long, 2147483648 int
        if ((hours | minutes | seconds | nanos) == 0) {
            return with(newDate, time);
        }
        long totDays = nanos / NANOS_PER_DAY               //   max/24*60*60*1B
                + seconds / SECONDS_PER_DAY                //   max/24*60*60
                + minutes / MINUTES_PER_DAY                //   max/24*60
                + hours / HOURS_PER_DAY;                   //   max/24
        long totNanos = nanos % NANOS_PER_DAY                      //   max  86400000000000
                + (seconds % SECONDS_PER_DAY) * NANOS_PER_SECOND   //   max  86400000000000
                + (minutes % MINUTES_PER_DAY) * NANOS_PER_MINUTE   //   max  86400000000000
                + (hours % HOURS_PER_DAY) * NANOS_PER_HOUR;        //   max  86400000000000
        long curNoD = time.toNanoOfDay();                          //   max  86400000000000
        totNanos = totNanos + curNoD;                              // total 432000000000000
        totDays += Jdk8Methods.floorDiv(totNanos, NANOS_PER_DAY);
        long newNoD = Jdk8Methods.floorMod(totNanos, NANOS_PER_DAY);
        LocalTime newTime = newNoD == curNoD ? time : LocalTime.ofNanoOfDay(newNoD);
        return with(newDate.plus(totDays, ChronoUnit.DAYS), newTime);
    }

    //-----------------------------------------------------------------------
    @Override
    public ChronoZonedDateTime<D> atZone(ZoneId zoneId) {
        return ChronoZonedDateTimeImpl.ofBest(this, zoneId, null);
    }

    //-----------------------------------------------------------------------
    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        @SuppressWarnings("unchecked")
        ChronoLocalDateTime<D> end = (ChronoLocalDateTime<D>) toLocalDate().getChronology().localDateTime(endExclusive);
        if (unit instanceof ChronoUnit) {
            ChronoUnit f = (ChronoUnit) unit;
            if (f.isTimeBased()) {
                long amount = end.getLong(EPOCH_DAY) - date.getLong(EPOCH_DAY);
                switch (f) {
                    case NANOS: amount = Jdk8Methods.safeMultiply(amount, NANOS_PER_DAY); break;
                    case MICROS: amount = Jdk8Methods.safeMultiply(amount, MICROS_PER_DAY); break;
                    case MILLIS: amount = Jdk8Methods.safeMultiply(amount, MILLIS_PER_DAY); break;
                    case SECONDS: amount = Jdk8Methods.safeMultiply(amount, SECONDS_PER_DAY); break;
                    case MINUTES: amount = Jdk8Methods.safeMultiply(amount, MINUTES_PER_DAY); break;
                    case HOURS: amount = Jdk8Methods.safeMultiply(amount, HOURS_PER_DAY); break;
                    case HALF_DAYS: amount = Jdk8Methods.safeMultiply(amount, 2); break;
                }
                return Jdk8Methods.safeAdd(amount, time.until(end.toLocalTime(), unit));
            }
            ChronoLocalDate endDate = end.toLocalDate();
            if (end.toLocalTime().isBefore(time)) {
                endDate = endDate.minus(1, ChronoUnit.DAYS);
            }
            return date.until(endDate, unit);
        }
        return unit.between(this, end);
    }
}
