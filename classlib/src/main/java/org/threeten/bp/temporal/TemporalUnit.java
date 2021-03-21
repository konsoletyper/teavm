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

import org.threeten.bp.DateTimeException;
import org.threeten.bp.Duration;
import org.threeten.bp.Period;

/**
 * A unit of date-time, such as Days or Hours.
 * <p>
 * Measurement of time is built on units, such as years, months, days, hours, minutes and seconds.
 * Implementations of this interface represent those units.
 * <p>
 * An instance of this interface represents the unit itself, rather than an amount of the unit.
 * See {@link Period} for a class that represents an amount in terms of the common units.
 * <p>
 * The most commonly used units are defined in {@link ChronoUnit}.
 * Further units are supplied in {@link IsoFields}.
 * Units can also be written by application code by implementing this interface.
 * <p>
 * The unit works using double dispatch. Client code calls methods on a date-time like
 * {@code LocalDateTime} which check if the unit is a {@code ChronoUnit}.
 * If it is, then the date-time must handle it.
 * Otherwise, the method call is re-dispatched to the matching method in this interface.
 *
 * <h3>Specification for implementors</h3>
 * This interface must be implemented with care to ensure other classes operate correctly.
 * All implementations that can be instantiated must be final, immutable and thread-safe.
 * It is recommended to use an enum where possible.
 */
public interface TemporalUnit {

    /**
     * Gets the duration of this unit, which may be an estimate.
     * <p>
     * All units return a duration measured in standard nanoseconds from this method.
     * The duration will be positive and non-zero.
     * For example, an hour has a duration of {@code 60 * 60 * 1,000,000,000ns}.
     * <p>
     * Some units may return an accurate duration while others return an estimate.
     * For example, days have an estimated duration due to the possibility of
     * daylight saving time changes.
     * To determine if the duration is an estimate, use {@link #isDurationEstimated()}.
     *
     * @return the duration of this unit, which may be an estimate, not null
     */
    Duration getDuration();

    /**
     * Checks if the duration of the unit is an estimate.
     * <p>
     * All units have a duration, however the duration is not always accurate.
     * For example, days have an estimated duration due to the possibility of
     * daylight saving time changes.
     * This method returns true if the duration is an estimate and false if it is
     * accurate. Note that accurate/estimated ignores leap seconds.
     *
     * @return true if the duration is estimated, false if accurate
     */
    boolean isDurationEstimated();

    //-----------------------------------------------------------------------
    /**
     * Checks if this unit is date-based.
     * 
     * @return true if date-based
     */
    boolean isDateBased();

    /**
     * Checks if this unit is time-based.
     * 
     * @return true if time-based
     */
    boolean isTimeBased();

    //-----------------------------------------------------------------------
    /**
     * Checks if this unit is supported by the specified temporal object.
     * <p>
     * This checks that the implementing date-time can add/subtract this unit.
     * This can be used to avoid throwing an exception.
     *
     * @param temporal  the temporal object to check, not null
     * @return true if the unit is supported
     */
    boolean isSupportedBy(Temporal temporal);

    /**
     * Returns a copy of the specified temporal object with the specified period added.
     * <p>
     * The period added is a multiple of this unit. For example, this method
     * could be used to add "3 days" to a date by calling this method on the
     * instance representing "days", passing the date and the period "3".
     * The period to be added may be negative, which is equivalent to subtraction.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method directly.
     * The second is to use {@link Temporal#plus(long, TemporalUnit)}:
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   temporal = thisUnit.doPlus(temporal);
     *   temporal = temporal.plus(thisUnit);
     * </pre>
     * It is recommended to use the second approach, {@code plus(TemporalUnit)},
     * as it is a lot clearer to read in code.
     * <p>
     * Implementations should perform any queries or calculations using the units
     * available in {@link ChronoUnit} or the fields available in {@link ChronoField}.
     * If the field is not supported a {@code DateTimeException} must be thrown.
     * <p>
     * Implementations must not alter the specified temporal object.
     * Instead, an adjusted copy of the original must be returned.
     * This provides equivalent, safe behavior for immutable and mutable implementations.
     *
     * @param <R>  the type of the Temporal object
     * @param dateTime  the temporal object to adjust, not null
     * @param periodToAdd  the period of this unit to add, positive or negative
     * @return the adjusted temporal object, not null
     * @throws DateTimeException if the period cannot be added
     */
    <R extends Temporal> R addTo(R dateTime, long periodToAdd);

    //-----------------------------------------------------------------------
    /**
     * Calculates the period in terms of this unit between two temporal objects of the same type.
     * <p>
     * This calculates the period between two temporals in terms of this unit.
     * The start and end points are supplied as temporal objects and must be of the same type.
     * The result will be negative if the end is before the start.
     * For example, the period in hours between two temporal objects can be calculated
     * using {@code HOURS.between(startTime, endTime)}.
     * <p>
     * The calculation returns a whole number, representing the number of complete units between the two temporals.
     * For example, the period in hours between the times 11:30 and 13:29 will only b
     * one hour as it is one minute short of two hours.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method directly.
     * The second is to use {@link Temporal#until(Temporal, TemporalUnit)}:
     * <pre>
     *   // these two lines are equivalent
     *   between = thisUnit.between(start, end);
     *   between = start.until(end, thisUnit);
     * </pre>
     * The choice should be made based on which makes the code more readable. 
     * <p>
     * For example, this method allows the number of days between two dates to be calculated:
     * <pre>
     *   long daysBetween = DAYS.between(start, end);
     *   // or alternatively
     *   long daysBetween = start.until(end, DAYS);
     * </pre>
     * Implementations should perform any queries or calculations using the units available in
     * {@link ChronoUnit} or the fields available in {@link ChronoField}.
     * If the unit is not supported a DateTimeException must be thrown.
     * Implementations must not alter the specified temporal objects.
     *
     * @param temporal1  the base temporal object, not null
     * @param temporal2  the other temporal object, not null
     * @return the period between temporal1 and temporal2 in terms of this unit;
     *  positive if temporal2 is later than temporal1, negative if earlier
     * @throws DateTimeException if the period cannot be calculated
     * @throws ArithmeticException if numeric overflow occurs
     */
    long between(Temporal temporal1, Temporal temporal2);

    //-----------------------------------------------------------------------
    /**
     * Outputs this unit as a {@code String} using the name.
     *
     * @return the name of this unit, not null
     */
    @Override
    String toString();

}
