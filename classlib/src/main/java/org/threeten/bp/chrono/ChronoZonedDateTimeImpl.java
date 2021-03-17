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

import static org.threeten.bp.temporal.ChronoUnit.SECONDS;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.temporal.ChronoField;
import org.threeten.bp.temporal.ChronoUnit;
import org.threeten.bp.temporal.Temporal;
import org.threeten.bp.temporal.TemporalField;
import org.threeten.bp.temporal.TemporalUnit;
import org.threeten.bp.zone.ZoneOffsetTransition;
import org.threeten.bp.zone.ZoneRules;

/**
 * A date-time with a time-zone in the calendar neutral API.
 * <p>
 * {@code ZoneChronoDateTime} is an immutable representation of a date-time with a time-zone.
 * This class stores all date and time fields, to a precision of nanoseconds,
 * as well as a time-zone and zone offset.
 * <p>
 * The purpose of storing the time-zone is to distinguish the ambiguous case where
 * the local time-line overlaps, typically as a result of the end of daylight time.
 * Information about the local-time can be obtained using methods on the time-zone.
 *
 * <h3>Specification for implementors</h3>
 * This class is immutable and thread-safe.
 *
 * @param <D> the date type
 */
final class ChronoZonedDateTimeImpl<D extends ChronoLocalDate>
        extends ChronoZonedDateTime<D>
        implements Serializable {

    /**
     * The local date-time.
     */
    private final ChronoLocalDateTimeImpl<D> dateTime;
    /**
     * The zone offset.
     */
    private final ZoneOffset offset;
    /**
     * The zone ID.
     */
    private final ZoneId zone;

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance from a local date-time using the preferred offset if possible.
     *
     * @param localDateTime  the local date-time, not null
     * @param zone  the zone identifier, not null
     * @param preferredOffset  the zone offset, null if no preference
     * @return the zoned date-time, not null
     */
    static <R extends ChronoLocalDate> ChronoZonedDateTime<R> ofBest(
            ChronoLocalDateTimeImpl<R> localDateTime, ZoneId zone, ZoneOffset preferredOffset) {
        Objects.requireNonNull(localDateTime, "localDateTime");
        Objects.requireNonNull(zone, "zone");
        if (zone instanceof ZoneOffset) {
            return new ChronoZonedDateTimeImpl<>(localDateTime, (ZoneOffset) zone, zone);
        }
        ZoneRules rules = zone.getRules();
        LocalDateTime isoLDT = LocalDateTime.from(localDateTime);
        List<ZoneOffset> validOffsets = rules.getValidOffsets(isoLDT);
        ZoneOffset offset;
        if (validOffsets.size() == 1) {
            offset = validOffsets.get(0);
        } else if (validOffsets.size() == 0) {
            ZoneOffsetTransition trans = rules.getTransition(isoLDT);
            localDateTime = localDateTime.plusSeconds(trans.getDuration().getSeconds());
            offset = trans.getOffsetAfter();
        } else {
            if (preferredOffset != null && validOffsets.contains(preferredOffset)) {
                offset = preferredOffset;
            } else {
                offset = validOffsets.get(0);
            }
        }
        Objects.requireNonNull(offset, "offset");  // protect against bad ZoneRules
        return new ChronoZonedDateTimeImpl<>(localDateTime, offset, zone);
    }

    /**
     * Obtains an instance from an instant using the specified time-zone.
     *
     * @param chrono  the chronology, not null
     * @param instant  the instant, not null
     * @param zone  the zone identifier, not null
     * @return the zoned date-time, not null
     */
    static <R extends ChronoLocalDate> ChronoZonedDateTimeImpl<R> ofInstant(Chronology chrono, Instant instant,
            ZoneId zone) {
        ZoneRules rules = zone.getRules();
        ZoneOffset offset = rules.getOffset(instant);
        Objects.requireNonNull(offset, "offset");  // protect against bad ZoneRules
        LocalDateTime ldt = LocalDateTime.ofEpochSecond(instant.getEpochSecond(), instant.getNano(), offset);
        @SuppressWarnings("unchecked")
        ChronoLocalDateTimeImpl<R> cldt = (ChronoLocalDateTimeImpl<R>) chrono.localDateTime(ldt);
        return new ChronoZonedDateTimeImpl<>(cldt, offset, zone);
    }

    /**
     * Obtains an instance from an {@code Instant}.
     *
     * @param instant  the instant to create the date-time from, not null
     * @param zone  the time-zone to use, validated not null
     * @return the zoned date-time, validated not null
     */
    private ChronoZonedDateTimeImpl<D> create(Instant instant, ZoneId zone) {
        return ofInstant(toLocalDate().getChronology(), instant, zone);
    }

    //-----------------------------------------------------------------------
    /**
     * Constructor.
     *
     * @param dateTime  the date-time, not null
     * @param offset  the zone offset, not null
     * @param zone  the zone ID, not null
     */
    private ChronoZonedDateTimeImpl(ChronoLocalDateTimeImpl<D> dateTime, ZoneOffset offset, ZoneId zone) {
        this.dateTime = Objects.requireNonNull(dateTime, "dateTime");
        this.offset = Objects.requireNonNull(offset, "offset");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit.isDateBased() || unit.isTimeBased();
        }
        return unit != null && unit.isSupportedBy(this);
    }

    @Override
    public ZoneOffset getOffset() {
        return offset;
    }

    @Override
    public ChronoZonedDateTime<D> withEarlierOffsetAtOverlap() {
        ZoneOffsetTransition trans = getZone().getRules().getTransition(LocalDateTime.from(this));
        if (trans != null && trans.isOverlap()) {
            ZoneOffset earlierOffset = trans.getOffsetBefore();
            if (!earlierOffset.equals(offset)) {
                return new ChronoZonedDateTimeImpl<>(dateTime, earlierOffset, zone);
            }
        }
        return this;
    }

    @Override
    public ChronoZonedDateTime<D> withLaterOffsetAtOverlap() {
        ZoneOffsetTransition trans = getZone().getRules().getTransition(LocalDateTime.from(this));
        if (trans != null) {
            ZoneOffset offset = trans.getOffsetAfter();
            if (!offset.equals(getOffset())) {
                return new ChronoZonedDateTimeImpl<>(dateTime, offset, zone);
            }
        }
        return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public ChronoLocalDateTime<D> toLocalDateTime() {
        return dateTime;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public ChronoZonedDateTime<D> withZoneSameLocal(ZoneId zone) {
        return ofBest(dateTime, zone, offset);
    }

    @Override
    public ChronoZonedDateTime<D> withZoneSameInstant(ZoneId zone) {
        Objects.requireNonNull(zone, "zone");
        return this.zone.equals(zone) ? this : create(dateTime.toInstant(offset), zone);
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean isSupported(TemporalField field) {
        return field instanceof ChronoField || (field != null && field.isSupportedBy(this));
    }

    //-----------------------------------------------------------------------
    @Override
    public ChronoZonedDateTime<D> with(TemporalField field, long newValue) {
        if (field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            switch (f) {
                case INSTANT_SECONDS: return plus(newValue - toEpochSecond(), SECONDS);
                case OFFSET_SECONDS: {
                    ZoneOffset offset = ZoneOffset.ofTotalSeconds(f.checkValidIntValue(newValue));
                    return create(dateTime.toInstant(offset), zone);
                }
            }
            return ofBest(dateTime.with(field, newValue), zone, offset);
        }
        return toLocalDate().getChronology().ensureChronoZonedDateTime(field.adjustInto(this, newValue));
    }

    //-----------------------------------------------------------------------
    @Override
    public ChronoZonedDateTime<D> plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return with(dateTime.plus(amountToAdd, unit));
        }
        /// TODO: Generics replacement Risk!
        return toLocalDate().getChronology().ensureChronoZonedDateTime(unit.addTo(this, amountToAdd));
    }

    //-----------------------------------------------------------------------
    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        @SuppressWarnings("unchecked")
        ChronoZonedDateTime<D> end = (ChronoZonedDateTime<D>) toLocalDate().getChronology().zonedDateTime(endExclusive);
        if (unit instanceof ChronoUnit) {
            end = end.withZoneSameInstant(offset);
            return dateTime.until(end.toLocalDateTime(), unit);
        }
        return unit.between(this, end);
    }

    //-------------------------------------------------------------------------
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

    @Override
    public int hashCode() {
        return toLocalDateTime().hashCode() ^ getOffset().hashCode() ^ Integer.rotateLeft(getZone().hashCode(), 3);
    }

    @Override
    public String toString() {
        String str = toLocalDateTime().toString() + getOffset().toString();
        if (getOffset() != getZone()) {
            str += '[' + getZone().toString() + ']';
        }
        return str;
    }


}
