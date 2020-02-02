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
package org.teavm.classlib.java.time.format;

import static org.teavm.classlib.java.time.temporal.TChronoField.EPOCH_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.INSTANT_SECONDS;

import java.util.Locale;
import java.util.Objects;

import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.chrono.TChronoLocalDate;
import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.jdk8.TDefaultInterfaceTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TValueRange;

final class TDateTimePrintContext {

    private TTemporalAccessor temporal;

    private Locale locale;

    private TDecimalStyle symbols;

    private int optional;

    TDateTimePrintContext(TTemporalAccessor temporal, TDateTimeFormatter formatter) {

        super();
        this.temporal = adjust(temporal, formatter);
        this.locale = formatter.getLocale();
        this.symbols = formatter.getDecimalStyle();
    }

    // for testing
    TDateTimePrintContext(TTemporalAccessor temporal, Locale locale, TDecimalStyle symbols) {

        this.temporal = temporal;
        this.locale = locale;
        this.symbols = symbols;
    }

    private static TTemporalAccessor adjust(final TTemporalAccessor temporal, TDateTimeFormatter formatter) {

        // normal case first
        TChronology overrideChrono = formatter.getChronology();
        TZoneId overrideZone = formatter.getZone();
        if (overrideChrono == null && overrideZone == null) {
            return temporal;
        }

        // ensure minimal change
        TChronology temporalChrono = temporal.query(TTemporalQueries.chronology());
        TZoneId temporalZone = temporal.query(TTemporalQueries.zoneId());
        if (Objects.equals(temporalChrono, overrideChrono)) {
            overrideChrono = null;
        }
        if (Objects.equals(temporalZone, overrideZone)) {
            overrideZone = null;
        }
        if (overrideChrono == null && overrideZone == null) {
            return temporal;
        }
        final TChronology effectiveChrono = (overrideChrono != null ? overrideChrono : temporalChrono);
        final TZoneId effectiveZone = (overrideZone != null ? overrideZone : temporalZone);

        // use overrides
        if (overrideZone != null) {
            // handle instant
            if (temporal.isSupported(INSTANT_SECONDS)) {
                TChronology chrono = (effectiveChrono != null ? effectiveChrono : TIsoChronology.INSTANCE);
                return chrono.zonedDateTime(TInstant.from(temporal), overrideZone);
            }
            // block changing zone on TOffsetTime, and similar problem cases
            TZoneId normalizedOffset = overrideZone.normalized();
            TZoneOffset temporalOffset = temporal.query(TTemporalQueries.offset());
            if (normalizedOffset instanceof TZoneOffset && temporalOffset != null
                    && normalizedOffset.equals(temporalOffset) == false) {
                throw new TDateTimeException("Invalid override zone for temporal: " + overrideZone + " " + temporal);
            }
        }
        final TChronoLocalDate effectiveDate;
        if (overrideChrono != null) {
            if (temporal.isSupported(EPOCH_DAY)) {
                effectiveDate = effectiveChrono.date(temporal);
            } else {
                // check for date fields other than epoch-day, ignoring case of converting null to ISO
                if (!(overrideChrono == TIsoChronology.INSTANCE && temporalChrono == null)) {
                    for (TChronoField f : TChronoField.values()) {
                        if (f.isDateBased() && temporal.isSupported(f)) {
                            throw new TDateTimeException(
                                    "Invalid override chronology for temporal: " + overrideChrono + " " + temporal);
                        }
                    }
                }
                effectiveDate = null;
            }
        } else {
            effectiveDate = null;
        }

        // need class here to handle non-standard cases
        return new TDefaultInterfaceTemporalAccessor() {
            @Override
            public boolean isSupported(TTemporalField field) {

                if (effectiveDate != null && field.isDateBased()) {
                    return effectiveDate.isSupported(field);
                }
                return temporal.isSupported(field);
            }

            @Override
            public TValueRange range(TTemporalField field) {

                if (effectiveDate != null && field.isDateBased()) {
                    return effectiveDate.range(field);
                }
                return temporal.range(field);
            }

            @Override
            public long getLong(TTemporalField field) {

                if (effectiveDate != null && field.isDateBased()) {
                    return effectiveDate.getLong(field);
                }
                return temporal.getLong(field);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <R> R query(TTemporalQuery<R> query) {

                if (query == TTemporalQueries.chronology()) {
                    return (R) effectiveChrono;
                }
                if (query == TTemporalQueries.zoneId()) {
                    return (R) effectiveZone;
                }
                if (query == TTemporalQueries.precision()) {
                    return temporal.query(query);
                }
                return query.queryFrom(this);
            }
        };
    }

    TTemporalAccessor getTemporal() {

        return this.temporal;
    }

    Locale getLocale() {

        return this.locale;
    }

    TDecimalStyle getSymbols() {

        return this.symbols;
    }

    void startOptional() {

        this.optional++;
    }

    void endOptional() {

        this.optional--;
    }

    <R> R getValue(TTemporalQuery<R> query) {

        R result = this.temporal.query(query);
        if (result == null && this.optional == 0) {
            throw new TDateTimeException("Unable to extract value: " + this.temporal.getClass());
        }
        return result;
    }

    Long getValue(TTemporalField field) {

        try {
            return this.temporal.getLong(field);
        } catch (TDateTimeException ex) {
            if (this.optional > 0) {
                return null;
            }
            throw ex;
        }
    }

    @Override
    public String toString() {

        return this.temporal.toString();
    }

    // for testing
    void setDateTime(TTemporalAccessor temporal) {

        Objects.requireNonNull(temporal, "temporal");
        this.temporal = temporal;
    }

    void setLocale(Locale locale) {

        Objects.requireNonNull(locale, "locale");
        this.locale = locale;
    }

}
