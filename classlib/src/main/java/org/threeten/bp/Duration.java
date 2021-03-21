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
import static org.threeten.bp.temporal.ChronoField.NANO_OF_SECOND;
import static org.threeten.bp.temporal.ChronoUnit.DAYS;
import static org.threeten.bp.temporal.ChronoUnit.NANOS;
import static org.threeten.bp.temporal.ChronoUnit.SECONDS;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.threeten.bp.format.DateTimeParseException;
import org.threeten.bp.jdk8.Jdk8Methods;
import org.threeten.bp.temporal.ChronoField;
import org.threeten.bp.temporal.ChronoUnit;
import org.threeten.bp.temporal.Temporal;
import org.threeten.bp.temporal.TemporalAmount;
import org.threeten.bp.temporal.TemporalUnit;
import org.threeten.bp.temporal.UnsupportedTemporalTypeException;

/**
 * A time-based amount of time, such as '34.5 seconds'.
 * <p>
 * This class models a quantity or amount of time in terms of seconds and nanoseconds.
 * It can be accessed using other duration-based units, such as minutes and hours.
 * In addition, the {@link ChronoUnit#DAYS DAYS} unit can be used and is treated as
 * exactly equal to 24 hours, thus ignoring daylight savings effects.
 * See {@link Period} for the date-based equivalent to this class.
 * <p>
 * A physical duration could be of infinite length.
 * For practicality, the duration is stored with constraints similar to {@link Instant}.
 * The duration uses nanosecond resolution with a maximum value of the seconds that can
 * be held in a {@code long}. This is greater than the current estimated age of the universe.
 * <p>
 * The range of a duration requires the storage of a number larger than a {@code long}.
 * To achieve this, the class stores a {@code long} representing seconds and an {@code int}
 * representing nanosecond-of-second, which will always be between 0 and 999,999,999.
 * <p>
 * The duration is measured in "seconds", but these are not necessarily identical to
 * the scientific "SI second" definition based on atomic clocks.
 * This difference only impacts durations measured near a leap-second and should not affect
 * most applications.
 * See {@link Instant} for a discussion as to the meaning of the second and time-scales.
 *
 * <h3>Specification for implementors</h3>
 * This class is immutable and thread-safe.
 */
