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
package org.threeten.bp.format;

import static org.threeten.bp.temporal.ChronoField.EPOCH_DAY;
import static org.threeten.bp.temporal.ChronoField.INSTANT_SECONDS;
import java.util.Locale;
import java.util.Objects;
import org.threeten.bp.DateTimeException;
import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.chrono.ChronoLocalDate;
import org.threeten.bp.chrono.Chronology;
import org.threeten.bp.chrono.IsoChronology;
import org.threeten.bp.temporal.ChronoField;
import org.threeten.bp.temporal.TemporalAccessor;
import org.threeten.bp.temporal.TemporalField;
import org.threeten.bp.temporal.TemporalQueries;
import org.threeten.bp.temporal.TemporalQuery;
import org.threeten.bp.temporal.ValueRange;

/**
 * Context object used during date and time printing.
 * <p>
 * This class provides a single wrapper to items used in the print.
 *
 * <h3>Specification for implementors</h3>
 * This class is a mutable context intended for use from a single thread.
 * Usage of the class is thread-safe within standard printing as the framework creates
 * a new instance of the class for each print and printing is single-threaded.
 */
public final class DateTimePrintContext {

    /**
     * The temporal being output.
     */
    private TemporalAccessor temporal;
    /**
     * The locale, not null.
     */
    private Locale locale;
    /**
     * The symbols, not null.
     */
    private DecimalStyle symbols;
    /**
     * Whether the current formatter is optional.
     */
    private int optional;

    /**
     * Creates a new instance of the context.
     *
     * @param temporal  the temporal object being output, not null
     * @param formatter  the formatter controlling the print, not null
     */
    DateTimePrintContext(TemporalAccessor temporal, DateTimeFormatter formatter) {
        super();
        this.temporal = adjust(temporal, formatter);
        this.locale = formatter.getLocale();
        this.symbols = formatter.getDecimalStyle();
    }

    // for testing
    public DateTimePrintContext(TemporalAccessor temporal, Locale locale, DecimalStyle symbols) {
        this.temporal = temporal;
        this.locale = locale;
        this.symbols = symbols;
    }

