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

import static org.threeten.bp.temporal.ChronoField.DAY_OF_WEEK;
import static org.threeten.bp.temporal.ChronoUnit.DAYS;
import java.util.Locale;
import org.threeten.bp.format.DateTimeFormatterBuilder;
import org.threeten.bp.format.TextStyle;
import org.threeten.bp.temporal.ChronoField;
import org.threeten.bp.temporal.Temporal;
import org.threeten.bp.temporal.TemporalAccessor;
import org.threeten.bp.temporal.TemporalAdjuster;
import org.threeten.bp.temporal.TemporalAdjusters;
import org.threeten.bp.temporal.TemporalField;
import org.threeten.bp.temporal.TemporalQueries;
import org.threeten.bp.temporal.TemporalQuery;
import org.threeten.bp.temporal.UnsupportedTemporalTypeException;
import org.threeten.bp.temporal.ValueRange;
import org.threeten.bp.temporal.WeekFields;

/**
 * A day-of-week, such as 'Tuesday'.
 * <p>
 * {@code DayOfWeek} is an enum representing the 7 days of the week -
 * Monday, Tuesday, Wednesday, Thursday, Friday, Saturday and Sunday.
 * <p>
 * In addition to the textual enum name, each day-of-week has an {@code int} value.
 * The {@code int} value follows the ISO-8601 standard, from 1 (Monday) to 7 (Sunday).
 * It is recommended that applications use the enum rather than the {@code int} value
 * to ensure code clarity.
 * <p>
 * This enum provides access to the localized textual form of the day-of-week.
 * Some locales also assign different numeric values to the days, declaring
 * Sunday to have the value 1, however this class provides no support for this.
 * See {@link WeekFields} for localized week-numbering.
 * <p>
 * <b>Do not use {@code ordinal()} to obtain the numeric representation of {@code DayOfWeek}.
 * Use {@code getValue()} instead.</b>
 * <p>
 * This enum represents a common concept that is found in many calendar systems.
 * As such, this enum may be used by any calendar system that has the day-of-week
 * concept defined exactly equivalent to the ISO calendar system.
 *
 * <h3>Specification for implementors</h3>
 * This is an immutable and thread-safe enum.
 */
public enum DayOfWeek implements TemporalAccessor, TemporalAdjuster {

