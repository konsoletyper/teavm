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
package org.threeten.bp.temporal;

import static org.threeten.bp.temporal.ChronoField.EPOCH_DAY;
import static org.threeten.bp.temporal.ChronoField.NANO_OF_DAY;
import static org.threeten.bp.temporal.ChronoField.OFFSET_SECONDS;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.chrono.Chronology;

/**
 * Common implementations of {@code TemporalQuery}.
 * <p>
 * This class provides common implementations of {@link TemporalQuery}.
 * These queries are primarily used as optimizations, allowing the internals
 * of other objects to be extracted effectively. Note that application code
 * can also use the {@code from(TemporalAccessor)} method on most temporal
 * objects as a method reference matching the query interface, such as
 * {@code LocalDate::from} and {@code ZoneId::from}.
 * <p>
 * There are two equivalent ways of using a {@code TemporalQuery}.
 * The first is to invoke the method on the interface directly.
 * The second is to use {@link TemporalAccessor#query(TemporalQuery)}:
 * <pre>
 *   // these two lines are equivalent, but the second approach is recommended
 *   dateTime = query.queryFrom(dateTime);
 *   dateTime = dateTime.query(query);
 * </pre>
 * It is recommended to use the second approach, {@code query(TemporalQuery)},
 * as it is a lot clearer to read in code.
 *
 * <h3>Specification for implementors</h3>
 * This is a thread-safe utility class.
 * All returned adjusters are immutable and thread-safe.
 */
public final class TemporalQueries {
    // note that it is vital that each method supplies a constant, not a
    // calculated value, as they will be checked for using ==
    // it is also vital that each constant is different (due to the == checking)

    /**
     * Private constructor since this is a utility class.
     */
    private TemporalQueries() {
    }

    //-----------------------------------------------------------------------
    // special constants should be used to extract information from a TemporalAccessor
    // that cannot be derived in other ways
    // Javadoc added here, so as to pretend they are more normal than they really are

    /**
     * A strict query for the {@code ZoneId}.
     * <p>
     * This queries a {@code TemporalAccessor} for the zone.
     * The zone is only returned if the date-time conceptually contains a {@code ZoneId}.
     * It will not be returned if the date-time only conceptually has an {@code ZoneOffset}.
     * Thus a {@link ZonedDateTime} will return the result of
     * {@code getZone()}, but an {@link OffsetDateTime} will
     * return null.
     * <p>
     * In most cases, applications should use {@link #ZONE} as this query is too strict.
     * <p>
     * The result from JDK classes implementing {@code TemporalAccessor} is as follows:<br>
     * {@code LocalDate} returns null<br>
     * {@code LocalTime} returns null<br>
     * {@code LocalDateTime} returns null<br>
     * {@code ZonedDateTime} returns the associated zone<br>
     * {@code OffsetTime} returns null<br>
     * {@code OffsetDateTime} returns null<br>
     * {@code ChronoLocalDate} returns null<br>
     * {@code ChronoLocalDateTime} returns null<br>
     * {@code ChronoZonedDateTime} returns the associated zone<br>
     * {@code Era} returns null<br>
     * {@code DayOfWeek} returns null<br>
     * {@code Month} returns null<br>
     * {@code Year} returns null<br>
     * {@code YearMonth} returns null<br>
     * {@code MonthDay} returns null<br>
     * {@code ZoneOffset} returns null<br>
     * {@code Instant} returns null<br>
     *
     * @return a query that can obtain the zone ID of a temporal, not null
     */
    public static TemporalQuery<ZoneId> zoneId() {
        return ZONE_ID;
    }
    static final TemporalQuery<ZoneId> ZONE_ID = new TemporalQuery<ZoneId>() {
        @Override
        public ZoneId queryFrom(TemporalAccessor temporal) {
            return temporal.query(this);
        }
    };

