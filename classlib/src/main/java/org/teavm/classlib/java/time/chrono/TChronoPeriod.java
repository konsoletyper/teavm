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

import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;

public abstract class TChronoPeriod
        implements TTemporalAmount {

    public static TChronoPeriod between(TChronoLocalDate startDateInclusive, TChronoLocalDate endDateExclusive) {
        TJdk8Methods.requireNonNull(startDateInclusive, "startDateInclusive");
        TJdk8Methods.requireNonNull(endDateExclusive, "endDateExclusive");
        return startDateInclusive.until(endDateExclusive);
    }

    //-----------------------------------------------------------------------
    @Override
    public abstract long get(TTemporalUnit unit);

    @Override
    public abstract List<TTemporalUnit> getUnits();

    public abstract TChronology getChronology();

    //-----------------------------------------------------------------------
    public boolean isZero() {
        for (TTemporalUnit unit : getUnits()) {
            if (get(unit) != 0) {
                return false;
            }
        }
        return true;
    }

    public boolean isNegative() {
        for (TTemporalUnit unit : getUnits()) {
            if (get(unit) < 0) {
                return true;
            }
        }
        return false;
    }

    //-----------------------------------------------------------------------
    public abstract TChronoPeriod plus(TTemporalAmount amountToAdd);

    public abstract TChronoPeriod minus(TTemporalAmount amountToSubtract);

    //-----------------------------------------------------------------------
    public abstract TChronoPeriod multipliedBy(int scalar);

    public TChronoPeriod negated() {
        return multipliedBy(-1);
    }

    //-----------------------------------------------------------------------
    public abstract TChronoPeriod normalized();

    //-------------------------------------------------------------------------
    @Override
    public abstract TTemporal addTo(TTemporal temporal);

    @Override
    public abstract TTemporal subtractFrom(TTemporal temporal);

    //-----------------------------------------------------------------------
    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    //-----------------------------------------------------------------------
    @Override
    public abstract String toString();

}
