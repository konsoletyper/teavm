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

import java.util.List;
import org.threeten.bp.DateTimeException;
import org.threeten.bp.Duration;
import org.threeten.bp.Period;

/**
 * Framework-level interface defining an amount of time,
 * such as "6 hours", "8 days" or "2 years and 3 months". 
 * <p>
 * This is the base interface type for amounts of time.
 * An amount is distinct from a date or time-of-day in that it is not tied
 * to any specific point on the time-line.
 * <p>
 * The amount can be thought of as a Map of {@code TemporalUnit} to long,
 * exposed via {@link #getUnits()} and {@link #get(TemporalUnit)}.
 * A simple case might have a single unit-value pair, such as "6 hours".
 * A more complex case may have multiple unit-value pairs, such as "7 years, 3 months and 5 days".
 * <p>
 * There are two common implementations.
 * {@link Period} is a date-based implementation, storing years, months and days.
 * {@link Duration} is a time-based implementation, storing seconds and
 * nanoseconds, but providing some access using other duration based units
 * such as minutes, hours and fixed 24-hour days.
 * <p>
 * This interface is a framework-level interface that should not be widely used
 * in application code. Instead, applications should create and pass around
 * instances of concrete types, such as {@code Period} and {@code Duration}.
 *
 * <h3>Specification for implementors</h3>
 * This interface places no restrictions on the mutability of implementations,
 * however immutability is strongly recommended.
 */
public interface TemporalAmount {

    /**
     * Gets the list of units, from largest to smallest, that fully define this amount.
     * 
     * @return the list of units.
     */
    List<TemporalUnit> getUnits();

    /**
     * Gets the amount associated with the specified unit.
     * 
     * @param unit  the unit to get, not null
     * @return the amount of the unit
     * @throws DateTimeException if the amount cannot be obtained
     */
    long get(TemporalUnit unit);

    /**
     * Adds to the specified temporal object.
     * <p>
     * This adds to the specified temporal object using the logic
     * encapsulated in the implementing class.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method directly.
     * The second is to use {@link Temporal#plus(TemporalAmount)}:
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   dateTime = amount.addTo(dateTime);
     *   dateTime = dateTime.plus(amount);
     * </pre>
     * It is recommended to use the second approach, {@code plus(TemporalAmount)},
     * as it is a lot clearer to read in code.
     *
     * <h3>Specification for implementors</h3>
     * The implementation must take the input object and add to it.
     * The implementation defines the logic of the addition and is responsible for
     * documenting that logic. It may use any method on {@code Temporal} to
     * query the temporal object and perform the addition.
     * The returned object must have the same observable type as the input object
     * <p>
     * The input object must not be altered.
     * Instead, an adjusted copy of the original must be returned.
     * This provides equivalent, safe behavior for immutable and mutable temporal objects.
     * <p>
     * The input temporal object may be in a calendar system other than ISO.
     * Implementations may choose to document compatibility with other calendar systems,
     * or reject non-ISO temporal objects by {@link TemporalQueries#chronology() querying the chronology}.
     * <p>
     * This method may be called from multiple threads in parallel.
     * It must be thread-safe when invoked.
     *
     * @param temporal  the temporal object to adjust, not null
     * @return an object of the same observable type with the addition made, not null
     * @throws DateTimeException if unable to add
     * @throws ArithmeticException if numeric overflow occurs
     */
    Temporal addTo(Temporal temporal);

    /**
     * Subtracts this object from the specified temporal object.
     * <p>
     * This adds to the specified temporal object using the logic
     * encapsulated in the implementing class.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method directly.
     * The second is to use {@link Temporal#minus(TemporalAmount)}:
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   dateTime = amount.subtractFrom(dateTime);
     *   dateTime = dateTime.minus(amount);
     * </pre>
     * It is recommended to use the second approach, {@code minus(TemporalAmount)},
     * as it is a lot clearer to read in code.
     *
     * <h3>Specification for implementors</h3>
     * The implementation must take the input object and subtract from it.
     * The implementation defines the logic of the subtraction and is responsible for
     * documenting that logic. It may use any method on {@code Temporal} to
     * query the temporal object and perform the subtraction.
     * The returned object must have the same observable type as the input object
     * <p>
     * The input object must not be altered.
     * Instead, an adjusted copy of the original must be returned.
     * This provides equivalent, safe behavior for immutable and mutable temporal objects.
     * <p>
     * The input temporal object may be in a calendar system other than ISO.
     * Implementations may choose to document compatibility with other calendar systems,
     * or reject non-ISO temporal objects by {@link TemporalQueries#chronology() querying the chronology}.
     * <p>
     * This method may be called from multiple threads in parallel.
     * It must be thread-safe when invoked.
     *
     * @param temporal  the temporal object to adjust, not null
     * @return an object of the same observable type with the subtraction made, not null
     * @throws DateTimeException if unable to subtract
     * @throws ArithmeticException if numeric overflow occurs
     */
    Temporal subtractFrom(Temporal temporal);

}
