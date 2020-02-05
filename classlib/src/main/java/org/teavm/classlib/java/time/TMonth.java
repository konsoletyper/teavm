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

import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MONTHS;

import java.util.Locale;

import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder;
import org.teavm.classlib.java.time.format.TTextStyle;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.time.temporal.TValueRange;

public enum TMonth implements TTemporalAccessor, TTemporalAdjuster {

    JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER;

    public static final TTemporalQuery<TMonth> FROM = new TTemporalQuery<TMonth>() {
        @Override
        public TMonth queryFrom(TTemporalAccessor temporal) {

            return TMonth.from(temporal);
        }
    };

    private static final TMonth[] ENUMS = TMonth.values();

    public static TMonth of(int month) {

        if (month < 1 || month > 12) {
            throw new TDateTimeException("Invalid value for MonthOfYear: " + month);
        }
        return ENUMS[month - 1];
    }

    public static TMonth from(TTemporalAccessor temporal) {

        if (temporal instanceof TMonth) {
            return (TMonth) temporal;
        }
        try {
            if (TIsoChronology.INSTANCE.equals(TChronology.from(temporal)) == false) {
                temporal = TLocalDate.from(temporal);
            }
            return of(temporal.get(MONTH_OF_YEAR));
        } catch (TDateTimeException ex) {
            throw new TDateTimeException("Unable to obtain TMonth from TTemporalAccessor: " + temporal + ", type "
                    + temporal.getClass().getName(), ex);
        }
    }

    public int getValue() {

        return ordinal() + 1;
    }

    public String getDisplayName(TTextStyle style, Locale locale) {

        return new TDateTimeFormatterBuilder().appendText(MONTH_OF_YEAR, style).toFormatter(locale).format(this);
    }

    @Override
    public boolean isSupported(TTemporalField field) {

        if (field instanceof TChronoField) {
            return field == MONTH_OF_YEAR;
        }
        return field != null && field.isSupportedBy(this);
    }

    @Override
    public TValueRange range(TTemporalField field) {

        if (field == MONTH_OF_YEAR) {
            return field.range();
        } else if (field instanceof TChronoField) {
            throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.rangeRefinedBy(this);
    }

    @Override
    public int get(TTemporalField field) {

        if (field == MONTH_OF_YEAR) {
            return getValue();
        }
        return range(field).checkValidIntValue(getLong(field), field);
    }

    @Override
    public long getLong(TTemporalField field) {

        if (field == MONTH_OF_YEAR) {
            return getValue();
        } else if (field instanceof TChronoField) {
            throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.getFrom(this);
    }

    public TMonth plus(long months) {

        int amount = (int) (months % 12);
        return ENUMS[(ordinal() + (amount + 12)) % 12];
    }

    public TMonth minus(long months) {

        return plus(-(months % 12));
    }

    public int length(boolean leapYear) {

        switch (this) {
            case FEBRUARY:
                return (leapYear ? 29 : 28);
            case APRIL:
            case JUNE:
            case SEPTEMBER:
            case NOVEMBER:
                return 30;
            default:
                return 31;
        }
    }

    public int minLength() {

        switch (this) {
            case FEBRUARY:
                return 28;
            case APRIL:
            case JUNE:
            case SEPTEMBER:
            case NOVEMBER:
                return 30;
            default:
                return 31;
        }
    }

    public int maxLength() {

        switch (this) {
            case FEBRUARY:
                return 29;
            case APRIL:
            case JUNE:
            case SEPTEMBER:
            case NOVEMBER:
                return 30;
            default:
                return 31;
        }
    }

    public int firstDayOfYear(boolean leapYear) {

        int leap = leapYear ? 1 : 0;
        switch (this) {
            case JANUARY:
                return 1;
            case FEBRUARY:
                return 32;
            case MARCH:
                return 60 + leap;
            case APRIL:
                return 91 + leap;
            case MAY:
                return 121 + leap;
            case JUNE:
                return 152 + leap;
            case JULY:
                return 182 + leap;
            case AUGUST:
                return 213 + leap;
            case SEPTEMBER:
                return 244 + leap;
            case OCTOBER:
                return 274 + leap;
            case NOVEMBER:
                return 305 + leap;
            case DECEMBER:
            default:
                return 335 + leap;
        }
    }

    public TMonth firstMonthOfQuarter() {

        return ENUMS[(ordinal() / 3) * 3];
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TTemporalQuery<R> query) {

        if (query == TTemporalQueries.chronology()) {
            return (R) TIsoChronology.INSTANCE;
        } else if (query == TTemporalQueries.precision()) {
            return (R) MONTHS;
        } else if (query == TTemporalQueries.localDate() || query == TTemporalQueries.localTime()
                || query == TTemporalQueries.zone() || query == TTemporalQueries.zoneId()
                || query == TTemporalQueries.offset()) {
            return null;
        }
        return query.queryFrom(this);
    }

    @Override
    public TTemporal adjustInto(TTemporal temporal) {

        if (TChronology.from(temporal).equals(TIsoChronology.INSTANCE) == false) {
            throw new TDateTimeException("Adjustment only supported on ISO date-time");
        }
        return temporal.with(MONTH_OF_YEAR, getValue());
    }

}
