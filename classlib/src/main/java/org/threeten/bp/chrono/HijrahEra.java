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

import static org.threeten.bp.temporal.ChronoField.ERA;
import java.util.Locale;
import org.threeten.bp.DateTimeException;
import org.threeten.bp.format.DateTimeFormatterBuilder;
import org.threeten.bp.format.TextStyle;
import org.threeten.bp.temporal.ChronoField;
import org.threeten.bp.temporal.ChronoUnit;
import org.threeten.bp.temporal.Temporal;
import org.threeten.bp.temporal.TemporalField;
import org.threeten.bp.temporal.TemporalQueries;
import org.threeten.bp.temporal.TemporalQuery;
import org.threeten.bp.temporal.UnsupportedTemporalTypeException;
import org.threeten.bp.temporal.ValueRange;

/**
 * An era in the Hijrah calendar system.
 * <p>
 * The Hijrah calendar system has two eras.
 * The date {@code 0001-01-01 (Hijrah)} is {@code 622-06-19 (ISO)}.
 * <p>
 * <b>Do not use {@code ordinal()} to obtain the numeric representation of {@code HijrahEra}.
 * Use {@code getValue()} instead.</b>
 *
 * <h3>Specification for implementors</h3>
 * This is an immutable and thread-safe enum.
 */
public enum HijrahEra implements Era {

    /**
     * The singleton instance for the era before the current one, 'Before Anno Hegirae',
     * which has the value 0.
     */
    BEFORE_AH,
    /**
     * The singleton instance for the current era, 'Anno Hegirae', which has the value 1.
     */
    AH;

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code HijrahEra} from a value.
     * <p>
     * The current era (from ISO date 622-06-19 onwards) has the value 1
     * The previous era has the value 0.
     *
     * @param hijrahEra  the era to represent, from 0 to 1
     * @return the HijrahEra singleton, never null
     * @throws DateTimeException if the era is invalid
     */
    public static HijrahEra of(int hijrahEra) {
        switch (hijrahEra) {
            case 0:
                return BEFORE_AH;
            case 1:
                return AH;
            default:
                throw new DateTimeException("HijrahEra not valid");
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the era numeric value.
     * <p>
     * The current era (from ISO date 622-06-19 onwards) has the value 1.
     * The previous era has the value 0.
     *
     * @return the era value, from 0 (BEFORE_AH) to 1 (AH)
     */
    @Override
    public int getValue() {
        return ordinal();
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return field == ERA;
        }
        return field != null && field.isSupportedBy(this);
    }

    @Override
    public ValueRange range(TemporalField field) {
        if (field == ERA) {
            return ValueRange.of(1, 1);
        } else if (field instanceof ChronoField) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.rangeRefinedBy(this);
    }

    @Override
    public int get(TemporalField field) {
        if (field == ERA) {
            return getValue();
        }
        return range(field).checkValidIntValue(getLong(field), field);
    }

    @Override
    public long getLong(TemporalField field) {
        if (field == ERA) {
            return getValue();
        } else if (field instanceof ChronoField) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.getFrom(this);
    }

    //-------------------------------------------------------------------------
    @Override
    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(ERA, getValue());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.precision()) {
            return (R) ChronoUnit.ERAS;
        }
        if (query == TemporalQueries.chronology() || query == TemporalQueries.zone()
                || query == TemporalQueries.zoneId() || query == TemporalQueries.offset()
                || query == TemporalQueries.localDate() || query == TemporalQueries.localTime()) {
            return null;
        }
        return query.queryFrom(this);
    }

    //-----------------------------------------------------------------------
    @Override
    public String getDisplayName(TextStyle style, Locale locale) {
        return new DateTimeFormatterBuilder().appendText(ERA, style).toFormatter(locale).format(this);
    }

    /**
     * Returns the proleptic year from this era and year of era.
     *
     * @param yearOfEra the year of Era
     * @return the computed prolepticYear
     */
    int prolepticYear(int yearOfEra) {
        return this == HijrahEra.AH ? yearOfEra : 1 - yearOfEra;
    }
}
