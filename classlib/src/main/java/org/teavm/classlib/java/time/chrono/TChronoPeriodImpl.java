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
import java.util.Objects;

import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;

final class TChronoPeriodImpl extends TChronoPeriod implements Serializable {

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
            return this.years;
        }
        if (unit == MONTHS) {
            return this.months;
        }
        if (unit == DAYS) {
            return this.days;
        }
        throw new TUnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    @Override
    public List<TTemporalUnit> getUnits() {

        return Collections.unmodifiableList(Arrays.<TTemporalUnit> asList(YEARS, MONTHS, DAYS));
    }

    @Override
    public TChronology getChronology() {

        return this.chronology;
    }

    @Override
    public TChronoPeriod plus(TTemporalAmount amountToAdd) {

        if (amountToAdd instanceof TChronoPeriodImpl) {
            TChronoPeriodImpl amount = (TChronoPeriodImpl) amountToAdd;
            if (amount.getChronology().equals(getChronology())) {
                return new TChronoPeriodImpl(this.chronology, Math.addExact(this.years, amount.years),
                        Math.addExact(this.months, amount.months), Math.addExact(this.days, amount.days));
            }
        }
        throw new TDateTimeException("Unable to add amount: " + amountToAdd);
    }

    @Override
    public TChronoPeriod minus(TTemporalAmount amountToSubtract) {

        if (amountToSubtract instanceof TChronoPeriodImpl) {
            TChronoPeriodImpl amount = (TChronoPeriodImpl) amountToSubtract;
            if (amount.getChronology().equals(getChronology())) {
                return new TChronoPeriodImpl(this.chronology, Math.subtractExact(this.years, amount.years),
                        Math.subtractExact(this.months, amount.months), Math.subtractExact(this.days, amount.days));
            }
        }
        throw new TDateTimeException("Unable to subtract amount: " + amountToSubtract);
    }

    @Override
    public TChronoPeriod multipliedBy(int scalar) {

        return new TChronoPeriodImpl(this.chronology, Math.multiplyExact(this.years, scalar),
                Math.multiplyExact(this.months, scalar), Math.multiplyExact(this.days, scalar));
    }

    @Override
    public TChronoPeriod normalized() {

        if (this.chronology.range(TChronoField.MONTH_OF_YEAR).isFixed()) {
            long monthLength = this.chronology.range(TChronoField.MONTH_OF_YEAR).getMaximum()
                    - this.chronology.range(TChronoField.MONTH_OF_YEAR).getMinimum() + 1;
            long total = this.years * monthLength + this.months;
            int years = Math.toIntExact(total / monthLength);
            int months = Math.toIntExact(total % monthLength);
            return new TChronoPeriodImpl(this.chronology, years, months, this.days);
        }
        return this;
    }

    @Override
    public TTemporal addTo(TTemporal temporal) {

        Objects.requireNonNull(temporal, "temporal");
        TChronology temporalChrono = temporal.query(TTemporalQueries.chronology());
        if (temporalChrono != null && this.chronology.equals(temporalChrono) == false) {
            throw new TDateTimeException("Invalid chronology, required: " + this.chronology.getId() + ", but was: "
                    + temporalChrono.getId());
        }
        if (this.years != 0) {
            temporal = temporal.plus(this.years, YEARS);
        }
        if (this.months != 0) {
            temporal = temporal.plus(this.months, MONTHS);
        }
        if (this.days != 0) {
            temporal = temporal.plus(this.days, DAYS);
        }
        return temporal;
    }

    @Override
    public TTemporal subtractFrom(TTemporal temporal) {

        Objects.requireNonNull(temporal, "temporal");
        TChronology temporalChrono = temporal.query(TTemporalQueries.chronology());
        if (temporalChrono != null && this.chronology.equals(temporalChrono) == false) {
            throw new TDateTimeException("Invalid chronology, required: " + this.chronology.getId() + ", but was: "
                    + temporalChrono.getId());
        }
        if (this.years != 0) {
            temporal = temporal.minus(this.years, YEARS);
        }
        if (this.months != 0) {
            temporal = temporal.minus(this.months, MONTHS);
        }
        if (this.days != 0) {
            temporal = temporal.minus(this.days, DAYS);
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
            return this.years == other.years && this.months == other.months && this.days == other.days
                    && this.chronology.equals(other.chronology);
        }
        return false;
    }

    @Override
    public int hashCode() {

        return this.chronology.hashCode() + Integer.rotateLeft(this.years, 16) + Integer.rotateLeft(this.months, 8)
                + this.days;
    }

    @Override
    public String toString() {

        if (isZero()) {
            return this.chronology + " P0D";
        } else {
            StringBuilder buf = new StringBuilder();
            buf.append(this.chronology).append(' ').append('P');
            if (this.years != 0) {
                buf.append(this.years).append('Y');
            }
            if (this.months != 0) {
                buf.append(this.months).append('M');
            }
            if (this.days != 0) {
                buf.append(this.days).append('D');
            }
            return buf.toString();
        }
    }

}
