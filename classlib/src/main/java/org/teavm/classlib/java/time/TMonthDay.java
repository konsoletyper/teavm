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

import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;

import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder;
import org.teavm.classlib.java.time.format.TDateTimeParseException;
import org.teavm.classlib.java.time.jdk8.TDefaultInterfaceTemporalAccessor;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.time.temporal.TValueRange;

public final class TMonthDay
        extends TDefaultInterfaceTemporalAccessor
        implements TTemporalAccessor, TTemporalAdjuster, Comparable<TMonthDay>, Serializable {

    public static final TTemporalQuery<TMonthDay> FROM = new TTemporalQuery<TMonthDay>() {
        @Override
        public TMonthDay queryFrom(TTemporalAccessor temporal) {
            return TMonthDay.from(temporal);
        }
    };

    private static final long serialVersionUID = -939150713474957432L;
    private static final TDateTimeFormatter PARSER = new TDateTimeFormatterBuilder()
        .appendLiteral("--")
        .appendValue(MONTH_OF_YEAR, 2)
        .appendLiteral('-')
        .appendValue(DAY_OF_MONTH, 2)
        .toFormatter();

    private final int month;
    private final int day;

    //-----------------------------------------------------------------------
    public static TMonthDay now() {
        return now(TClock.systemDefaultZone());
    }

    public static TMonthDay now(TZoneId zone) {
        return now(TClock.system(zone));
    }

    public static TMonthDay now(TClock clock) {
        final TLocalDate now = TLocalDate.now(clock);  // called once
        return TMonthDay.of(now.getMonth(), now.getDayOfMonth());
    }

    //-----------------------------------------------------------------------
    public static TMonthDay of(TMonth month, int dayOfMonth) {
        TJdk8Methods.requireNonNull(month, "month");
        DAY_OF_MONTH.checkValidValue(dayOfMonth);
        if (dayOfMonth > month.maxLength()) {
            throw new TDateTimeException("Illegal value for DayOfMonth field, value " + dayOfMonth +
                    " is not valid for month " + month.name());
        }
        return new TMonthDay(month.getValue(), dayOfMonth);
    }

    public static TMonthDay of(int month, int dayOfMonth) {
        return of(TMonth.of(month), dayOfMonth);
    }

    //-----------------------------------------------------------------------
    public static TMonthDay from(TTemporalAccessor temporal) {
        if (temporal instanceof TMonthDay) {
            return (TMonthDay) temporal;
        }
        try {
            if (TIsoChronology.INSTANCE.equals(TChronology.from(temporal)) == false) {
                temporal = TLocalDate.from(temporal);
            }
            return of(temporal.get(MONTH_OF_YEAR), temporal.get(DAY_OF_MONTH));
        } catch (TDateTimeException ex) {
            throw new TDateTimeException("Unable to obtain TMonthDay from TTemporalAccessor: " +
                    temporal + ", type " + temporal.getClass().getName());
        }
    }

    //-----------------------------------------------------------------------
    public static TMonthDay parse(CharSequence text) {
        return parse(text, PARSER);
    }

    public static TMonthDay parse(CharSequence text, TDateTimeFormatter formatter) {
        TJdk8Methods.requireNonNull(formatter, "formatter");
        return formatter.parse(text, TMonthDay.FROM);
    }

    //-----------------------------------------------------------------------
    private TMonthDay(int month, int dayOfMonth) {
        this.month = month;
        this.day = dayOfMonth;
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean isSupported(TTemporalField field) {
        if (field instanceof TChronoField) {
            return field == MONTH_OF_YEAR || field == DAY_OF_MONTH;
        }
        return field != null && field.isSupportedBy(this);
    }

    @Override
    public TValueRange range(TTemporalField field) {
        if (field == MONTH_OF_YEAR) {
            return field.range();
        } else if (field == DAY_OF_MONTH) {
            return TValueRange.of(1, getMonth().minLength(), getMonth().maxLength());
        }
        return super.range(field);
    }

    @Override  // override for Javadoc
    public int get(TTemporalField field) {
        return range(field).checkValidIntValue(getLong(field), field);
    }

    @Override
    public long getLong(TTemporalField field) {
        if (field instanceof TChronoField) {
            switch ((TChronoField) field) {
                // alignedDOW and alignedWOM not supported because they cannot be set in with()
                case DAY_OF_MONTH: return day;
                case MONTH_OF_YEAR: return month;
            }
            throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.getFrom(this);
    }

    //-----------------------------------------------------------------------
    public int getMonthValue() {
        return month;
    }

    public TMonth getMonth() {
        return TMonth.of(month);
    }

    public int getDayOfMonth() {
        return day;
    }

    //-----------------------------------------------------------------------
    public boolean isValidYear(int year) {
        return (day == 29 && month == 2 && TYear.isLeap(year) == false) == false;
    }

    //-----------------------------------------------------------------------
    public TMonthDay withMonth(int month) {
        return with(TMonth.of(month));
    }

    public TMonthDay with(TMonth month) {
        TJdk8Methods.requireNonNull(month, "month");
        if (month.getValue() == this.month) {
            return this;
        }
        int day = Math.min(this.day, month.maxLength());
        return new TMonthDay(month.getValue(), day);
    }

    public TMonthDay withDayOfMonth(int dayOfMonth) {
        if (dayOfMonth == this.day) {
            return this;
        }
        return of(month, dayOfMonth);
    }

    //-----------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TTemporalQuery<R> query) {
        if (query == TTemporalQueries.chronology()) {
            return (R) TIsoChronology.INSTANCE;
        }
        return super.query(query);
    }

    @Override
    public TTemporal adjustInto(TTemporal temporal) {
        if (TChronology.from(temporal).equals(TIsoChronology.INSTANCE) == false) {
            throw new TDateTimeException("Adjustment only supported on ISO date-time");
        }
        temporal = temporal.with(MONTH_OF_YEAR, month);
        return temporal.with(DAY_OF_MONTH, Math.min(temporal.range(DAY_OF_MONTH).getMaximum(), day));
    }

    //-----------------------------------------------------------------------
    public TLocalDate atYear(int year) {
        return TLocalDate.of(year, month, isValidYear(year) ? day : 28);
    }

    //-----------------------------------------------------------------------
    public int compareTo(TMonthDay other) {
        int cmp = (month - other.month);
        if (cmp == 0) {
            cmp = (day - other.day);
        }
        return cmp;
    }

    public boolean isAfter(TMonthDay other) {
        return compareTo(other) > 0;
    }

    public boolean isBefore(TMonthDay other) {
        return compareTo(other) < 0;
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TMonthDay) {
            TMonthDay other = (TMonthDay) obj;
            return month == other.month && day == other.day;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (month << 6) + day;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
        return new StringBuilder(10).append("--")
            .append(month < 10 ? "0" : "").append(month)
            .append(day < 10 ? "-0" : "-").append(day)
            .toString();
    }

    public String format(TDateTimeFormatter formatter) {
        TJdk8Methods.requireNonNull(formatter, "formatter");
        return formatter.format(this);
    }

    //-----------------------------------------------------------------------
    private Object writeReplace() {
        return new Ser(Ser.MONTH_DAY_TYPE, this);
    }

    private Object readResolve() throws ObjectStreamException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    void writeExternal(DataOutput out) throws IOException {
        out.writeByte(month);
        out.writeByte(day);
    }

    static TMonthDay readExternal(DataInput in) throws IOException {
        byte month = in.readByte();
        byte day = in.readByte();
        return TMonthDay.of(month, day);
    }

}
