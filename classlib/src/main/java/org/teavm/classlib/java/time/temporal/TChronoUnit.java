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
package org.teavm.classlib.java.time.temporal;

import org.teavm.classlib.java.time.TDuration;
import org.teavm.classlib.java.time.chrono.TChronoLocalDate;
import org.teavm.classlib.java.time.chrono.TChronoLocalDateTime;
import org.teavm.classlib.java.time.chrono.TChronoZonedDateTime;

public enum TChronoUnit implements TTemporalUnit {

    NANOS("Nanos", TDuration.ofNanos(1)), //
    MICROS("Micros", TDuration.ofNanos(1000)), //
    MILLIS("Millis", TDuration.ofNanos(1000000)), //
    SECONDS("Seconds", TDuration.ofSeconds(1)), //
    MINUTES("Minutes", TDuration.ofSeconds(60)), //
    HOURS("Hours", TDuration.ofSeconds(3600)), //
    HALF_DAYS("HalfDays", TDuration.ofSeconds(43200)), //
    DAYS("Days", TDuration.ofSeconds(86400)), //
    WEEKS("Weeks", TDuration.ofSeconds(7 * 86400L)), //
    MONTHS("Months", TDuration.ofSeconds(31556952L / 12)), //
    YEARS("Years", TDuration.ofSeconds(31556952L)), //
    DECADES("Decades", TDuration.ofSeconds(31556952L * 10L)), //
    CENTURIES("Centuries", TDuration.ofSeconds(31556952L * 100L)), //
    MILLENNIA("Millennia", TDuration.ofSeconds(31556952L * 1000L)), //
    ERAS("Eras", TDuration.ofSeconds(31556952L * 1000000000L)), //
    FOREVER("Forever", TDuration.ofSeconds(Long.MAX_VALUE, 999999999)); //

    private final String name;

    private final TDuration duration;

    private TChronoUnit(String name, TDuration estimatedDuration) {

        this.name = name;
        this.duration = estimatedDuration;
    }

    @Override
    public TDuration getDuration() {

        return this.duration;
    }

    @Override
    public boolean isDurationEstimated() {

        return isDateBased() || this == FOREVER;
    }

    @Override
    public boolean isDateBased() {

        return compareTo(DAYS) >= 0 && this != FOREVER;
    }

    @Override
    public boolean isTimeBased() {

        return compareTo(DAYS) < 0;
    }

    @Override
    public boolean isSupportedBy(TTemporal temporal) {

        if (this == FOREVER) {
            return false;
        }
        if (temporal instanceof TChronoLocalDate) {
            return isDateBased();
        }
        if (temporal instanceof TChronoLocalDateTime || temporal instanceof TChronoZonedDateTime) {
            return true;
        }
        try {
            temporal.plus(1, this);
            return true;
        } catch (RuntimeException ex) {
            try {
                temporal.plus(-1, this);
                return true;
            } catch (RuntimeException ex2) {
                return false;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends TTemporal> R addTo(R dateTime, long periodToAdd) {

        return (R) dateTime.plus(periodToAdd, this);
    }

    @Override
    public long between(TTemporal temporal1, TTemporal temporal2) {

        return temporal1.until(temporal2, this);
    }

    @Override
    public String toString() {

        return this.name;
    }

}
