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
import static org.threeten.bp.temporal.ChronoUnit.DAYS;
import static org.threeten.bp.temporal.ChronoUnit.FOREVER;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.threeten.bp.DateTimeException;
import org.threeten.bp.chrono.Chronology;
import org.threeten.bp.format.ResolverStyle;
import org.threeten.bp.jdk8.Jdk8Methods;

/**
 * A set of date fields that provide access to Julian Days.
 * <p>
 * The Julian Day is a standard way of expressing date and time commonly used in the scientific community.
 * It is expressed as a decimal number of whole days where days start at midday.
 * This class represents variations on Julian Days that count whole days from midnight.
 *
 * <h3>Specification for implementors</h3>
 * This is an immutable and thread-safe class.
 */
public final class JulianFields {

    /**
     * Julian Day field.
     * <p>
     * This is an integer-based version of the Julian Day Number.
     * Julian Day is a well-known system that represents the count of whole days since day 0,
     * which is defined to be January 1, 4713 BCE in the Julian calendar, and -4713-11-24 Gregorian.
     * The field  has "JulianDay" as 'name', and 'DAYS' as 'baseUnit'.
     * The field always refers to the local date-time, ignoring the offset or zone.
     * <p>
     * For date-times, 'JULIAN_DAY.getFrom()' assumes the same value from
     * midnight until just before the next midnight.
     * When 'JULIAN_DAY.adjustInto()' is applied to a date-time, the time of day portion remains unaltered.
     * 'JULIAN_DAY.adjustInto()' and 'JULIAN_DAY.getFrom()' only apply to {@code Temporal} objects that
     * can be converted into {@link ChronoField#EPOCH_DAY}.
     * A {@link DateTimeException} is thrown for any other type of object.
     * <p>
     * <h3>Astronomical and Scientific Notes</h3>
     * The standard astronomical definition uses a fraction to indicate the time-of-day,
     * thus 3.25 would represent the time 18:00, since days start at midday.
     * This implementation uses an integer and days starting at midnight.
     * The integer value for the Julian Day Number is the astronomical Julian Day value at midday
     * of the date in question.
     * This amounts to the astronomical Julian Day, rounded to an integer {@code JDN = floor(JD + 0.5)}.
     * <p>
     * <pre>
     *  | ISO date          |  Julian Day Number | Astronomical Julian Day |
     *  | 1970-01-01T00:00  |         2,440,588  |         2,440,587.5     |
     *  | 1970-01-01T06:00  |         2,440,588  |         2,440,587.75    |
     *  | 1970-01-01T12:00  |         2,440,588  |         2,440,588.0     |
     *  | 1970-01-01T18:00  |         2,440,588  |         2,440,588.25    |
     *  | 1970-01-02T00:00  |         2,440,589  |         2,440,588.5     |
     *  | 1970-01-02T06:00  |         2,440,589  |         2,440,588.75    |
     *  | 1970-01-02T12:00  |         2,440,589  |         2,440,589.0     |
     * </pre>
     * <p>
     * Julian Days are sometimes taken to imply Universal Time or UTC, but this
     * implementation always uses the Julian Day number for the local date,
     * regardless of the offset or time-zone.
     */
    public static final TemporalField JULIAN_DAY = Field.JULIAN_DAY;
    /**
     * Modified Julian Day field.
     * <p>
     * This is an integer-based version of the Modified Julian Day Number.
     * Modified Julian Day (MJD) is a well-known system that counts days continuously.
     * It is defined relative to astronomical Julian Day as  {@code MJD = JD - 2400000.5}.
     * Each Modified Julian Day runs from midnight to midnight.
     * The field always refers to the local date-time, ignoring the offset or zone.
     * <p>
     * For date-times, 'MODIFIED_JULIAN_DAY.getFrom()' assumes the same value from
     * midnight until just before the next midnight.
     * When 'MODIFIED_JULIAN_DAY.adjustInto()' is applied to a date-time, the time of day portion remains unaltered.
     * 'MODIFIED_JULIAN_DAY.adjustInto()' and 'MODIFIED_JULIAN_DAY.getFrom()' only apply to {@code Temporal} objects
     * that can be converted into {@link ChronoField#EPOCH_DAY}.
     * A {@link DateTimeException} is thrown for any other type of object.
     * <p>
     * This implementation is an integer version of MJD with the decimal part rounded to floor.
     * <p>
     * <h3>Astronomical and Scientific Notes</h3>
     * <pre>
     *  | ISO date          | Modified Julian Day |      Decimal MJD |
     *  | 1970-01-01T00:00  |             40,587  |       40,587.0   |
     *  | 1970-01-01T06:00  |             40,587  |       40,587.25  |
     *  | 1970-01-01T12:00  |             40,587  |       40,587.5   |
     *  | 1970-01-01T18:00  |             40,587  |       40,587.75  |
     *  | 1970-01-02T00:00  |             40,588  |       40,588.0   |
     *  | 1970-01-02T06:00  |             40,588  |       40,588.25  |
     *  | 1970-01-02T12:00  |             40,588  |       40,588.5   |
     * </pre>
     * <p>
     * Modified Julian Days are sometimes taken to imply Universal Time or UTC, but this
     * implementation always uses the Modified Julian Day for the local date,
     * regardless of the offset or time-zone.
     */
    public static final TemporalField MODIFIED_JULIAN_DAY = Field.MODIFIED_JULIAN_DAY;
    /**
     * Rata Die field.
     * <p>
     * Rata Die counts whole days continuously starting day 1 at midnight at the beginning of 0001-01-01 (ISO).
     * The field always refers to the local date-time, ignoring the offset or zone.
     * <p>
     * For date-times, 'RATA_DIE.getFrom()' assumes the same value from
     * midnight until just before the next midnight.
     * When 'RATA_DIE.adjustInto()' is applied to a date-time, the time of day portion remains unaltered.
     * 'MODIFIED_JULIAN_DAY.adjustInto()' and 'RATA_DIE.getFrom()' only apply to {@code Temporal} objects
     * that can be converted into {@link ChronoField#EPOCH_DAY}.
     * A {@link DateTimeException} is thrown for any other type of object.
     */
    public static final TemporalField RATA_DIE = Field.RATA_DIE;

