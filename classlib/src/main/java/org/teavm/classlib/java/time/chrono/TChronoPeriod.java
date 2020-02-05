/*
 *  Copyright 2020, adopted to TeaVM by Joerg Hohwiller
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
package org.teavm.classlib.java.time.chrono;

import java.util.List;
import java.util.Objects;

import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;

public interface TChronoPeriod extends TTemporalAmount {

    static TChronoPeriod between(TChronoLocalDate startDateInclusive, TChronoLocalDate endDateExclusive) {

        Objects.requireNonNull(startDateInclusive, "startDateInclusive");
        Objects.requireNonNull(endDateExclusive, "endDateExclusive");
        return startDateInclusive.until(endDateExclusive);
    }

    @Override
    long get(TTemporalUnit unit);

    @Override
    List<TTemporalUnit> getUnits();

    TChronology getChronology();

    default boolean isZero() {

        for (TTemporalUnit unit : getUnits()) {
            if (get(unit) != 0) {
                return false;
            }
        }
        return true;
    }

    default boolean isNegative() {

        for (TTemporalUnit unit : getUnits()) {
            if (get(unit) < 0) {
                return true;
            }
        }
        return false;
    }

    TChronoPeriod plus(TTemporalAmount amountToAdd);

    TChronoPeriod minus(TTemporalAmount amountToSubtract);

    TChronoPeriod multipliedBy(int scalar);

    default TChronoPeriod negated() {

        return multipliedBy(-1);
    }

    TChronoPeriod normalized();

    @Override
    TTemporal addTo(TTemporal temporal);

    @Override
    TTemporal subtractFrom(TTemporal temporal);

}
