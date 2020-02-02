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

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;

abstract class TChronoLocalDateImpl<D extends TChronoLocalDate> extends TChronoLocalDate
        implements TTemporal, TTemporalAdjuster, TSerializable {

    TChronoLocalDateImpl() {

    }

    @SuppressWarnings("unchecked")
    @Override
    public TChronoLocalDateImpl<D> plus(long amountToAdd, TTemporalUnit unit) {

        if (unit instanceof TChronoUnit) {
            TChronoUnit f = (TChronoUnit) unit;
            switch (f) {
                case DAYS:
                    return plusDays(amountToAdd);
                case WEEKS:
                    return plusDays(Math.multiplyExact(amountToAdd, 7));
                case MONTHS:
                    return plusMonths(amountToAdd);
                case YEARS:
                    return plusYears(amountToAdd);
                case DECADES:
                    return plusYears(Math.multiplyExact(amountToAdd, 10));
                case CENTURIES:
                    return plusYears(Math.multiplyExact(amountToAdd, 100));
                case MILLENNIA:
                    return plusYears(Math.multiplyExact(amountToAdd, 1000));
                // case ERAS: throw new TDateTimeException("Unable to add era, standard calendar system only has one
                // era");
                // case FOREVER: return (period == 0 ? this : (period > 0 ? TLocalDate.MAX_DATE : TLocalDate.MIN_DATE));
            }
            throw new TDateTimeException(unit + " not valid for chronology " + getChronology().getId());
        }
        return (TChronoLocalDateImpl<D>) ((TAbstractChronology) getChronology())
                .ensureChronoLocalDate(unit.addTo(this, amountToAdd));
    }

    abstract TChronoLocalDateImpl<D> plusYears(long yearsToAdd);

    abstract TChronoLocalDateImpl<D> plusMonths(long monthsToAdd);

    TChronoLocalDateImpl<D> plusWeeks(long weeksToAdd) {

        return plusDays(Math.multiplyExact(weeksToAdd, 7));
    }

    abstract TChronoLocalDateImpl<D> plusDays(long daysToAdd);

    TChronoLocalDateImpl<D> minusYears(long yearsToSubtract) {

        return (yearsToSubtract == Long.MIN_VALUE ? plusYears(Long.MAX_VALUE).plusYears(1)
                : plusYears(-yearsToSubtract));
    }

    TChronoLocalDateImpl<D> minusMonths(long monthsToSubtract) {

        return (monthsToSubtract == Long.MIN_VALUE ? plusMonths(Long.MAX_VALUE).plusMonths(1)
                : plusMonths(-monthsToSubtract));
    }

    TChronoLocalDateImpl<D> minusWeeks(long weeksToSubtract) {

        return (weeksToSubtract == Long.MIN_VALUE ? plusWeeks(Long.MAX_VALUE).plusWeeks(1)
                : plusWeeks(-weeksToSubtract));
    }

    TChronoLocalDateImpl<D> minusDays(long daysToSubtract) {

        return (daysToSubtract == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1) : plusDays(-daysToSubtract));
    }

    @Override
    public TChronoLocalDateTime<?> atTime(TLocalTime localTime) {

        return TChronoLocalDateTimeImpl.of(this, localTime);
    }

    @Override
    public long until(TTemporal endExclusive, TTemporalUnit unit) {

        TChronoLocalDate end = getChronology().date(endExclusive);
        if (unit instanceof TChronoUnit) {
            return TLocalDate.from(this).until(end, unit); // TODO: this is wrong
        }
        return unit.between(this, end);
    }

    @Override
    public TChronoPeriod until(TChronoLocalDate endDate) {

        throw new UnsupportedOperationException();
    }

}
