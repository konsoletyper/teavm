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

import java.util.Locale;
import java.util.Map;
import org.threeten.bp.DateTimeException;
import org.threeten.bp.format.ResolverStyle;

/**
 * A field of date-time, such as month-of-year or hour-of-minute.
 * <p>
 * Date and time is expressed using fields which partition the time-line into something
 * meaningful for humans. Implementations of this interface represent those fields.
 * <p>
 * The most commonly used units are defined in {@link ChronoField}.
 * Further fields are supplied in {@link IsoFields}, {@link WeekFields} and {@link JulianFields}.
 * Fields can also be written by application code by implementing this interface.
 * <p>
 * The field works using double dispatch. Client code calls methods on a date-time like
 * {@code LocalDateTime} which check if the field is a {@code ChronoField}.
 * If it is, then the date-time must handle it.
 * Otherwise, the method call is re-dispatched to the matching method in this interface.
 *
 * <h3>Specification for implementors</h3>
 * This interface must be implemented with care to ensure other classes operate correctly.
 * All implementations that can be instantiated must be final, immutable and thread-safe.
 * Implementations should be {@code Serializable} where possible.
 * An enum is as effective implementation choice.
 */
public interface TemporalField {

    /**
     * Gets the unit that the field is measured in.
     * <p>
     * The unit of the field is the period that varies within the range.
     * For example, in the field 'MonthOfYear', the unit is 'Months'.
     * See also {@link #getRangeUnit()}.
     *
     * @return the period unit defining the base unit of the field, not null
     */
    TemporalUnit getBaseUnit();

    /**
     * Gets the range that the field is bound by.
     * <p>
     * The range of the field is the period that the field varies within.
     * For example, in the field 'MonthOfYear', the range is 'Years'.
     * See also {@link #getBaseUnit()}.
     * <p>
     * The range is never null. For example, the 'Year' field is shorthand for
     * 'YearOfForever'. It therefore has a unit of 'Years' and a range of 'Forever'.
     *
     * @return the period unit defining the range of the field, not null
     */
    TemporalUnit getRangeUnit();

    //-----------------------------------------------------------------------
    /**
     * Gets the range of valid values for the field.
     * <p>
     * All fields can be expressed as a {@code long} integer.
     * This method returns an object that describes the valid range for that value.
     * This method is generally only applicable to the ISO-8601 calendar system.
     * <p>
     * Note that the result only describes the minimum and maximum valid values
     * and it is important not to read too much into them. For example, there
     * could be values within the range that are invalid for the field.
     *
     * @return the range of valid values for the field, not null
     */
    ValueRange range();

    //-----------------------------------------------------------------------
    /**
     * Checks if this field is date-based.
     * <p>
     * A date-based field can be derived from epoch-day
     * 
     * @return true if date-based
     */
    boolean isDateBased();

    /**
     * Checks if this field is time-based.
     * <p>
     * A time-based field can be derived from nano-of-day
     * 
     * @return true if time-based
     */
    boolean isTimeBased();

    //-----------------------------------------------------------------------
    /**
     * Checks if this field is supported by the temporal object.
     * <p>
     * This determines whether the temporal accessor supports this field.
     * If this returns false, the the temporal cannot be queried for this field.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method directly.
     * The second is to use {@link TemporalAccessor#isSupported(TemporalField)}:
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   temporal = thisField.isSupportedBy(temporal);
     *   temporal = temporal.isSupported(thisField);
     * </pre>
     * It is recommended to use the second approach, {@code isSupported(TemporalField)},
     * as it is a lot clearer to read in code.
     * <p>
     * Implementations should determine whether they are supported using the fields
     * available in {@link ChronoField}.
     *
     * @param temporal  the temporal object to query, not null
     * @return true if the date-time can be queried for this field, false if not
     */
    boolean isSupportedBy(TemporalAccessor temporal);

