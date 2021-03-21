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
package org.threeten.bp;

import static org.threeten.bp.LocalTime.SECONDS_PER_DAY;
import static org.threeten.bp.LocalTime.SECONDS_PER_HOUR;
import static org.threeten.bp.LocalTime.SECONDS_PER_MINUTE;
import static org.threeten.bp.temporal.ChronoField.INSTANT_SECONDS;
import static org.threeten.bp.temporal.ChronoField.MICRO_OF_SECOND;
import static org.threeten.bp.temporal.ChronoField.MILLI_OF_SECOND;
import static org.threeten.bp.temporal.ChronoField.NANO_OF_SECOND;
import static org.threeten.bp.temporal.ChronoUnit.DAYS;
import static org.threeten.bp.temporal.ChronoUnit.NANOS;
import java.io.Serializable;
import java.util.Objects;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeFormatterBuilder;
import org.threeten.bp.format.DateTimeParseException;
import org.threeten.bp.format.DateTimePrintContext;
import org.threeten.bp.jdk8.Jdk8Methods;
import org.threeten.bp.temporal.ChronoField;
import org.threeten.bp.temporal.ChronoUnit;
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
 * An instantaneous point on the time-line.
 * <p>
 * This class models a single instantaneous point on the time-line.
 * This might be used to record event time-stamps in the application.
 * <p>
 * For practicality, the instant is stored with some constraints.
 * The measurable time-line is restricted to the number of seconds that can be held
 * in a {@code long}. This is greater than the current estimated age of the universe.
 * The instant is stored to nanosecond resolution.
 * <p>
 * The range of an instant requires the storage of a number larger than a {@code long}.
 * To achieve this, the class stores a {@code long} representing epoch-seconds and an
 * {@code int} representing nanosecond-of-second, which will always be between 0 and 999,999,999.
 * The epoch-seconds are measured from the standard Java epoch of {@code 1970-01-01T00:00:00Z}
 * where instants after the epoch have positive values, and earlier instants have negative values.
 * For both the epoch-second and nanosecond parts, a larger value is always later on the time-line
 * than a smaller value.
 *
 * <h3>Time-scale</h3>
 * <p>
 * The length of the solar day is the standard way that humans measure time.
 * This has traditionally been subdivided into 24 hours of 60 minutes of 60 seconds,
 * forming a 86400 second day.
 * <p>
 * Modern timekeeping is based on atomic clocks which precisely define an SI second
 * relative to the transitions of a Caesium atom. The length of an SI second was defined
 * to be very close to the 86400th fraction of a day.
 * <p>
 * Unfortunately, as the Earth rotates the length of the day varies.
 * In addition, over time the average length of the day is getting longer as the Earth slows.
 * As a result, the length of a solar day in 2012 is slightly longer than 86400 SI seconds.
 * The actual length of any given day and the amount by which the Earth is slowing
 * are not predictable and can only be determined by measurement.
 * The UT1 time-scale captures the accurate length of day, but is only available some
 * time after the day has completed.
 * <p>
 * The UTC time-scale is a standard approach to bundle up all the additional fractions
 * of a second from UT1 into whole seconds, known as <i>leap-seconds</i>.
 * A leap-second may be added or removed depending on the Earth's rotational changes.
 * As such, UTC permits a day to have 86399 SI seconds or 86401 SI seconds where
 * necessary in order to keep the day aligned with the Sun.
 * <p>
 * The modern UTC time-scale was introduced in 1972, introducing the concept of whole leap-seconds.
 * Between 1958 and 1972, the definition of UTC was complex, with minor sub-second leaps and
 * alterations to the length of the notional second. As of 2012, discussions are underway
 * to change the definition of UTC again, with the potential to remove leap seconds or
 * introduce other changes.
 * <p>
 * Given the complexity of accurate timekeeping described above, this Java API defines
 * its own time-scale with a simplification. The Java time-scale is defined as follows:
 * <p><ul>
 * <li>midday will always be exactly as defined by the agreed international civil time</li>
 * <li>other times during the day will be broadly in line with the agreed international civil time</li>
 * <li>the day will be divided into exactly 86400 subdivisions, referred to as "seconds"</li>
 * <li>the Java "second" may differ from an SI second</li>
 * </ul><p>
 * Agreed international civil time is the base time-scale agreed by international convention,
 * which in 2012 is UTC (with leap-seconds).
 * <p>
 * In 2012, the definition of the Java time-scale is the same as UTC for all days except
 * those where a leap-second occurs. On days where a leap-second does occur, the time-scale
 * effectively eliminates the leap-second, maintaining the fiction of 86400 seconds in the day.
 * <p>
 * The main benefit of always dividing the day into 86400 subdivisions is that it matches the
 * expectations of most users of the API. The alternative is to force every user to understand
 * what a leap second is and to force them to have special logic to handle them.
 * Most applications do not have access to a clock that is accurate enough to record leap-seconds.
 * Most applications also do not have a problem with a second being a very small amount longer or
 * shorter than a real SI second during a leap-second.
 * <p>
 * If an application does have access to an accurate clock that reports leap-seconds, then the
 * recommended technique to implement the Java time-scale is to use the UTC-SLS convention.
 * <a href="https://www.cl.cam.ac.uk/~mgk25/time/utc-sls/">UTC-SLS</a> effectively smoothes the
 * leap-second over the last 1000 seconds of the day, making each of the last 1000 "seconds"
 * 1/1000th longer or shorter than a real SI second.
 * <p>
 * One final problem is the definition of the agreed international civil time before the
 * introduction of modern UTC in 1972. This includes the Java epoch of {@code 1970-01-01}.
 * It is intended that instants before 1972 be interpreted based on the solar day divided
 * into 86400 subdivisions.
 * <p>
 * The Java time-scale is used by all date-time classes.
 * This includes {@code Instant}, {@code LocalDate}, {@code LocalTime}, {@code OffsetDateTime},
 * {@code ZonedDateTime} and {@code Duration}.
 *
 * <h3>Specification for implementors</h3>
 * This class is immutable and thread-safe.
 */