    /**
     * Hidden implementation.
     */
    private enum Field implements TemporalField {
        /**
         * Julian Day field.
         */
        // 719163L + 1721425L = 2440588L
        JULIAN_DAY("JulianDay", DAYS, FOREVER, 2440588L),
        /**
         * Modified Julian Day field.
         */
        // 719163L - 678576L = 40587L
        MODIFIED_JULIAN_DAY("ModifiedJulianDay", DAYS, FOREVER, 40587L),
        /**
         * Rata Die field.
         */
        RATA_DIE("RataDie", DAYS, FOREVER, 719163L),
        // lots of others Truncated,Lilian, ANSI COBOL (also dotnet related), Excel?
        ;

        private final String name;
        private final TemporalUnit baseUnit;
        private final TemporalUnit rangeUnit;
        private final ValueRange range;
        private final long offset;

        Field(String name, TemporalUnit baseUnit, TemporalUnit rangeUnit, long offset) {
            this.name = name;
            this.baseUnit = baseUnit;
            this.rangeUnit = rangeUnit;
            this.range = ValueRange.of(-365243219162L + offset, 365241780471L + offset);
            this.offset = offset;
        }

        //-----------------------------------------------------------------------
        @Override
        public TemporalUnit getBaseUnit() {
            return baseUnit;
        }

        @Override
        public TemporalUnit getRangeUnit() {
            return rangeUnit;
        }

        @Override
        public ValueRange range() {
            return range;
        }

        @Override
        public boolean isDateBased() {
            return true;
        }

        @Override
        public boolean isTimeBased() {
            return false;
        }

        //-----------------------------------------------------------------------
        @Override
        public boolean isSupportedBy(TemporalAccessor temporal) {
            return temporal.isSupported(EPOCH_DAY);
        }

        @Override
        public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
            if (!isSupportedBy(temporal)) {
                throw new UnsupportedTemporalTypeException("Unsupported field: " + this);
            }
            return range();
        }

        @Override
        public long getFrom(TemporalAccessor temporal) {
            return temporal.getLong(EPOCH_DAY) + offset;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R extends Temporal> R adjustInto(R dateTime, long newValue) {
            if (!range().isValidValue(newValue)) {
                throw new DateTimeException("Invalid value: " + name + " " + newValue);
            }
            return (R) dateTime.with(EPOCH_DAY, Jdk8Methods.safeSubtract(newValue, offset));
        }

        @Override
        public String getDisplayName(Locale locale) {
            Objects.requireNonNull(locale, "locale");
            return toString();
        }

        //-----------------------------------------------------------------------
        @Override
        public TemporalAccessor resolve(Map<TemporalField, Long> fieldValues,
                TemporalAccessor partialTemporal, ResolverStyle resolverStyle) {
            long value = fieldValues.remove(this);
            Chronology chrono = Chronology.from(partialTemporal);
            return chrono.dateEpochDay(Jdk8Methods.safeSubtract(value, offset));
        }

        //-----------------------------------------------------------------------
        @Override
        public String toString() {
            return name;
        }
    }

    private JulianFields() {
    }
}
