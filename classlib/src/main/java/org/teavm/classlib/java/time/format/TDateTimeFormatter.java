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

import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.HOUR_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.MINUTE_OF_HOUR;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoField.SECOND_OF_MINUTE;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;

import java.io.IOException;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TPeriod;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder.CompositePrinterParser;
import org.teavm.classlib.java.time.format.TDateTimeParseContext.Parsed;
import org.teavm.classlib.java.time.temporal.TIsoFields;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;

public final class TDateTimeFormatter {

    public static final TDateTimeFormatter ISO_LOCAL_DATE;
    static {
        ISO_LOCAL_DATE = new TDateTimeFormatterBuilder().appendValue(YEAR, 4, 10, TSignStyle.EXCEEDS_PAD)
                .appendLiteral('-').appendValue(MONTH_OF_YEAR, 2).appendLiteral('-').appendValue(DAY_OF_MONTH, 2)
                .toFormatter(TResolverStyle.STRICT).withChronology(TIsoChronology.INSTANCE);
    }

    public static final TDateTimeFormatter ISO_OFFSET_DATE;
    static {
        ISO_OFFSET_DATE = new TDateTimeFormatterBuilder().parseCaseInsensitive().append(ISO_LOCAL_DATE).appendOffsetId()
                .toFormatter(TResolverStyle.STRICT).withChronology(TIsoChronology.INSTANCE);
    }

    public static final TDateTimeFormatter ISO_DATE;
    static {
        ISO_DATE = new TDateTimeFormatterBuilder().parseCaseInsensitive().append(ISO_LOCAL_DATE).optionalStart()
                .appendOffsetId().toFormatter(TResolverStyle.STRICT).withChronology(TIsoChronology.INSTANCE);
    }