public final class Duration
        implements TemporalAmount, Comparable<Duration>, Serializable {

    /**
     * Constant for a duration of zero.
     */
    public static final Duration ZERO = new Duration(0, 0);
    /**
     * Constant for nanos per second.
     */
    private static final int NANOS_PER_SECOND = 1000000000;
    /**
     * Constant for nanos per milli.
     */
    private static final int NANOS_PER_MILLI = 1000000;
    /**
     * Constant for nanos per second.
     */
    private static BigInteger bigIntNanosPerSecond;

    /**
     * The number of seconds in the duration.
     */
    private final long seconds;
    /**
     * The number of nanoseconds in the duration, expressed as a fraction of the
     * number of seconds. This is always positive, and never exceeds 999,999,999.
     */
    private final int nanos;

    private static BigInteger getBigIntNanosPerSecond() {
        if (bigIntNanosPerSecond == null) {
            bigIntNanosPerSecond = BigInteger.valueOf(NANOS_PER_SECOND);
        }
        return bigIntNanosPerSecond;
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code Duration} from a number of standard 24 hour days.
     * <p>
     * The seconds are calculated based on the standard definition of a day,
     * where each day is 86400 seconds which implies a 24 hour day.
     * The nanosecond in second field is set to zero.
     *
     * @param days  the number of days, positive or negative
     * @return a {@code Duration}, not null
     * @throws ArithmeticException if the input days exceeds the capacity of {@code Duration}
     */
    public static Duration ofDays(long days) {
        return create(Jdk8Methods.safeMultiply(days, 86400), 0);
    }

    /**
     * Obtains an instance of {@code Duration} from a number of standard length hours.
     * <p>
     * The seconds are calculated based on the standard definition of an hour,
     * where each hour is 3600 seconds.
     * The nanosecond in second field is set to zero.
     *
     * @param hours  the number of hours, positive or negative
     * @return a {@code Duration}, not null
     * @throws ArithmeticException if the input hours exceeds the capacity of {@code Duration}
     */
    public static Duration ofHours(long hours) {
        return create(Jdk8Methods.safeMultiply(hours, 3600), 0);
    }

    /**
     * Obtains an instance of {@code Duration} from a number of standard length minutes.
     * <p>
     * The seconds are calculated based on the standard definition of a minute,
     * where each minute is 60 seconds.
     * The nanosecond in second field is set to zero.
     *
     * @param minutes  the number of minutes, positive or negative
     * @return a {@code Duration}, not null
     * @throws ArithmeticException if the input minutes exceeds the capacity of {@code Duration}
     */
    public static Duration ofMinutes(long minutes) {
        return create(Jdk8Methods.safeMultiply(minutes, 60), 0);
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code Duration} from a number of seconds.
     * <p>
     * The nanosecond in second field is set to zero.
     *
     * @param seconds  the number of seconds, positive or negative
     * @return a {@code Duration}, not null
     */
    public static Duration ofSeconds(long seconds) {
        return create(seconds, 0);
    }

    /**
     * Obtains an instance of {@code Duration} from a number of seconds
     * and an adjustment in nanoseconds.
     * <p>
     * This method allows an arbitrary number of nanoseconds to be passed in.
     * The factory will alter the values of the second and nanosecond in order
     * to ensure that the stored nanosecond is in the range 0 to 999,999,999.
     * For example, the following will result in the exactly the same duration:
     * <pre>
     *  Duration.ofSeconds(3, 1);
     *  Duration.ofSeconds(4, -999_999_999);
     *  Duration.ofSeconds(2, 1000_000_001);
     * </pre>
     *
     * @param seconds  the number of seconds, positive or negative
     * @param nanoAdjustment  the nanosecond adjustment to the number of seconds, positive or negative
     * @return a {@code Duration}, not null
     * @throws ArithmeticException if the adjustment causes the seconds to exceed the capacity of {@code Duration}
     */
    public static Duration ofSeconds(long seconds, long nanoAdjustment) {
        long secs = Jdk8Methods.safeAdd(seconds, Jdk8Methods.floorDiv(nanoAdjustment, NANOS_PER_SECOND));
        int nos = Jdk8Methods.floorMod(nanoAdjustment, NANOS_PER_SECOND);
        return create(secs, nos);
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code Duration} from a number of milliseconds.
     * <p>
     * The seconds and nanoseconds are extracted from the specified milliseconds.
     *
     * @param millis  the number of milliseconds, positive or negative
     * @return a {@code Duration}, not null
     */
    public static Duration ofMillis(long millis) {
        long secs = millis / 1000;
        int mos = (int) (millis % 1000);
        if (mos < 0) {
            mos += 1000;
            secs--;
        }
        return create(secs, mos * NANOS_PER_MILLI);
    }

    /**
     * Obtains an instance of {@code Duration} from a number of nanoseconds.
     * <p>
     * The seconds and nanoseconds are extracted from the specified nanoseconds.
     *
     * @param nanos  the number of nanoseconds, positive or negative
     * @return a {@code Duration}, not null
     */
    public static Duration ofNanos(long nanos) {
        long secs = nanos / NANOS_PER_SECOND;
        int nos = (int) (nanos % NANOS_PER_SECOND);
        if (nos < 0) {
            nos += NANOS_PER_SECOND;
            secs--;
        }
        return create(secs, nos);
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code Duration} from a duration in the specified unit.
     * <p>
     * The parameters represent the two parts of a phrase like '6 Hours'. For example:
     * <pre>
     *  Duration.of(3, SECONDS);
     *  Duration.of(465, HOURS);
     * </pre>
     * Only a subset of units are accepted by this method.
     * The unit must either have an {@link TemporalUnit#isDurationEstimated() exact duration} or
     * be {@link ChronoUnit#DAYS} which is treated as 24 hours. Other units throw an exception.
     *
     * @param amount  the amount of the duration, measured in terms of the unit, positive or negative
     * @param unit  the unit that the duration is measured in, must have an exact duration, not null
     * @return a {@code Duration}, not null
     * @throws DateTimeException if the period unit has an estimated duration
     * @throws ArithmeticException if a numeric overflow occurs
     */
    public static Duration of(long amount, TemporalUnit unit) {
        return ZERO.plus(amount, unit);
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code Duration} from an amount.
     * <p>
     * This obtains a duration based on the specified amount.
     * A TemporalAmount represents an amount of time, which may be date-based
     * or time-based, which this factory extracts to a duration.
     * <p>
     * The conversion loops around the set of units from the amount and uses
     * the duration of the unit to calculate the total Duration.
     * Only a subset of units are accepted by this method.
     * The unit must either have an exact duration or be ChronoUnit.DAYS which
     * is treated as 24 hours. If any other units are found then an exception is thrown.
     *
     * @param amount  the amount to convert, not null
     * @return a {@code Duration}, not null
     * @throws DateTimeException if the amount cannot be converted
     * @throws ArithmeticException if a numeric overflow occurs
     */
    public static Duration from(TemporalAmount amount) {
        Objects.requireNonNull(amount, "amount");
        Duration duration = ZERO;
        for (TemporalUnit unit : amount.getUnits()) {
            duration = duration.plus(amount.get(unit), unit);
        }
        return duration;
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code Duration} representing the duration between two instants.
     * <p>
     * Obtains a {@code Duration} representing the duration between two instants.
     * This calculates the duration between two temporal objects of the same type.
     * The difference in seconds is calculated using {@link Temporal#until(Temporal, TemporalUnit)}.
     * The difference in nanoseconds is calculated using by querying the
     * {@link ChronoField#NANO_OF_SECOND NANO_OF_SECOND} field.
     * <p>
     * The result of this method can be a negative period if the end is before the start.
     * To guarantee to obtain a positive duration call abs() on the result.
     *
     * @param startInclusive  the start instant, inclusive, not null
     * @param endExclusive  the end instant, exclusive, not null
     * @return a {@code Duration}, not null
     * @throws DateTimeException if the seconds between the temporals cannot be obtained
     * @throws ArithmeticException if the calculation exceeds the capacity of {@code Duration}
     */
    public static Duration between(Temporal startInclusive, Temporal endExclusive) {
        long secs = startInclusive.until(endExclusive, SECONDS);
        long nanos = 0;
        if (startInclusive.isSupported(NANO_OF_SECOND) && endExclusive.isSupported(NANO_OF_SECOND)) {
            try {
                long startNos = startInclusive.getLong(NANO_OF_SECOND);
                nanos = endExclusive.getLong(NANO_OF_SECOND) - startNos;
                if (secs > 0 && nanos < 0) {
                    nanos += NANOS_PER_SECOND;
                } else if (secs < 0 && nanos > 0) {
                    nanos -= NANOS_PER_SECOND;
                } else if (secs == 0 && nanos != 0) {
                    // two possible meanings for result, so recalculate secs
                    Temporal adjustedEnd = endExclusive.with(NANO_OF_SECOND, startNos);
                    secs = startInclusive.until(adjustedEnd, SECONDS);
                }
            } catch (DateTimeException | ArithmeticException ex2) {
                // ignore and only use seconds
            }
        }
        return ofSeconds(secs, nanos);
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains a {@code Duration} from a text string such as {@code PnDTnHnMn.nS}.
     * <p>
     * This will parse a textual representation of a duration, including the
     * string produced by {@code toString()}. The formats accepted are based
     * on the ISO-8601 duration format {@code PnDTnHnMn.nS} with days
     * considered to be exactly 24 hours.
     * <p>
     * The string starts with an optional sign, denoted by the ASCII negative
     * or positive symbol. If negative, the whole period is negated.
     * The ASCII letter "P" is next in upper or lower case.
     * There are then four sections, each consisting of a number and a suffix.
     * The sections have suffixes in ASCII of "D", "H", "M" and "S" for
     * days, hours, minutes and seconds, accepted in upper or lower case.
     * The suffixes must occur in order. The ASCII letter "T" must occur before
     * the first occurrence, if any, of an hour, minute or second section.
     * At least one of the four sections must be present, and if "T" is present
     * there must be at least one section after the "T".
     * The number part of each section must consist of one or more ASCII digits.
     * The number may be prefixed by the ASCII negative or positive symbol.
     * The number of days, hours and minutes must parse to a {@code long}.
     * The number of seconds must parse to a {@code long} with optional fraction.
     * The decimal point may be either a dot or a comma.
     * The fractional part may have from zero to 9 digits.
     * <p>
     * The leading plus/minus sign, and negative values for other units are
     * not part of the ISO-8601 standard.
     * <p>
     * Examples:
     * <pre>
     *    "PT20.345S" -> parses as "20.345 seconds"
     *    "PT15M"     -> parses as "15 minutes" (where a minute is 60 seconds)
     *    "PT10H"     -> parses as "10 hours" (where an hour is 3600 seconds)
     *    "P2D"       -> parses as "2 days" (where a day is 24 hours or 86400 seconds)
     *    "P2DT3H4M"  -> parses as "2 days, 3 hours and 4 minutes"
     *    "P-6H3M"    -> parses as "-6 hours and +3 minutes"
     *    "-P6H3M"    -> parses as "-6 hours and -3 minutes"
     *    "-P-6H+3M"  -> parses as "+6 hours and -3 minutes"
     * </pre>
     *
     * @param text  the text to parse, not null
     * @return the parsed duration, not null
     * @throws DateTimeParseException if the text cannot be parsed to a duration
     */
    public static Duration parse(CharSequence text) {
        Objects.requireNonNull(text, "text");
        Parser parser = new Parser(text);
        if (!parser.parse() || !parser.hasOneField) {
            throw new DateTimeParseException("Text cannot be parsed to a Duration", text, parser.ptr);
        }
        return create(parser.negative, parser.days * SECONDS_PER_DAY, parser.hours * SECONDS_PER_HOUR,
                parser.minutes * SECONDS_PER_MINUTE, parser.seconds, parser.nanos);
    }

    static class Parser {
        private int ptr;
        private CharSequence text;
        private int days;
        private int hours;
        private int minutes;
        private int seconds;
        private int nanos;
        private boolean negative;
        private boolean hasOneField;
        private int parsedNumber;

        Parser(CharSequence text) {
            this.text = text;
        }

        boolean parse() {
            negative = sign();
            if (eof() || text.charAt(ptr) != 'P') {
                return false;
            }
            ptr++;

            if (eof()) {
                return false;
            }

            if (text.charAt(ptr) != 'T') {
                if (!tryParseDays()) {
                    return false;
                }
                if (eof()) {
                    return true;
                }
                if (text.charAt(ptr) != 'T') {
                    return false;
                }
                ++ptr;
                hasOneField = false;
            } else {
                ++ptr;
            }

            int state = 0;
            loop: do {
                if (!number()) {
                    break;
                }
                if (eof()) {
                    return false;
                }
                hasOneField = true;
                char c = text.charAt(ptr);

                //CHECKSTYLE.OFF: FallThrough
                switch (state) {
                    case 0:
                        if (c == 'H') {
                            ++ptr;
                            hours = parsedNumber;
                            state = 1;
                            break;
                        }
                    case 1:
                        if (c == 'M') {
                            ++ptr;
                            minutes = parsedNumber;
                            state = 2;
                            break;
                        }
                    case 2:
                        if (c == 'S') {
                            ++ptr;
                            seconds = parsedNumber;
                            break loop;
                        } else if (c == '.') {
                            seconds = parsedNumber;
                            if (!number()) {
                                return false;
                            }
                            nanos = parsedNumber;
                            if (eof() || text.charAt(ptr) != 'S') {
                                return false;
                            }
                            ++ptr;
                            break loop;
                        }
                    default:
                        return false;
                }
                //CHECKSTYLE.ON: FallThrough
            } while (true);

            return eof() && hasOneField;
        }

        private boolean tryParseDays() {
            if (!number()) {
                return false;
            }
            days = parsedNumber;
            hasOneField = true;
            if (ptr >= text.length() || text.charAt(ptr) != 'D') {
                return false;
            }
            ++ptr;
            return true;
        }

        boolean eof() {
            return ptr >= text.length();
        }

        boolean sign() {
            if (!eof()) {
                if (text.charAt(ptr) == '-') {
                    ptr++;
                    return true;
                } else if (text.charAt(ptr) == '+') {
                    ptr++;
                }
            }
            return false;
        }

        boolean number() {
            boolean negative = sign();
            parsedNumber = 0;
            boolean hasDigits = false;
            while (ptr < text.length()) {
                char c = text.charAt(ptr);
                if (c < '0' || c >= '9') {
                    break;
                }
                ++ptr;
                hasDigits = true;
                parsedNumber = parsedNumber * 10 + c - '0';
            }
            if (negative) {
                parsedNumber = -parsedNumber;
            }
            return hasDigits;
        }
    }

    private static Duration create(boolean negate, long daysAsSecs, long hoursAsSecs, long minsAsSecs,
            long secs, int nanos) {
        long seconds = Jdk8Methods.safeAdd(daysAsSecs, Jdk8Methods.safeAdd(hoursAsSecs,
                Jdk8Methods.safeAdd(minsAsSecs, secs)));
        if (negate) {
            return ofSeconds(seconds, nanos).negated();
        }
        return ofSeconds(seconds, nanos);
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code Duration} using seconds and nanoseconds.
     *
     * @param seconds  the length of the duration in seconds, positive or negative
     * @param nanoAdjustment  the nanosecond adjustment within the second, from 0 to 999,999,999
     */
    private static Duration create(long seconds, int nanoAdjustment) {
        if ((seconds | nanoAdjustment) == 0) {
            return ZERO;
        }
        return new Duration(seconds, nanoAdjustment);
    }

    /**
     * Constructs an instance of {@code Duration} using seconds and nanoseconds.
     *
     * @param seconds  the length of the duration in seconds, positive or negative
     * @param nanos  the nanoseconds within the second, from 0 to 999,999,999
     */
    private Duration(long seconds, int nanos) {
        super();
        this.seconds = seconds;
        this.nanos = nanos;
    }

    //-----------------------------------------------------------------------
    @Override
    public List<TemporalUnit> getUnits() {
        return Collections.<TemporalUnit>unmodifiableList(Arrays.asList(SECONDS, NANOS));
    }

    @Override
    public long get(TemporalUnit unit) {
        if (unit == SECONDS) {
            return seconds;
        }
        if (unit == NANOS) {
            return nanos;
        }
        throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if this duration is zero length.
     * <p>
     * A {@code Duration} represents a directed distance between two points on
     * the time-line and can therefore be positive, zero or negative.
     * This method checks whether the length is zero.
     *
     * @return true if this duration has a total length equal to zero
     */
    public boolean isZero() {
        return (seconds | nanos) == 0;
    }

    /**
     * Checks if this duration is negative, excluding zero.
     * <p>
     * A {@code Duration} represents a directed distance between two points on
     * the time-line and can therefore be positive, zero or negative.
     * This method checks whether the length is less than zero.
     *
     * @return true if this duration has a total length less than zero
     */
    public boolean isNegative() {
        return seconds < 0;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the number of seconds in this duration.
     * <p>
     * The length of the duration is stored using two fields - seconds and nanoseconds.
     * The nanoseconds part is a value from 0 to 999,999,999 that is an adjustment to
     * the length in seconds.
     * The total duration is defined by calling this method and {@link #getNano()}.
     * <p>
     * A {@code Duration} represents a directed distance between two points on the time-line.
     * A negative duration is expressed by the negative sign of the seconds part.
     * A duration of -1 nanosecond is stored as -1 seconds plus 999,999,999 nanoseconds.
     *
     * @return the whole seconds part of the length of the duration, positive or negative
     */
    public long getSeconds() {
        return seconds;
    }

    /**
     * Gets the number of nanoseconds within the second in this duration.
     * <p>
     * The length of the duration is stored using two fields - seconds and nanoseconds.
     * The nanoseconds part is a value from 0 to 999,999,999 that is an adjustment to
     * the length in seconds.
     * The total duration is defined by calling this method and {@link #getSeconds()}.
     * <p>
     * A {@code Duration} represents a directed distance between two points on the time-line.
     * A negative duration is expressed by the negative sign of the seconds part.
     * A duration of -1 nanosecond is stored as -1 seconds plus 999,999,999 nanoseconds.
     *
     * @return the nanoseconds within the second part of the length of the duration, from 0 to 999,999,999
     */
    public int getNano() {
        return nanos;
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this duration with the specified amount of seconds.
     * <p>
     * This returns a duration with the specified seconds, retaining the
     * nano-of-second part of this duration.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param seconds  the seconds to represent, may be negative
     * @return a {@code Duration} based on this period with the requested seconds, not null
     */
    public Duration withSeconds(long seconds) {
        return create(seconds, nanos);
    }

    /**
     * Returns a copy of this duration with the specified nano-of-second.
     * <p>
     * This returns a duration with the specified nano-of-second, retaining the
     * seconds part of this duration.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param nanoOfSecond  the nano-of-second to represent, from 0 to 999,999,999
     * @return a {@code Duration} based on this period with the requested nano-of-second, not null
     * @throws DateTimeException if the nano-of-second is invalid
     */
    public Duration withNanos(int nanoOfSecond) {
        NANO_OF_SECOND.checkValidIntValue(nanoOfSecond);
        return create(seconds, nanoOfSecond);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this duration with the specified duration added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param duration  the duration to add, positive or negative, not null
     * @return a {@code Duration} based on this duration with the specified duration added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration plus(Duration duration) {
        return plus(duration.getSeconds(), duration.getNano());
     }

    /**
     * Returns a copy of this duration with the specified duration added.
     * <p>
     * The duration amount is measured in terms of the specified unit.
     * Only a subset of units are accepted by this method.
     * The unit must either have an {@link TemporalUnit#isDurationEstimated() exact duration} or
     * be {@link ChronoUnit#DAYS} which is treated as 24 hours. Other units throw an exception.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param amountToAdd  the amount of the period, measured in terms of the unit, positive or negative
     * @param unit  the unit that the period is measured in, must have an exact duration, not null
     * @return a {@code Duration} based on this duration with the specified duration added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration plus(long amountToAdd, TemporalUnit unit) {
        Objects.requireNonNull(unit, "unit");
        if (unit == DAYS) {
            return plus(Jdk8Methods.safeMultiply(amountToAdd, SECONDS_PER_DAY), 0);
        }
        if (unit.isDurationEstimated()) {
            throw new DateTimeException("Unit must not have an estimated duration");
        }
        if (amountToAdd == 0) {
            return this;
        }
        if (unit instanceof ChronoUnit) {
            switch ((ChronoUnit) unit) {
                case NANOS:
                    return plusNanos(amountToAdd);
                case MICROS:
                    return plusSeconds((amountToAdd / (1000000L * 1000)) * 1000)
                            .plusNanos((amountToAdd % (1000000L * 1000)) * 1000);
                case MILLIS:
                    return plusMillis(amountToAdd);
                case SECONDS:
                    return plusSeconds(amountToAdd);
            }
            return plusSeconds(Jdk8Methods.safeMultiply(unit.getDuration().seconds, amountToAdd));
        }
        Duration duration = unit.getDuration().multipliedBy(amountToAdd);
        return plusSeconds(duration.getSeconds()).plusNanos(duration.getNano());
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this duration with the specified duration in 24 hour days added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param daysToAdd  the days to add, positive or negative
     * @return a {@code Duration} based on this duration with the specified days added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration plusDays(long daysToAdd) {
        return plus(Jdk8Methods.safeMultiply(daysToAdd, SECONDS_PER_DAY), 0);
    }

    /**
     * Returns a copy of this duration with the specified duration in hours added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param hoursToAdd  the hours to add, positive or negative
     * @return a {@code Duration} based on this duration with the specified hours added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration plusHours(long hoursToAdd) {
        return plus(Jdk8Methods.safeMultiply(hoursToAdd, SECONDS_PER_HOUR), 0);
    }

    /**
     * Returns a copy of this duration with the specified duration in minutes added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param minutesToAdd  the minutes to add, positive or negative
     * @return a {@code Duration} based on this duration with the specified minutes added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration plusMinutes(long minutesToAdd) {
        return plus(Jdk8Methods.safeMultiply(minutesToAdd, SECONDS_PER_MINUTE), 0);
    }

    /**
     * Returns a copy of this duration with the specified duration in seconds added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param secondsToAdd  the seconds to add, positive or negative
     * @return a {@code Duration} based on this duration with the specified seconds added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration plusSeconds(long secondsToAdd) {
        return plus(secondsToAdd, 0);
    }

    /**
     * Returns a copy of this duration with the specified duration in milliseconds added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param millisToAdd  the milliseconds to add, positive or negative
     * @return a {@code Duration} based on this duration with the specified milliseconds added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration plusMillis(long millisToAdd) {
        return plus(millisToAdd / 1000, (millisToAdd % 1000) * NANOS_PER_MILLI);
    }

    /**
     * Returns a copy of this duration with the specified duration in nanoseconds added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param nanosToAdd  the nanoseconds to add, positive or negative
     * @return a {@code Duration} based on this duration with the specified nanoseconds added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration plusNanos(long nanosToAdd) {
        return plus(0, nanosToAdd);
    }

    /**
     * Returns a copy of this duration with the specified duration added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param secondsToAdd  the seconds to add, positive or negative
     * @param nanosToAdd  the nanos to add, positive or negative
     * @return a {@code Duration} based on this duration with the specified seconds added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    private Duration plus(long secondsToAdd, long nanosToAdd) {
        if ((secondsToAdd | nanosToAdd) == 0) {
            return this;
        }
        long epochSec = Jdk8Methods.safeAdd(seconds, secondsToAdd);
        epochSec = Jdk8Methods.safeAdd(epochSec, nanosToAdd / NANOS_PER_SECOND);
        nanosToAdd = nanosToAdd % NANOS_PER_SECOND;
        long nanoAdjustment = nanos + nanosToAdd;  // safe int+NANOS_PER_SECOND
        return ofSeconds(epochSec, nanoAdjustment);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this duration with the specified duration subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param duration  the duration to subtract, positive or negative, not null
     * @return a {@code Duration} based on this duration with the specified duration subtracted, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration minus(Duration duration) {
        long secsToSubtract = duration.getSeconds();
        int nanosToSubtract = duration.getNano();
        if (secsToSubtract == Long.MIN_VALUE) {
            return plus(Long.MAX_VALUE, -nanosToSubtract).plus(1, 0);
        }
        return plus(-secsToSubtract, -nanosToSubtract);
     }

    /**
     * Returns a copy of this duration with the specified duration subtracted.
     * <p>
     * The duration amount is measured in terms of the specified unit.
     * Only a subset of units are accepted by this method.
     * The unit must either have an {@link TemporalUnit#isDurationEstimated() exact duration} or
     * be {@link ChronoUnit#DAYS} which is treated as 24 hours. Other units throw an exception.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param amountToSubtract  the amount of the period, measured in terms of the unit, positive or negative
     * @param unit  the unit that the period is measured in, must have an exact duration, not null
     * @return a {@code Duration} based on this duration with the specified duration subtracted, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration minus(long amountToSubtract, TemporalUnit unit) {
        return amountToSubtract == Long.MIN_VALUE
                ? plus(Long.MAX_VALUE, unit).plus(1, unit)
                : plus(-amountToSubtract, unit);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this duration with the specified duration in 24 hour days subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param daysToSubtract  the days to subtract, positive or negative
     * @return a {@code Duration} based on this duration with the specified days subtracted, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration minusDays(long daysToSubtract) {
        return daysToSubtract == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1) : plusDays(-daysToSubtract);
    }

    /**
     * Returns a copy of this duration with the specified duration in hours subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param hoursToSubtract  the hours to subtract, positive or negative
     * @return a {@code Duration} based on this duration with the specified hours subtracted, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration minusHours(long hoursToSubtract) {
        return hoursToSubtract == Long.MIN_VALUE
                ? plusHours(Long.MAX_VALUE).plusHours(1)
                : plusHours(-hoursToSubtract);
    }

    /**
     * Returns a copy of this duration with the specified duration in minutes subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param minutesToSubtract  the minutes to subtract, positive or negative
     * @return a {@code Duration} based on this duration with the specified minutes subtracted, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration minusMinutes(long minutesToSubtract) {
        return minutesToSubtract == Long.MIN_VALUE
                ? plusMinutes(Long.MAX_VALUE).plusMinutes(1)
                : plusMinutes(-minutesToSubtract);
    }

    /**
     * Returns a copy of this duration with the specified duration in seconds subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param secondsToSubtract  the seconds to subtract, positive or negative
     * @return a {@code Duration} based on this duration with the specified seconds subtracted, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration minusSeconds(long secondsToSubtract) {
        return secondsToSubtract == Long.MIN_VALUE
                ? plusSeconds(Long.MAX_VALUE).plusSeconds(1)
                : plusSeconds(-secondsToSubtract);
    }

    /**
     * Returns a copy of this duration with the specified duration in milliseconds subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param millisToSubtract  the milliseconds to subtract, positive or negative
     * @return a {@code Duration} based on this duration with the specified milliseconds subtracted, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration minusMillis(long millisToSubtract) {
        return millisToSubtract == Long.MIN_VALUE
                ? plusMillis(Long.MAX_VALUE).plusMillis(1)
                : plusMillis(-millisToSubtract);
    }

    /**
     * Returns a copy of this duration with the specified duration in nanoseconds subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param nanosToSubtract  the nanoseconds to subtract, positive or negative
     * @return a {@code Duration} based on this duration with the specified nanoseconds subtracted, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration minusNanos(long nanosToSubtract) {
        return nanosToSubtract == Long.MIN_VALUE
                ? plusNanos(Long.MAX_VALUE).plusNanos(1)
                : plusNanos(-nanosToSubtract);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this duration multiplied by the scalar.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param multiplicand  the value to multiply the duration by, positive or negative
     * @return a {@code Duration} based on this duration multiplied by the specified scalar, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration multipliedBy(long multiplicand) {
        if (multiplicand == 0) {
            return ZERO;
        }
        if (multiplicand == 1) {
            return this;
        }
        return create(toSeconds().multiply(BigDecimal.valueOf(multiplicand)));
     }

    /**
     * Returns a copy of this duration divided by the specified value.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param divisor  the value to divide the duration by, positive or negative, not zero
     * @return a {@code Duration} based on this duration divided by the specified divisor, not null
     * @throws ArithmeticException if the divisor is zero
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration dividedBy(long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        if (divisor == 1) {
            return this;
        }
        return create(toSeconds().divide(BigDecimal.valueOf(divisor), RoundingMode.DOWN));
     }

    /**
     * Converts this duration to the total length in seconds and
     * fractional nanoseconds expressed as a {@code BigDecimal}.
     *
     * @return the total length of the duration in seconds, with a scale of 9, not null
     */
    private BigDecimal toSeconds() {
        return BigDecimal.valueOf(seconds).add(BigDecimal.valueOf(nanos, 9));
    }

    /**
     * Creates an instance of {@code Duration} from a number of seconds.
     *
     * @param seconds  the number of seconds, up to scale 9, positive or negative
     * @return a {@code Duration}, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    private static Duration create(BigDecimal seconds) {
        BigInteger nanos = seconds.movePointRight(9).toBigIntegerExact();
        BigInteger[] divRem = nanos.divideAndRemainder(getBigIntNanosPerSecond());
        if (divRem[0].bitLength() > 63) {
            throw new ArithmeticException("Exceeds capacity of Duration: " + nanos);
        }
        return ofSeconds(divRem[0].longValue(), divRem[1].intValue());
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this duration with the length negated.
     * <p>
     * This method swaps the sign of the total length of this duration.
     * For example, {@code PT1.3S} will be returned as {@code PT-1.3S}.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @return a {@code Duration} based on this duration with the amount negated, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration negated() {
        return multipliedBy(-1);
    }

    /**
     * Returns a copy of this duration with a positive length.
     * <p>
     * This method returns a positive duration by effectively removing the sign from any negative total length.
     * For example, {@code PT-1.3S} will be returned as {@code PT1.3S}.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @return a {@code Duration} based on this duration with an absolute length, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration abs() {
        return isNegative() ? negated() : this;
    }

    //-------------------------------------------------------------------------
    /**
     * Adds this duration to the specified temporal object.
     * <p>
     * This returns a temporal object of the same observable type as the input
     * with this duration added.
     * <p>
     * In most cases, it is clearer to reverse the calling pattern by using
     * {@link Temporal#plus(TemporalAmount)}.
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   dateTime = thisDuration.addTo(dateTime);
     *   dateTime = dateTime.plus(thisDuration);
     * </pre>
     * <p>
     * The calculation will add the seconds, then nanos.
     * Only non-zero amounts will be added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param temporal  the temporal object to adjust, not null
     * @return an object of the same type with the adjustment made, not null
     * @throws DateTimeException if unable to add
     * @throws ArithmeticException if numeric overflow occurs
     */
    @Override
    public Temporal addTo(Temporal temporal) {
        if (seconds != 0) {
            temporal = temporal.plus(seconds, SECONDS);
        }
        if (nanos != 0) {
            temporal = temporal.plus(nanos, NANOS);
        }
        return temporal;
    }

    /**
     * Subtracts this duration from the specified temporal object.
     * <p>
     * This returns a temporal object of the same observable type as the input
     * with this duration subtracted.
     * <p>
     * In most cases, it is clearer to reverse the calling pattern by using
     * {@link Temporal#minus(TemporalAmount)}.
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   dateTime = thisDuration.subtractFrom(dateTime);
     *   dateTime = dateTime.minus(thisDuration);
     * </pre>
     * <p>
     * The calculation will subtract the seconds, then nanos.
     * Only non-zero amounts will be added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param temporal  the temporal object to adjust, not null
     * @return an object of the same type with the adjustment made, not null
     * @throws DateTimeException if unable to subtract
     * @throws ArithmeticException if numeric overflow occurs
     */
    @Override
    public Temporal subtractFrom(Temporal temporal) {
        if (seconds != 0) {
            temporal = temporal.minus(seconds, SECONDS);
        }
        if (nanos != 0) {
            temporal = temporal.minus(nanos, NANOS);
        }
        return temporal;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the number of days in this duration.
     * <p>
     * This returns the total number of days in the duration by dividing the
     * number of seconds by 86400.
     * This is based on the standard definition of a day as 24 hours.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @return the number of days in the duration, may be negative
     */
    public long toDays() {
        return seconds / SECONDS_PER_DAY;
    }

    /**
     * Gets the number of hours in this duration.
     * <p>
     * This returns the total number of hours in the duration by dividing the
     * number of seconds by 3600.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @return the number of hours in the duration, may be negative
     */
    public long toHours() {
        return seconds / SECONDS_PER_HOUR;
    }

    /**
     * Gets the number of minutes in this duration.
     * <p>
     * This returns the total number of minutes in the duration by dividing the
     * number of seconds by 60.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @return the number of minutes in the duration, may be negative
     */
    public long toMinutes() {
        return seconds / SECONDS_PER_MINUTE;
    }

    /**
     * Converts this duration to the total length in milliseconds.
     * <p>
     * If this duration is too large to fit in a {@code long} milliseconds, then an
     * exception is thrown.
     * <p>
     * If this duration has greater than millisecond precision, then the conversion
     * will drop any excess precision information as though the amount in nanoseconds
     * was subject to integer division by one million.
     *
     * @return the total length of the duration in milliseconds
     * @throws ArithmeticException if numeric overflow occurs
     */
    public long toMillis() {
        long result = Jdk8Methods.safeMultiply(seconds, 1000);
        result = Jdk8Methods.safeAdd(result, nanos / NANOS_PER_MILLI);
        return result;
    }

    /**
     * Converts this duration to the total length in nanoseconds expressed as a {@code long}.
     * <p>
     * If this duration is too large to fit in a {@code long} nanoseconds, then an
     * exception is thrown.
     *
     * @return the total length of the duration in nanoseconds
     * @throws ArithmeticException if numeric overflow occurs
     */
    public long toNanos() {
        long result = Jdk8Methods.safeMultiply(seconds, NANOS_PER_SECOND);
        result = Jdk8Methods.safeAdd(result, nanos);
        return result;
    }

    //-----------------------------------------------------------------------
    /**
     * Compares this duration to the specified {@code Duration}.
     * <p>
     * The comparison is based on the total length of the durations.
     * It is "consistent with equals", as defined by {@link Comparable}.
     *
     * @param otherDuration  the other duration to compare to, not null
     * @return the comparator value, negative if less, positive if greater
     */
    @Override
    public int compareTo(Duration otherDuration) {
        int cmp = Long.compare(seconds, otherDuration.seconds);
        if (cmp != 0) {
            return cmp;
        }
        return nanos - otherDuration.nanos;
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if this duration is equal to the specified {@code Duration}.
     * <p>
     * The comparison is based on the total length of the durations.
     *
     * @param otherDuration  the other duration, null returns false
     * @return true if the other duration is equal to this one
     */
    @Override
    public boolean equals(Object otherDuration) {
        if (this == otherDuration) {
            return true;
        }
        if (otherDuration instanceof Duration) {
            Duration other = (Duration) otherDuration;
            return this.seconds == other.seconds && this.nanos == other.nanos;
        }
        return false;
    }

    /**
     * A hash code for this duration.
     *
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        return ((int) (seconds ^ (seconds >>> 32))) + (51 * nanos);
    }

    //-----------------------------------------------------------------------
    /**
     * A string representation of this duration using ISO-8601 seconds
     * based representation, such as {@code PT8H6M12.345S}.
     * <p>
     * The format of the returned string will be {@code PTnHnMnS}, where n is
     * the relevant hours, minutes or seconds part of the duration.
     * Any fractional seconds are placed after a decimal point i the seconds section.
     * If a section has a zero value, it is omitted.
     * The hours, minutes and seconds will all have the same sign.
     * <p>
     * Examples:
     * <pre>
     *    "20.345 seconds"                 -> "PT20.345S
     *    "15 minutes" (15 * 60 seconds)   -> "PT15M"
     *    "10 hours" (10 * 3600 seconds)   -> "PT10H"
     *    "2 days" (2 * 86400 seconds)     -> "PT48H"
     * </pre>
     * Note that multiples of 24 hours are not output as days to avoid confusion
     * with {@code Period}.
     *
     * @return an ISO-8601 representation of this duration, not null
     */
    @Override
    public String toString() {
        if (this == ZERO) {
            return "PT0S";
        }
        long hours = seconds / SECONDS_PER_HOUR;
        int minutes = (int) ((seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE);
        int secs = (int) (seconds % SECONDS_PER_MINUTE);
        StringBuilder buf = new StringBuilder(24);
        buf.append("PT");
        if (hours != 0) {
            buf.append(hours).append('H');
        }
        if (minutes != 0) {
            buf.append(minutes).append('M');
        }
        if (secs == 0 && nanos == 0 && buf.length() > 2) {
            return buf.toString();
        }
        if (secs < 0 && nanos > 0) {
            if (secs == -1) {
                buf.append("-0");
            } else {
                buf.append(secs + 1);
            }
        } else {
            buf.append(secs);
        }
        if (nanos > 0) {
            int pos = buf.length();
            if (secs < 0) {
                buf.append(2 * NANOS_PER_SECOND - nanos);
            } else {
                buf.append(nanos + NANOS_PER_SECOND);
            }
            while (buf.charAt(buf.length() - 1) == '0') {
                buf.setLength(buf.length() - 1);
            }
            buf.setCharAt(pos, '.');
        }
        buf.append('S');
        return buf.toString();
    }
}
