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

import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MONTHS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.YEARS;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;

final class TChronoPeriodImpl
        extends TChronoPeriod
        implements Serializable {

    private static final long serialVersionUID = 275618735781L;

    private final TChronology chronology;
    private final int years;
    private final int months;
    private final int days;

    public TChronoPeriodImpl(TChronology chronology, int years, int months, int days) {
        this.chronology = chronology;
        this.years = years;
        this.months = months;
        this.days = days;
    }

    @Override
    public long get(TTemporalUnit unit) {
        if (unit == YEARS) {
            return years;
        }
        if (unit == MONTHS) {
            return months;
        }
        if (unit == DAYS) {
            return days;
        }
        throw new TUnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    @Override
    public List<TTemporalUnit> getUnits() {
        return Collections.unmodifiableList(Arrays.<TTemporalUnit>asList(YEARS, MONTHS, DAYS));
    }

    @Override
    public TChronology getChronology() {
        return chronology;
    }

    @Override
    public TChronoPeriod plus(TTemporalAmount amountToAdd) {
        if (amountToAdd instanceof TChronoPeriodImpl) {
            TChronoPeriodImpl amount = (TChronoPeriodImpl) amountToAdd;
            if (amount.getChronology().equals(getChronology())) {
                return new TChronoPeriodImpl(
                        chronology,
                        TJdk8Methods.safeAdd(years, amount.years),
                        TJdk8Methods.safeAdd(months, amount.months),
                        TJdk8Methods.safeAdd(days, amount.days));
            }
        }
        throw new TDateTimeException("Unable to add amount: " + amountToAdd);
    }

    @Override
    public TChronoPeriod minus(TTemporalAmount amountToSubtract) {
        if (amountToSubtract instanceof TChronoPeriodImpl) {
            TChronoPeriodImpl amount = (TChronoPeriodImpl) amountToSubtract;
            if (amount.getChronology().equals(getChronology())) {
                return new TChronoPeriodImpl(
                        chronology,
                        TJdk8Methods.safeSubtract(years, amount.years),
                        TJdk8Methods.safeSubtract(months, amount.months),
                        TJdk8Methods.safeSubtract(days, amount.days));
            }
        }
        throw new TDateTimeException("Unable to subtract amount: " + amountToSubtract);
    }

    @Override
    public TChronoPeriod multipliedBy(int scalar) {
        return new TChronoPeriodImpl(
                chronology,
                TJdk8Methods.safeMultiply(years, scalar),
                TJdk8Methods.safeMultiply(months, scalar),
                TJdk8Methods.safeMultiply(days, scalar));
    }

    @Override
    public TChronoPeriod normalized() {
        if (chronology.range(TChronoField.MONTH_OF_YEAR).isFixed()) {
            long monthLength = chronology.range(TChronoField.MONTH_OF_YEAR).getMaximum() - chronology.range(TChronoField.MONTH_OF_YEAR).getMinimum() + 1;
            long total = years * monthLength + months;
            int years = TJdk8Methods.safeToInt(total / monthLength);
            int months = TJdk8Methods.safeToInt(total % monthLength);
            return new TChronoPeriodImpl(chronology, years, months, days);
        }
        return this;
    }

    @Override
    public TTemporal addTo(TTemporal temporal) {
        TJdk8Methods.requireNonNull(temporal, "temporal");
        TChronology temporalChrono = temporal.query(TTemporalQueries.chronology());
        if (temporalChrono != null && chronology.equals(temporalChrono) == false) {
            throw new TDateTimeException("Invalid chronology, required: " + chronology.getId() + ", but was: " + temporalChrono.getId());
        }
        if (years != 0) {
            temporal = temporal.plus(years, YEARS);
        }
        if (months != 0) {
            temporal = temporal.plus(months, MONTHS);
        }
        if (days != 0) {
            temporal = temporal.plus(days, DAYS);
        }
        return temporal;
    }

    @Override
    public TTemporal subtractFrom(TTemporal temporal) {
        TJdk8Methods.requireNonNull(temporal, "temporal");
        TChronology temporalChrono = temporal.query(TTemporalQueries.chronology());
        if (temporalChrono != null && chronology.equals(temporalChrono) == false) {
            throw new TDateTimeException("Invalid chronology, required: " + chronology.getId() + ", but was: " + temporalChrono.getId());
        }
        if (years != 0) {
            temporal = temporal.minus(years, YEARS);
        }
        if (months != 0) {
            temporal = temporal.minus(months, MONTHS);
        }
        if (days != 0) {
            temporal = temporal.minus(days, DAYS);
        }
        return temporal;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TChronoPeriodImpl) {
            TChronoPeriodImpl other = (TChronoPeriodImpl) obj;
            return years == other.years && months == other.months &&
                    days == other.days && chronology.equals(other.chronology);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return chronology.hashCode() + Integer.rotateLeft(years, 16) + Integer.rotateLeft(months, 8) + days;
    }

    @Override
    public String toString() {
        if (isZero()) {
            return chronology + " P0D";
        } else {
            StringBuilder buf = new StringBuilder();
            buf.append(chronology).append(' ').append('P');
            if (years != 0) {
                buf.append(years).append('Y');
            }
            if (months != 0) {
                buf.append(months).append('M');
            }
            if (days != 0) {
                buf.append(days).append('D');
            }
            return buf.toString();
        }
    }

}