    public static final TDateTimeFormatter ISO_LOCAL_TIME;
    static {
        ISO_LOCAL_TIME = new TDateTimeFormatterBuilder().appendValue(HOUR_OF_DAY, 2).appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2).optionalStart().appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2)
                .optionalStart().appendFraction(NANO_OF_SECOND, 0, 9, true).toFormatter(TResolverStyle.STRICT);
    }

    public static final TDateTimeFormatter ISO_OFFSET_TIME;
    static {
        ISO_OFFSET_TIME = new TDateTimeFormatterBuilder().parseCaseInsensitive().append(ISO_LOCAL_TIME).appendOffsetId()
                .toFormatter(TResolverStyle.STRICT);
    }

    public static final TDateTimeFormatter ISO_TIME;
    static {
        ISO_TIME = new TDateTimeFormatterBuilder().parseCaseInsensitive().append(ISO_LOCAL_TIME).optionalStart()
                .appendOffsetId().toFormatter(TResolverStyle.STRICT);
    }

    public static final TDateTimeFormatter ISO_LOCAL_DATE_TIME;
    static {
        ISO_LOCAL_DATE_TIME = new TDateTimeFormatterBuilder().parseCaseInsensitive().append(ISO_LOCAL_DATE)
                .appendLiteral('T').append(ISO_LOCAL_TIME).toFormatter(TResolverStyle.STRICT)
                .withChronology(TIsoChronology.INSTANCE);
    }

    public static final TDateTimeFormatter ISO_OFFSET_DATE_TIME;
    static {
        ISO_OFFSET_DATE_TIME = new TDateTimeFormatterBuilder().parseCaseInsensitive().append(ISO_LOCAL_DATE_TIME)
                .appendOffsetId().toFormatter(TResolverStyle.STRICT).withChronology(TIsoChronology.INSTANCE);
    }

    public static final TDateTimeFormatter ISO_ZONED_DATE_TIME;
    static {
        ISO_ZONED_DATE_TIME = new TDateTimeFormatterBuilder().append(ISO_OFFSET_DATE_TIME).optionalStart()
                .appendLiteral('[').parseCaseSensitive().appendZoneRegionId().appendLiteral(']')
                .toFormatter(TResolverStyle.STRICT).withChronology(TIsoChronology.INSTANCE);
    }

    public static final TDateTimeFormatter ISO_DATE_TIME;
    static {
        ISO_DATE_TIME = new TDateTimeFormatterBuilder().append(ISO_LOCAL_DATE_TIME).optionalStart().appendOffsetId()
                .optionalStart().appendLiteral('[').parseCaseSensitive().appendZoneRegionId().appendLiteral(']')
                .toFormatter(TResolverStyle.STRICT).withChronology(TIsoChronology.INSTANCE);
    }

    public static final TDateTimeFormatter ISO_ORDINAL_DATE;
    static {
        ISO_ORDINAL_DATE = new TDateTimeFormatterBuilder().parseCaseInsensitive()
                .appendValue(YEAR, 4, 10, TSignStyle.EXCEEDS_PAD).appendLiteral('-').appendValue(DAY_OF_YEAR, 3)
                .optionalStart().appendOffsetId().toFormatter(TResolverStyle.STRICT)
                .withChronology(TIsoChronology.INSTANCE);
    }

    public static final TDateTimeFormatter ISO_WEEK_DATE;
    static {
        ISO_WEEK_DATE = new TDateTimeFormatterBuilder().parseCaseInsensitive()
                .appendValue(TIsoFields.WEEK_BASED_YEAR, 4, 10, TSignStyle.EXCEEDS_PAD).appendLiteral("-W")
                .appendValue(TIsoFields.WEEK_OF_WEEK_BASED_YEAR, 2).appendLiteral('-').appendValue(DAY_OF_WEEK, 1)
                .optionalStart().appendOffsetId().toFormatter(TResolverStyle.STRICT)
                .withChronology(TIsoChronology.INSTANCE);
    }

    public static final TDateTimeFormatter ISO_INSTANT;
    static {
        ISO_INSTANT = new TDateTimeFormatterBuilder().parseCaseInsensitive().appendInstant()
                .toFormatter(TResolverStyle.STRICT);
    }

    public static final TDateTimeFormatter BASIC_ISO_DATE;
    static {
        BASIC_ISO_DATE = new TDateTimeFormatterBuilder().parseCaseInsensitive().appendValue(YEAR, 4)
                .appendValue(MONTH_OF_YEAR, 2).appendValue(DAY_OF_MONTH, 2).optionalStart().appendOffset("+HHMMss", "Z")
                .toFormatter(TResolverStyle.STRICT).withChronology(TIsoChronology.INSTANCE);
    }

    public static final TDateTimeFormatter RFC_1123_DATE_TIME;
    static {
        // manually code maps to ensure correct data always used
        // (locale data can be changed by application code)
        Map<Long, String> dow = new HashMap<Long, String>();
        dow.put(1L, "Mon");
        dow.put(2L, "Tue");
        dow.put(3L, "Wed");
        dow.put(4L, "Thu");
        dow.put(5L, "Fri");
        dow.put(6L, "Sat");
        dow.put(7L, "Sun");
        Map<Long, String> moy = new HashMap<Long, String>();
        moy.put(1L, "Jan");
        moy.put(2L, "Feb");
        moy.put(3L, "Mar");
        moy.put(4L, "Apr");
        moy.put(5L, "May");
        moy.put(6L, "Jun");
        moy.put(7L, "Jul");
        moy.put(8L, "Aug");
        moy.put(9L, "Sep");
        moy.put(10L, "Oct");
        moy.put(11L, "Nov");
        moy.put(12L, "Dec");
        RFC_1123_DATE_TIME = new TDateTimeFormatterBuilder().parseCaseInsensitive().parseLenient().optionalStart()
                .appendText(DAY_OF_WEEK, dow).appendLiteral(", ").optionalEnd()
                .appendValue(DAY_OF_MONTH, 1, 2, TSignStyle.NOT_NEGATIVE).appendLiteral(' ')
                .appendText(MONTH_OF_YEAR, moy).appendLiteral(' ').appendValue(YEAR, 4) // 2 digit year not handled
                .appendLiteral(' ').appendValue(HOUR_OF_DAY, 2).appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2)
                .optionalStart().appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2).optionalEnd().appendLiteral(' ')
                .appendOffset("+HHMM", "GMT") // should handle UT/Z/EST/EDT/CST/CDT/MST/MDT/PST/MDT
                .toFormatter(TResolverStyle.SMART).withChronology(TIsoChronology.INSTANCE);
    }

    public static TDateTimeFormatter ofPattern(String pattern) {

        return new TDateTimeFormatterBuilder().appendPattern(pattern).toFormatter();
    }

    public static TDateTimeFormatter ofPattern(String pattern, Locale locale) {

        return new TDateTimeFormatterBuilder().appendPattern(pattern).toFormatter(locale);
    }

    public static TDateTimeFormatter ofLocalizedDate(TFormatStyle dateStyle) {

        Objects.requireNonNull(dateStyle, "dateStyle");
        return new TDateTimeFormatterBuilder().appendLocalized(dateStyle, null).toFormatter()
                .withChronology(TIsoChronology.INSTANCE);
    }

    public static TDateTimeFormatter ofLocalizedTime(TFormatStyle timeStyle) {

        Objects.requireNonNull(timeStyle, "timeStyle");
        return new TDateTimeFormatterBuilder().appendLocalized(null, timeStyle).toFormatter()
                .withChronology(TIsoChronology.INSTANCE);
    }

    public static TDateTimeFormatter ofLocalizedDateTime(TFormatStyle dateTimeStyle) {

        Objects.requireNonNull(dateTimeStyle, "dateTimeStyle");
        return new TDateTimeFormatterBuilder().appendLocalized(dateTimeStyle, dateTimeStyle).toFormatter()
                .withChronology(TIsoChronology.INSTANCE);
    }

    public static TDateTimeFormatter ofLocalizedDateTime(TFormatStyle dateStyle, TFormatStyle timeStyle) {

        Objects.requireNonNull(dateStyle, "dateStyle");
        Objects.requireNonNull(timeStyle, "timeStyle");
        return new TDateTimeFormatterBuilder().appendLocalized(dateStyle, timeStyle).toFormatter()
                .withChronology(TIsoChronology.INSTANCE);
    }

    public static final TTemporalQuery<TPeriod> parsedExcessDays() {

        return PARSED_EXCESS_DAYS;
    }

    private static final TTemporalQuery<TPeriod> PARSED_EXCESS_DAYS = new TTemporalQuery<TPeriod>() {
        @Override
        public TPeriod queryFrom(TTemporalAccessor temporal) {

            if (temporal instanceof TDateTimeBuilder) {
                return ((TDateTimeBuilder) temporal).excessDays;
            } else {
                return TPeriod.ZERO;
            }
        }
    };

    public static final TTemporalQuery<Boolean> parsedLeapSecond() {

        return PARSED_LEAP_SECOND;
    }

    private static final TTemporalQuery<Boolean> PARSED_LEAP_SECOND = new TTemporalQuery<Boolean>() {
        @Override
        public Boolean queryFrom(TTemporalAccessor temporal) {

            if (temporal instanceof TDateTimeBuilder) {
                return ((TDateTimeBuilder) temporal).leapSecond;
            } else {
                return Boolean.FALSE;
            }
        }
    };

    private final CompositePrinterParser printerParser;

    private final Locale locale;

    private final TDecimalStyle decimalStyle;

    private final TResolverStyle resolverStyle;

    private final Set<TTemporalField> resolverFields;

    private final TChronology chrono;

    private final TZoneId zone;

    TDateTimeFormatter(CompositePrinterParser printerParser, Locale locale, TDecimalStyle decimalStyle,
            TResolverStyle resolverStyle, Set<TTemporalField> resolverFields, TChronology chrono, TZoneId zone) {

        this.printerParser = Objects.requireNonNull(printerParser, "printerParser");
        this.locale = Objects.requireNonNull(locale, "locale");
        this.decimalStyle = Objects.requireNonNull(decimalStyle, "decimalStyle");
        this.resolverStyle = Objects.requireNonNull(resolverStyle, "resolverStyle");
        this.resolverFields = resolverFields;
        this.chrono = chrono;
        this.zone = zone;
    }

    public Locale getLocale() {

        return this.locale;
    }

    public TDateTimeFormatter withLocale(Locale locale) {

        if (this.locale.equals(locale)) {
            return this;
        }
        return new TDateTimeFormatter(this.printerParser, locale, this.decimalStyle, this.resolverStyle,
                this.resolverFields, this.chrono, this.zone);
    }

    public TDecimalStyle getDecimalStyle() {

        return this.decimalStyle;
    }

    public TDateTimeFormatter withDecimalStyle(TDecimalStyle decimalStyle) {

        if (this.decimalStyle.equals(decimalStyle)) {
            return this;
        }
        return new TDateTimeFormatter(this.printerParser, this.locale, decimalStyle, this.resolverStyle,
                this.resolverFields, this.chrono, this.zone);
    }

    public TChronology getChronology() {

        return this.chrono;
    }

    public TDateTimeFormatter withChronology(TChronology chrono) {

        if (Objects.equals(this.chrono, chrono)) {
            return this;
        }
        return new TDateTimeFormatter(this.printerParser, this.locale, this.decimalStyle, this.resolverStyle,
                this.resolverFields, chrono, this.zone);
    }

    public TZoneId getZone() {

        return this.zone;
    }

    public TDateTimeFormatter withZone(TZoneId zone) {

        if (Objects.equals(this.zone, zone)) {
            return this;
        }
        return new TDateTimeFormatter(this.printerParser, this.locale, this.decimalStyle, this.resolverStyle,
                this.resolverFields, this.chrono, zone);
    }

    public TResolverStyle getResolverStyle() {

        return this.resolverStyle;
    }

    public TDateTimeFormatter withResolverStyle(TResolverStyle resolverStyle) {

        Objects.requireNonNull(resolverStyle, "resolverStyle");
        if (Objects.equals(this.resolverStyle, resolverStyle)) {
            return this;
        }
        return new TDateTimeFormatter(this.printerParser, this.locale, this.decimalStyle, resolverStyle,
                this.resolverFields, this.chrono, this.zone);
    }

    public Set<TTemporalField> getResolverFields() {

        return this.resolverFields;
    }

    public TDateTimeFormatter withResolverFields(TTemporalField... resolverFields) {

        if (resolverFields == null) {
            return new TDateTimeFormatter(this.printerParser, this.locale, this.decimalStyle, this.resolverStyle, null,
                    this.chrono, this.zone);
        }
        Set<TTemporalField> fields = new HashSet<>(Arrays.asList(resolverFields));
        if (Objects.equals(this.resolverFields, fields)) {
            return this;
        }
        fields = Collections.unmodifiableSet(fields);
        return new TDateTimeFormatter(this.printerParser, this.locale, this.decimalStyle, this.resolverStyle, fields,
                this.chrono, this.zone);
    }

    public TDateTimeFormatter withResolverFields(Set<TTemporalField> resolverFields) {

        if (resolverFields == null) {
            return new TDateTimeFormatter(this.printerParser, this.locale, this.decimalStyle, this.resolverStyle, null,
                    this.chrono, this.zone);
        }
        if (Objects.equals(this.resolverFields, resolverFields)) {
            return this;
        }
        resolverFields = Collections.unmodifiableSet(new HashSet<>(resolverFields));
        return new TDateTimeFormatter(this.printerParser, this.locale, this.decimalStyle, this.resolverStyle,
                resolverFields, this.chrono, this.zone);
    }

    public String format(TTemporalAccessor temporal) {

        StringBuilder buf = new StringBuilder(32);
        formatTo(temporal, buf);
        return buf.toString();
    }

    public void formatTo(TTemporalAccessor temporal, Appendable appendable) {

        Objects.requireNonNull(temporal, "temporal");
        Objects.requireNonNull(appendable, "appendable");
        try {
            TDateTimePrintContext context = new TDateTimePrintContext(temporal, this);
            if (appendable instanceof StringBuilder) {
                this.printerParser.print(context, (StringBuilder) appendable);
            } else {
                // buffer output to avoid writing to appendable in case of error
                StringBuilder buf = new StringBuilder(32);
                this.printerParser.print(context, buf);
                appendable.append(buf);
            }
        } catch (IOException ex) {
            throw new TDateTimeException(ex.getMessage(), ex);
        }
    }

    public TTemporalAccessor parse(CharSequence text) {

        Objects.requireNonNull(text, "text");
        try {
            return parseToBuilder(text, null).resolve(this.resolverStyle, this.resolverFields);
        } catch (TDateTimeParseException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw createError(text, ex);
        }
    }

    public TTemporalAccessor parse(CharSequence text, ParsePosition position) {

        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(position, "position");
        try {
            return parseToBuilder(text, position).resolve(this.resolverStyle, this.resolverFields);
        } catch (TDateTimeParseException ex) {
            throw ex;
        } catch (IndexOutOfBoundsException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw createError(text, ex);
        }
    }

    public <T> T parse(CharSequence text, TTemporalQuery<T> type) {

        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(type, "type");
        try {
            TDateTimeBuilder builder = parseToBuilder(text, null).resolve(this.resolverStyle, this.resolverFields);
            return builder.build(type);
        } catch (TDateTimeParseException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw createError(text, ex);
        }
    }

    public TTemporalAccessor parseBest(CharSequence text, TTemporalQuery<?>... types) {

        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(types, "types");
        if (types.length < 2) {
            throw new IllegalArgumentException("At least two types must be specified");
        }
        try {
            TDateTimeBuilder builder = parseToBuilder(text, null).resolve(this.resolverStyle, this.resolverFields);
            for (TTemporalQuery<?> type : types) {
                try {
                    return (TTemporalAccessor) builder.build(type);
                } catch (RuntimeException ex) {
                    // continue
                }
            }
            throw new TDateTimeException(
                    "Unable to convert parsed text to any specified type: " + Arrays.toString(types));
        } catch (TDateTimeParseException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw createError(text, ex);
        }
    }

    private TDateTimeParseException createError(CharSequence text, RuntimeException ex) {

        String abbr = "";
        if (text.length() > 64) {
            abbr = text.subSequence(0, 64).toString() + "...";
        } else {
            abbr = text.toString();
        }
        return new TDateTimeParseException("Text '" + abbr + "' could not be parsed: " + ex.getMessage(), text, 0, ex);
    }

    private TDateTimeBuilder parseToBuilder(final CharSequence text, final ParsePosition position) {

        ParsePosition pos = (position != null ? position : new ParsePosition(0));
        Parsed result = parseUnresolved0(text, pos);
        if (result == null || pos.getErrorIndex() >= 0 || (position == null && pos.getIndex() < text.length())) {
            String abbr = "";
            if (text.length() > 64) {
                abbr = text.subSequence(0, 64).toString() + "...";
            } else {
                abbr = text.toString();
            }
            if (pos.getErrorIndex() >= 0) {
                throw new TDateTimeParseException(
                        "Text '" + abbr + "' could not be parsed at index " + pos.getErrorIndex(), text,
                        pos.getErrorIndex());
            } else {
                throw new TDateTimeParseException(
                        "Text '" + abbr + "' could not be parsed, unparsed text found at index " + pos.getIndex(), text,
                        pos.getIndex());
            }
        }
        return result.toBuilder();
    }

    public TTemporalAccessor parseUnresolved(CharSequence text, ParsePosition position) {

        return parseUnresolved0(text, position);
    }

    private Parsed parseUnresolved0(CharSequence text, ParsePosition position) {

        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(position, "position");
        TDateTimeParseContext context = new TDateTimeParseContext(this);
        int pos = position.getIndex();
        pos = this.printerParser.parse(context, text, pos);
        if (pos < 0) {
            position.setErrorIndex(~pos); // index not updated from input
            return null;
        }
        position.setIndex(pos); // errorIndex not updated from input
        return context.toParsed();
    }

    CompositePrinterParser toPrinterParser(boolean optional) {

        return this.printerParser.withOptional(optional);
    }

    public Format toFormat() {

        return new ClassicFormat(this, null);
    }

    public Format toFormat(TTemporalQuery<?> query) {

        Objects.requireNonNull(query, "query");
        return new ClassicFormat(this, query);
    }

    @Override
    public String toString() {

        String pattern = this.printerParser.toString();
        return pattern.startsWith("[") ? pattern : pattern.substring(1, pattern.length() - 1);
    }

    @SuppressWarnings("serial")
    static class ClassicFormat extends Format {
        private final TDateTimeFormatter formatter;

        private final TTemporalQuery<?> query;

        public ClassicFormat(TDateTimeFormatter formatter, TTemporalQuery<?> query) {

            this.formatter = formatter;
            this.query = query;
        }

        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {

            Objects.requireNonNull(obj, "obj");
            Objects.requireNonNull(toAppendTo, "toAppendTo");
            Objects.requireNonNull(pos, "pos");
            if (obj instanceof TTemporalAccessor == false) {
                throw new IllegalArgumentException("Format target must implement TTemporalAccessor");
            }
            pos.setBeginIndex(0);
            pos.setEndIndex(0);
            try {
                this.formatter.formatTo((TTemporalAccessor) obj, toAppendTo);
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
            return toAppendTo;
        }

        @Override
        public Object parseObject(String text) throws ParseException {

            Objects.requireNonNull(text, "text");
            try {
                if (this.query == null) {
                    return this.formatter.parseToBuilder(text, null).resolve(this.formatter.getResolverStyle(),
                            this.formatter.getResolverFields());
                }
                return this.formatter.parse(text, this.query);
            } catch (TDateTimeParseException ex) {
                throw new ParseException(ex.getMessage(), ex.getErrorIndex());
            } catch (RuntimeException ex) {
                throw (ParseException) new ParseException(ex.getMessage(), 0).initCause(ex);
            }
        }

        @Override
        public Object parseObject(String text, ParsePosition pos) {

            Objects.requireNonNull(text, "text");
            Parsed unresolved;
            try {
                unresolved = this.formatter.parseUnresolved0(text, pos);
            } catch (IndexOutOfBoundsException ex) {
                if (pos.getErrorIndex() < 0) {
                    pos.setErrorIndex(0);
                }
                return null;
            }
            if (unresolved == null) {
                if (pos.getErrorIndex() < 0) {
                    pos.setErrorIndex(0);
                }
                return null;
            }
            try {
                TDateTimeBuilder builder = unresolved.toBuilder().resolve(this.formatter.getResolverStyle(),
                        this.formatter.getResolverFields());
                if (this.query == null) {
                    return builder;
                }
                return builder.build(this.query);
            } catch (RuntimeException ex) {
                pos.setErrorIndex(0);
                return null;
            }
        }
    }

}
