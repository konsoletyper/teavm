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

import static org.threeten.bp.temporal.ChronoField.INSTANT_SECONDS;
import static org.threeten.bp.temporal.ChronoField.OFFSET_SECONDS;
import static org.threeten.bp.temporal.ChronoUnit.NANOS;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.ToLongFunction;
import org.threeten.bp.DateTimeException;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.temporal.ChronoField;
import org.threeten.bp.temporal.Temporal;
import org.threeten.bp.temporal.TemporalAccessor;
import org.threeten.bp.temporal.TemporalAdjuster;
import org.threeten.bp.temporal.TemporalAmount;
import org.threeten.bp.temporal.TemporalField;
import org.threeten.bp.temporal.TemporalQueries;
import org.threeten.bp.temporal.TemporalQuery;
import org.threeten.bp.temporal.TemporalUnit;
import org.threeten.bp.temporal.UnsupportedTemporalTypeException;
import org.threeten.bp.temporal.ValueRange;

/**
 * A date-time with a time-zone in an arbitrary chronology,
 * intended for advanced globalization use cases.
 * <p>
 * <b>Most applications should declare method signatures, fields and variables
 * as {@link ZonedDateTime}, not this interface.</b>
 * <p>
 * A {@code ChronoZonedDateTime} is the abstract representation of an offset date-time
 * where the {@code Chronology chronology}, or calendar system, is pluggable.
 * The date-time is defined in terms of fields expressed by {@link TemporalField},
 * where most common implementations are defined in {@link ChronoField}.
 * The chronology defines how the calendar system operates and the meaning of
 * the standard fields.
 *
 * <h4>When to use this interface</h4>
 * The design of the API encourages the use of {@code ZonedDateTime} rather than this
 * interface, even in the case where the application needs to deal with multiple
 * calendar systems. The rationale for this is explored in detail in {@link ChronoLocalDate}.
 * <p>
 * Ensure that the discussion in {@code ChronoLocalDate} has been read and understood
 * before using this interface.
 *
 * <h3>Specification for implementors</h3>
 * This interface must be implemented with care to ensure other classes operate correctly.
 * All implementations that can be instantiated must be final, immutable and thread-safe.
 * Subclasses should be Serializable wherever possible.
 * <p>
 * In JDK 8, this is an interface with default methods.
 * Since there are no default methods in JDK 7, an abstract class is used.
 *
 * @param <D> the date type
 */