    /**
     * A query for the {@code Chronology}.
     * <p>
     * This queries a {@code TemporalAccessor} for the chronology.
     * If the target {@code TemporalAccessor} represents a date, or part of a date,
     * then it should return the chronology that the date is expressed in.
     * As a result of this definition, objects only representing time, such as
     * {@code LocalTime}, will return null.
     * <p>
     * The result from JDK classes implementing {@code TemporalAccessor} is as follows:<br>
     * {@code LocalDate} returns {@code IsoChronology.INSTANCE}<br>
     * {@code LocalTime} returns null (does not represent a date)<br>
     * {@code LocalDateTime} returns {@code IsoChronology.INSTANCE}<br>
     * {@code ZonedDateTime} returns {@code IsoChronology.INSTANCE}<br>
     * {@code OffsetTime} returns null (does not represent a date)<br>
     * {@code OffsetDateTime} returns {@code IsoChronology.INSTANCE}<br>
     * {@code ChronoLocalDate} returns the associated chronology<br>
     * {@code ChronoLocalDateTime} returns the associated chronology<br>
     * {@code ChronoZonedDateTime} returns the associated chronology<br>
     * {@code Era} returns the associated chronology<br>
     * {@code DayOfWeek} returns null (shared across chronologies)<br>
     * {@code Month} returns {@code IsoChronology.INSTANCE}<br>
     * {@code Year} returns {@code IsoChronology.INSTANCE}<br>
     * {@code YearMonth} returns {@code IsoChronology.INSTANCE}<br>
     * {@code MonthDay} returns null {@code IsoChronology.INSTANCE}<br>
     * {@code ZoneOffset} returns null (does not represent a date)<br>
     * {@code Instant} returns null (does not represent a date)<br>
     * <p>
     * The method {@link Chronology#from(TemporalAccessor)} can be used as a
     * {@code TemporalQuery} via a method reference, {@code Chrono::from}.
     * That method is equivalent to this query, except that it throws an
     * exception if a chronology cannot be obtained.
     *
     * @return a query that can obtain the chronology of a temporal, not null
     */
    public static TemporalQuery<Chronology> chronology() {
        return CHRONO;
    }
    static final TemporalQuery<Chronology> CHRONO = new TemporalQuery<Chronology>() {
        @Override
        public Chronology queryFrom(TemporalAccessor temporal) {
            return temporal.query(this);
        }
    };

    /**
     * A query for the smallest supported unit.
     * <p>
     * This queries a {@code TemporalAccessor} for the time precision.
     * If the target {@code TemporalAccessor} represents a consistent or complete date-time,
     * date or time then this must return the smallest precision actually supported.
     * Note that fields such as {@code NANO_OF_DAY} and {@code NANO_OF_SECOND}
     * are defined to always return ignoring the precision, thus this is the only
     * way to find the actual smallest supported unit.
     * For example, were {@code GregorianCalendar} to implement {@code TemporalAccessor}
     * it would return a precision of {@code MILLIS}.
     * <p>
     * The result from JDK classes implementing {@code TemporalAccessor} is as follows:<br>
     * {@code LocalDate} returns {@code DAYS}<br>
     * {@code LocalTime} returns {@code NANOS}<br>
     * {@code LocalDateTime} returns {@code NANOS}<br>
     * {@code ZonedDateTime} returns {@code NANOS}<br>
     * {@code OffsetTime} returns {@code NANOS}<br>
     * {@code OffsetDateTime} returns {@code NANOS}<br>
     * {@code ChronoLocalDate} returns {@code DAYS}<br>
     * {@code ChronoLocalDateTime} returns {@code NANOS}<br>
     * {@code ChronoZonedDateTime} returns {@code NANOS}<br>
     * {@code Era} returns {@code ERAS}<br>
     * {@code DayOfWeek} returns {@code DAYS}<br>
     * {@code Month} returns {@code MONTHS}<br>
     * {@code Year} returns {@code YEARS}<br>
     * {@code YearMonth} returns {@code MONTHS}<br>
     * {@code MonthDay} returns null (does not represent a complete date or time)<br>
     * {@code ZoneOffset} returns null (does not represent a date or time)<br>
     * {@code Instant} returns {@code NANOS}<br>
     *
     * @return a query that can obtain the precision of a temporal, not null
     */
    public static TemporalQuery<TemporalUnit> precision() {
        return PRECISION;
    }
    static final TemporalQuery<TemporalUnit> PRECISION = new TemporalQuery<TemporalUnit>() {
        @Override
        public TemporalUnit queryFrom(TemporalAccessor temporal) {
            return temporal.query(this);
        }
    };