public final class Instant
        implements Temporal, TemporalAdjuster, Comparable<Instant>, Serializable, TemporalAccessor {

    /**
     * Constant for the 1970-01-01T00:00:00Z epoch instant.
     */
    public static final Instant EPOCH = new Instant(0, 0);
    /**
     * The minimum supported epoch second.
     */
    private static final long MIN_SECOND = -31557014167219200L;
    /**
     * The maximum supported epoch second.
     */
    private static final long MAX_SECOND = 31556889864403199L;
    /**
     * The minimum supported {@code Instant}, '-1000000000-01-01T00:00Z'.
     * This could be used by an application as a "far past" instant.
     * <p>
     * This is one year earlier than the minimum {@code LocalDateTime}.
     * This provides sufficient values to handle the range of {@code ZoneOffset}
     * which affect the instant in addition to the local date-time.
     * The value is also chosen such that the value of the year fits in
     * an {@code int}.
     */
    public static final Instant MIN = Instant.ofEpochSecond(MIN_SECOND, 0);
    /**
     * The maximum supported {@code Instant}, '1000000000-12-31T23:59:59.999999999Z'.
     * This could be used by an application as a "far future" instant.
     * <p>
     * This is one year later than the maximum {@code LocalDateTime}.
     * This provides sufficient values to handle the range of {@code ZoneOffset}
     * which affect the instant in addition to the local date-time.
     * The value is also chosen such that the value of the year fits in
     * an {@code int}.
     */
    public static final Instant MAX = Instant.ofEpochSecond(MAX_SECOND, 999999999);
    /**
     * Simulate JDK 8 method reference Instant::from.
     */
    public static final TemporalQuery<Instant> FROM = new TemporalQuery<Instant>() {
        @Override
        public Instant queryFrom(TemporalAccessor temporal) {
            return Instant.from(temporal);
        }
    };

    /**
     * Constant for nanos per second.
     */
    private static final int NANOS_PER_SECOND = 1000000000;
    /**
     * Constant for nanos per milli.
     */
    private static final int NANOS_PER_MILLI = 1000000;
    /**
     * Constant for millis per sec.
     */
    private static final long MILLIS_PER_SEC = 1000;

    /**
     * The number of seconds from the epoch of 1970-01-01T00:00:00Z.
     */
    private final long seconds;
    /**
     * The number of nanoseconds, later along the time-line, from the seconds field.
     * This is always positive, and never exceeds 999,999,999.
     */
    private final int nanos;

    //-----------------------------------------------------------------------
    /**
     * Obtains the current instant from the system clock.
     * <p>
     * This will query the {@link Clock#systemUTC() system UTC clock} to
     * obtain the current instant.
     * <p>
     * Using this method will prevent the ability to use an alternate time-source for
     * testing because the clock is effectively hard-coded.
     *
     * @return the current instant using the system clock, not null
     */
    public static Instant now() {
        return Clock.systemUTC().instant();
    }

    /**
     * Obtains the current instant from the specified clock.
     * <p>
     * This will query the specified clock to obtain the current time.
     * <p>
     * Using this method allows the use of an alternate clock for testing.
     * The alternate clock may be introduced using {@link Clock dependency injection}.
     *
     * @param clock  the clock to use, not null
     * @return the current instant, not null
     */
    public static Instant now(Clock clock) {
        Objects.requireNonNull(clock, "clock");
        return clock.instant();
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code Instant} using seconds from the
     * epoch of 1970-01-01T00:00:00Z.
     * <p>
     * The nanosecond field is set to zero.
     *
     * @param epochSecond  the number of seconds from 1970-01-01T00:00:00Z
     * @return an instant, not null
     * @throws DateTimeException if the instant exceeds the maximum or minimum instant
     */
    public static Instant ofEpochSecond(long epochSecond) {
        return create(epochSecond, 0);
    }

    /**
     * Obtains an instance of {@code Instant} using seconds from the
     * epoch of 1970-01-01T00:00:00Z and nanosecond fraction of second.
     * <p>
     * This method allows an arbitrary number of nanoseconds to be passed in.
     * The factory will alter the values of the second and nanosecond in order
     * to ensure that the stored nanosecond is in the range 0 to 999,999,999.
     * For example, the following will result in the exactly the same instant:
     * <pre>
     *  Instant.ofSeconds(3, 1);
     *  Instant.ofSeconds(4, -999_999_999);
     *  Instant.ofSeconds(2, 1000_000_001);
     * </pre>
     *
     * @param epochSecond  the number of seconds from 1970-01-01T00:00:00Z
     * @param nanoAdjustment  the nanosecond adjustment to the number of seconds, positive or negative
     * @return an instant, not null
     * @throws DateTimeException if the instant exceeds the maximum or minimum instant
     * @throws ArithmeticException if numeric overflow occurs
     */
    public static Instant ofEpochSecond(long epochSecond, long nanoAdjustment) {
        long secs = Jdk8Methods.safeAdd(epochSecond, Jdk8Methods.floorDiv(nanoAdjustment, NANOS_PER_SECOND));
        int nos = Jdk8Methods.floorMod(nanoAdjustment, NANOS_PER_SECOND);
        return create(secs, nos);
    }

    /**
     * Obtains an instance of {@code Instant} using milliseconds from the
     * epoch of 1970-01-01T00:00:00Z.
     * <p>
     * The seconds and nanoseconds are extracted from the specified milliseconds.
     *
     * @param epochMilli  the number of milliseconds from 1970-01-01T00:00:00Z
     * @return an instant, not null
     * @throws DateTimeException if the instant exceeds the maximum or minimum instant
     */
    public static Instant ofEpochMilli(long epochMilli) {
        long secs = Jdk8Methods.floorDiv(epochMilli, 1000);
        int mos = Jdk8Methods.floorMod(epochMilli, 1000);
        return create(secs, mos * NANOS_PER_MILLI);
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code Instant} from a temporal object.
     * <p>
     * A {@code TemporalAccessor} represents some form of date and time information.
     * This factory converts the arbitrary temporal object to an instance of {@code Instant}.
     * <p>
     * The conversion extracts the {@link ChronoField#INSTANT_SECONDS INSTANT_SECONDS}
     * and {@link ChronoField#NANO_OF_SECOND NANO_OF_SECOND} fields.
     * <p>
     * This method matches the signature of the functional interface {@link TemporalQuery}
     * allowing it to be used as a query via method reference, {@code Instant::from}.
     *
     * @param temporal  the temporal object to convert, not null
     * @return the instant, not null
     * @throws DateTimeException if unable to convert to an {@code Instant}
     */
    public static Instant from(TemporalAccessor temporal) {
        try {
            long instantSecs = temporal.getLong(INSTANT_SECONDS);
            int nanoOfSecond = temporal.get(NANO_OF_SECOND);
            return Instant.ofEpochSecond(instantSecs, nanoOfSecond);
        } catch (DateTimeException ex) {
            throw new DateTimeException("Unable to obtain Instant from TemporalAccessor: "
                    + temporal + ", type " + temporal.getClass().getName(), ex);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code Instant} from a text string such as
     * {@code 2007-12-23T10:15:30.000Z}.
     * <p>
     * The string must represent a valid instant in UTC and is parsed using
     * {@link DateTimeFormatter#ISO_INSTANT}.
     *
     * @param text  the text to parse, not null
     * @return the parsed instant, not null
     * @throws DateTimeParseException if the text cannot be parsed
     */
    public static Instant parse(final CharSequence text) {
        return DateTimeFormatter.ISO_INSTANT.parse(text, Instant.FROM);
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code Instant} using seconds and nanoseconds.
     *
     * @param seconds  the length of the duration in seconds
     * @param nanoOfSecond  the nano-of-second, from 0 to 999,999,999
     * @throws DateTimeException if the instant exceeds the maximum or minimum instant
     */
    private static Instant create(long seconds, int nanoOfSecond) {
        if ((seconds | nanoOfSecond) == 0) {
            return EPOCH;
        }
        if (seconds < MIN_SECOND || seconds > MAX_SECOND) {
            throw new DateTimeException("Instant exceeds minimum or maximum instant");
        }
        return new Instant(seconds, nanoOfSecond);
    }

    /**
     * Constructs an instance of {@code Instant} using seconds from the epoch of
     * 1970-01-01T00:00:00Z and nanosecond fraction of second.
     *
     * @param epochSecond  the number of seconds from 1970-01-01T00:00:00Z
     * @param nanos  the nanoseconds within the second, must be positive
     */
    private Instant(long epochSecond, int nanos) {
        super();
        this.seconds = epochSecond;
        this.nanos = nanos;
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if the specified field is supported.
     * <p>
     * This checks if this instant can be queried for the specified field.
     * If false, then calling the {@link #range(TemporalField) range} and
     * {@link #get(TemporalField) get} methods will throw an exception.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The supported fields are:
     * <ul>
     * <li>{@code NANO_OF_SECOND}
     * <li>{@code MICRO_OF_SECOND}
     * <li>{@code MILLI_OF_SECOND}
     * <li>{@code INSTANT_SECONDS}
     * </ul>
     * All other {@code ChronoField} instances will return false.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.isSupportedBy(TemporalAccessor)}
     * passing {@code this} as the argument.
     * Whether the field is supported is determined by the field.
     *
     * @param field  the field to check, null returns false
     * @return true if the field is supported on this instant, false if not
     */
    @Override
    public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return field == INSTANT_SECONDS || field == NANO_OF_SECOND || field == MICRO_OF_SECOND
                    || field == MILLI_OF_SECOND;
        }
        return field != null && field.isSupportedBy(this);
    }

    @Override
    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit.isTimeBased() || unit == DAYS;
        }
        return unit != null && unit.isSupportedBy(this);
    }

    /**
     * Gets the range of valid values for the specified field.
     * <p>
     * The range object expresses the minimum and maximum valid values for a field.
     * This instant is used to enhance the accuracy of the returned range.
     * If it is not possible to return the range, because the field is not supported
     * or for some other reason, an exception is thrown.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The {@link #isSupported(TemporalField) supported fields} will return
     * appropriate range instances.
     * All other {@code ChronoField} instances will throw a {@code DateTimeException}.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.rangeRefinedBy(TemporalAccessor)}
     * passing {@code this} as the argument.
     * Whether the range can be obtained is determined by the field.
     *
     * @param field  the field to query the range for, not null
     * @return the range of valid values for the field, not null
     * @throws DateTimeException if the range for the field cannot be obtained
     */
    @Override  // override for Javadoc
    public ValueRange range(TemporalField field) {
        return Temporal.super.range(field);
    }

    /**
     * Gets the value of the specified field from this instant as an {@code int}.
     * <p>
     * This queries this instant for the value for the specified field.
     * The returned value will always be within the valid range of values for the field.
     * If it is not possible to return the value, because the field is not supported
     * or for some other reason, an exception is thrown.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The {@link #isSupported(TemporalField) supported fields} will return valid
     * values based on this date-time, except {@code INSTANT_SECONDS} which is too
     * large to fit in an {@code int} and throws a {@code DateTimeException}.
     * All other {@code ChronoField} instances will throw a {@code DateTimeException}.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.getFrom(TemporalAccessor)}
     * passing {@code this} as the argument. Whether the value can be obtained,
     * and what the value represents, is determined by the field.
     *
     * @param field  the field to get, not null
     * @return the value for the field
     * @throws DateTimeException if a value for the field cannot be obtained
     * @throws ArithmeticException if numeric overflow occurs
     */
    @Override  // override for Javadoc and performance
    public int get(TemporalField field) {
        if (field instanceof ChronoField) {
            switch ((ChronoField) field) {
                case NANO_OF_SECOND: return nanos;
                case MICRO_OF_SECOND: return nanos / 1000;
                case MILLI_OF_SECOND: return nanos / NANOS_PER_MILLI;
            }
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return range(field).checkValidIntValue(field.getFrom(this), field);
    }

    /**
     * Gets the value of the specified field from this instant as a {@code long}.
     * <p>
     * This queries this instant for the value for the specified field.
     * If it is not possible to return the value, because the field is not supported
     * or for some other reason, an exception is thrown.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The {@link #isSupported(TemporalField) supported fields} will return valid
     * values based on this date-time.
     * All other {@code ChronoField} instances will throw a {@code DateTimeException}.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.getFrom(TemporalAccessor)}
     * passing {@code this} as the argument. Whether the value can be obtained,
     * and what the value represents, is determined by the field.
     *
     * @param field  the field to get, not null
     * @return the value for the field
     * @throws DateTimeException if a value for the field cannot be obtained
     * @throws ArithmeticException if numeric overflow occurs
     */
    @Override
    public long getLong(TemporalField field) {
        if (field instanceof ChronoField) {
            switch ((ChronoField) field) {
                case NANO_OF_SECOND: return nanos;
                case MICRO_OF_SECOND: return nanos / 1000;
                case MILLI_OF_SECOND: return nanos / NANOS_PER_MILLI;
                case INSTANT_SECONDS: return seconds;
            }
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.getFrom(this);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the number of seconds from the Java epoch of 1970-01-01T00:00:00Z.
     * <p>
     * The epoch second count is a simple incrementing count of seconds where
     * second 0 is 1970-01-01T00:00:00Z.
     * The nanosecond part of the day is returned by {@code getNanosOfSecond}.
     *
     * @return the seconds from the epoch of 1970-01-01T00:00:00Z
     */
    public long getEpochSecond() {
        return seconds;
    }

    /**
     * Gets the number of nanoseconds, later along the time-line, from the start
     * of the second.
     * <p>
     * The nanosecond-of-second value measures the total number of nanoseconds from
     * the second returned by {@code getEpochSecond}.
     *
     * @return the nanoseconds within the second, always positive, never exceeds 999,999,999
     */
    public int getNano() {
        return nanos;
    }

    //-------------------------------------------------------------------------
    /**
     * Returns an adjusted copy of this instant.
     * <p>
     * This returns a new {@code Instant}, based on this one, with the date adjusted.
     * The adjustment takes place using the specified adjuster strategy object.
     * Read the documentation of the adjuster to understand what adjustment will be made.
     * <p>
     * The result of this method is obtained by invoking the
     * {@link TemporalAdjuster#adjustInto(Temporal)} method on the
     * specified adjuster passing {@code this} as the argument.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param adjuster the adjuster to use, not null
     * @return an {@code Instant} based on {@code this} with the adjustment made, not null
     * @throws DateTimeException if the adjustment cannot be made
     * @throws ArithmeticException if numeric overflow occurs
     */
    @Override
    public Instant with(TemporalAdjuster adjuster) {
        return (Instant) adjuster.adjustInto(this);
    }

    /**
     * Returns a copy of this instant with the specified field set to a new value.
     * <p>
     * This returns a new {@code Instant}, based on this one, with the value
     * for the specified field changed.
     * If it is not possible to set the value, because the field is not supported or for
     * some other reason, an exception is thrown.
     * <p>
     * If the field is a {@link ChronoField} then the adjustment is implemented here.
     * The supported fields behave as follows:
     * <ul>
     * <li>{@code NANO_OF_SECOND} -
     *  Returns an {@code Instant} with the specified nano-of-second.
     *  The epoch-second will be unchanged.
     * <li>{@code MICRO_OF_SECOND} -
     *  Returns an {@code Instant} with the nano-of-second replaced by the specified
     *  micro-of-second multiplied by 1,000. The epoch-second will be unchanged.
     * <li>{@code MILLI_OF_SECOND} -
     *  Returns an {@code Instant} with the nano-of-second replaced by the specified
     *  milli-of-second multiplied by 1,000,000. The epoch-second will be unchanged.
     * <li>{@code INSTANT_SECONDS} -
     *  Returns an {@code Instant} with the specified epoch-second.
     *  The nano-of-second will be unchanged.
     * </ul>
     * <p>
     * In all cases, if the new value is outside the valid range of values for the field
     * then a {@code DateTimeException} will be thrown.
     * <p>
     * All other {@code ChronoField} instances will throw a {@code DateTimeException}.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.adjustInto(Temporal, long)}
     * passing {@code this} as the argument. In this case, the field determines
     * whether and how to adjust the instant.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param field  the field to set in the result, not null
     * @param newValue  the new value of the field in the result
     * @return an {@code Instant} based on {@code this} with the specified field set, not null
     * @throws DateTimeException if the field cannot be set
     * @throws ArithmeticException if numeric overflow occurs
     */
    @Override
    public Instant with(TemporalField field, long newValue) {
        if (field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            f.checkValidValue(newValue);
            switch (f) {
                case MILLI_OF_SECOND: {
                    int nval = (int) newValue * NANOS_PER_MILLI;
                    return nval != nanos ? create(seconds, nval) : this;
                }
                case MICRO_OF_SECOND: {
                    int nval = (int) newValue * 1000;
                    return nval != nanos ? create(seconds, nval) : this;
                }
                case NANO_OF_SECOND:
                    return newValue != nanos ? create(seconds, (int) newValue) : this;
                case INSTANT_SECONDS:
                    return newValue != seconds ? create(newValue, nanos) : this;
            }
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.adjustInto(this, newValue);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this {@code Instant} truncated to the specified unit.
     * <p>
     * Truncating the instant returns a copy of the original with fields
     * smaller than the specified unit set to zero.
     * The fields are calculated on the basis of using a UTC offset as seen
     * in {@code toString}.
     * For example, truncating with the {@link ChronoUnit#MINUTES MINUTES} unit will
     * round down to the nearest minute, setting the seconds and nanoseconds to zero.
     * <p>
     * The unit must have a {@linkplain TemporalUnit#getDuration() duration}
     * that divides into the length of a standard day without remainder.
     * This includes all supplied time units on {@link ChronoUnit} and
     * {@link ChronoUnit#DAYS DAYS}. Other units throw an exception.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param unit  the unit to truncate to, not null
     * @return an {@code Instant} based on this instant with the time truncated, not null
     * @throws DateTimeException if the unit is invalid for truncation
     */
    public Instant truncatedTo(TemporalUnit unit) {
        if (unit == ChronoUnit.NANOS) {
            return this;
        }
        Duration unitDur = unit.getDuration();
        if (unitDur.getSeconds() > LocalTime.SECONDS_PER_DAY) {
            throw new DateTimeException("Unit is too large to be used for truncation");
        }
        long dur = unitDur.toNanos();
        if ((LocalTime.NANOS_PER_DAY % dur) != 0) {
            throw new DateTimeException("Unit must divide into a standard day without remainder");
        }
        long nod = (seconds % LocalTime.SECONDS_PER_DAY) * LocalTime.NANOS_PER_SECOND + nanos;
        long result = Jdk8Methods.floorDiv(nod, dur) * dur;
        return plusNanos(result - nod);
    }

    //-----------------------------------------------------------------------
    /**
     * {@inheritDoc}
     * @throws DateTimeException {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    @Override
    public Instant plus(TemporalAmount amount) {
        return (Instant) amount.addTo(this);
    }

    /**
     * {@inheritDoc}
     * @throws DateTimeException {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    @Override
    public Instant plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            switch ((ChronoUnit) unit) {
                case NANOS: return plusNanos(amountToAdd);
                case MICROS: return plus(amountToAdd / 1000000, (amountToAdd % 1000000) * 1000);
                case MILLIS: return plusMillis(amountToAdd);
                case SECONDS: return plusSeconds(amountToAdd);
                case MINUTES: return plusSeconds(Jdk8Methods.safeMultiply(amountToAdd, SECONDS_PER_MINUTE));
                case HOURS: return plusSeconds(Jdk8Methods.safeMultiply(amountToAdd, SECONDS_PER_HOUR));
                case HALF_DAYS: return plusSeconds(Jdk8Methods.safeMultiply(amountToAdd, SECONDS_PER_DAY / 2));
                case DAYS: return plusSeconds(Jdk8Methods.safeMultiply(amountToAdd, SECONDS_PER_DAY));
            }
            throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
        return unit.addTo(this, amountToAdd);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this instant with the specified duration in seconds added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param secondsToAdd  the seconds to add, positive or negative
     * @return an {@code Instant} based on this instant with the specified seconds added, not null
     * @throws DateTimeException if the result exceeds the maximum or minimum instant
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Instant plusSeconds(long secondsToAdd) {
        return plus(secondsToAdd, 0);
    }

    /**
     * Returns a copy of this instant with the specified duration in milliseconds added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param millisToAdd  the milliseconds to add, positive or negative
     * @return an {@code Instant} based on this instant with the specified milliseconds added, not null
     * @throws DateTimeException if the result exceeds the maximum or minimum instant
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Instant plusMillis(long millisToAdd) {
        return plus(millisToAdd / 1000, (millisToAdd % 1000) * NANOS_PER_MILLI);
    }

    /**
     * Returns a copy of this instant with the specified duration in nanoseconds added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param nanosToAdd  the nanoseconds to add, positive or negative
     * @return an {@code Instant} based on this instant with the specified nanoseconds added, not null
     * @throws DateTimeException if the result exceeds the maximum or minimum instant
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Instant plusNanos(long nanosToAdd) {
        return plus(0, nanosToAdd);
    }

    /**
     * Returns a copy of this instant with the specified duration added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param secondsToAdd  the seconds to add, positive or negative
     * @param nanosToAdd  the nanos to add, positive or negative
     * @return an {@code Instant} based on this instant with the specified seconds added, not null
     * @throws DateTimeException if the result exceeds the maximum or minimum instant
     * @throws ArithmeticException if numeric overflow occurs
     */
    private Instant plus(long secondsToAdd, long nanosToAdd) {
        if ((secondsToAdd | nanosToAdd) == 0) {
            return this;
        }
        long epochSec = Jdk8Methods.safeAdd(seconds, secondsToAdd);
        epochSec = Jdk8Methods.safeAdd(epochSec, nanosToAdd / NANOS_PER_SECOND);
        nanosToAdd = nanosToAdd % NANOS_PER_SECOND;
        long nanoAdjustment = nanos + nanosToAdd;  // safe int+NANOS_PER_SECOND
        return ofEpochSecond(epochSec, nanoAdjustment);
    }

    //-----------------------------------------------------------------------
    /**
     * {@inheritDoc}
     * @throws DateTimeException {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    @Override
    public Instant minus(TemporalAmount amount) {
        return (Instant) amount.subtractFrom(this);
    }

    /**
     * {@inheritDoc}
     * @throws DateTimeException {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    @Override
    public Instant minus(long amountToSubtract, TemporalUnit unit) {
        return amountToSubtract == Long.MIN_VALUE
                ? plus(Long.MAX_VALUE, unit).plus(1, unit)
                : plus(-amountToSubtract, unit);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this instant with the specified duration in seconds subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param secondsToSubtract  the seconds to subtract, positive or negative
     * @return an {@code Instant} based on this instant with the specified seconds subtracted, not null
     * @throws DateTimeException if the result exceeds the maximum or minimum instant
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Instant minusSeconds(long secondsToSubtract) {
        if (secondsToSubtract == Long.MIN_VALUE) {
            return plusSeconds(Long.MAX_VALUE).plusSeconds(1);
        }
        return plusSeconds(-secondsToSubtract);
    }

    /**
     * Returns a copy of this instant with the specified duration in milliseconds subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param millisToSubtract  the milliseconds to subtract, positive or negative
     * @return an {@code Instant} based on this instant with the specified milliseconds subtracted, not null
     * @throws DateTimeException if the result exceeds the maximum or minimum instant
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Instant minusMillis(long millisToSubtract) {
        if (millisToSubtract == Long.MIN_VALUE) {
            return plusMillis(Long.MAX_VALUE).plusMillis(1);
        }
        return plusMillis(-millisToSubtract);
    }

    /**
     * Returns a copy of this instant with the specified duration in nanoseconds subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param nanosToSubtract  the nanoseconds to subtract, positive or negative
     * @return an {@code Instant} based on this instant with the specified nanoseconds subtracted, not null
     * @throws DateTimeException if the result exceeds the maximum or minimum instant
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Instant minusNanos(long nanosToSubtract) {
        if (nanosToSubtract == Long.MIN_VALUE) {
            return plusNanos(Long.MAX_VALUE).plusNanos(1);
        }
        return plusNanos(-nanosToSubtract);
    }

    //-------------------------------------------------------------------------
    /**
     * Queries this instant using the specified query.
     * <p>
     * This queries this instant using the specified query strategy object.
     * The {@code TemporalQuery} object defines the logic to be used to
     * obtain the result. Read the documentation of the query to understand
     * what the result of this method will be.
     * <p>
     * The result of this method is obtained by invoking the
     * {@link TemporalQuery#queryFrom(TemporalAccessor)} method on the
     * specified query passing {@code this} as the argument.
     *
     * @param <R> the type of the result
     * @param query  the query to invoke, not null
     * @return the query result, null may be returned (defined by the query)
     * @throws DateTimeException if unable to query (defined by the query)
     * @throws ArithmeticException if numeric overflow occurs (defined by the query)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.precision()) {
            return (R) NANOS;
        }
        // inline TemporalAccessor.super.query(query) as an optimization
        if (query == TemporalQueries.localDate() || query == TemporalQueries.localTime()
                || query == TemporalQueries.chronology() || query == TemporalQueries.zoneId()
                || query == TemporalQueries.zone() || query == TemporalQueries.offset()) {
            return null;
        }
        return query.queryFrom(this);
    }

    /**
     * Adjusts the specified temporal object to have this instant.
     * <p>
     * This returns a temporal object of the same observable type as the input
     * with the instant changed to be the same as this.
     * <p>
     * The adjustment is equivalent to using {@link Temporal#with(TemporalField, long)}
     * twice, passing {@link ChronoField#INSTANT_SECONDS} and
     * {@link ChronoField#NANO_OF_SECOND} as the fields.
     * <p>
     * In most cases, it is clearer to reverse the calling pattern by using
     * {@link Temporal#with(TemporalAdjuster)}:
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   temporal = thisInstant.adjustInto(temporal);
     *   temporal = temporal.with(thisInstant);
     * </pre>
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param temporal  the target object to be adjusted, not null
     * @return the adjusted object, not null
     * @throws DateTimeException if unable to make the adjustment
     * @throws ArithmeticException if numeric overflow occurs
     */
    @Override
    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(INSTANT_SECONDS, seconds).with(NANO_OF_SECOND, nanos);
    }

    /**
     * Calculates the period between this instant and another instant in
     * terms of the specified unit.
     * <p>
     * This calculates the period between two instants in terms of a single unit.
     * The start and end points are {@code this} and the specified instant.
     * The result will be negative if the end is before the start.
     * The calculation returns a whole number, representing the number of
     * complete units between the two instants.
     * The {@code Temporal} passed to this method is converted to a
     * {@code Instant} using {@link #from(TemporalAccessor)}.
     * For example, the period in days between two dates can be calculated
     * using {@code startInstant.until(endInstant, SECONDS)}.
     * <p>
     * This method operates in association with {@link TemporalUnit#between}.
     * The result of this method is a {@code long} representing the amount of
     * the specified unit. By contrast, the result of {@code between} is an
     * object that can be used directly in addition/subtraction:
     * <pre>
     *   long period = start.until(end, SECONDS);   // this method
     *   dateTime.plus(SECONDS.between(start, end));      // use in plus/minus
     * </pre>
     * <p>
     * The calculation is implemented in this method for {@link ChronoUnit}.
     * The units {@code NANOS}, {@code MICROS}, {@code MILLIS}, {@code SECONDS},
     * {@code MINUTES}, {@code HOURS}, {@code HALF_DAYS} and {@code DAYS}
     * are supported. Other {@code ChronoUnit} values will throw an exception.
     * <p>
     * If the unit is not a {@code ChronoUnit}, then the result of this method
     * is obtained by invoking {@code TemporalUnit.between(Temporal, Temporal)}
     * passing {@code this} as the first argument and the input temporal as
     * the second argument.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param endExclusive  the end date, which is converted to an {@code Instant}, not null
     * @param unit  the unit to measure the period in, not null
     * @return the amount of the period between this date and the end date
     * @throws DateTimeException if the period cannot be calculated
     * @throws ArithmeticException if numeric overflow occurs
     */
    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        Instant end = Instant.from(endExclusive);
        if (unit instanceof ChronoUnit) {
            ChronoUnit f = (ChronoUnit) unit;
            switch (f) {
                case NANOS: return nanosUntil(end);
                case MICROS: return nanosUntil(end) / 1000;
                case MILLIS: return Jdk8Methods.safeSubtract(end.toEpochMilli(), toEpochMilli());
                case SECONDS: return secondsUntil(end);
                case MINUTES: return secondsUntil(end) / SECONDS_PER_MINUTE;
                case HOURS: return secondsUntil(end) / SECONDS_PER_HOUR;
                case HALF_DAYS: return secondsUntil(end) / (12 * SECONDS_PER_HOUR);
                case DAYS: return secondsUntil(end) / SECONDS_PER_DAY;
            }
            throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
        return unit.between(this, end);
    }

    private long nanosUntil(Instant end) {
        long secsDiff = Jdk8Methods.safeSubtract(end.seconds, seconds);
        long totalNanos = Jdk8Methods.safeMultiply(secsDiff, NANOS_PER_SECOND);
        return Jdk8Methods.safeAdd(totalNanos, end.nanos - nanos);
    }

    private long secondsUntil(Instant end) {
        long secsDiff = Jdk8Methods.safeSubtract(end.seconds, seconds);
        long nanosDiff = end.nanos - nanos;
        if (secsDiff > 0 && nanosDiff < 0) {
            secsDiff--;
        } else if (secsDiff < 0 && nanosDiff > 0) {
            secsDiff++;
        }
        return secsDiff;
    }
    //-----------------------------------------------------------------------
    /**
     * Combines this instant with an offset to create an {@code OffsetDateTime}.
     * <p>
     * This returns an {@code OffsetDateTime} formed from this instant at the
     * specified offset from UTC/Greenwich. An exception will be thrown if the
     * instant is too large to fit into an offset date-time.
     * <p>
     * This method is equivalent to
     * {@link OffsetDateTime#ofInstant(Instant, ZoneId) OffsetDateTime.ofInstant(this, offset)}.
     *
     * @param offset  the offset to combine with, not null
     * @return the offset date-time formed from this instant and the specified offset, not null
     * @throws DateTimeException if the result exceeds the supported range
     */
    public OffsetDateTime atOffset(ZoneOffset offset) {
        return OffsetDateTime.ofInstant(this, offset);
    }

    /**
     * Combines this instant with a time-zone to create a {@code ZonedDateTime}.
     * <p>
     * This returns an {@code ZonedDateTime} formed from this instant at the
     * specified time-zone. An exception will be thrown if the instant is too
     * large to fit into a zoned date-time.
     * <p>
     * This method is equivalent to
     * {@link ZonedDateTime#ofInstant(Instant, ZoneId) ZonedDateTime.ofInstant(this, zone)}.
     *
     * @param zone  the zone to combine with, not null
     * @return the zoned date-time formed from this instant and the specified zone, not null
     * @throws DateTimeException if the result exceeds the supported range
     */
    public ZonedDateTime atZone(ZoneId zone) {
        return ZonedDateTime.ofInstant(this, zone);
    }

    //-----------------------------------------------------------------------
    /**
     * Converts this instant to the number of milliseconds from the epoch
     * of 1970-01-01T00:00:00Z.
     * <p>
     * If this instant represents a point on the time-line too far in the future
     * or past to fit in a {@code long} milliseconds, then an exception is thrown.
     * <p>
     * If this instant has greater than millisecond precision, then the conversion
     * will drop any excess precision information as though the amount in nanoseconds
     * was subject to integer division by one million.
     *
     * @return the number of milliseconds since the epoch of 1970-01-01T00:00:00Z
     * @throws ArithmeticException if numeric overflow occurs
     */
    public long toEpochMilli() {
        if (seconds >= 0) {
            long millis = Jdk8Methods.safeMultiply(seconds, MILLIS_PER_SEC);
            return Jdk8Methods.safeAdd(millis, nanos / NANOS_PER_MILLI);
        } else {
            // prevent an overflow in seconds * 1000
            // instead of going form the second farther away from 0
            // going toward 0
            // we go from the second closer to 0 away from 0
            // that way we always stay in the valid long range
            // seconds + 1 can not overflow because it is negative
            long millis = Jdk8Methods.safeMultiply(seconds + 1, MILLIS_PER_SEC);
            return Jdk8Methods.safeSubtract(millis, MILLIS_PER_SEC - nanos / NANOS_PER_MILLI);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Compares this instant to the specified instant.
     * <p>
     * The comparison is based on the time-line position of the instants.
     * It is "consistent with equals", as defined by {@link Comparable}.
     *
     * @param otherInstant  the other instant to compare to, not null
     * @return the comparator value, negative if less, positive if greater
     * @throws NullPointerException if otherInstant is null
     */
    @Override
    public int compareTo(Instant otherInstant) {
        int cmp = Long.compare(seconds, otherInstant.seconds);
        if (cmp != 0) {
            return cmp;
        }
        return nanos - otherInstant.nanos;
    }

    /**
     * Checks if this instant is after the specified instant.
     * <p>
     * The comparison is based on the time-line position of the instants.
     *
     * @param otherInstant  the other instant to compare to, not null
     * @return true if this instant is after the specified instant
     * @throws NullPointerException if otherInstant is null
     */
    public boolean isAfter(Instant otherInstant) {
        return compareTo(otherInstant) > 0;
    }

    /**
     * Checks if this instant is before the specified instant.
     * <p>
     * The comparison is based on the time-line position of the instants.
     *
     * @param otherInstant  the other instant to compare to, not null
     * @return true if this instant is before the specified instant
     * @throws NullPointerException if otherInstant is null
     */
    public boolean isBefore(Instant otherInstant) {
        return compareTo(otherInstant) < 0;
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if this instant is equal to the specified instant.
     * <p>
     * The comparison is based on the time-line position of the instants.
     *
     * @param otherInstant  the other instant, null returns false
     * @return true if the other instant is equal to this one
     */
    @Override
    public boolean equals(Object otherInstant) {
        if (this == otherInstant) {
            return true;
        }
        if (otherInstant instanceof Instant) {
            Instant other = (Instant) otherInstant;
            return this.seconds == other.seconds && this.nanos == other.nanos;
        }
        return false;
    }

    /**
     * Returns a hash code for this instant.
     *
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        return ((int) (seconds ^ (seconds >>> 32))) + 51 * nanos;
    }

    //-----------------------------------------------------------------------
    /**
     * A string representation of this instant using ISO-8601 representation.
     * <p>
     * The format used is the same as {@link DateTimeFormatter#ISO_INSTANT}.
     *
     * @return an ISO-8601 representation of this instant, not null
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        new DateTimeFormatterBuilder.InstantPrinterParser(-2).print(
                new DateTimePrintContext(this, null, null), sb);
        return sb.toString();
    }
}