public abstract class ChronoZonedDateTime<D extends ChronoLocalDate>
        implements Temporal, Comparable<ChronoZonedDateTime<?>> {

    /**
     * Gets a comparator that compares {@code ChronoZonedDateTime} in
     * time-line order ignoring the chronology.
     * <p>
     * This comparator differs from the comparison in {@link #compareTo} in that it
     * only compares the underlying instant and not the chronology.
     * This allows dates in different calendar systems to be compared based
     * on the position of the date-time on the instant time-line.
     * The underlying comparison is equivalent to comparing the epoch-second and nano-of-second.
     *
     * @return a comparator that compares in time-line order ignoring the chronology
     * @see #isAfter
     * @see #isBefore
     * @see #isEqual
     */
    public static Comparator<ChronoZonedDateTime<?>> timeLineOrder() {
        return INSTANT_COMPARATOR;
    }
    private static final Comparator<ChronoZonedDateTime<?>> INSTANT_COMPARATOR =
            Comparator.comparingLong((ToLongFunction<ChronoZonedDateTime<?>>) ChronoZonedDateTime::toEpochSecond)
                    .thenComparingLong(datetime -> datetime.toLocalTime().toNanoOfDay());

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code ChronoZonedDateTime} from a temporal object.
     * <p>
     * This creates a zoned date-time based on the specified temporal.
     * A {@code TemporalAccessor} represents an arbitrary set of date and time information,
     * which this factory converts to an instance of {@code ChronoZonedDateTime}.
     * <p>
     * The conversion extracts and combines the chronology, date, time and zone
     * from the temporal object. The behavior is equivalent to using
     * {@link Chronology#zonedDateTime(TemporalAccessor)} with the extracted chronology.
     * Implementations are permitted to perform optimizations such as accessing
     * those fields that are equivalent to the relevant objects.
     * <p>
     * This method matches the signature of the functional interface {@link TemporalQuery}
     * allowing it to be used as a query via method reference, {@code ChronoZonedDateTime::from}.
     *
     * @param temporal  the temporal object to convert, not null
     * @return the date-time, not null
     * @throws DateTimeException if unable to convert to a {@code ChronoZonedDateTime}
     * @see Chronology#zonedDateTime(TemporalAccessor)
     */
    public static ChronoZonedDateTime<?> from(TemporalAccessor temporal) {
        Objects.requireNonNull(temporal, "temporal");
        if (temporal instanceof ChronoZonedDateTime) {
            return (ChronoZonedDateTime<?>) temporal;
        }
        Chronology chrono = temporal.query(TemporalQueries.chronology());
        if (chrono == null) {
            throw new DateTimeException("No Chronology found to create ChronoZonedDateTime: " + temporal.getClass());
        }
        return chrono.zonedDateTime(temporal);
    }

    //-------------------------------------------------------------------------
    @Override
    public ValueRange range(TemporalField field) {
        if (field instanceof ChronoField) {
            if (field == INSTANT_SECONDS || field == OFFSET_SECONDS) {
                return field.range();
            }
            return toLocalDateTime().range(field);
        }
        return field.rangeRefinedBy(this);
    }

    @Override
    public int get(TemporalField field) {
        if (field instanceof ChronoField) {
            switch ((ChronoField) field) {
                case INSTANT_SECONDS:
                    throw new UnsupportedTemporalTypeException("Field too large for an int: " + field);
                case OFFSET_SECONDS:
                    return getOffset().getTotalSeconds();
            }
            return toLocalDateTime().get(field);
        }
        return Temporal.super.get(field);
    }

    @Override
    public long getLong(TemporalField field) {
        if (field instanceof ChronoField) {
            switch ((ChronoField) field) {
                case INSTANT_SECONDS: return toEpochSecond();
                case OFFSET_SECONDS: return getOffset().getTotalSeconds();
            }
            return toLocalDateTime().getLong(field);
        }
        return field.getFrom(this);
    }

    //-------------------------------------------------------------------------
    /**
     * Gets the local date part of this date-time.
     * <p>
     * This returns a local date with the same year, month and day
     * as this date-time.
     *
     * @return the date part of this date-time, not null
     */
    public D toLocalDate() {
        return toLocalDateTime().toLocalDate();
    }

    /**
     * Gets the local time part of this date-time.
     * <p>
     * This returns a local time with the same hour, minute, second and
     * nanosecond as this date-time.
     *
     * @return the time part of this date-time, not null
     */
    public LocalTime toLocalTime() {
        return toLocalDateTime().toLocalTime();
    }

    /**
     * Gets the local date-time part of this date-time.
     * <p>
     * This returns a local date with the same year, month and day
     * as this date-time.
     *
     * @return the local date-time part of this date-time, not null
     */
    public abstract ChronoLocalDateTime<D> toLocalDateTime();

    /**
     * Gets the chronology of this date-time.
     * <p>
     * The {@code Chronology} represents the calendar system in use.
     * The era and other fields in {@link ChronoField} are defined by the chronology.
     *
     * @return the chronology, not null
     */
    public Chronology getChronology() {
        return toLocalDate().getChronology();
    }

    /**
     * Gets the zone offset, such as '+01:00'.
     * <p>
     * This is the offset of the local date-time from UTC/Greenwich.
     *
     * @return the zone offset, not null
     */
    public abstract ZoneOffset getOffset();

    /**
     * Gets the zone ID, such as 'Europe/Paris'.
     * <p>
     * This returns the stored time-zone id used to determine the time-zone rules.
     *
     * @return the zone ID, not null
     */
    public abstract ZoneId getZone();

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this date-time changing the zone offset to the
     * earlier of the two valid offsets at a local time-line overlap.
     * <p>
     * This method only has any effect when the local time-line overlaps, such as
     * at an autumn daylight savings cutover. In this scenario, there are two
     * valid offsets for the local date-time. Calling this method will return
     * a zoned date-time with the earlier of the two selected.
     * <p>
     * If this method is called when it is not an overlap, {@code this}
     * is returned.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @return a {@code ZoneChronoDateTime} based on this date-time with the earlier offset, not null
     * @throws DateTimeException if no rules can be found for the zone
     * @throws DateTimeException if no rules are valid for this date-time
     */
    public abstract ChronoZonedDateTime<D> withEarlierOffsetAtOverlap();

    /**
     * Returns a copy of this date-time changing the zone offset to the
     * later of the two valid offsets at a local time-line overlap.
     * <p>
     * This method only has any effect when the local time-line overlaps, such as
     * at an autumn daylight savings cutover. In this scenario, there are two
     * valid offsets for the local date-time. Calling this method will return
     * a zoned date-time with the later of the two selected.
     * <p>
     * If this method is called when it is not an overlap, {@code this}
     * is returned.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @return a {@code ChronoZonedDateTime} based on this date-time with the later offset, not null
     * @throws DateTimeException if no rules can be found for the zone
     * @throws DateTimeException if no rules are valid for this date-time
     */
    public abstract ChronoZonedDateTime<D> withLaterOffsetAtOverlap();

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this ZonedDateTime with a different time-zone,
     * retaining the local date-time if possible.
     * <p>
     * This method changes the time-zone and retains the local date-time.
     * The local date-time is only changed if it is invalid for the new zone.
     * <p>
     * To change the zone and adjust the local date-time,
     * use {@link #withZoneSameInstant(ZoneId)}.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param zoneId  the time-zone to change to, not null
     * @return a {@code ChronoZonedDateTime} based on this date-time with the requested zone, not null
     */
    public abstract ChronoZonedDateTime<D> withZoneSameLocal(ZoneId zoneId);

    /**
     * Returns a copy of this date-time with a different time-zone,
     * retaining the instant.
     * <p>
     * This method changes the time-zone and retains the instant.
     * This normally results in a change to the local date-time.
     * <p>
     * This method is based on retaining the same instant, thus gaps and overlaps
     * in the local time-line have no effect on the result.
     * <p>
     * To change the offset while keeping the local time,
     * use {@link #withZoneSameLocal(ZoneId)}.
     *
     * @param zoneId  the time-zone to change to, not null
     * @return a {@code ChronoZonedDateTime} based on this date-time with the requested zone, not null
     * @throws DateTimeException if the result exceeds the supported date range
     */
    public abstract ChronoZonedDateTime<D> withZoneSameInstant(ZoneId zoneId);

    //-------------------------------------------------------------------------
    // override for covariant return type
    @Override
    public ChronoZonedDateTime<D> with(TemporalAdjuster adjuster) {
        return toLocalDate().getChronology().ensureChronoZonedDateTime(Temporal.super.with(adjuster));
    }

    @Override
    public abstract ChronoZonedDateTime<D> with(TemporalField field, long newValue);

    @Override
    public ChronoZonedDateTime<D> plus(TemporalAmount amount) {
        return toLocalDate().getChronology().ensureChronoZonedDateTime(Temporal.super.plus(amount));
    }

    @Override
    public abstract ChronoZonedDateTime<D> plus(long amountToAdd, TemporalUnit unit);

    @Override
    public ChronoZonedDateTime<D> minus(TemporalAmount amount) {
        return toLocalDate().getChronology().ensureChronoZonedDateTime(Temporal.super.minus(amount));
    }

    @Override
    public ChronoZonedDateTime<D> minus(long amountToSubtract, TemporalUnit unit) {
        return toLocalDate().getChronology().ensureChronoZonedDateTime(Temporal.super.minus(amountToSubtract, unit));
    }

    //-----------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.zoneId() || query == TemporalQueries.zone()) {
            return (R) getZone();
        } else if (query == TemporalQueries.chronology()) {
            return (R) toLocalDate().getChronology();
        } else if (query == TemporalQueries.precision()) {
            return (R) NANOS;
        } else if (query == TemporalQueries.offset()) {
            return (R) getOffset();
        } else if (query == TemporalQueries.localDate()) {
            return (R) LocalDate.ofEpochDay(toLocalDate().toEpochDay());
        } else if (query == TemporalQueries.localTime()) {
            return (R) toLocalTime();
        }
        return Temporal.super.query(query);
    }

    /**
     * Outputs this date-time as a {@code String} using the formatter.
     *
     * @param formatter  the formatter to use, not null
     * @return the formatted date-time string, not null
     * @throws DateTimeException if an error occurs during printing
     */
    public String format(DateTimeFormatter formatter) {
        Objects.requireNonNull(formatter, "formatter");
        return formatter.format(this);
    }

    //-----------------------------------------------------------------------
    /**
     * Converts this date-time to an {@code Instant}.
     * <p>
     * This returns an {@code Instant} representing the same point on the
     * time-line as this date-time. The calculation combines the
     * {@linkplain #toLocalDateTime() local date-time} and
     * {@linkplain #getOffset() offset}.
     *
     * @return an {@code Instant} representing the same instant, not null
     */
    public Instant toInstant() {
        return Instant.ofEpochSecond(toEpochSecond(), toLocalTime().getNano());
    }

    /**
     * Converts this date-time to the number of seconds from the epoch
     * of 1970-01-01T00:00:00Z.
     * <p>
     * This uses the {@linkplain #toLocalDateTime() local date-time} and
     * {@linkplain #getOffset() offset} to calculate the epoch-second value,
     * which is the number of elapsed seconds from 1970-01-01T00:00:00Z.
     * Instants on the time-line after the epoch are positive, earlier are negative.
     *
     * @return the number of seconds from the epoch of 1970-01-01T00:00:00Z
     */
    public long toEpochSecond() {
        long epochDay = toLocalDate().toEpochDay();
        long secs = epochDay * 86400 + toLocalTime().toSecondOfDay();
        secs -= getOffset().getTotalSeconds();
        return secs;
    }

    //-----------------------------------------------------------------------
    /**
     * Compares this date-time to another date-time, including the chronology.
     * <p>
     * The comparison is based first on the instant, then on the local date-time,
     * then on the zone ID, then on the chronology.
     * It is "consistent with equals", as defined by {@link Comparable}.
     * <p>
     * If all the date-time objects being compared are in the same chronology, then the
     * additional chronology stage is not required.
     *
     * @param other  the other date-time to compare to, not null
     * @return the comparator value, negative if less, positive if greater
     */
    @Override
    public int compareTo(ChronoZonedDateTime<?> other) {
        int cmp = Long.compare(toEpochSecond(), other.toEpochSecond());
        if (cmp == 0) {
            cmp = toLocalTime().getNano() - other.toLocalTime().getNano();
            if (cmp == 0) {
                cmp = toLocalDateTime().compareTo(other.toLocalDateTime());
                if (cmp == 0) {
                    cmp = getZone().getId().compareTo(other.getZone().getId());
                    if (cmp == 0) {
                        cmp = toLocalDate().getChronology().compareTo(other.toLocalDate().getChronology());
                    }
                }
            }
        }
        return cmp;
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if the instant of this date-time is after that of the specified date-time.
     * <p>
     * This method differs from the comparison in {@link #compareTo} in that it
     * only compares the instant of the date-time. This is equivalent to using
     * {@code dateTime1.toInstant().isAfter(dateTime2.toInstant());}.
     *
     * @param other  the other date-time to compare to, not null
     * @return true if this is after the specified date-time
     */
    public boolean isAfter(ChronoZonedDateTime<?> other) {
        long thisEpochSec = toEpochSecond();
        long otherEpochSec = other.toEpochSecond();
        return thisEpochSec > otherEpochSec
                || (thisEpochSec == otherEpochSec && toLocalTime().getNano() > other.toLocalTime().getNano());
    }

    /**
     * Checks if the instant of this date-time is before that of the specified date-time.
     * <p>
     * This method differs from the comparison in {@link #compareTo} in that it
     * only compares the instant of the date-time. This is equivalent to using
     * {@code dateTime1.toInstant().isBefore(dateTime2.toInstant());}.
     *
     * @param other  the other date-time to compare to, not null
     * @return true if this point is before the specified date-time
     */
    public boolean isBefore(ChronoZonedDateTime<?> other) {
        long thisEpochSec = toEpochSecond();
        long otherEpochSec = other.toEpochSecond();
        return thisEpochSec < otherEpochSec
                || (thisEpochSec == otherEpochSec && toLocalTime().getNano() < other.toLocalTime().getNano());
    }

    /**
     * Checks if the instant of this date-time is equal to that of the specified date-time.
     * <p>
     * This method differs from the comparison in {@link #compareTo} and {@link #equals}
     * in that it only compares the instant of the date-time. This is equivalent to using
     * {@code dateTime1.toInstant().equals(dateTime2.toInstant());}.
     *
     * @param other  the other date-time to compare to, not null
     * @return true if the instant equals the instant of the specified date-time
     */
    public boolean isEqual(ChronoZonedDateTime<?> other) {
        return toEpochSecond() == other.toEpochSecond()
                && toLocalTime().getNano() == other.toLocalTime().getNano();
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if this date-time is equal to another date-time.
     * <p>
     * The comparison is based on the offset date-time and the zone.
     * To compare for the same instant on the time-line, use {@link #compareTo}.
     * Only objects of type {@code ChronoZoneDateTime} are compared, other types return false.
     *
     * @param obj  the object to check, null returns false
     * @return true if this is equal to the other date-time
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ChronoZonedDateTime) {
            return compareTo((ChronoZonedDateTime<?>) obj) == 0;
        }
        return false;
    }

    /**
     * A hash code for this date-time.
     *
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        return toLocalDateTime().hashCode() ^ getOffset().hashCode() ^ Integer.rotateLeft(getZone().hashCode(), 3);
    }

    //-----------------------------------------------------------------------
    /**
     * Outputs this date-time as a {@code String}.
     * <p>
     * The output will include the full zoned date-time and the chronology ID.
     *
     * @return a string representation of this date-time, not null
     */
    @Override
    public String toString() {
        String str = toLocalDateTime().toString() + getOffset().toString();
        if (getOffset() != getZone()) {
            str += '[' + getZone().toString() + ']';
        }
        return str;
    }

}
