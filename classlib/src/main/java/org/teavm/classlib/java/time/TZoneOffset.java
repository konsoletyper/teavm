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

import static org.teavm.classlib.java.time.temporal.TChronoField.OFFSET_SECONDS;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.teavm.classlib.java.lang.TComparable;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.time.temporal.TValueRange;
import org.teavm.classlib.java.time.zone.TZoneRules;

public final class TZoneOffset extends TZoneId
        implements TTemporalAccessor, TTemporalAdjuster, TComparable<TZoneOffset> {

    public static final TTemporalQuery<TZoneOffset> FROM = new TTemporalQuery<TZoneOffset>() {
        @Override
        public TZoneOffset queryFrom(TTemporalAccessor temporal) {

            return TZoneOffset.from(temporal);
        }
    };

    private static final ConcurrentMap<Integer, TZoneOffset> SECONDS_CACHE = new ConcurrentHashMap<Integer, TZoneOffset>(
            16, 0.75f, 4);

    private static final ConcurrentMap<String, TZoneOffset> ID_CACHE = new ConcurrentHashMap<String, TZoneOffset>(16,
            0.75f, 4);

    private static final int SECONDS_PER_HOUR = 60 * 60;

    private static final int SECONDS_PER_MINUTE = 60;

    private static final int MINUTES_PER_HOUR = 60;

    private static final int MAX_SECONDS = 18 * SECONDS_PER_HOUR;

    public static final TZoneOffset UTC = TZoneOffset.ofTotalSeconds(0);

    public static final TZoneOffset MIN = TZoneOffset.ofTotalSeconds(-MAX_SECONDS);

    public static final TZoneOffset MAX = TZoneOffset.ofTotalSeconds(MAX_SECONDS);

    private final int totalSeconds;

    private final transient String id;

    public static TZoneOffset of(String offsetId) {

        Objects.requireNonNull(offsetId, "offsetId");
        // "Z" is always in the cache
        TZoneOffset offset = ID_CACHE.get(offsetId);
        if (offset != null) {
            return offset;
        }

        // parse - +h, +hh, +hhmm, +hh:mm, +hhmmss, +hh:mm:ss
        final int hours, minutes, seconds;
        switch (offsetId.length()) {
            case 2:
                offsetId = offsetId.charAt(0) + "0" + offsetId.charAt(1); // fallthru
            case 3:
                hours = parseNumber(offsetId, 1, false);
                minutes = 0;
                seconds = 0;
                break;
            case 5:
                hours = parseNumber(offsetId, 1, false);
                minutes = parseNumber(offsetId, 3, false);
                seconds = 0;
                break;
            case 6:
                hours = parseNumber(offsetId, 1, false);
                minutes = parseNumber(offsetId, 4, true);
                seconds = 0;
                break;
            case 7:
                hours = parseNumber(offsetId, 1, false);
                minutes = parseNumber(offsetId, 3, false);
                seconds = parseNumber(offsetId, 5, false);
                break;
            case 9:
                hours = parseNumber(offsetId, 1, false);
                minutes = parseNumber(offsetId, 4, true);
                seconds = parseNumber(offsetId, 7, true);
                break;
            default:
                throw new TDateTimeException("Invalid ID for TZoneOffset, invalid format: " + offsetId);
        }
        char first = offsetId.charAt(0);
        if (first != '+' && first != '-') {
            throw new TDateTimeException("Invalid ID for TZoneOffset, plus/minus not found when expected: " + offsetId);
        }
        if (first == '-') {
            return ofHoursMinutesSeconds(-hours, -minutes, -seconds);
        } else {
            return ofHoursMinutesSeconds(hours, minutes, seconds);
        }
    }

    private static int parseNumber(CharSequence offsetId, int pos, boolean precededByColon) {

        if (precededByColon && offsetId.charAt(pos - 1) != ':') {
            throw new TDateTimeException("Invalid ID for TZoneOffset, colon not found when expected: " + offsetId);
        }
        char ch1 = offsetId.charAt(pos);
        char ch2 = offsetId.charAt(pos + 1);
        if (ch1 < '0' || ch1 > '9' || ch2 < '0' || ch2 > '9') {
            throw new TDateTimeException("Invalid ID for TZoneOffset, non numeric characters found: " + offsetId);
        }
        return (ch1 - 48) * 10 + (ch2 - 48);
    }

    public static TZoneOffset ofHours(int hours) {

        return ofHoursMinutesSeconds(hours, 0, 0);
    }

    public static TZoneOffset ofHoursMinutes(int hours, int minutes) {

        return ofHoursMinutesSeconds(hours, minutes, 0);
    }

    public static TZoneOffset ofHoursMinutesSeconds(int hours, int minutes, int seconds) {

        validate(hours, minutes, seconds);
        int totalSeconds = totalSeconds(hours, minutes, seconds);
        return ofTotalSeconds(totalSeconds);
    }

    public static TZoneOffset from(TTemporalAccessor temporal) {

        TZoneOffset offset = temporal.query(TTemporalQueries.offset());
        if (offset == null) {
            throw new TDateTimeException("Unable to obtain TZoneOffset from TTemporalAccessor: " + temporal + ", type "
                    + temporal.getClass().getName());
        }
        return offset;
    }

    private static void validate(int hours, int minutes, int seconds) {

        if (hours < -18 || hours > 18) {
            throw new TDateTimeException(
                    "Zone offset hours not in valid range: value " + hours + " is not in the range -18 to 18");
        }
        if (hours > 0) {
            if (minutes < 0 || seconds < 0) {
                throw new TDateTimeException(
                        "Zone offset minutes and seconds must be positive because hours is positive");
            }
        } else if (hours < 0) {
            if (minutes > 0 || seconds > 0) {
                throw new TDateTimeException(
                        "Zone offset minutes and seconds must be negative because hours is negative");
            }
        } else if ((minutes > 0 && seconds < 0) || (minutes < 0 && seconds > 0)) {
            throw new TDateTimeException("Zone offset minutes and seconds must have the same sign");
        }
        if (Math.abs(minutes) > 59) {
            throw new TDateTimeException("Zone offset minutes not in valid range: abs(value) " + Math.abs(minutes)
                    + " is not in the range 0 to 59");
        }
        if (Math.abs(seconds) > 59) {
            throw new TDateTimeException("Zone offset seconds not in valid range: abs(value) " + Math.abs(seconds)
                    + " is not in the range 0 to 59");
        }
        if (Math.abs(hours) == 18 && (Math.abs(minutes) > 0 || Math.abs(seconds) > 0)) {
            throw new TDateTimeException("Zone offset not in valid range: -18:00 to +18:00");
        }
    }

    private static int totalSeconds(int hours, int minutes, int seconds) {

        return hours * SECONDS_PER_HOUR + minutes * SECONDS_PER_MINUTE + seconds;
    }

    public static TZoneOffset ofTotalSeconds(int totalSeconds) {

        if (Math.abs(totalSeconds) > MAX_SECONDS) {
            throw new TDateTimeException("Zone offset not in valid range: -18:00 to +18:00");
        }
        if (totalSeconds % (15 * SECONDS_PER_MINUTE) == 0) {
            Integer totalSecs = totalSeconds;
            TZoneOffset result = SECONDS_CACHE.get(totalSecs);
            if (result == null) {
                result = new TZoneOffset(totalSeconds);
                SECONDS_CACHE.putIfAbsent(totalSecs, result);
                result = SECONDS_CACHE.get(totalSecs);
                ID_CACHE.putIfAbsent(result.getId(), result);
            }
            return result;
        } else {
            return new TZoneOffset(totalSeconds);
        }
    }

    private TZoneOffset(int totalSeconds) {

        super();
        this.totalSeconds = totalSeconds;
        this.id = buildId(totalSeconds);
    }

    private static String buildId(int totalSeconds) {

        if (totalSeconds == 0) {
            return "Z";
        } else {
            int absTotalSeconds = Math.abs(totalSeconds);
            StringBuilder buf = new StringBuilder();
            int absHours = absTotalSeconds / SECONDS_PER_HOUR;
            int absMinutes = (absTotalSeconds / SECONDS_PER_MINUTE) % MINUTES_PER_HOUR;
            buf.append(totalSeconds < 0 ? "-" : "+").append(absHours < 10 ? "0" : "").append(absHours)
                    .append(absMinutes < 10 ? ":0" : ":").append(absMinutes);
            int absSeconds = absTotalSeconds % SECONDS_PER_MINUTE;
            if (absSeconds != 0) {
                buf.append(absSeconds < 10 ? ":0" : ":").append(absSeconds);
            }
            return buf.toString();
        }
    }

    public int getTotalSeconds() {

        return this.totalSeconds;
    }

    @Override
    public String getId() {

        return this.id;
    }

    @Override
    public TZoneRules getRules() {

        return TZoneRules.of(this);
    }

    @Override
    public boolean isSupported(TTemporalField field) {

        if (field instanceof TChronoField) {
            return field == OFFSET_SECONDS;
        }
        return field != null && field.isSupportedBy(this);
    }

    @Override
    public TValueRange range(TTemporalField field) {

        if (field == OFFSET_SECONDS) {
            return field.range();
        } else if (field instanceof TChronoField) {
            throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.rangeRefinedBy(this);
    }

    @Override
    public int get(TTemporalField field) {

        if (field == OFFSET_SECONDS) {
            return this.totalSeconds;
        } else if (field instanceof TChronoField) {
            throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return range(field).checkValidIntValue(getLong(field), field);
    }

    @Override
    public long getLong(TTemporalField field) {

        if (field == OFFSET_SECONDS) {
            return this.totalSeconds;
        } else if (field instanceof TChronoField) {
            throw new TDateTimeException("Unsupported field: " + field);
        }
        return field.getFrom(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TTemporalQuery<R> query) {

        if (query == TTemporalQueries.offset() || query == TTemporalQueries.zone()) {
            return (R) this;
        } else if (query == TTemporalQueries.localDate() || query == TTemporalQueries.localTime()
                || query == TTemporalQueries.precision() || query == TTemporalQueries.chronology()
                || query == TTemporalQueries.zoneId()) {
            return null;
        }
        return query.queryFrom(this);
    }

    @Override
    public TTemporal adjustInto(TTemporal temporal) {

        return temporal.with(OFFSET_SECONDS, this.totalSeconds);
    }

    @Override
    public int compareTo(TZoneOffset other) {

        return other.totalSeconds - this.totalSeconds;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj instanceof TZoneOffset) {
            return this.totalSeconds == ((TZoneOffset) obj).totalSeconds;
        }
        return false;
    }

    @Override
    public int hashCode() {

        return this.totalSeconds;
    }

    @Override
    public String toString() {

        return this.id;
    }

}
