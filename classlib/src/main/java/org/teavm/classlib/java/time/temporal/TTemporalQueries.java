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
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.OFFSET_SECONDS;

import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.chrono.TChronology;

public final class TTemporalQueries {
    // note that it is vital that each method supplies a constant, not a
    // calculated value, as they will be checked for using ==
    // it is also vital that each constant is different (due to the == checking)

    private TTemporalQueries() {

    }

    // special constants should be used to extract information from a TTemporalAccessor
    // that cannot be derived in other ways
    // Javadoc added here, so as to pretend they are more normal than they really are

    public static final TTemporalQuery<TZoneId> zoneId() {

        return ZONE_ID;
    }

    static final TTemporalQuery<TZoneId> ZONE_ID = new TTemporalQuery<TZoneId>() {
        @Override
        public TZoneId queryFrom(TTemporalAccessor temporal) {

            return temporal.query(this);
        }
    };

    public static final TTemporalQuery<TChronology> chronology() {

        return CHRONO;
    }

    static final TTemporalQuery<TChronology> CHRONO = new TTemporalQuery<TChronology>() {
        @Override
        public TChronology queryFrom(TTemporalAccessor temporal) {

            return temporal.query(this);
        }
    };

    public static final TTemporalQuery<TTemporalUnit> precision() {

        return PRECISION;
    }

    static final TTemporalQuery<TTemporalUnit> PRECISION = new TTemporalQuery<TTemporalUnit>() {
        @Override
        public TTemporalUnit queryFrom(TTemporalAccessor temporal) {

            return temporal.query(this);
        }
    };

    // non-special constants are standard queries that derive information from other information
    public static final TTemporalQuery<TZoneId> zone() {

        return ZONE;
    }

    static final TTemporalQuery<TZoneId> ZONE = new TTemporalQuery<TZoneId>() {
        @Override
        public TZoneId queryFrom(TTemporalAccessor temporal) {

            TZoneId zone = temporal.query(ZONE_ID);
            return (zone != null ? zone : temporal.query(OFFSET));
        }
    };

    public static final TTemporalQuery<TZoneOffset> offset() {

        return OFFSET;
    }

    static final TTemporalQuery<TZoneOffset> OFFSET = new TTemporalQuery<TZoneOffset>() {
        @Override
        public TZoneOffset queryFrom(TTemporalAccessor temporal) {

            if (temporal.isSupported(OFFSET_SECONDS)) {
                return TZoneOffset.ofTotalSeconds(temporal.get(OFFSET_SECONDS));
            }
            return null;
        }
    };

    public static final TTemporalQuery<TLocalDate> localDate() {

        return LOCAL_DATE;
    }

    static final TTemporalQuery<TLocalDate> LOCAL_DATE = new TTemporalQuery<TLocalDate>() {
        @Override
        public TLocalDate queryFrom(TTemporalAccessor temporal) {

            if (temporal.isSupported(EPOCH_DAY)) {
                return TLocalDate.ofEpochDay(temporal.getLong(EPOCH_DAY));
            }
            return null;
        }
    };

    public static final TTemporalQuery<TLocalTime> localTime() {

        return LOCAL_TIME;
    }

    static final TTemporalQuery<TLocalTime> LOCAL_TIME = new TTemporalQuery<TLocalTime>() {
        @Override
        public TLocalTime queryFrom(TTemporalAccessor temporal) {

            if (temporal.isSupported(NANO_OF_DAY)) {
                return TLocalTime.ofNanoOfDay(temporal.getLong(NANO_OF_DAY));
            }
            return null;
        }
    };

}
