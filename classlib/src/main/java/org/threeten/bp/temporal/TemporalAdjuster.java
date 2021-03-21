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

/**
 * Strategy for adjusting a temporal object.
 * <p>
 * Adjusters are a key tool for modifying temporal objects.
 * They exist to externalize the process of adjustment, permitting different
 * approaches, as per the strategy design pattern.
 * Examples might be an adjuster that sets the date avoiding weekends, or one that
 * sets the date to the last day of the month.
 * <p>
 * There are two equivalent ways of using a {@code TemporalAdjuster}.
 * The first is to invoke the method on this interface directly.
 * The second is to use {@link Temporal#with(TemporalAdjuster)}:
 * <pre>
 *   // these two lines are equivalent, but the second approach is recommended
 *   temporal = thisAdjuster.adjustInto(temporal);
 *   temporal = temporal.with(thisAdjuster);
 * </pre>
 * It is recommended to use the second approach, {@code with(TemporalAdjuster)},
 * as it is a lot clearer to read in code.
 * <p>
 * See {@link TemporalAdjusters} for a standard set of adjusters, including finding the
 * last day of the month.
 * Adjusters may also be defined by applications.
 *
 * <h3>Specification for implementors</h3>
 * This interface places no restrictions on the mutability of implementations,
 * however immutability is strongly recommended.
 */
public interface TemporalAdjuster {

    /**
     * Adjusts the specified temporal object.
     * <p>
     * This adjusts the specified temporal object using the logic
     * encapsulated in the implementing class.
     * Examples might be an adjuster that sets the date avoiding weekends, or one that
     * sets the date to the last day of the month.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method directly.
     * The second is to use {@link Temporal#with(TemporalAdjuster)}:
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   temporal = thisAdjuster.adjustInto(temporal);
     *   temporal = temporal.with(thisAdjuster);
     * </pre>
     * It is recommended to use the second approach, {@code with(TemporalAdjuster)},
     * as it is a lot clearer to read in code.
     *
     * <h3>Specification for implementors</h3>
     * The implementation must take the input object and adjust it.
     * The implementation defines the logic of the adjustment and is responsible for
     * documenting that logic. It may use any method on {@code Temporal} to
     * query the temporal object and perform the adjustment.
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
     * @return an object of the same observable type with the adjustment made, not null
     * @throws DateTimeException if unable to make the adjustment
     * @throws ArithmeticException if numeric overflow occurs
     */
    Temporal adjustInto(Temporal temporal);

}