    /**
     * The singleton instance for the day-of-week of Monday.
     * This has the numeric value of {@code 1}.
     */
    MONDAY,
    /**
     * The singleton instance for the day-of-week of Tuesday.
     * This has the numeric value of {@code 2}.
     */
    TUESDAY,
    /**
     * The singleton instance for the day-of-week of Wednesday.
     * This has the numeric value of {@code 3}.
     */
    WEDNESDAY,
    /**
     * The singleton instance for the day-of-week of Thursday.
     * This has the numeric value of {@code 4}.
     */
    THURSDAY,
    /**
     * The singleton instance for the day-of-week of Friday.
     * This has the numeric value of {@code 5}.
     */
    FRIDAY,
    /**
     * The singleton instance for the day-of-week of Saturday.
     * This has the numeric value of {@code 6}.
     */
    SATURDAY,
    /**
     * The singleton instance for the day-of-week of Sunday.
     * This has the numeric value of {@code 7}.
     */
    SUNDAY;
    /**
     * Simulate JDK 8 method reference DayOfWeek::from.
     */
    public static final TemporalQuery<DayOfWeek> FROM = new TemporalQuery<DayOfWeek>() {
        @Override
        public DayOfWeek queryFrom(TemporalAccessor temporal) {
            return DayOfWeek.from(temporal);
        }
    };
    /**
     * Private cache of all the constants.
     */
    private static final DayOfWeek[] ENUMS = DayOfWeek.values();

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code DayOfWeek} from an {@code int} value.
     * <p>
     * {@code DayOfWeek} is an enum representing the 7 days of the week.
     * This factory allows the enum to be obtained from the {@code int} value.
     * The {@code int} value follows the ISO-8601 standard, from 1 (Monday) to 7 (Sunday).
     *
     * @param dayOfWeek  the day-of-week to represent, from 1 (Monday) to 7 (Sunday)
     * @return the day-of-week singleton, not null
     * @throws DateTimeException if the day-of-week is invalid
     */
    public static DayOfWeek of(int dayOfWeek) {
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new DateTimeException("Invalid value for DayOfWeek: " + dayOfWeek);
        }
        return ENUMS[dayOfWeek - 1];
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code DayOfWeek} from a temporal object.
     * <p>
     * A {@code TemporalAccessor} represents some form of date and time information.
     * This factory converts the arbitrary temporal object to an instance of {@code DayOfWeek}.
     * <p>
     * The conversion extracts the {@link ChronoField#DAY_OF_WEEK DAY_OF_WEEK} field.
     * <p>
     * This method matches the signature of the functional interface {@link TemporalQuery}
     * allowing it to be used as a query via method reference, {@code DayOfWeek::from}.
     *
     * @param temporal  the temporal object to convert, not null
     * @return the day-of-week, not null
     * @throws DateTimeException if unable to convert to a {@code DayOfWeek}
     */
    public static DayOfWeek from(TemporalAccessor temporal) {
        if (temporal instanceof DayOfWeek) {
            return (DayOfWeek) temporal;
        }
        try {
            return of(temporal.get(DAY_OF_WEEK));
        } catch (DateTimeException ex) {
            throw new DateTimeException("Unable to obtain DayOfWeek from TemporalAccessor: "
                    + temporal + ", type " + temporal.getClass().getName(), ex);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the day-of-week {@code int} value.
     * <p>
     * The values are numbered following the ISO-8601 standard, from 1 (Monday) to 7 (Sunday).
     * See {@link WeekFields#dayOfWeek()} for localized week-numbering.
     *
     * @return the day-of-week, from 1 (Monday) to 7 (Sunday)
     */
    public int getValue() {
        return ordinal() + 1;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the textual representation, such as 'Mon' or 'Friday'.
     * <p>
     * This returns the textual name used to identify the day-of-week.
     * The parameters control the length of the returned text and the locale.
     * <p>
     * If no textual mapping is found then the {@link #getValue() numeric value} is returned.
     *
     * @param style  the length of the text required, not null
     * @param locale  the locale to use, not null
     * @return the text value of the day-of-week, not null
     */
    public String getDisplayName(TextStyle style, Locale locale) {
        return new DateTimeFormatterBuilder().appendText(DAY_OF_WEEK, style).toFormatter(locale).format(this);
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if the specified field is supported.
     * <p>
     * This checks if this day-of-week can be queried for the specified field.
     * If false, then calling the {@link #range(TemporalField) range} and
     * {@link #get(TemporalField) get} methods will throw an exception.
     * <p>
     * If the field is {@link ChronoField#DAY_OF_WEEK DAY_OF_WEEK} then
     * this method returns true.
     * All other {@code ChronoField} instances will return false.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.isSupportedBy(TemporalAccessor)}
     * passing {@code this} as the argument.
     * Whether the field is supported is determined by the field.
     *
     * @param field  the field to check, null returns false
     * @return true if the field is supported on this day-of-week, false if not
     */
    @Override
    public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return field == DAY_OF_WEEK;
        }
        return field != null && field.isSupportedBy(this);
    }

    /**
     * Gets the range of valid values for the specified field.
     * <p>
     * The range object expresses the minimum and maximum valid values for a field.
     * This day-of-week is used to enhance the accuracy of the returned range.
     * If it is not possible to return the range, because the field is not supported
     * or for some other reason, an exception is thrown.
     * <p>
     * If the field is {@link ChronoField#DAY_OF_WEEK DAY_OF_WEEK} then the
     * range of the day-of-week, from 1 to 7, will be returned.
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
    @Override
    public ValueRange range(TemporalField field) {
        if (field == DAY_OF_WEEK) {
            return field.range();
        } else if (field instanceof ChronoField) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.rangeRefinedBy(this);
    }

    /**
     * Gets the value of the specified field from this day-of-week as an {@code int}.
     * <p>
     * This queries this day-of-week for the value for the specified field.
     * The returned value will always be within the valid range of values for the field.
     * If it is not possible to return the value, because the field is not supported
     * or for some other reason, an exception is thrown.
     * <p>
     * If the field is {@link ChronoField#DAY_OF_WEEK DAY_OF_WEEK} then the
     * value of the day-of-week, from 1 to 7, will be returned.
     * All other {@code ChronoField} instances will throw a {@code DateTimeException}.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.getFrom(TemporalAccessor)}
     * passing {@code this} as the argument. Whether the value can be obtained,
     * and what the value represents, is determined by the field.
     *
     * @param field  the field to get, not null
     * @return the value for the field, within the valid range of values
     * @throws DateTimeException if a value for the field cannot be obtained
     * @throws DateTimeException if the range of valid values for the field exceeds an {@code int}
     * @throws DateTimeException if the value is outside the range of valid values for the field
     * @throws ArithmeticException if numeric overflow occurs
     */
    @Override
    public int get(TemporalField field) {
        if (field == DAY_OF_WEEK) {
            return getValue();
        }
        return range(field).checkValidIntValue(getLong(field), field);
    }

    /**
     * Gets the value of the specified field from this day-of-week as a {@code long}.
     * <p>
     * This queries this day-of-week for the value for the specified field.
     * If it is not possible to return the value, because the field is not supported
     * or for some other reason, an exception is thrown.
     * <p>
     * If the field is {@link ChronoField#DAY_OF_WEEK DAY_OF_WEEK} then the
     * value of the day-of-week, from 1 to 7, will be returned.
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
        if (field == DAY_OF_WEEK) {
            return getValue();
        } else if (field instanceof ChronoField) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.getFrom(this);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns the day-of-week that is the specified number of days after this one.
     * <p>
     * The calculation rolls around the end of the week from Sunday to Monday.
     * The specified period may be negative.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param days  the days to add, positive or negative
     * @return the resulting day-of-week, not null
     */
    public DayOfWeek plus(long days) {
        int amount = (int) (days % 7);
        return ENUMS[(ordinal() + (amount + 7)) % 7];
    }

    /**
     * Returns the day-of-week that is the specified number of days before this one.
     * <p>
     * The calculation rolls around the start of the year from Monday to Sunday.
     * The specified period may be negative.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param days  the days to subtract, positive or negative
     * @return the resulting day-of-week, not null
     */
    public DayOfWeek minus(long days) {
        return plus(-(days % 7));
    }

    //-----------------------------------------------------------------------
    /**
     * Queries this day-of-week using the specified query.
     * <p>
     * This queries this day-of-week using the specified query strategy object.
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
            return (R) DAYS;
        } else if (query == TemporalQueries.localDate()
                || query == TemporalQueries.localTime()
                || query == TemporalQueries.chronology()
                || query == TemporalQueries.zone()
                || query == TemporalQueries.zoneId()
                || query == TemporalQueries.offset()) {
            return null;
        }
        return query.queryFrom(this);
    }

    /**
     * Adjusts the specified temporal object to have this day-of-week.
     * <p>
     * This returns a temporal object of the same observable type as the input
     * with the day-of-week changed to be the same as this.
     * <p>
     * The adjustment is equivalent to using {@link Temporal#with(TemporalField, long)}
     * passing {@link ChronoField#DAY_OF_WEEK} as the field.
     * Note that this adjusts forwards or backwards within a Monday to Sunday week.
     * See {@link WeekFields#dayOfWeek()} for localized week start days.
     * See {@link TemporalAdjusters} for other adjusters
     * with more control, such as {@code next(MONDAY)}.
     * <p>
     * In most cases, it is clearer to reverse the calling pattern by using
     * {@link Temporal#with(TemporalAdjuster)}:
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   temporal = thisDayOfWeek.adjustInto(temporal);
     *   temporal = temporal.with(thisDayOfWeek);
     * </pre>
     * <p>
     * For example, given a date that is a Wednesday, the following are output:
     * <pre>
     *   dateOnWed.with(MONDAY);     // two days earlier
     *   dateOnWed.with(TUESDAY);    // one day earlier
     *   dateOnWed.with(WEDNESDAY);  // same date
     *   dateOnWed.with(THURSDAY);   // one day later
     *   dateOnWed.with(FRIDAY);     // two days later
     *   dateOnWed.with(SATURDAY);   // three days later
     *   dateOnWed.with(SUNDAY);     // four days later
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
        return temporal.with(DAY_OF_WEEK, getValue());
    }

}
