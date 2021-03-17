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
package org.threeten.bp.chrono;

import static org.threeten.bp.temporal.ChronoUnit.DAYS;
import static org.threeten.bp.temporal.ChronoUnit.MONTHS;
import static org.threeten.bp.temporal.ChronoUnit.YEARS;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.threeten.bp.DateTimeException;
import org.threeten.bp.jdk8.Jdk8Methods;
import org.threeten.bp.temporal.ChronoField;
import org.threeten.bp.temporal.Temporal;
import org.threeten.bp.temporal.TemporalAmount;
import org.threeten.bp.temporal.TemporalQueries;
import org.threeten.bp.temporal.TemporalUnit;
import org.threeten.bp.temporal.UnsupportedTemporalTypeException;

/**
 * An implementation of {@code ChronoPeriod}.
 */
final class ChronoPeriodImpl
        extends ChronoPeriod
        implements Serializable {

    private final Chronology chronology;
    private final int years;
    private final int months;
    private final int days;

    public ChronoPeriodImpl(Chronology chronology, int years, int months, int days) {
        this.chronology = chronology;
        this.years = years;
        this.months = months;
        this.days = days;
    }

    //-------------------------------------------------------------------------
    @Override
    public long get(TemporalUnit unit) {
        if (unit == YEARS) {
            return years;
        }
        if (unit == MONTHS) {
            return months;
        }
        if (unit == DAYS) {
            return days;
        }
        throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    @Override
    public List<TemporalUnit> getUnits() {
        return Collections.unmodifiableList(Arrays.<TemporalUnit>asList(YEARS, MONTHS, DAYS));
    }

    @Override
    public Chronology getChronology() {
        return chronology;
    }

    //-------------------------------------------------------------------------
    @Override
    public ChronoPeriod plus(TemporalAmount amountToAdd) {
        if (amountToAdd instanceof ChronoPeriodImpl) {
            ChronoPeriodImpl amount = (ChronoPeriodImpl) amountToAdd;
            if (amount.getChronology().equals(getChronology())) {
                return new ChronoPeriodImpl(
                        chronology,
                        Jdk8Methods.safeAdd(years, amount.years),
                        Jdk8Methods.safeAdd(months, amount.months),
                        Jdk8Methods.safeAdd(days, amount.days));
            }
        }
        throw new DateTimeException("Unable to add amount: " + amountToAdd);
    }

    @Override
    public ChronoPeriod minus(TemporalAmount amountToSubtract) {
        if (amountToSubtract instanceof ChronoPeriodImpl) {
            ChronoPeriodImpl amount = (ChronoPeriodImpl) amountToSubtract;
            if (amount.getChronology().equals(getChronology())) {
                return new ChronoPeriodImpl(
                        chronology,
                        Jdk8Methods.safeSubtract(years, amount.years),
                        Jdk8Methods.safeSubtract(months, amount.months),
                        Jdk8Methods.safeSubtract(days, amount.days));
            }
        }
        throw new DateTimeException("Unable to subtract amount: " + amountToSubtract);
    }

    @Override
    public ChronoPeriod multipliedBy(int scalar) {
        return new ChronoPeriodImpl(
                chronology,
                Jdk8Methods.safeMultiply(years, scalar),
                Jdk8Methods.safeMultiply(months, scalar),
                Jdk8Methods.safeMultiply(days, scalar));
    }

    @Override
    public ChronoPeriod normalized() {
        if (chronology.range(ChronoField.MONTH_OF_YEAR).isFixed()) {
            long monthLength = chronology.range(ChronoField.MONTH_OF_YEAR).getMaximum()
                    - chronology.range(ChronoField.MONTH_OF_YEAR).getMinimum() + 1;
            long total = years * monthLength + months;
            int years = Jdk8Methods.safeToInt(total / monthLength);
            int months = Jdk8Methods.safeToInt(total % monthLength);
            return new ChronoPeriodImpl(chronology, years, months, days);
        }
        return this;
    }

    @Override
    public Temporal addTo(Temporal temporal) {
        Objects.requireNonNull(temporal, "temporal");
        Chronology temporalChrono = temporal.query(TemporalQueries.chronology());
        if (temporalChrono != null && !chronology.equals(temporalChrono)) {
            throw new DateTimeException("Invalid chronology, required: " + chronology.getId() + ", but was: "
                    + temporalChrono.getId());
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
    public Temporal subtractFrom(Temporal temporal) {
        Objects.requireNonNull(temporal, "temporal");
        Chronology temporalChrono = temporal.query(TemporalQueries.chronology());
        if (temporalChrono != null && !chronology.equals(temporalChrono)) {
            throw new DateTimeException("Invalid chronology, required: " + chronology.getId()
                    + ", but was: " + temporalChrono.getId());
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

    //-------------------------------------------------------------------------
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ChronoPeriodImpl) {
            ChronoPeriodImpl other = (ChronoPeriodImpl) obj;
            return years == other.years && months == other.months
                    && days == other.days && chronology.equals(other.chronology);
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
