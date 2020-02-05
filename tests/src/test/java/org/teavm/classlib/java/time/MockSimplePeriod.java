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
package org.teavm.classlib.java.time;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.FOREVER;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.DateTimeException;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class MockSimplePeriod implements TemporalAmount, Comparable<MockSimplePeriod> {

    public static final MockSimplePeriod ZERO_DAYS = new MockSimplePeriod(0, DAYS);

    public static final MockSimplePeriod ZERO_SECONDS = new MockSimplePeriod(0, SECONDS);

    private final long amount;

    private final TemporalUnit unit;

    public static MockSimplePeriod of(long amount, TemporalUnit unit) {

        return new MockSimplePeriod(amount, unit);
    }

    private MockSimplePeriod(long amount, TemporalUnit unit) {

        Objects.requireNonNull(unit, "unit");
        if (unit == FOREVER) {
            throw new DateTimeException("Cannot create a period of the Forever unit");
        }
        this.amount = amount;
        this.unit = unit;
    }

    @Override
    public List<TemporalUnit> getUnits() {

        return Collections.singletonList(this.unit);
    }

    @Override
    public long get(TemporalUnit unit) {

        if (this.unit.equals(unit)) {
            return this.amount;
        }
        throw new DateTimeException("Unsupported unit: " + unit);
    }

    public long getAmount() {

        return this.amount;
    }

    public TemporalUnit getUnit() {

        return this.unit;
    }

    @Override
    public Temporal addTo(Temporal dateTime) {

        return dateTime.plus(this.amount, this.unit);
    }

    @Override
    public Temporal subtractFrom(Temporal dateTime) {

        return dateTime.minus(this.amount, this.unit);
    }

    @Override
    public int compareTo(MockSimplePeriod otherPeriod) {

        if (this.unit.equals(otherPeriod.getUnit()) == false) {
            throw new IllegalArgumentException(
                    "Units cannot be compared: " + this.unit + " and " + otherPeriod.getUnit());
        }
        return Long.compare(this.amount, otherPeriod.amount);
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj instanceof MockSimplePeriod) {
            MockSimplePeriod other = (MockSimplePeriod) obj;
            return this.amount == other.amount && this.unit.equals(other.unit);
        }
        return false;
    }

    @Override
    public int hashCode() {

        return this.unit.hashCode() ^ (int) (this.amount ^ (this.amount >>> 32));
    }

    @Override
    public String toString() {

        return this.amount + " " + this.unit;
    }

}
