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

import static org.teavm.classlib.java.time.temporal.TChronoField.EPOCH_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.FOREVER;

import org.teavm.classlib.java.util.TLocale;
import java.util.Map;

import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.format.TResolverStyle;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;

public final class TJulianFields {

    public static final TTemporalField JULIAN_DAY = Field.JULIAN_DAY;
    public static final TTemporalField MODIFIED_JULIAN_DAY = Field.MODIFIED_JULIAN_DAY;
    public static final TTemporalField RATA_DIE = Field.RATA_DIE;

    private static enum Field implements TTemporalField {
        // 719163L + 1721425L = 2440588L
        JULIAN_DAY("JulianDay", DAYS, FOREVER, 2440588L),
        // 719163L - 678576L = 40587L
        MODIFIED_JULIAN_DAY("ModifiedJulianDay", DAYS, FOREVER, 40587L),
        RATA_DIE("RataDie", DAYS, FOREVER, 719163L),
        // lots of others Truncated,Lilian, ANSI COBOL (also dotnet related), Excel?
        ;

        private final String name;
        private final TTemporalUnit baseUnit;
        private final TTemporalUnit rangeUnit;
        private final TValueRange range;
        private final long offset;

        private Field(String name, TTemporalUnit baseUnit, TTemporalUnit rangeUnit, long offset) {
            this.name = name;
            this.baseUnit = baseUnit;
            this.rangeUnit = rangeUnit;
            this.range = TValueRange.of(-365243219162L + offset, 365241780471L + offset);
            this.offset = offset;
        }

        //-----------------------------------------------------------------------
        @Override
        public TTemporalUnit getBaseUnit() {
            return baseUnit;
        }

        @Override
        public TTemporalUnit getRangeUnit() {
            return rangeUnit;
        }

        @Override
        public TValueRange range() {
            return range;
        }

        @Override
        public boolean isDateBased() {
            return true;
        }

        @Override
        public boolean isTimeBased() {
            return false;
        }

        //-----------------------------------------------------------------------
        @Override
        public boolean isSupportedBy(TTemporalAccessor temporal) {
            return temporal.isSupported(EPOCH_DAY);
        }

        @Override
        public TValueRange rangeRefinedBy(TTemporalAccessor temporal) {
            if (isSupportedBy(temporal) == false) {
                throw new TUnsupportedTemporalTypeException("Unsupported field: " + this);
            }
            return range();
        }

        @Override
        public long getFrom(TTemporalAccessor temporal) {
            return temporal.getLong(EPOCH_DAY) + offset;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R extends TTemporal> R adjustInto(R dateTime, long newValue) {
            if (range().isValidValue(newValue) == false) {
                throw new TDateTimeException("Invalid value: " + name + " " + newValue);
            }
            return (R) dateTime.with(EPOCH_DAY, TJdk8Methods.safeSubtract(newValue, offset));
        }

        @Override
        public String getDisplayName(TLocale locale) {
            TJdk8Methods.requireNonNull(locale, "locale");
            return toString();
        }

        //-----------------------------------------------------------------------
        @Override
        public TTemporalAccessor resolve(Map<TTemporalField, Long> fieldValues,
                        TTemporalAccessor partialTemporal, TResolverStyle resolverStyle) {
            long value = fieldValues.remove(this);
            TChronology chrono = TChronology.from(partialTemporal);
            return chrono.dateEpochDay(TJdk8Methods.safeSubtract(value, offset));
        }

        //-----------------------------------------------------------------------
        @Override
        public String toString() {
            return name;
        }

    }
}
