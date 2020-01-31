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
package org.threeten.bp;

import static org.threeten.bp.temporal.ChronoUnit.DAYS;
import static org.threeten.bp.temporal.ChronoUnit.FOREVER;
import static org.threeten.bp.temporal.ChronoUnit.SECONDS;

import java.util.Collections;
import java.util.List;

import org.threeten.bp.jdk8.Jdk8Methods;
import org.threeten.bp.temporal.Temporal;
import org.threeten.bp.temporal.TemporalAmount;
import org.threeten.bp.temporal.TemporalUnit;

/**
 * Mock period of time measured using a single unit, such as {@code 3 Days}.
 */
public final class MockSimplePeriod
        implements TemporalAmount, Comparable<MockSimplePeriod> {

    /**
     * A constant for a period of zero, measured in days.
     */
    public static final MockSimplePeriod ZERO_DAYS = new MockSimplePeriod(0, DAYS);
    /**
     * A constant for a period of zero, measured in seconds.
     */
    public static final MockSimplePeriod ZERO_SECONDS = new MockSimplePeriod(0, SECONDS);

    /**
     * The amount of the period.
     */
    private final long amount;
    /**
     * The unit the period is measured in.
     */
    private final TemporalUnit unit;

    /**
     * Obtains a {@code MockSimplePeriod} from an amount and unit.
     * <p>
     * The parameters represent the two parts of a phrase like '6 Days'.
     *
     * @param amount  the amount of the period, measured in terms of the unit, positive or negative
     * @param unit  the unit that the period is measured in, must not be the 'Forever' unit, not null
     * @return the {@code MockSimplePeriod} instance, not null
     * @throws DateTimeException if the period unit is {@link org.threeten.bp.temporal.ChronoUnit#FOREVER}.
     */
    public static MockSimplePeriod of(long amount, TemporalUnit unit) {
        return new MockSimplePeriod(amount, unit);
    }

    private MockSimplePeriod(long amount, TemporalUnit unit) {
        Jdk8Methods.requireNonNull(unit, "unit");
        if (unit == FOREVER) {
            throw new DateTimeException("Cannot create a period of the Forever unit");
        }
        this.amount = amount;
        this.unit = unit;
    }

    //-----------------------------------------------------------------------
    @Override
    public List<TemporalUnit> getUnits() {
        return Collections.singletonList(unit);
    }

    @Override
    public long get(TemporalUnit unit) {
        if (this.unit.equals(unit)) {
            return amount;
        }
        throw new DateTimeException("Unsupported unit: " + unit);
    }

    //-----------------------------------------------------------------------
    public long getAmount() {
        return amount;
    }

    public TemporalUnit getUnit() {
        return unit;
    }

    //-------------------------------------------------------------------------
    @Override
    public Temporal addTo(Temporal dateTime) {
        return dateTime.plus(amount, unit);
    }

    @Override
    public Temporal subtractFrom(Temporal dateTime) {
        return dateTime.minus(amount, unit);
    }

    //-----------------------------------------------------------------------
    @Override
    public int compareTo(MockSimplePeriod otherPeriod) {
        if (unit.equals(otherPeriod.getUnit()) == false) {
            throw new IllegalArgumentException("Units cannot be compared: " + unit + " and " + otherPeriod.getUnit());
        }
        return Jdk8Methods.compareLongs(amount, otherPeriod.amount);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
           return true;
        }
        if (obj instanceof MockSimplePeriod) {
            MockSimplePeriod other = (MockSimplePeriod) obj;
            return this.amount == other.amount &&
                    this.unit.equals(other.unit);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return unit.hashCode() ^ (int) (amount ^ (amount >>> 32));
    }

    @Override
    public String toString() {
        return amount + " " + unit;
    }

}
