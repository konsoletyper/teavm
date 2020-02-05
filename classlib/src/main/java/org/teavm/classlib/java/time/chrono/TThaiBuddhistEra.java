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
package org.teavm.classlib.java.time.chrono;

import static org.teavm.classlib.java.time.temporal.TChronoField.ERA;

import java.util.Locale;

import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder;
import org.teavm.classlib.java.time.format.TTextStyle;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.time.temporal.TValueRange;

public enum TThaiBuddhistEra implements TEra {

    BEFORE_BE, BE;

    public static TThaiBuddhistEra of(int thaiBuddhistEra) {

        switch (thaiBuddhistEra) {
            case 0:
                return BEFORE_BE;
            case 1:
                return BE;
            default:
                throw new TDateTimeException("TEra is not valid for ThaiBuddhistEra");
        }
    }

    @Override
    public int getValue() {

        return ordinal();
    }

    @Override
    public boolean isSupported(TTemporalField field) {

        if (field instanceof TChronoField) {
            return field == ERA;
        }
        return field != null && field.isSupportedBy(this);
    }

    @Override
    public TValueRange range(TTemporalField field) {

        if (field == ERA) {
            return field.range();
        } else if (field instanceof TChronoField) {
            throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.rangeRefinedBy(this);
    }

    @Override
    public int get(TTemporalField field) {

        if (field == ERA) {
            return getValue();
        }
        return range(field).checkValidIntValue(getLong(field), field);
    }

    @Override
    public long getLong(TTemporalField field) {

        if (field == ERA) {
            return getValue();
        } else if (field instanceof TChronoField) {
            throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.getFrom(this);
    }

    @Override
    public TTemporal adjustInto(TTemporal temporal) {

        return temporal.with(ERA, getValue());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TTemporalQuery<R> query) {

        if (query == TTemporalQueries.precision()) {
            return (R) TChronoUnit.ERAS;
        }
        if (query == TTemporalQueries.chronology() || query == TTemporalQueries.zone()
                || query == TTemporalQueries.zoneId() || query == TTemporalQueries.offset()
                || query == TTemporalQueries.localDate() || query == TTemporalQueries.localTime()) {
            return null;
        }
        return query.queryFrom(this);
    }

    @Override
    public String getDisplayName(TTextStyle style, Locale locale) {

        return new TDateTimeFormatterBuilder().appendText(ERA, style).toFormatter(locale).format(this);
    }

}