    private static TemporalAccessor adjust(final TemporalAccessor temporal, DateTimeFormatter formatter) {
        // normal case first
        Chronology overrideChrono = formatter.getChronology();
        ZoneId overrideZone = formatter.getZone();
        if (overrideChrono == null && overrideZone == null) {
            return temporal;
        }

        // ensure minimal change
        Chronology temporalChrono = temporal.query(TemporalQueries.chronology());
        ZoneId temporalZone = temporal.query(TemporalQueries.zoneId());
        if (Objects.equals(temporalChrono, overrideChrono)) {
            overrideChrono = null;
        }
        if (Objects.equals(temporalZone, overrideZone)) {
            overrideZone = null;
        }
        if (overrideChrono == null && overrideZone == null) {
            return temporal;
        }
        final Chronology effectiveChrono = overrideChrono != null ? overrideChrono : temporalChrono;
        final ZoneId effectiveZone = overrideZone != null ? overrideZone : temporalZone;
        
        // use overrides
        if (overrideZone != null) {
            // handle instant
            if (temporal.isSupported(INSTANT_SECONDS)) {
                Chronology chrono = effectiveChrono != null ? effectiveChrono : IsoChronology.INSTANCE;
                return chrono.zonedDateTime(Instant.from(temporal), overrideZone);
            }
            // block changing zone on OffsetTime, and similar problem cases
            ZoneId normalizedOffset = overrideZone.normalized();
            ZoneOffset temporalOffset = temporal.query(TemporalQueries.offset());
            if (normalizedOffset instanceof ZoneOffset && temporalOffset != null
                    && !normalizedOffset.equals(temporalOffset)) {
                throw new DateTimeException("Invalid override zone for temporal: " + overrideZone + " " + temporal);
            }
        }
        final ChronoLocalDate effectiveDate;
        if (overrideChrono != null) {
            if (temporal.isSupported(EPOCH_DAY)) {
                effectiveDate = effectiveChrono.date(temporal);
            } else {
                // check for date fields other than epoch-day, ignoring case of converting null to ISO
                if (!(overrideChrono == IsoChronology.INSTANCE && temporalChrono == null)) {
                    for (ChronoField f : ChronoField.values()) {
                        if (f.isDateBased() && temporal.isSupported(f)) {
                            throw new DateTimeException("Invalid override chronology for temporal: "
                                    + overrideChrono + " " + temporal);
                        }
                    }
                }
                effectiveDate = null;
            }
        } else {
            effectiveDate = null;
        }

        // need class here to handle non-standard cases
        return new TemporalAccessor() {
            @Override
            public boolean isSupported(TemporalField field) {
                if (effectiveDate != null && field.isDateBased()) {
                    return effectiveDate.isSupported(field);
                }
                return temporal.isSupported(field);
            }
            @Override
            public ValueRange range(TemporalField field) {
                if (effectiveDate != null && field.isDateBased()) {
                    return effectiveDate.range(field);
                }
                return temporal.range(field);
            }
            @Override
            public long getLong(TemporalField field) {
                if (effectiveDate != null && field.isDateBased()) {
                    return effectiveDate.getLong(field);
                }
                return temporal.getLong(field);
            }
            @SuppressWarnings("unchecked")
            @Override
            public <R> R query(TemporalQuery<R> query) {
                if (query == TemporalQueries.chronology()) {
                    return (R) effectiveChrono;
                }
                if (query == TemporalQueries.zoneId()) {
                    return (R) effectiveZone;
                }
                if (query == TemporalQueries.precision()) {
                    return temporal.query(query);
                }
                return query.queryFrom(this);
            }
        };
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the temporal object being output.
     *
     * @return the temporal object, not null
     */
    TemporalAccessor getTemporal() {
        return temporal;
    }

    /**
     * Gets the locale.
     * <p>
     * This locale is used to control localization in the print output except
     * where localization is controlled by the symbols.
     *
     * @return the locale, not null
     */
    Locale getLocale() {
        return locale;
    }

    /**
     * Gets the formatting symbols.
     * <p>
     * The symbols control the localization of numeric output.
     *
     * @return the formatting symbols, not null
     */
    DecimalStyle getSymbols() {
        return symbols;
    }

    //-----------------------------------------------------------------------
    /**
     * Starts the printing of an optional segment of the input.
     */
    void startOptional() {
        this.optional++;
    }

    /**
     * Ends the printing of an optional segment of the input.
     */
    void endOptional() {
        this.optional--;
    }

    /**
     * Gets a value using a query.
     *
     * @param query  the query to use, not null
     * @return the result, null if not found and optional is true
     * @throws DateTimeException if the type is not available and the section is not optional
     */
    <R> R getValue(TemporalQuery<R> query) {
        R result = temporal.query(query);
        if (result == null && optional == 0) {
            throw new DateTimeException("Unable to extract value: " + temporal.getClass());
        }
        return result;
    }

    /**
     * Gets the value of the specified field.
     * <p>
     * This will return the value for the specified field.
     *
     * @param field  the field to find, not null
     * @return the value, null if not found and optional is true
     * @throws DateTimeException if the field is not available and the section is not optional
     */
    Long getValue(TemporalField field) {
        try {
            return temporal.getLong(field);
        } catch (DateTimeException ex) {
            if (optional > 0) {
                return null;
            }
            throw ex;
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a string version of the context for debugging.
     *
     * @return a string representation of the context, not null
     */
    @Override
    public String toString() {
        return temporal.toString();
    }

    //-------------------------------------------------------------------------
    // for testing
    /**
     * Sets the date-time being output.
     *
     * @param temporal  the date-time object, not null
     */
    void setDateTime(TemporalAccessor temporal) {
        Objects.requireNonNull(temporal, "temporal");
        this.temporal = temporal;
    }

    /**
     * Sets the locale.
     * <p>
     * This locale is used to control localization in the print output except
     * where localization is controlled by the symbols.
     *
     * @param locale  the locale, not null
     */
    void setLocale(Locale locale) {
        Objects.requireNonNull(locale, "locale");
        this.locale = locale;
    }

}