    /**
     * Get the range of valid values for this field using the temporal object to
     * refine the result.
     * <p>
     * This uses the temporal object to find the range of valid values for the field.
     * This is similar to {@link #range()}, however this method refines the result
     * using the temporal. For example, if the field is {@code DAY_OF_MONTH} the
     * {@code range} method is not accurate as there are four possible month lengths,
     * 28, 29, 30 and 31 days. Using this method with a date allows the range to be
     * accurate, returning just one of those four options.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method directly.
     * The second is to use {@link TemporalAccessor#range(TemporalField)}:
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   temporal = thisField.rangeRefinedBy(temporal);
     *   temporal = temporal.range(thisField);
     * </pre>
     * It is recommended to use the second approach, {@code range(TemporalField)},
     * as it is a lot clearer to read in code.
     * <p>
     * Implementations should perform any queries or calculations using the fields
     * available in {@link ChronoField}.
     * If the field is not supported a {@code DateTimeException} must be thrown.
     *
     * @param temporal  the temporal object used to refine the result, not null
     * @return the range of valid values for this field, not null
     * @throws DateTimeException if the range for the field cannot be obtained
     */
    ValueRange rangeRefinedBy(TemporalAccessor temporal);

    /**
     * Gets the value of this field from the specified temporal object.
     * <p>
     * This queries the temporal object for the value of this field.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method directly.
     * The second is to use {@link TemporalAccessor#getLong(TemporalField)}
     * (or {@link TemporalAccessor#get(TemporalField)}):
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   temporal = thisField.getFrom(temporal);
     *   temporal = temporal.getLong(thisField);
     * </pre>
     * It is recommended to use the second approach, {@code getLong(TemporalField)},
     * as it is a lot clearer to read in code.
     * <p>
     * Implementations should perform any queries or calculations using the fields
     * available in {@link ChronoField}.
     * If the field is not supported a {@code DateTimeException} must be thrown.
     *
     * @param temporal  the temporal object to query, not null
     * @return the value of this field, not null
     * @throws DateTimeException if a value for the field cannot be obtained
     */
    long getFrom(TemporalAccessor temporal);

    /**
     * Gets the display name for the field in the requested locale.
     * <p>
     * If there is no display name for the locale then a suitable default must be returned.
     * <p>
     * The default implementation must check the locale is not null
     * and return {@code toString()}.
     *
     * @param locale  the locale to use, not null
     * @return the display name for the locale or a suitable default, not null
     */
    String getDisplayName(Locale locale);

    /**
     * Returns a copy of the specified temporal object with the value of this field set.
     * <p>
     * This returns a new temporal object based on the specified one with the value for
     * this field changed. For example, on a {@code LocalDate}, this could be used to
     * set the year, month or day-of-month.
     * The returned object has the same observable type as the specified object.
     * <p>
     * In some cases, changing a field is not fully defined. For example, if the target object is
     * a date representing the 31st January, then changing the month to February would be unclear.
     * In cases like this, the implementation is responsible for resolving the result.
     * Typically it will choose the previous valid date, which would be the last valid
     * day of February in this example.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method directly.
     * The second is to use {@link Temporal#with(TemporalField, long)}:
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   temporal = thisField.adjustInto(temporal);
     *   temporal = temporal.with(thisField);
     * </pre>
     * It is recommended to use the second approach, {@code with(TemporalField)},
     * as it is a lot clearer to read in code.
     * <p>
     * Implementations should perform any queries or calculations using the fields
     * available in {@link ChronoField}.
     * If the field is not supported a {@code DateTimeException} must be thrown.
     * <p>
     * Implementations must not alter the specified temporal object.
     * Instead, an adjusted copy of the original must be returned.
     * This provides equivalent, safe behavior for immutable and mutable implementations.
     *
     * @param <R>  the type of the Temporal object
     * @param temporal the temporal object to adjust, not null
     * @param newValue the new value of the field
     * @return the adjusted temporal object, not null
     * @throws DateTimeException if the field cannot be set
     */
    <R extends Temporal> R adjustInto(R temporal, long newValue);

    /**
     * Resolves the date/time information in the builder
     * <p>
     * This method is invoked during the resolve of the builder.
     * Implementations should combine the associated field with others to form
     * objects like {@code LocalDate}, {@code LocalTime} and {@code LocalDateTime}
     *
     * @param fieldValues  the map of fields to values, which can be updated, not null
     * @param partialTemporal  the partially complete temporal to query for zone and
     *  chronology; querying for other things is undefined and not recommended, not null
     * @param resolverStyle  the requested type of resolve, not null
     * @return the resolved temporal object; null if resolving only
     *  changed the map, or no resolve occurred
     * @throws ArithmeticException if numeric overflow occurs
     * @throws DateTimeException if resolving results in an error. This must not be thrown
     *  by querying a field on the temporal without first checking if it is supported
     */
    TemporalAccessor resolve(
                    Map<TemporalField, Long> fieldValues,
                    TemporalAccessor partialTemporal,
                    ResolverStyle resolverStyle);

}