    //-----------------------------------------------------------------------
    // non-special constants are standard queries that derive information from other information
    /**
     * A lenient query for the {@code ZoneId}, falling back to the {@code ZoneOffset}.
     * <p>
     * This queries a {@code TemporalAccessor} for the zone.
     * It first tries to obtain the zone, using {@link #zoneId()}.
     * If that is not found it tries to obtain the {@link #offset()}.
     * <p>
     * In most cases, applications should use this query rather than {@code #zoneId()}.
     * <p>
     * This query examines the {@link ChronoField#OFFSET_SECONDS offset-seconds}
     * field and uses it to create a {@code ZoneOffset}.
     * <p>
     * The method {@link ZoneId#from(TemporalAccessor)} can be used as a
     * {@code TemporalQuery} via a method reference, {@code ZoneId::from}.
     * That method is equivalent to this query, except that it throws an
     * exception if a zone cannot be obtained.
     *
     * @return a query that can obtain the zone ID or offset of a temporal, not null
     */
    public static TemporalQuery<ZoneId> zone() {
        return ZONE;
    }
    static final TemporalQuery<ZoneId> ZONE = new TemporalQuery<ZoneId>() {
        @Override
        public ZoneId queryFrom(TemporalAccessor temporal) {
            ZoneId zone = temporal.query(ZONE_ID);
            return zone != null ? zone : temporal.query(OFFSET);
        }
    };

    /**
     * A query for {@code ZoneOffset} returning null if not found.
     * <p>
     * This returns a {@code TemporalQuery} that can be used to query a temporal
     * object for the offset. The query will return null if the temporal
     * object cannot supply an offset.
     * <p>
     * The query implementation examines the {@link ChronoField#OFFSET_SECONDS OFFSET_SECONDS}
     * field and uses it to create a {@code ZoneOffset}.
     *
     * @return a query that can obtain the offset of a temporal, not null
     */
    public static TemporalQuery<ZoneOffset> offset() {
        return OFFSET;
    }
    static final TemporalQuery<ZoneOffset> OFFSET = temporal -> {
        if (temporal.isSupported(OFFSET_SECONDS)) {
            return ZoneOffset.ofTotalSeconds(temporal.get(OFFSET_SECONDS));
        }
        return null;
    };

    /**
     * A query for {@code LocalDate} returning null if not found.
     * <p>
     * This returns a {@code TemporalQuery} that can be used to query a temporal
     * object for the local date. The query will return null if the temporal
     * object cannot supply a local date.
     * <p>
     * The query implementation examines the {@link ChronoField#EPOCH_DAY EPOCH_DAY}
     * field and uses it to create a {@code LocalDate}.
     *
     * @return a query that can obtain the date of a temporal, not null
     */
    public static TemporalQuery<LocalDate> localDate() {
        return LOCAL_DATE;
    }
    static final TemporalQuery<LocalDate> LOCAL_DATE = temporal -> {
        if (temporal.isSupported(EPOCH_DAY)) {
            return LocalDate.ofEpochDay(temporal.getLong(EPOCH_DAY));
        }
        return null;
    };

    /**
     * A query for {@code LocalTime} returning null if not found.
     * <p>
     * This returns a {@code TemporalQuery} that can be used to query a temporal
     * object for the local time. The query will return null if the temporal
     * object cannot supply a local time.
     * <p>
     * The query implementation examines the {@link ChronoField#NANO_OF_DAY NANO_OF_DAY}
     * field and uses it to create a {@code LocalTime}.
     *
     * @return a query that can obtain the date of a temporal, not null
     */
    public static TemporalQuery<LocalTime> localTime() {
        return LOCAL_TIME;
    }
    static final TemporalQuery<LocalTime> LOCAL_TIME = temporal -> {
        if (temporal.isSupported(NANO_OF_DAY)) {
            return LocalTime.ofNanoOfDay(temporal.getLong(NANO_OF_DAY));
        }
        return null;
    };

}
