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
import static org.teavm.classlib.java.time.temporal.TChronoField.HOUR_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.INSTANT_SECONDS;
import static org.teavm.classlib.java.time.temporal.TChronoField.MINUTE_OF_HOUR;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoField.OFFSET_SECONDS;
import static org.teavm.classlib.java.time.temporal.TChronoField.SECOND_OF_MINUTE;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.chrono.TChronoLocalDate;
import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.format.TSimpleDateTimeTextProvider.LocaleStore;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TIsoFields;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TValueRange;
import org.teavm.classlib.java.time.temporal.TWeekFields;
import org.teavm.classlib.java.time.zone.TZoneRulesProvider;
import org.teavm.classlib.java.util.TTimeZone;

public final class TDateTimeFormatterBuilder {

    private static final TTemporalQuery<TZoneId> QUERY_REGION_ONLY = new TTemporalQuery<TZoneId>() {
        @Override
        public TZoneId queryFrom(TTemporalAccessor temporal) {

            TZoneId zone = temporal.query(TTemporalQueries.zoneId());
            return (zone != null && zone instanceof TZoneOffset == false ? zone : null);
        }
    };

    private TDateTimeFormatterBuilder active = this;

    private final TDateTimeFormatterBuilder parent;

    private final List<DateTimePrinterParser> printerParsers = new ArrayList<>();

    private final boolean optional;

    private int padNextWidth;

    private char padNextChar;

    private int valueParserIndex = -1;

    public static String getLocalizedDateTimePattern(TFormatStyle dateStyle, TFormatStyle timeStyle, TChronology chrono,
            Locale locale) {

        TJdk8Methods.requireNonNull(locale, "locale");
        TJdk8Methods.requireNonNull(chrono, "chrono");
        if (dateStyle == null && timeStyle == null) {
            throw new IllegalArgumentException("Either dateStyle or timeStyle must be non-null");
        }
        DateFormat dateFormat;
        if (dateStyle != null) {
            if (timeStyle != null) {
                dateFormat = DateFormat.getDateTimeInstance(dateStyle.ordinal(), timeStyle.ordinal(), locale);
            } else {
                dateFormat = DateFormat.getDateInstance(dateStyle.ordinal(), locale);
            }
        } else {
            dateFormat = DateFormat.getTimeInstance(timeStyle.ordinal(), locale);
        }
        if (dateFormat instanceof SimpleDateFormat) {
            return ((SimpleDateFormat) dateFormat).toPattern();
        }
        throw new IllegalArgumentException("Unable to determine pattern");
    }

    public TDateTimeFormatterBuilder() {

        super();
        this.parent = null;
        this.optional = false;
    }

    private TDateTimeFormatterBuilder(TDateTimeFormatterBuilder parent, boolean optional) {

        super();
        this.parent = parent;
        this.optional = optional;
    }

    public TDateTimeFormatterBuilder parseCaseSensitive() {

        appendInternal(SettingsParser.SENSITIVE);
        return this;
    }

    public TDateTimeFormatterBuilder parseCaseInsensitive() {

        appendInternal(SettingsParser.INSENSITIVE);
        return this;
    }

    public TDateTimeFormatterBuilder parseStrict() {

        appendInternal(SettingsParser.STRICT);
        return this;
    }

    public TDateTimeFormatterBuilder parseLenient() {

        appendInternal(SettingsParser.LENIENT);
        return this;
    }

    public TDateTimeFormatterBuilder parseDefaulting(TTemporalField field, long value) {

        TJdk8Methods.requireNonNull(field, "field");
        appendInternal(new DefaultingParser(field, value));
        return this;
    }

    public TDateTimeFormatterBuilder appendValue(TTemporalField field) {

        TJdk8Methods.requireNonNull(field, "field");
        appendValue(new NumberPrinterParser(field, 1, 19, TSignStyle.NORMAL));
        return this;
    }

    public TDateTimeFormatterBuilder appendValue(TTemporalField field, int width) {

        TJdk8Methods.requireNonNull(field, "field");
        if (width < 1 || width > 19) {
            throw new IllegalArgumentException("The width must be from 1 to 19 inclusive but was " + width);
        }
        NumberPrinterParser pp = new NumberPrinterParser(field, width, width, TSignStyle.NOT_NEGATIVE);
        appendValue(pp);
        return this;
    }

    public TDateTimeFormatterBuilder appendValue(TTemporalField field, int minWidth, int maxWidth,
            TSignStyle signStyle) {

        if (minWidth == maxWidth && signStyle == TSignStyle.NOT_NEGATIVE) {
            return appendValue(field, maxWidth);
        }
        TJdk8Methods.requireNonNull(field, "field");
        TJdk8Methods.requireNonNull(signStyle, "signStyle");
        if (minWidth < 1 || minWidth > 19) {
            throw new IllegalArgumentException("The minimum width must be from 1 to 19 inclusive but was " + minWidth);
        }
        if (maxWidth < 1 || maxWidth > 19) {
            throw new IllegalArgumentException("The maximum width must be from 1 to 19 inclusive but was " + maxWidth);
        }
        if (maxWidth < minWidth) {
            throw new IllegalArgumentException(
                    "The maximum width must exceed or equal the minimum width but " + maxWidth + " < " + minWidth);
        }
        NumberPrinterParser pp = new NumberPrinterParser(field, minWidth, maxWidth, signStyle);
        appendValue(pp);
        return this;
    }

    public TDateTimeFormatterBuilder appendValueReduced(TTemporalField field, int width, int maxWidth, int baseValue) {

        TJdk8Methods.requireNonNull(field, "field");
        ReducedPrinterParser pp = new ReducedPrinterParser(field, width, maxWidth, baseValue, null);
        appendValue(pp);
        return this;
    }

    public TDateTimeFormatterBuilder appendValueReduced(TTemporalField field, int width, int maxWidth,
            TChronoLocalDate baseDate) {

        TJdk8Methods.requireNonNull(field, "field");
        TJdk8Methods.requireNonNull(baseDate, "baseDate");
        ReducedPrinterParser pp = new ReducedPrinterParser(field, width, maxWidth, 0, baseDate);
        appendValue(pp);
        return this;
    }

    private TDateTimeFormatterBuilder appendValue(NumberPrinterParser pp) {

        if (this.active.valueParserIndex >= 0
                && this.active.printerParsers.get(this.active.valueParserIndex) instanceof NumberPrinterParser) {
            final int activeValueParser = this.active.valueParserIndex;

            // adjacent parsing mode, update setting in previous parsers
            NumberPrinterParser basePP = (NumberPrinterParser) this.active.printerParsers.get(activeValueParser);
            if (pp.minWidth == pp.maxWidth && pp.signStyle == TSignStyle.NOT_NEGATIVE) {
                // Append the width to the subsequentWidth of the active parser
                basePP = basePP.withSubsequentWidth(pp.maxWidth);
                // Append the new parser as a fixed width
                appendInternal(pp.withFixedWidth());
                // Retain the previous active parser
                this.active.valueParserIndex = activeValueParser;
            } else {
                // Modify the active parser to be fixed width
                basePP = basePP.withFixedWidth();
                // The new parser becomes the mew active parser
                this.active.valueParserIndex = appendInternal(pp);
            }
            // Replace the modified parser with the updated one
            this.active.printerParsers.set(activeValueParser, basePP);
        } else {
            // The new Parser becomes the active parser
            this.active.valueParserIndex = appendInternal(pp);
        }
        return this;
    }

    public TDateTimeFormatterBuilder appendFraction(TTemporalField field, int minWidth, int maxWidth,
            boolean decimalPoint) {

        appendInternal(new FractionPrinterParser(field, minWidth, maxWidth, decimalPoint));
        return this;
    }

    public TDateTimeFormatterBuilder appendText(TTemporalField field) {

        return appendText(field, TTextStyle.FULL);
    }

    public TDateTimeFormatterBuilder appendText(TTemporalField field, TTextStyle textStyle) {

        TJdk8Methods.requireNonNull(field, "field");
        TJdk8Methods.requireNonNull(textStyle, "textStyle");
        appendInternal(new TextPrinterParser(field, textStyle, TDateTimeTextProvider.getInstance()));
        return this;
    }

    public TDateTimeFormatterBuilder appendText(TTemporalField field, Map<Long, String> textLookup) {

        TJdk8Methods.requireNonNull(field, "field");
        TJdk8Methods.requireNonNull(textLookup, "textLookup");
        Map<Long, String> copy = new LinkedHashMap<Long, String>(textLookup);
        Map<TTextStyle, Map<Long, String>> map = Collections.singletonMap(TTextStyle.FULL, copy);
        final LocaleStore store = new LocaleStore(map);
        TDateTimeTextProvider provider = new TDateTimeTextProvider() {
            @Override
            public String getText(TTemporalField field, long value, TTextStyle style, Locale locale) {

                return store.getText(value, style);
            }

            @Override
            public Iterator<Entry<String, Long>> getTextIterator(TTemporalField field, TTextStyle style,
                    Locale locale) {

                return store.getTextIterator(style);
            }
        };
        appendInternal(new TextPrinterParser(field, TTextStyle.FULL, provider));
        return this;
    }

    public TDateTimeFormatterBuilder appendInstant() {

        appendInternal(new InstantPrinterParser(-2));
        return this;
    }

    public TDateTimeFormatterBuilder appendInstant(int fractionalDigits) {

        if (fractionalDigits < -1 || fractionalDigits > 9) {
            throw new IllegalArgumentException("Invalid fractional digits: " + fractionalDigits);
        }
        appendInternal(new InstantPrinterParser(fractionalDigits));
        return this;
    }

    public TDateTimeFormatterBuilder appendOffsetId() {

        appendInternal(OffsetIdPrinterParser.INSTANCE_ID);
        return this;
    }

    public TDateTimeFormatterBuilder appendOffset(String pattern, String noOffsetText) {

        appendInternal(new OffsetIdPrinterParser(noOffsetText, pattern));
        return this;
    }

    public TDateTimeFormatterBuilder appendLocalizedOffset(TTextStyle style) {

        TJdk8Methods.requireNonNull(style, "style");
        if (style != TTextStyle.FULL && style != TTextStyle.SHORT) {
            throw new IllegalArgumentException("Style must be either full or short");
        }
        appendInternal(new LocalizedOffsetPrinterParser(style));
        return this;
    }

    public TDateTimeFormatterBuilder appendZoneId() {

        appendInternal(new ZoneIdPrinterParser(TTemporalQueries.zoneId(), "TZoneId()"));
        return this;
    }

    public TDateTimeFormatterBuilder appendZoneRegionId() {

        appendInternal(new ZoneIdPrinterParser(QUERY_REGION_ONLY, "ZoneRegionId()"));
        return this;
    }

    public TDateTimeFormatterBuilder appendZoneOrOffsetId() {

        appendInternal(new ZoneIdPrinterParser(TTemporalQueries.zone(), "ZoneOrOffsetId()"));
        return this;
    }

    public TDateTimeFormatterBuilder appendZoneText(TTextStyle textStyle) {

        appendInternal(new ZoneTextPrinterParser(textStyle));
        return this;
    }

    public TDateTimeFormatterBuilder appendZoneText(TTextStyle textStyle, Set<TZoneId> preferredZones) {

        // TODO: preferred zones currently ignored
        TJdk8Methods.requireNonNull(preferredZones, "preferredZones");
        appendInternal(new ZoneTextPrinterParser(textStyle));
        return this;
    }

    public TDateTimeFormatterBuilder appendChronologyId() {

        appendInternal(new ChronoPrinterParser(null));
        return this;
    }

    public TDateTimeFormatterBuilder appendChronologyText(TTextStyle textStyle) {

        TJdk8Methods.requireNonNull(textStyle, "textStyle");
        appendInternal(new ChronoPrinterParser(textStyle));
        return this;
    }

    public TDateTimeFormatterBuilder appendLocalized(TFormatStyle dateStyle, TFormatStyle timeStyle) {

        if (dateStyle == null && timeStyle == null) {
            throw new IllegalArgumentException("Either the date or time style must be non-null");
        }
        appendInternal(new LocalizedPrinterParser(dateStyle, timeStyle));
        return this;
    }

    public TDateTimeFormatterBuilder appendLiteral(char literal) {

        appendInternal(new CharLiteralPrinterParser(literal));
        return this;
    }

    public TDateTimeFormatterBuilder appendLiteral(String literal) {

        TJdk8Methods.requireNonNull(literal, "literal");
        if (literal.length() > 0) {
            if (literal.length() == 1) {
                appendInternal(new CharLiteralPrinterParser(literal.charAt(0)));
            } else {
                appendInternal(new StringLiteralPrinterParser(literal));
            }
        }
        return this;
    }

    public TDateTimeFormatterBuilder append(TDateTimeFormatter formatter) {

        TJdk8Methods.requireNonNull(formatter, "formatter");
        appendInternal(formatter.toPrinterParser(false));
        return this;
    }

    public TDateTimeFormatterBuilder appendOptional(TDateTimeFormatter formatter) {

        TJdk8Methods.requireNonNull(formatter, "formatter");
        appendInternal(formatter.toPrinterParser(true));
        return this;
    }

    public TDateTimeFormatterBuilder appendPattern(String pattern) {

        TJdk8Methods.requireNonNull(pattern, "pattern");
        parsePattern(pattern);
        return this;
    }

    private void parsePattern(String pattern) {

        for (int pos = 0; pos < pattern.length(); pos++) {
            char cur = pattern.charAt(pos);
            if ((cur >= 'A' && cur <= 'Z') || (cur >= 'a' && cur <= 'z')) {
                int start = pos++;
                for (; pos < pattern.length() && pattern.charAt(pos) == cur; pos++)
                    ; // short loop
                int count = pos - start;
                // padding
                if (cur == 'p') {
                    int pad = 0;
                    if (pos < pattern.length()) {
                        cur = pattern.charAt(pos);
                        if ((cur >= 'A' && cur <= 'Z') || (cur >= 'a' && cur <= 'z')) {
                            pad = count;
                            start = pos++;
                            for (; pos < pattern.length() && pattern.charAt(pos) == cur; pos++)
                                ; // short loop
                            count = pos - start;
                        }
                    }
                    if (pad == 0) {
                        throw new IllegalArgumentException(
                                "Pad letter 'p' must be followed by valid pad pattern: " + pattern);
                    }
                    padNext(pad); // pad and continue parsing
                }
                // main rules
                TTemporalField field = FIELD_MAP.get(cur);
                if (field != null) {
                    parseField(cur, count, field);
                } else if (cur == 'z') {
                    if (count > 4) {
                        throw new IllegalArgumentException("Too many pattern letters: " + cur);
                    } else if (count == 4) {
                        appendZoneText(TTextStyle.FULL);
                    } else {
                        appendZoneText(TTextStyle.SHORT);
                    }
                } else if (cur == 'V') {
                    if (count != 2) {
                        throw new IllegalArgumentException("Pattern letter count must be 2: " + cur);
                    }
                    appendZoneId();
                } else if (cur == 'Z') {
                    if (count < 4) {
                        appendOffset("+HHMM", "+0000");
                    } else if (count == 4) {
                        appendLocalizedOffset(TTextStyle.FULL);
                    } else if (count == 5) {
                        appendOffset("+HH:MM:ss", "Z");
                    } else {
                        throw new IllegalArgumentException("Too many pattern letters: " + cur);
                    }
                } else if (cur == 'O') {
                    if (count == 1) {
                        appendLocalizedOffset(TTextStyle.SHORT);
                    } else if (count == 4) {
                        appendLocalizedOffset(TTextStyle.FULL);
                    } else {
                        throw new IllegalArgumentException("Pattern letter count must be 1 or 4: " + cur);
                    }
                } else if (cur == 'X') {
                    if (count > 5) {
                        throw new IllegalArgumentException("Too many pattern letters: " + cur);
                    }
                    appendOffset(OffsetIdPrinterParser.PATTERNS[count + (count == 1 ? 0 : 1)], "Z");
                } else if (cur == 'x') {
                    if (count > 5) {
                        throw new IllegalArgumentException("Too many pattern letters: " + cur);
                    }
                    String zero = (count == 1 ? "+00" : (count % 2 == 0 ? "+0000" : "+00:00"));
                    appendOffset(OffsetIdPrinterParser.PATTERNS[count + (count == 1 ? 0 : 1)], zero);
                } else if (cur == 'W') {
                    if (count > 1) {
                        throw new IllegalArgumentException("Too many pattern letters: " + cur);
                    }
                    appendInternal(new WeekFieldsPrinterParser('W', count));
                } else if (cur == 'w') {
                    if (count > 2) {
                        throw new IllegalArgumentException("Too many pattern letters: " + cur);
                    }
                    appendInternal(new WeekFieldsPrinterParser('w', count));
                } else if (cur == 'Y') {
                    appendInternal(new WeekFieldsPrinterParser('Y', count));
                } else {
                    throw new IllegalArgumentException("Unknown pattern letter: " + cur);
                }
                pos--;

            } else if (cur == '\'') {
                // parse literals
                int start = pos++;
                for (; pos < pattern.length(); pos++) {
                    if (pattern.charAt(pos) == '\'') {
                        if (pos + 1 < pattern.length() && pattern.charAt(pos + 1) == '\'') {
                            pos++;
                        } else {
                            break; // end of literal
                        }
                    }
                }
                if (pos >= pattern.length()) {
                    throw new IllegalArgumentException("Pattern ends with an incomplete string literal: " + pattern);
                }
                String str = pattern.substring(start + 1, pos);
                if (str.length() == 0) {
                    appendLiteral('\'');
                } else {
                    appendLiteral(str.replace("''", "'"));
                }

            } else if (cur == '[') {
                optionalStart();

            } else if (cur == ']') {
                if (this.active.parent == null) {
                    throw new IllegalArgumentException("Pattern invalid as it contains ] without previous [");
                }
                optionalEnd();

            } else if (cur == '{' || cur == '}' || cur == '#') {
                throw new IllegalArgumentException("Pattern includes reserved character: '" + cur + "'");
            } else {
                appendLiteral(cur);
            }
        }
    }

    private void parseField(char cur, int count, TTemporalField field) {

        switch (cur) {
            case 'u':
            case 'y':
                if (count == 2) {
                    appendValueReduced(field, 2, 2, ReducedPrinterParser.BASE_DATE);
                } else if (count < 4) {
                    appendValue(field, count, 19, TSignStyle.NORMAL);
                } else {
                    appendValue(field, count, 19, TSignStyle.EXCEEDS_PAD);
                }
                break;
            case 'M':
            case 'Q':
                switch (count) {
                    case 1:
                        appendValue(field);
                        break;
                    case 2:
                        appendValue(field, 2);
                        break;
                    case 3:
                        appendText(field, TTextStyle.SHORT);
                        break;
                    case 4:
                        appendText(field, TTextStyle.FULL);
                        break;
                    case 5:
                        appendText(field, TTextStyle.NARROW);
                        break;
                    default:
                        throw new IllegalArgumentException("Too many pattern letters: " + cur);
                }
                break;
            case 'L':
            case 'q':
                switch (count) {
                    case 1:
                        appendValue(field);
                        break;
                    case 2:
                        appendValue(field, 2);
                        break;
                    case 3:
                        appendText(field, TTextStyle.SHORT_STANDALONE);
                        break;
                    case 4:
                        appendText(field, TTextStyle.FULL_STANDALONE);
                        break;
                    case 5:
                        appendText(field, TTextStyle.NARROW_STANDALONE);
                        break;
                    default:
                        throw new IllegalArgumentException("Too many pattern letters: " + cur);
                }
                break;
            case 'e':
                switch (count) {
                    case 1:
                    case 2:
                        appendInternal(new WeekFieldsPrinterParser('e', count));
                        break;
                    case 3:
                        appendText(field, TTextStyle.SHORT);
                        break;
                    case 4:
                        appendText(field, TTextStyle.FULL);
                        break;
                    case 5:
                        appendText(field, TTextStyle.NARROW);
                        break;
                    default:
                        throw new IllegalArgumentException("Too many pattern letters: " + cur);
                }
                break;
            case 'c':
                switch (count) {
                    case 1:
                        appendInternal(new WeekFieldsPrinterParser('c', count));
                        break;
                    case 2:
                        throw new IllegalArgumentException("Invalid number of pattern letters: " + cur);
                    case 3:
                        appendText(field, TTextStyle.SHORT_STANDALONE);
                        break;
                    case 4:
                        appendText(field, TTextStyle.FULL_STANDALONE);
                        break;
                    case 5:
                        appendText(field, TTextStyle.NARROW_STANDALONE);
                        break;
                    default:
                        throw new IllegalArgumentException("Too many pattern letters: " + cur);
                }
                break;
            case 'a':
                if (count == 1) {
                    appendText(field, TTextStyle.SHORT);
                } else {
                    throw new IllegalArgumentException("Too many pattern letters: " + cur);
                }
                break;
            case 'E':
            case 'G':
                switch (count) {
                    case 1:
                    case 2:
                    case 3:
                        appendText(field, TTextStyle.SHORT);
                        break;
                    case 4:
                        appendText(field, TTextStyle.FULL);
                        break;
                    case 5:
                        appendText(field, TTextStyle.NARROW);
                        break;
                    default:
                        throw new IllegalArgumentException("Too many pattern letters: " + cur);
                }
                break;
            case 'S':
                appendFraction(NANO_OF_SECOND, count, count, false);
                break;
            case 'F':
                if (count == 1) {
                    appendValue(field);
                } else {
                    throw new IllegalArgumentException("Too many pattern letters: " + cur);
                }
                break;
            case 'd':
            case 'h':
            case 'H':
            case 'k':
            case 'K':
            case 'm':
            case 's':
                if (count == 1) {
                    appendValue(field);
                } else if (count == 2) {
                    appendValue(field, count);
                } else {
                    throw new IllegalArgumentException("Too many pattern letters: " + cur);
                }
                break;
            case 'D':
                if (count == 1) {
                    appendValue(field);
                } else if (count <= 3) {
                    appendValue(field, count);
                } else {
                    throw new IllegalArgumentException("Too many pattern letters: " + cur);
                }
                break;
            default:
                if (count == 1) {
                    appendValue(field);
                } else {
                    appendValue(field, count);
                }
                break;
        }
    }

    private static final Map<Character, TTemporalField> FIELD_MAP = new HashMap<Character, TTemporalField>();
    static {
        FIELD_MAP.put('G', TChronoField.ERA);
        FIELD_MAP.put('y', TChronoField.YEAR_OF_ERA);
        FIELD_MAP.put('u', TChronoField.YEAR);
        FIELD_MAP.put('Q', TIsoFields.QUARTER_OF_YEAR);
        FIELD_MAP.put('q', TIsoFields.QUARTER_OF_YEAR);
        FIELD_MAP.put('M', TChronoField.MONTH_OF_YEAR);
        FIELD_MAP.put('L', TChronoField.MONTH_OF_YEAR);
        FIELD_MAP.put('D', TChronoField.DAY_OF_YEAR);
        FIELD_MAP.put('d', TChronoField.DAY_OF_MONTH);
        FIELD_MAP.put('F', TChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH);
        FIELD_MAP.put('E', TChronoField.DAY_OF_WEEK);
        FIELD_MAP.put('c', TChronoField.DAY_OF_WEEK);
        FIELD_MAP.put('e', TChronoField.DAY_OF_WEEK);
        FIELD_MAP.put('a', TChronoField.AMPM_OF_DAY);
        FIELD_MAP.put('H', TChronoField.HOUR_OF_DAY);
        FIELD_MAP.put('k', TChronoField.CLOCK_HOUR_OF_DAY);
        FIELD_MAP.put('K', TChronoField.HOUR_OF_AMPM);
        FIELD_MAP.put('h', TChronoField.CLOCK_HOUR_OF_AMPM);
        FIELD_MAP.put('m', TChronoField.MINUTE_OF_HOUR);
        FIELD_MAP.put('s', TChronoField.SECOND_OF_MINUTE);
        FIELD_MAP.put('S', TChronoField.NANO_OF_SECOND);
        FIELD_MAP.put('A', TChronoField.MILLI_OF_DAY);
        FIELD_MAP.put('n', TChronoField.NANO_OF_SECOND);
        FIELD_MAP.put('N', TChronoField.NANO_OF_DAY);
    }

    public TDateTimeFormatterBuilder padNext(int padWidth) {

        return padNext(padWidth, ' ');
    }

    public TDateTimeFormatterBuilder padNext(int padWidth, char padChar) {

        if (padWidth < 1) {
            throw new IllegalArgumentException("The pad width must be at least one but was " + padWidth);
        }
        this.active.padNextWidth = padWidth;
        this.active.padNextChar = padChar;
        this.active.valueParserIndex = -1;
        return this;
    }

    public TDateTimeFormatterBuilder optionalStart() {

        this.active.valueParserIndex = -1;
        this.active = new TDateTimeFormatterBuilder(this.active, true);
        return this;
    }

    public TDateTimeFormatterBuilder optionalEnd() {

        if (this.active.parent == null) {
            throw new IllegalStateException(
                    "Cannot call optionalEnd() as there was no previous call to optionalStart()");
        }
        if (this.active.printerParsers.size() > 0) {
            CompositePrinterParser cpp = new CompositePrinterParser(this.active.printerParsers, this.active.optional);
            this.active = this.active.parent;
            appendInternal(cpp);
        } else {
            this.active = this.active.parent;
        }
        return this;
    }

    private int appendInternal(DateTimePrinterParser pp) {

        TJdk8Methods.requireNonNull(pp, "pp");
        if (this.active.padNextWidth > 0) {
            if (pp != null) {
                pp = new PadPrinterParserDecorator(pp, this.active.padNextWidth, this.active.padNextChar);
            }
            this.active.padNextWidth = 0;
            this.active.padNextChar = 0;
        }
        this.active.printerParsers.add(pp);
        this.active.valueParserIndex = -1;
        return this.active.printerParsers.size() - 1;
    }

    public TDateTimeFormatter toFormatter() {

        return toFormatter(Locale.getDefault());
    }

    public TDateTimeFormatter toFormatter(Locale locale) {

        TJdk8Methods.requireNonNull(locale, "locale");
        while (this.active.parent != null) {
            optionalEnd();
        }
        CompositePrinterParser pp = new CompositePrinterParser(this.printerParsers, false);
        return new TDateTimeFormatter(pp, locale, TDecimalStyle.STANDARD, TResolverStyle.SMART, null, null, null);
    }

    TDateTimeFormatter toFormatter(TResolverStyle style) {

        return toFormatter().withResolverStyle(style);
    }

    interface DateTimePrinterParser {

        boolean print(TDateTimePrintContext context, StringBuilder buf);

        int parse(TDateTimeParseContext context, CharSequence text, int position);
    }

    static final class CompositePrinterParser implements DateTimePrinterParser {
        private final DateTimePrinterParser[] printerParsers;

        private final boolean optional;

        CompositePrinterParser(List<DateTimePrinterParser> printerParsers, boolean optional) {

            this(printerParsers.toArray(new DateTimePrinterParser[printerParsers.size()]), optional);
        }

        CompositePrinterParser(DateTimePrinterParser[] printerParsers, boolean optional) {

            this.printerParsers = printerParsers;
            this.optional = optional;
        }

        public CompositePrinterParser withOptional(boolean optional) {

            if (optional == this.optional) {
                return this;
            }
            return new CompositePrinterParser(this.printerParsers, optional);
        }

        @Override
        public boolean print(TDateTimePrintContext context, StringBuilder buf) {

            int length = buf.length();
            if (this.optional) {
                context.startOptional();
            }
            try {
                for (DateTimePrinterParser pp : this.printerParsers) {
                    if (pp.print(context, buf) == false) {
                        buf.setLength(length); // reset buffer
                        return true;
                    }
                }
            } finally {
                if (this.optional) {
                    context.endOptional();
                }
            }
            return true;
        }

        @Override
        public int parse(TDateTimeParseContext context, CharSequence text, int position) {

            if (this.optional) {
                context.startOptional();
                int pos = position;
                for (DateTimePrinterParser pp : this.printerParsers) {
                    pos = pp.parse(context, text, pos);
                    if (pos < 0) {
                        context.endOptional(false);
                        return position; // return original position
                    }
                }
                context.endOptional(true);
                return pos;
            } else {
                for (DateTimePrinterParser pp : this.printerParsers) {
                    position = pp.parse(context, text, position);
                    if (position < 0) {
                        break;
                    }
                }
                return position;
            }
        }

        @Override
        public String toString() {

            StringBuilder buf = new StringBuilder();
            if (this.printerParsers != null) {
                buf.append(this.optional ? "[" : "(");
                for (DateTimePrinterParser pp : this.printerParsers) {
                    buf.append(pp);
                }
                buf.append(this.optional ? "]" : ")");
            }
            return buf.toString();
        }
    }

    static final class PadPrinterParserDecorator implements DateTimePrinterParser {
        private final DateTimePrinterParser printerParser;

        private final int padWidth;

        private final char padChar;

        PadPrinterParserDecorator(DateTimePrinterParser printerParser, int padWidth, char padChar) {

            // input checked by TDateTimeFormatterBuilder
            this.printerParser = printerParser;
            this.padWidth = padWidth;
            this.padChar = padChar;
        }

        @Override
        public boolean print(TDateTimePrintContext context, StringBuilder buf) {

            int preLen = buf.length();
            if (this.printerParser.print(context, buf) == false) {
                return false;
            }
            int len = buf.length() - preLen;
            if (len > this.padWidth) {
                throw new TDateTimeException(
                        "Cannot print as output of " + len + " characters exceeds pad width of " + this.padWidth);
            }
            for (int i = 0; i < this.padWidth - len; i++) {
                buf.insert(preLen, this.padChar);
            }
            return true;
        }

        @Override
        public int parse(TDateTimeParseContext context, CharSequence text, int position) {

            // cache context before changed by decorated parser
            final boolean strict = context.isStrict();
            final boolean caseSensitive = context.isCaseSensitive();
            // parse
            if (position > text.length()) {
                throw new IndexOutOfBoundsException();
            }
            if (position == text.length()) {
                return ~position; // no more characters in the string
            }
            int endPos = position + this.padWidth;
            if (endPos > text.length()) {
                if (strict) {
                    return ~position; // not enough characters in the string to meet the parse width
                }
                endPos = text.length();
            }
            int pos = position;
            while (pos < endPos && (caseSensitive ? text.charAt(pos) == this.padChar
                    : context.charEquals(text.charAt(pos), this.padChar))) {
                pos++;
            }
            text = text.subSequence(0, endPos);
            int resultPos = this.printerParser.parse(context, text, pos);
            if (resultPos != endPos && strict) {
                return ~(position + pos); // parse of decorated field didn't parse to the end
            }
            return resultPos;
        }

        @Override
        public String toString() {

            return "Pad(" + this.printerParser + "," + this.padWidth
                    + (this.padChar == ' ' ? ")" : ",'" + this.padChar + "')");
        }
    }

    static enum SettingsParser implements DateTimePrinterParser {
        SENSITIVE, INSENSITIVE, STRICT, LENIENT;

        @Override
        public boolean print(TDateTimePrintContext context, StringBuilder buf) {

            return true; // nothing to do here
        }

        @Override
        public int parse(TDateTimeParseContext context, CharSequence text, int position) {

            // using ordinals to avoid javac synthetic inner class
            switch (ordinal()) {
                case 0:
                    context.setCaseSensitive(true);
                    break;
                case 1:
                    context.setCaseSensitive(false);
                    break;
                case 2:
                    context.setStrict(true);
                    break;
                case 3:
                    context.setStrict(false);
                    break;
            }
            return position;
        }

        @Override
        public String toString() {

            // using ordinals to avoid javac synthetic inner class
            switch (ordinal()) {
                case 0:
                    return "ParseCaseSensitive(true)";
                case 1:
                    return "ParseCaseSensitive(false)";
                case 2:
                    return "ParseStrict(true)";
                case 3:
                    return "ParseStrict(false)";
            }
            throw new IllegalStateException("Unreachable");
        }
    }

    static class DefaultingParser implements DateTimePrinterParser {
        private final TTemporalField field;

        private final long value;

        DefaultingParser(TTemporalField field, long value) {

            this.field = field;
            this.value = value;
        }

        @Override
        public boolean print(TDateTimePrintContext context, StringBuilder buf) {

            return true;
        }

        @Override
        public int parse(TDateTimeParseContext context, CharSequence text, int position) {

            if (context.getParsed(this.field) == null) {
                context.setParsedField(this.field, this.value, position, position);
            }
            return position;
        }
    }

    static final class CharLiteralPrinterParser implements DateTimePrinterParser {
        private final char literal;

        CharLiteralPrinterParser(char literal) {

            this.literal = literal;
        }

        @Override
        public boolean print(TDateTimePrintContext context, StringBuilder buf) {

            buf.append(this.literal);
            return true;
        }

        @Override
        public int parse(TDateTimeParseContext context, CharSequence text, int position) {

            int length = text.length();
            if (position == length) {
                return ~position;
            }
            char ch = text.charAt(position);
            if (context.charEquals(this.literal, ch) == false) {
                return ~position;
            }
            return position + 1;
        }

        @Override
        public String toString() {

            if (this.literal == '\'') {
                return "''";
            }
            return "'" + this.literal + "'";
        }
    }

    static final class StringLiteralPrinterParser implements DateTimePrinterParser {
        private final String literal;

        StringLiteralPrinterParser(String literal) {

            this.literal = literal; // validated by caller
        }

        @Override
        public boolean print(TDateTimePrintContext context, StringBuilder buf) {

            buf.append(this.literal);
            return true;
        }

        @Override
        public int parse(TDateTimeParseContext context, CharSequence text, int position) {

            int length = text.length();
            if (position > length || position < 0) {
                throw new IndexOutOfBoundsException();
            }
            if (context.subSequenceEquals(text, position, this.literal, 0, this.literal.length()) == false) {
                return ~position;
            }
            return position + this.literal.length();
        }

        @Override
        public String toString() {

            String converted = this.literal.replace("'", "''");
            return "'" + converted + "'";
        }
    }

    static class NumberPrinterParser implements DateTimePrinterParser {

        static final int[] EXCEED_POINTS = new int[] { 0, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000,
        1000000000, };

        final TTemporalField field;

        final int minWidth;

        final int maxWidth;

        final TSignStyle signStyle;

        final int subsequentWidth;

        NumberPrinterParser(TTemporalField field, int minWidth, int maxWidth, TSignStyle signStyle) {

            // validated by caller
            this.field = field;
            this.minWidth = minWidth;
            this.maxWidth = maxWidth;
            this.signStyle = signStyle;
            this.subsequentWidth = 0;
        }

        private NumberPrinterParser(TTemporalField field, int minWidth, int maxWidth, TSignStyle signStyle,
                int subsequentWidth) {

            // validated by caller
            this.field = field;
            this.minWidth = minWidth;
            this.maxWidth = maxWidth;
            this.signStyle = signStyle;
            this.subsequentWidth = subsequentWidth;
        }

        NumberPrinterParser withFixedWidth() {

            if (this.subsequentWidth == -1) {
                return this;
            }
            return new NumberPrinterParser(this.field, this.minWidth, this.maxWidth, this.signStyle, -1);
        }

        NumberPrinterParser withSubsequentWidth(int subsequentWidth) {

            return new NumberPrinterParser(this.field, this.minWidth, this.maxWidth, this.signStyle,
                    this.subsequentWidth + subsequentWidth);
        }

        @Override
        public boolean print(TDateTimePrintContext context, StringBuilder buf) {

            Long valueLong = context.getValue(this.field);
            if (valueLong == null) {
                return false;
            }
            long value = getValue(context, valueLong);
            TDecimalStyle symbols = context.getSymbols();
            String str = (value == Long.MIN_VALUE ? "9223372036854775808" : Long.toString(Math.abs(value)));
            if (str.length() > this.maxWidth) {
                throw new TDateTimeException("Field " + this.field + " cannot be printed as the value " + value
                        + " exceeds the maximum print width of " + this.maxWidth);
            }
            str = symbols.convertNumberToI18N(str);

            if (value >= 0) {
                switch (this.signStyle) {
                    case EXCEEDS_PAD:
                        if (this.minWidth < 19 && value >= EXCEED_POINTS[this.minWidth]) {
                            buf.append(symbols.getPositiveSign());
                        }
                        break;
                    case ALWAYS:
                        buf.append(symbols.getPositiveSign());
                        break;
                }
            } else {
                switch (this.signStyle) {
                    case NORMAL:
                    case EXCEEDS_PAD:
                    case ALWAYS:
                        buf.append(symbols.getNegativeSign());
                        break;
                    case NOT_NEGATIVE:
                        throw new TDateTimeException("Field " + this.field + " cannot be printed as the value " + value
                                + " cannot be negative according to the TSignStyle");
                }
            }
            for (int i = 0; i < this.minWidth - str.length(); i++) {
                buf.append(symbols.getZeroDigit());
            }
            buf.append(str);
            return true;
        }

        long getValue(TDateTimePrintContext context, long value) {

            return value;
        }

        boolean isFixedWidth(TDateTimeParseContext context) {

            return this.subsequentWidth == -1 || (this.subsequentWidth > 0 && this.minWidth == this.maxWidth
                    && this.signStyle == TSignStyle.NOT_NEGATIVE);
        }

        @Override
        public int parse(TDateTimeParseContext context, CharSequence text, int position) {

            int length = text.length();
            if (position == length) {
                return ~position;
            }
            char sign = text.charAt(position); // IOOBE if invalid position
            boolean negative = false;
            boolean positive = false;
            if (sign == context.getSymbols().getPositiveSign()) {
                if (this.signStyle.parse(true, context.isStrict(), this.minWidth == this.maxWidth) == false) {
                    return ~position;
                }
                positive = true;
                position++;
            } else if (sign == context.getSymbols().getNegativeSign()) {
                if (this.signStyle.parse(false, context.isStrict(), this.minWidth == this.maxWidth) == false) {
                    return ~position;
                }
                negative = true;
                position++;
            } else {
                if (this.signStyle == TSignStyle.ALWAYS && context.isStrict()) {
                    return ~position;
                }
            }
            int effMinWidth = (context.isStrict() || isFixedWidth(context) ? this.minWidth : 1);
            int minEndPos = position + effMinWidth;
            if (minEndPos > length) {
                return ~position;
            }
            int effMaxWidth = (context.isStrict() || isFixedWidth(context) ? this.maxWidth : 9)
                    + Math.max(this.subsequentWidth, 0);
            long total = 0;
            BigInteger totalBig = null;
            int pos = position;
            for (int pass = 0; pass < 2; pass++) {
                int maxEndPos = Math.min(pos + effMaxWidth, length);
                while (pos < maxEndPos) {
                    char ch = text.charAt(pos++);
                    int digit = context.getSymbols().convertToDigit(ch);
                    if (digit < 0) {
                        pos--;
                        if (pos < minEndPos) {
                            return ~position; // need at least min width digits
                        }
                        break;
                    }
                    if ((pos - position) > 18) {
                        if (totalBig == null) {
                            totalBig = BigInteger.valueOf(total);
                        }
                        totalBig = totalBig.multiply(BigInteger.TEN).add(BigInteger.valueOf(digit));
                    } else {
                        total = total * 10 + digit;
                    }
                }
                if (this.subsequentWidth > 0 && pass == 0) {
                    // re-parse now we know the correct width
                    int parseLen = pos - position;
                    effMaxWidth = Math.max(effMinWidth, parseLen - this.subsequentWidth);
                    pos = position;
                    total = 0;
                    totalBig = null;
                } else {
                    break;
                }
            }
            if (negative) {
                if (totalBig != null) {
                    if (totalBig.equals(BigInteger.ZERO) && context.isStrict()) {
                        return ~(position - 1); // minus zero not allowed
                    }
                    totalBig = totalBig.negate();
                } else {
                    if (total == 0 && context.isStrict()) {
                        return ~(position - 1); // minus zero not allowed
                    }
                    total = -total;
                }
            } else if (this.signStyle == TSignStyle.EXCEEDS_PAD && context.isStrict()) {
                int parseLen = pos - position;
                if (positive) {
                    if (parseLen <= this.minWidth) {
                        return ~(position - 1); // '+' only parsed if minWidth exceeded
                    }
                } else {
                    if (parseLen > this.minWidth) {
                        return ~position; // '+' must be parsed if minWidth exceeded
                    }
                }
            }
            if (totalBig != null) {
                if (totalBig.bitLength() > 63) {
                    // overflow, parse 1 less digit
                    totalBig = totalBig.divide(BigInteger.TEN);
                    pos--;
                }
                return setValue(context, totalBig.longValue(), position, pos);
            }
            return setValue(context, total, position, pos);
        }

        int setValue(TDateTimeParseContext context, long value, int errorPos, int successPos) {

            return context.setParsedField(this.field, value, errorPos, successPos);
        }

        @Override
        public String toString() {

            if (this.minWidth == 1 && this.maxWidth == 19 && this.signStyle == TSignStyle.NORMAL) {
                return "Value(" + this.field + ")";
            }
            if (this.minWidth == this.maxWidth && this.signStyle == TSignStyle.NOT_NEGATIVE) {
                return "Value(" + this.field + "," + this.minWidth + ")";
            }
            return "Value(" + this.field + "," + this.minWidth + "," + this.maxWidth + "," + this.signStyle + ")";
        }
    }

    static final class ReducedPrinterParser extends NumberPrinterParser {
        static final TLocalDate BASE_DATE = TLocalDate.of(2000, 1, 1);

        private final int baseValue;

        private final TChronoLocalDate baseDate;

        ReducedPrinterParser(TTemporalField field, int width, int maxWidth, int baseValue, TChronoLocalDate baseDate) {

            super(field, width, maxWidth, TSignStyle.NOT_NEGATIVE);
            if (width < 1 || width > 10) {
                throw new IllegalArgumentException("The width must be from 1 to 10 inclusive but was " + width);
            }
            if (maxWidth < 1 || maxWidth > 10) {
                throw new IllegalArgumentException("The maxWidth must be from 1 to 10 inclusive but was " + maxWidth);
            }
            if (maxWidth < width) {
                throw new IllegalArgumentException("The maxWidth must be greater than the width");
            }
            if (baseDate == null) {
                if (field.range().isValidValue(baseValue) == false) {
                    throw new IllegalArgumentException("The base value must be within the range of the field");
                }
                if ((((long) baseValue) + EXCEED_POINTS[width]) > Integer.MAX_VALUE) {
                    throw new TDateTimeException(
                            "Unable to add printer-parser as the range exceeds the capacity of an int");
                }
            }
            this.baseValue = baseValue;
            this.baseDate = baseDate;
        }

        private ReducedPrinterParser(TTemporalField field, int minWidth, int maxWidth, int baseValue,
                TChronoLocalDate baseDate, int subsequentWidth) {

            super(field, minWidth, maxWidth, TSignStyle.NOT_NEGATIVE, subsequentWidth);
            this.baseValue = baseValue;
            this.baseDate = baseDate;
        }

        @Override
        long getValue(TDateTimePrintContext context, long value) {

            long absValue = Math.abs(value);
            int baseValue = this.baseValue;
            if (this.baseDate != null) {
                TChronology chrono = TChronology.from(context.getTemporal());
                baseValue = chrono.date(this.baseDate).get(this.field);
            }
            if (value >= baseValue && value < baseValue + EXCEED_POINTS[this.minWidth]) {
                return absValue % EXCEED_POINTS[this.minWidth];
            }
            return absValue % EXCEED_POINTS[this.maxWidth];
        }

        @Override
        int setValue(TDateTimeParseContext context, long value, int errorPos, int successPos) {

            int baseValue = this.baseValue;
            if (this.baseDate != null) {
                TChronology chrono = context.getEffectiveChronology();
                baseValue = chrono.date(this.baseDate).get(this.field);
                context.addChronologyChangedParser(this, value, errorPos, successPos);
            }
            int parseLen = successPos - errorPos;
            if (parseLen == this.minWidth && value >= 0) {
                long range = EXCEED_POINTS[this.minWidth];
                long lastPart = baseValue % range;
                long basePart = baseValue - lastPart;
                if (baseValue > 0) {
                    value = basePart + value;
                } else {
                    value = basePart - value;
                }
                if (value < baseValue) {
                    value += range;
                }
            }
            return context.setParsedField(this.field, value, errorPos, successPos);
        }

        @Override
        NumberPrinterParser withFixedWidth() {

            if (this.subsequentWidth == -1) {
                return this;
            }
            return new ReducedPrinterParser(this.field, this.minWidth, this.maxWidth, this.baseValue, this.baseDate,
                    -1);
        }

        @Override
        ReducedPrinterParser withSubsequentWidth(int subsequentWidth) {

            return new ReducedPrinterParser(this.field, this.minWidth, this.maxWidth, this.baseValue, this.baseDate,
                    this.subsequentWidth + subsequentWidth);
        }

        @Override
        boolean isFixedWidth(TDateTimeParseContext context) {

            if (context.isStrict() == false) {
                return false;
            }
            return super.isFixedWidth(context);
        }

        @Override
        public String toString() {

            return "ReducedValue(" + this.field + "," + this.minWidth + "," + this.maxWidth + ","
                    + (this.baseDate != null ? this.baseDate : this.baseValue) + ")";
        }
    }

    static final class FractionPrinterParser implements DateTimePrinterParser {
        private final TTemporalField field;

        private final int minWidth;

        private final int maxWidth;

        private final boolean decimalPoint;

        FractionPrinterParser(TTemporalField field, int minWidth, int maxWidth, boolean decimalPoint) {

            TJdk8Methods.requireNonNull(field, "field");
            if (field.range().isFixed() == false) {
                throw new IllegalArgumentException("Field must have a fixed set of values: " + field);
            }
            if (minWidth < 0 || minWidth > 9) {
                throw new IllegalArgumentException("Minimum width must be from 0 to 9 inclusive but was " + minWidth);
            }
            if (maxWidth < 1 || maxWidth > 9) {
                throw new IllegalArgumentException("Maximum width must be from 1 to 9 inclusive but was " + maxWidth);
            }
            if (maxWidth < minWidth) {
                throw new IllegalArgumentException(
                        "Maximum width must exceed or equal the minimum width but " + maxWidth + " < " + minWidth);
            }
            this.field = field;
            this.minWidth = minWidth;
            this.maxWidth = maxWidth;
            this.decimalPoint = decimalPoint;
        }

        @Override
        public boolean print(TDateTimePrintContext context, StringBuilder buf) {

            Long value = context.getValue(this.field);
            if (value == null) {
                return false;
            }
            TDecimalStyle symbols = context.getSymbols();
            BigDecimal fraction = convertToFraction(value);
            if (fraction.scale() == 0) { // scale is zero if value is zero
                if (this.minWidth > 0) {
                    if (this.decimalPoint) {
                        buf.append(symbols.getDecimalSeparator());
                    }
                    for (int i = 0; i < this.minWidth; i++) {
                        buf.append(symbols.getZeroDigit());
                    }
                }
            } else {
                int outputScale = Math.min(Math.max(fraction.scale(), this.minWidth), this.maxWidth);
                fraction = fraction.setScale(outputScale, RoundingMode.FLOOR);
                String str = fraction.toPlainString().substring(2);
                str = symbols.convertNumberToI18N(str);
                if (this.decimalPoint) {
                    buf.append(symbols.getDecimalSeparator());
                }
                buf.append(str);
            }
            return true;
        }

        @Override
        public int parse(TDateTimeParseContext context, CharSequence text, int position) {

            int effectiveMin = (context.isStrict() ? this.minWidth : 0);
            int effectiveMax = (context.isStrict() ? this.maxWidth : 9);
            int length = text.length();
            if (position == length) {
                // valid if whole field is optional, invalid if minimum width
                return (effectiveMin > 0 ? ~position : position);
            }
            if (this.decimalPoint) {
                if (text.charAt(position) != context.getSymbols().getDecimalSeparator()) {
                    // valid if whole field is optional, invalid if minimum width
                    return (effectiveMin > 0 ? ~position : position);
                }
                position++;
            }
            int minEndPos = position + effectiveMin;
            if (minEndPos > length) {
                return ~position; // need at least min width digits
            }
            int maxEndPos = Math.min(position + effectiveMax, length);
            int total = 0; // can use int because we are only parsing up to 9 digits
            int pos = position;
            while (pos < maxEndPos) {
                char ch = text.charAt(pos++);
                int digit = context.getSymbols().convertToDigit(ch);
                if (digit < 0) {
                    if (pos < minEndPos) {
                        return ~position; // need at least min width digits
                    }
                    pos--;
                    break;
                }
                total = total * 10 + digit;
            }
            BigDecimal fraction = new BigDecimal(total).movePointLeft(pos - position);
            long value = convertFromFraction(fraction);
            return context.setParsedField(this.field, value, position, pos);
        }

        private BigDecimal convertToFraction(long value) {

            TValueRange range = this.field.range();
            range.checkValidValue(value, this.field);
            BigDecimal minBD = BigDecimal.valueOf(range.getMinimum());
            BigDecimal rangeBD = BigDecimal.valueOf(range.getMaximum()).subtract(minBD).add(BigDecimal.ONE);
            BigDecimal valueBD = BigDecimal.valueOf(value).subtract(minBD);
            BigDecimal fraction = valueBD.divide(rangeBD, 9, RoundingMode.FLOOR);
            // stripTrailingZeros bug
            return fraction.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : fraction.stripTrailingZeros();
        }

        private long convertFromFraction(BigDecimal fraction) {

            TValueRange range = this.field.range();
            BigDecimal minBD = BigDecimal.valueOf(range.getMinimum());
            BigDecimal rangeBD = BigDecimal.valueOf(range.getMaximum()).subtract(minBD).add(BigDecimal.ONE);
            BigDecimal valueBD = fraction.multiply(rangeBD).setScale(0, RoundingMode.FLOOR).add(minBD);
            return valueBD.longValueExact();
        }

        @Override
        public String toString() {

            String decimal = (this.decimalPoint ? ",DecimalPoint" : "");
            return "Fraction(" + this.field + "," + this.minWidth + "," + this.maxWidth + decimal + ")";
        }
    }

    static final class TextPrinterParser implements DateTimePrinterParser {
        private final TTemporalField field;

        private final TTextStyle textStyle;

        private final TDateTimeTextProvider provider;

        private volatile NumberPrinterParser numberPrinterParser;

        TextPrinterParser(TTemporalField field, TTextStyle textStyle, TDateTimeTextProvider provider) {

            // validated by caller
            this.field = field;
            this.textStyle = textStyle;
            this.provider = provider;
        }

        @Override
        public boolean print(TDateTimePrintContext context, StringBuilder buf) {

            Long value = context.getValue(this.field);
            if (value == null) {
                return false;
            }
            String text = this.provider.getText(this.field, value, this.textStyle, context.getLocale());
            if (text == null) {
                return numberPrinterParser().print(context, buf);
            }
            buf.append(text);
            return true;
        }

        @Override
        public int parse(TDateTimeParseContext context, CharSequence parseText, int position) {

            int length = parseText.length();
            if (position < 0 || position > length) {
                throw new IndexOutOfBoundsException();
            }
            TTextStyle style = (context.isStrict() ? this.textStyle : null);
            Iterator<Entry<String, Long>> it = this.provider.getTextIterator(this.field, style, context.getLocale());
            if (it != null) {
                while (it.hasNext()) {
                    Entry<String, Long> entry = it.next();
                    String itText = entry.getKey();
                    if (context.subSequenceEquals(itText, 0, parseText, position, itText.length())) {
                        return context.setParsedField(this.field, entry.getValue(), position,
                                position + itText.length());
                    }
                }
                if (context.isStrict()) {
                    return ~position;
                }
            }
            return numberPrinterParser().parse(context, parseText, position);
        }

        private NumberPrinterParser numberPrinterParser() {

            if (this.numberPrinterParser == null) {
                this.numberPrinterParser = new NumberPrinterParser(this.field, 1, 19, TSignStyle.NORMAL);
            }
            return this.numberPrinterParser;
        }

        @Override
        public String toString() {

            if (this.textStyle == TTextStyle.FULL) {
                return "Text(" + this.field + ")";
            }
            return "Text(" + this.field + "," + this.textStyle + ")";
        }
    }

    static final class InstantPrinterParser implements DateTimePrinterParser {
        // days in a 400 year cycle = 146097
        // days in a 10,000 year cycle = 146097 * 25
        // seconds per day = 86400
        private static final long SECONDS_PER_10000_YEARS = 146097L * 25L * 86400L;

        private static final long SECONDS_0000_TO_1970 = ((146097L * 5L) - (30L * 365L + 7L)) * 86400L;

        private final int fractionalDigits;

        InstantPrinterParser(int fractionalDigits) {

            this.fractionalDigits = fractionalDigits;
        }

        @Override
        public boolean print(TDateTimePrintContext context, StringBuilder buf) {

            // use INSTANT_SECONDS, thus this code is not bound by TInstant.MAX
            Long inSecs = context.getValue(INSTANT_SECONDS);
            Long inNanos = 0L;
            if (context.getTemporal().isSupported(NANO_OF_SECOND)) {
                inNanos = context.getTemporal().getLong(NANO_OF_SECOND);
            }
            if (inSecs == null) {
                return false;
            }
            long inSec = inSecs;
            int inNano = NANO_OF_SECOND.checkValidIntValue(inNanos);
            if (inSec >= -SECONDS_0000_TO_1970) {
                // current era
                long zeroSecs = inSec - SECONDS_PER_10000_YEARS + SECONDS_0000_TO_1970;
                long hi = TJdk8Methods.floorDiv(zeroSecs, SECONDS_PER_10000_YEARS) + 1;
                long lo = TJdk8Methods.floorMod(zeroSecs, SECONDS_PER_10000_YEARS);
                TLocalDateTime ldt = TLocalDateTime.ofEpochSecond(lo - SECONDS_0000_TO_1970, 0, TZoneOffset.UTC);
                if (hi > 0) {
                    buf.append('+').append(hi);
                }
                buf.append(ldt);
                if (ldt.getSecond() == 0) {
                    buf.append(":00");
                }
            } else {
                // before current era
                long zeroSecs = inSec + SECONDS_0000_TO_1970;
                long hi = zeroSecs / SECONDS_PER_10000_YEARS;
                long lo = zeroSecs % SECONDS_PER_10000_YEARS;
                TLocalDateTime ldt = TLocalDateTime.ofEpochSecond(lo - SECONDS_0000_TO_1970, 0, TZoneOffset.UTC);
                int pos = buf.length();
                buf.append(ldt);
                if (ldt.getSecond() == 0) {
                    buf.append(":00");
                }
                if (hi < 0) {
                    if (ldt.getYear() == -10000) {
                        buf.replace(pos, pos + 2, Long.toString(hi - 1));
                    } else if (lo == 0) {
                        buf.insert(pos, hi);
                    } else {
                        buf.insert(pos + 1, Math.abs(hi));
                    }
                }
            }
            // fraction
            if (this.fractionalDigits == -2) {
                if (inNano != 0) {
                    buf.append('.');
                    if (inNano % 1000000 == 0) {
                        buf.append(Integer.toString((inNano / 1000000) + 1000).substring(1));
                    } else if (inNano % 1000 == 0) {
                        buf.append(Integer.toString((inNano / 1000) + 1000000).substring(1));
                    } else {
                        buf.append(Integer.toString((inNano) + 1000000000).substring(1));
                    }
                }
            } else if (this.fractionalDigits > 0 || (this.fractionalDigits == -1 && inNano > 0)) {
                buf.append('.');
                int div = 100000000;
                for (int i = 0; ((this.fractionalDigits == -1 && inNano > 0) || i < this.fractionalDigits); i++) {
                    int digit = inNano / div;
                    buf.append((char) (digit + '0'));
                    inNano = inNano - (digit * div);
                    div = div / 10;
                }
            }
            buf.append('Z');
            return true;
        }

        @Override
        public int parse(TDateTimeParseContext context, CharSequence text, int position) {

            // new context to avoid overwriting fields like year/month/day
            TDateTimeParseContext newContext = context.copy();
            int minDigits = (this.fractionalDigits < 0 ? 0 : this.fractionalDigits);
            int maxDigits = (this.fractionalDigits < 0 ? 9 : this.fractionalDigits);
            CompositePrinterParser parser = new TDateTimeFormatterBuilder().append(TDateTimeFormatter.ISO_LOCAL_DATE)
                    .appendLiteral('T').appendValue(HOUR_OF_DAY, 2).appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2)
                    .appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2)
                    .appendFraction(NANO_OF_SECOND, minDigits, maxDigits, true).appendLiteral('Z').toFormatter()
                    .toPrinterParser(false);
            int pos = parser.parse(newContext, text, position);
            if (pos < 0) {
                return pos;
            }
            // parser restricts most fields to 2 digits, so definitely int
            // correctly parsed nano is also guaranteed to be valid
            long yearParsed = newContext.getParsed(YEAR);
            int month = newContext.getParsed(MONTH_OF_YEAR).intValue();
            int day = newContext.getParsed(DAY_OF_MONTH).intValue();
            int hour = newContext.getParsed(HOUR_OF_DAY).intValue();
            int min = newContext.getParsed(MINUTE_OF_HOUR).intValue();
            Long secVal = newContext.getParsed(SECOND_OF_MINUTE);
            Long nanoVal = newContext.getParsed(NANO_OF_SECOND);
            int sec = (secVal != null ? secVal.intValue() : 0);
            int nano = (nanoVal != null ? nanoVal.intValue() : 0);
            int year = (int) yearParsed % 10000;
            int days = 0;
            if (hour == 24 && min == 0 && sec == 0 && nano == 0) {
                hour = 0;
                days = 1;
            } else if (hour == 23 && min == 59 && sec == 60) {
                context.setParsedLeapSecond();
                sec = 59;
            }
            long instantSecs;
            try {
                TLocalDateTime ldt = TLocalDateTime.of(year, month, day, hour, min, sec, 0).plusDays(days);
                instantSecs = ldt.toEpochSecond(TZoneOffset.UTC);
                instantSecs += TJdk8Methods.safeMultiply(yearParsed / 10000L, SECONDS_PER_10000_YEARS);
            } catch (RuntimeException ex) {
                return ~position;
            }
            int successPos = pos;
            successPos = context.setParsedField(INSTANT_SECONDS, instantSecs, position, successPos);
            return context.setParsedField(NANO_OF_SECOND, nano, position, successPos);
        }

        @Override
        public String toString() {

            return "TInstant()";
        }
    }

    static final class OffsetIdPrinterParser implements DateTimePrinterParser {
        static final String[] PATTERNS = new String[] { "+HH", "+HHmm", "+HH:mm", "+HHMM", "+HH:MM", "+HHMMss",
        "+HH:MM:ss", "+HHMMSS", "+HH:MM:SS", }; // order used in pattern builder

        static final OffsetIdPrinterParser INSTANCE_ID = new OffsetIdPrinterParser("Z", "+HH:MM:ss");

        private final String noOffsetText;

        private final int type;

        OffsetIdPrinterParser(String noOffsetText, String pattern) {

            TJdk8Methods.requireNonNull(noOffsetText, "noOffsetText");
            TJdk8Methods.requireNonNull(pattern, "pattern");
            this.noOffsetText = noOffsetText;
            this.type = checkPattern(pattern);
        }

        private int checkPattern(String pattern) {

            for (int i = 0; i < PATTERNS.length; i++) {
                if (PATTERNS[i].equals(pattern)) {
                    return i;
                }
            }
            throw new IllegalArgumentException("Invalid zone offset pattern: " + pattern);
        }

        @Override
        public boolean print(TDateTimePrintContext context, StringBuilder buf) {

            Long offsetSecs = context.getValue(OFFSET_SECONDS);
            if (offsetSecs == null) {
                return false;
            }
            int totalSecs = TJdk8Methods.safeToInt(offsetSecs);
            if (totalSecs == 0) {
                buf.append(this.noOffsetText);
            } else {
                int absHours = Math.abs((totalSecs / 3600) % 100); // anything larger than 99 silently dropped
                int absMinutes = Math.abs((totalSecs / 60) % 60);
                int absSeconds = Math.abs(totalSecs % 60);
                int bufPos = buf.length();
                int output = absHours;
                buf.append(totalSecs < 0 ? "-" : "+").append((char) (absHours / 10 + '0'))
                        .append((char) (absHours % 10 + '0'));
                if (this.type >= 3 || (this.type >= 1 && absMinutes > 0)) {
                    buf.append((this.type % 2) == 0 ? ":" : "").append((char) (absMinutes / 10 + '0'))
                            .append((char) (absMinutes % 10 + '0'));
                    output += absMinutes;
                    if (this.type >= 7 || (this.type >= 5 && absSeconds > 0)) {
                        buf.append((this.type % 2) == 0 ? ":" : "").append((char) (absSeconds / 10 + '0'))
                                .append((char) (absSeconds % 10 + '0'));
                        output += absSeconds;
                    }
                }
                if (output == 0) {
                    buf.setLength(bufPos);
                    buf.append(this.noOffsetText);
                }
            }
            return true;
        }

        @Override
        public int parse(TDateTimeParseContext context, CharSequence text, int position) {

            int length = text.length();
            int noOffsetLen = this.noOffsetText.length();
            if (noOffsetLen == 0) {
                if (position == length) {
                    return context.setParsedField(OFFSET_SECONDS, 0, position, position);
                }
            } else {
                if (position == length) {
                    return ~position;
                }
                if (context.subSequenceEquals(text, position, this.noOffsetText, 0, noOffsetLen)) {
                    return context.setParsedField(OFFSET_SECONDS, 0, position, position + noOffsetLen);
                }
            }

            // parse normal plus/minus offset
            char sign = text.charAt(position); // IOOBE if invalid position
            if (sign == '+' || sign == '-') {
                // starts
                int negative = (sign == '-' ? -1 : 1);
                int[] array = new int[4];
                array[0] = position + 1;
                if ((parseNumber(array, 1, text, true) || parseNumber(array, 2, text, this.type >= 3)
                        || parseNumber(array, 3, text, false)) == false) {
                    // success
                    long offsetSecs = negative * (array[1] * 3600L + array[2] * 60L + array[3]);
                    return context.setParsedField(OFFSET_SECONDS, offsetSecs, position, array[0]);
                }
            }
            // handle special case of empty no offset text
            if (noOffsetLen == 0) {
                return context.setParsedField(OFFSET_SECONDS, 0, position, position + noOffsetLen);
            }
            return ~position;
        }

        private boolean parseNumber(int[] array, int arrayIndex, CharSequence parseText, boolean required) {

            if ((this.type + 3) / 2 < arrayIndex) {
                return false; // ignore seconds/minutes
            }
            int pos = array[0];
            if ((this.type % 2) == 0 && arrayIndex > 1) {
                if (pos + 1 > parseText.length() || parseText.charAt(pos) != ':') {
                    return required;
                }
                pos++;
            }
            if (pos + 2 > parseText.length()) {
                return required;
            }
            char ch1 = parseText.charAt(pos++);
            char ch2 = parseText.charAt(pos++);
            if (ch1 < '0' || ch1 > '9' || ch2 < '0' || ch2 > '9') {
                return required;
            }
            int value = (ch1 - 48) * 10 + (ch2 - 48);
            if (value < 0 || value > 59) {
                return required;
            }
            array[arrayIndex] = value;
            array[0] = pos;
            return false;
        }

        @Override
        public String toString() {

            String converted = this.noOffsetText.replace("'", "''");
            return "Offset(" + PATTERNS[this.type] + ",'" + converted + "')";
        }
    }

    static final class LocalizedOffsetPrinterParser implements DateTimePrinterParser {
        private final TTextStyle style;

        public LocalizedOffsetPrinterParser(TTextStyle style) {

            this.style = style;
        }

        @Override
        public boolean print(TDateTimePrintContext context, StringBuilder buf) {

            Long offsetSecs = context.getValue(OFFSET_SECONDS);
            if (offsetSecs == null) {
                return false;
            }
            buf.append("GMT");
            if (this.style == TTextStyle.FULL) {
                return new OffsetIdPrinterParser("", "+HH:MM:ss").print(context, buf);
            }
            int totalSecs = TJdk8Methods.safeToInt(offsetSecs);
            if (totalSecs != 0) {
                int absHours = Math.abs((totalSecs / 3600) % 100); // anything larger than 99 silently dropped
                int absMinutes = Math.abs((totalSecs / 60) % 60);
                int absSeconds = Math.abs(totalSecs % 60);
                buf.append(totalSecs < 0 ? "-" : "+").append(absHours);
                if (absMinutes > 0 || absSeconds > 0) {
                    buf.append(":").append((char) (absMinutes / 10 + '0')).append((char) (absMinutes % 10 + '0'));
                    if (absSeconds > 0) {
                        buf.append(":").append((char) (absSeconds / 10 + '0')).append((char) (absSeconds % 10 + '0'));
                    }
                }
            }
            return true;
        }

        @Override
        public int parse(TDateTimeParseContext context, CharSequence text, int position) {

            if (context.subSequenceEquals(text, position, "GMT", 0, 3) == false) {
                return ~position;
            }
            position += 3;
            if (this.style == TTextStyle.FULL) {
                return new OffsetIdPrinterParser("", "+HH:MM:ss").parse(context, text, position);
            }
            int end = text.length();
            if (position == end) {
                return context.setParsedField(OFFSET_SECONDS, 0, position, position);
            }
            char sign = text.charAt(position);
            if (sign != '+' && sign != '-') {
                return context.setParsedField(OFFSET_SECONDS, 0, position, position);
            }
            int negative = (sign == '-' ? -1 : 1);
            if (position == end) {
                return ~position;
            }
            position++;
            // hour
            char ch = text.charAt(position);
            if (ch < '0' || ch > '9') {
                return ~position;
            }
            position++;
            int hour = (ch - 48);
            if (position != end) {
                ch = text.charAt(position);
                if (ch >= '0' && ch <= '9') {
                    hour = hour * 10 + (ch - 48);
                    if (hour > 23) {
                        return ~position;
                    }
                    position++;
                }
            }
            if (position == end || text.charAt(position) != ':') {
                int offset = negative * 3600 * hour;
                return context.setParsedField(OFFSET_SECONDS, offset, position, position);
            }
            position++;
            // minute
            if (position > end - 2) {
                return ~position;
            }
            ch = text.charAt(position);
            if (ch < '0' || ch > '9') {
                return ~position;
            }
            position++;
            int min = (ch - 48);
            ch = text.charAt(position);
            if (ch < '0' || ch > '9') {
                return ~position;
            }
            position++;
            min = min * 10 + (ch - 48);
            if (min > 59) {
                return ~position;
            }
            if (position == end || text.charAt(position) != ':') {
                int offset = negative * (3600 * hour + 60 * min);
                return context.setParsedField(OFFSET_SECONDS, offset, position, position);
            }
            position++;
            // second
            if (position > end - 2) {
                return ~position;
            }
            ch = text.charAt(position);
            if (ch < '0' || ch > '9') {
                return ~position;
            }
            position++;
            int sec = (ch - 48);
            ch = text.charAt(position);
            if (ch < '0' || ch > '9') {
                return ~position;
            }
            position++;
            sec = sec * 10 + (ch - 48);
            if (sec > 59) {
                return ~position;
            }
            int offset = negative * (3600 * hour + 60 * min + sec);
            return context.setParsedField(OFFSET_SECONDS, offset, position, position);
        }
    }

    static final class ZoneTextPrinterParser implements DateTimePrinterParser {
        private static final Comparator<String> LENGTH_COMPARATOR = new Comparator<String>() {
            @Override
            public int compare(String str1, String str2) {

                int cmp = str2.length() - str1.length();
                if (cmp == 0) {
                    cmp = str1.compareTo(str2);
                }
                return cmp;
            }
        };

        private final TTextStyle textStyle;

        ZoneTextPrinterParser(TTextStyle textStyle) {

            this.textStyle = TJdk8Methods.requireNonNull(textStyle, "textStyle");
        }

        @Override
        public boolean print(TDateTimePrintContext context, StringBuilder buf) {

            TZoneId zone = context.getValue(TTemporalQueries.zoneId());
            if (zone == null) {
                return false;
            }
            if (zone.normalized() instanceof TZoneOffset) {
                buf.append(zone.getId());
                return true;
            }
            TTemporalAccessor temporal = context.getTemporal();
            boolean daylight = false;
            if (temporal.isSupported(INSTANT_SECONDS)) {
                TInstant instant = TInstant.ofEpochSecond(temporal.getLong(INSTANT_SECONDS));
                daylight = zone.getRules().isDaylightSavings(instant);
            }
            TimeZone tz = TimeZone.getTimeZone(zone.getId());
            int tzstyle = (this.textStyle.asNormal() == TTextStyle.FULL ? TTimeZone.LONG : TTimeZone.SHORT);
            String text = tz.getDisplayName(daylight, tzstyle, context.getLocale());
            buf.append(text);
            return true;
        }

        @Override
        public int parse(TDateTimeParseContext context, CharSequence text, int position) {

            // this is a poor implementation that handles some but not all of the spec
            // JDK8 has a lot of extra information here
            Map<String, String> ids = new TreeMap<>(LENGTH_COMPARATOR);
            for (String id : TZoneId.getAvailableZoneIds()) {
                ids.put(id, id);
                TimeZone tz = TimeZone.getTimeZone(id);
                int tzstyle = (this.textStyle.asNormal() == TTextStyle.FULL ? TTimeZone.LONG : TTimeZone.SHORT);
                String textWinter = tz.getDisplayName(false, tzstyle, context.getLocale());
                if (id.startsWith("Etc/") || (!textWinter.startsWith("GMT+") && !textWinter.startsWith("GMT+"))) {
                    ids.put(textWinter, id);
                }
                String textSummer = tz.getDisplayName(true, tzstyle, context.getLocale());
                if (id.startsWith("Etc/") || (!textSummer.startsWith("GMT+") && !textSummer.startsWith("GMT+"))) {
                    ids.put(textSummer, id);
                }
            }
            for (Entry<String, String> entry : ids.entrySet()) {
                String name = entry.getKey();
                if (context.subSequenceEquals(text, position, name, 0, name.length())) {
                    context.setParsed(TZoneId.of(entry.getValue()));
                    return position + name.length();
                }
            }
            return ~position;
        }

        @Override
        public String toString() {

            return "ZoneText(" + this.textStyle + ")";
        }
    }

    static final class ZoneIdPrinterParser implements DateTimePrinterParser {
        private final TTemporalQuery<TZoneId> query;

        private final String description;

        ZoneIdPrinterParser(TTemporalQuery<TZoneId> query, String description) {

            this.query = query;
            this.description = description;
        }

        @Override
        public boolean print(TDateTimePrintContext context, StringBuilder buf) {

            TZoneId zone = context.getValue(this.query);
            if (zone == null) {
                return false;
            }
            buf.append(zone.getId());
            return true;
        }

        private static volatile Entry<Integer, SubstringTree> cachedSubstringTree;

        @Override
        public int parse(TDateTimeParseContext context, CharSequence text, int position) {

            int length = text.length();
            if (position > length) {
                throw new IndexOutOfBoundsException();
            }
            if (position == length) {
                return ~position;
            }

            // handle fixed time-zone IDs
            char nextChar = text.charAt(position);
            if (nextChar == '+' || nextChar == '-') {
                TDateTimeParseContext newContext = context.copy();
                int endPos = OffsetIdPrinterParser.INSTANCE_ID.parse(newContext, text, position);
                if (endPos < 0) {
                    return endPos;
                }
                int offset = (int) newContext.getParsed(OFFSET_SECONDS).longValue();
                TZoneId zone = TZoneOffset.ofTotalSeconds(offset);
                context.setParsed(zone);
                return endPos;
            } else if (length >= position + 2) {
                char nextNextChar = text.charAt(position + 1);
                if (context.charEquals(nextChar, 'U') && context.charEquals(nextNextChar, 'T')) {
                    if (length >= position + 3 && context.charEquals(text.charAt(position + 2), 'C')) {
                        return parsePrefixedOffset(context, text, position, position + 3);
                    }
                    return parsePrefixedOffset(context, text, position, position + 2);
                } else if (context.charEquals(nextChar, 'G') && length >= position + 3
                        && context.charEquals(nextNextChar, 'M')
                        && context.charEquals(text.charAt(position + 2), 'T')) {
                    return parsePrefixedOffset(context, text, position, position + 3);
                }
            }

            // prepare parse tree
            Set<String> regionIds = TZoneRulesProvider.getAvailableZoneIds();
            final int regionIdsSize = regionIds.size();
            Entry<Integer, SubstringTree> cached = cachedSubstringTree;
            if (cached == null || cached.getKey() != regionIdsSize) {
                synchronized (this) {
                    cached = cachedSubstringTree;
                    if (cached == null || cached.getKey() != regionIdsSize) {
                        cachedSubstringTree = cached = new SimpleImmutableEntry<Integer, SubstringTree>(regionIdsSize,
                                prepareParser(regionIds));
                    }
                }
            }
            SubstringTree tree = cached.getValue();

            // parse
            String parsedZoneId = null;
            String lastZoneId = null;
            while (tree != null) {
                int nodeLength = tree.length;
                if (position + nodeLength > length) {
                    break;
                }
                lastZoneId = parsedZoneId;
                parsedZoneId = text.subSequence(position, position + nodeLength).toString();
                tree = tree.get(parsedZoneId, context.isCaseSensitive());
            }
            TZoneId zone = convertToZone(regionIds, parsedZoneId, context.isCaseSensitive());
            if (zone == null) {
                zone = convertToZone(regionIds, lastZoneId, context.isCaseSensitive());
                if (zone == null) {
                    if (context.charEquals(nextChar, 'Z')) {
                        context.setParsed(TZoneOffset.UTC);
                        return position + 1;
                    }
                    return ~position;
                }
                parsedZoneId = lastZoneId;
            }
            context.setParsed(zone);
            return position + parsedZoneId.length();
        }

        private TZoneId convertToZone(Set<String> regionIds, String parsedZoneId, boolean caseSensitive) {

            if (parsedZoneId == null) {
                return null;
            }
            if (caseSensitive) {
                return (regionIds.contains(parsedZoneId) ? TZoneId.of(parsedZoneId) : null);
            } else {
                for (String regionId : regionIds) {
                    if (regionId.equalsIgnoreCase(parsedZoneId)) {
                        return TZoneId.of(regionId);
                    }
                }
                return null;
            }
        }

        private int parsePrefixedOffset(TDateTimeParseContext context, CharSequence text, int prefixPos, int position) {

            String prefix = text.subSequence(prefixPos, position).toString().toUpperCase();
            TDateTimeParseContext newContext = context.copy();
            if (position < text.length() && context.charEquals(text.charAt(position), 'Z')) {
                context.setParsed(TZoneId.ofOffset(prefix, TZoneOffset.UTC));
                return position;
            }
            int endPos = OffsetIdPrinterParser.INSTANCE_ID.parse(newContext, text, position);
            if (endPos < 0) {
                context.setParsed(TZoneId.ofOffset(prefix, TZoneOffset.UTC));
                return position;
            }
            int offsetSecs = (int) newContext.getParsed(OFFSET_SECONDS).longValue();
            TZoneOffset offset = TZoneOffset.ofTotalSeconds(offsetSecs);
            context.setParsed(TZoneId.ofOffset(prefix, offset));
            return endPos;
        }

        private static final class SubstringTree {
            final int length;

            private final Map<CharSequence, SubstringTree> substringMap = new HashMap<>();

            private final Map<String, SubstringTree> substringMapCI = new HashMap<>();

            private SubstringTree(int length) {

                this.length = length;
            }

            private SubstringTree get(CharSequence substring2, boolean caseSensitive) {

                if (caseSensitive) {
                    return this.substringMap.get(substring2);
                } else {
                    return this.substringMapCI.get(substring2.toString().toLowerCase(Locale.ENGLISH));
                }
            }

            private void add(String newSubstring) {

                int idLen = newSubstring.length();
                if (idLen == this.length) {
                    this.substringMap.put(newSubstring, null);
                    this.substringMapCI.put(newSubstring.toLowerCase(Locale.ENGLISH), null);
                } else if (idLen > this.length) {
                    String substring = newSubstring.substring(0, this.length);
                    SubstringTree parserTree = this.substringMap.get(substring);
                    if (parserTree == null) {
                        parserTree = new SubstringTree(idLen);
                        this.substringMap.put(substring, parserTree);
                        this.substringMapCI.put(substring.toLowerCase(Locale.ENGLISH), parserTree);
                    }
                    parserTree.add(newSubstring);
                }
            }
        }

        private static SubstringTree prepareParser(Set<String> availableIDs) {

            // sort by length
            List<String> ids = new ArrayList<>(availableIDs);
            Collections.sort(ids, LENGTH_SORT);

            // build the tree
            SubstringTree tree = new SubstringTree(ids.get(0).length());
            for (String id : ids) {
                tree.add(id);
            }
            return tree;
        }

        @Override
        public String toString() {

            return this.description;
        }
    }

    static final class ChronoPrinterParser implements DateTimePrinterParser {
        private final TTextStyle textStyle;

        ChronoPrinterParser(TTextStyle textStyle) {

            // validated by caller
            this.textStyle = textStyle;
        }

        @Override
        public boolean print(TDateTimePrintContext context, StringBuilder buf) {

            TChronology chrono = context.getValue(TTemporalQueries.chronology());
            if (chrono == null) {
                return false;
            }
            if (this.textStyle == null) {
                buf.append(chrono.getId());
            } else {
                ResourceBundle bundle = ResourceBundle.getBundle("org.teavm.classlib.java.time.format.ChronologyText",
                        context.getLocale(), TDateTimeFormatterBuilder.class.getClassLoader());
                try {
                    String text = bundle.getString(chrono.getId());
                    buf.append(text);
                } catch (MissingResourceException ex) {
                    buf.append(chrono.getId());
                }
            }
            return true;
        }

        @Override
        public int parse(TDateTimeParseContext context, CharSequence text, int position) {

            // simple looping parser to find the chronology
            if (position < 0 || position > text.length()) {
                throw new IndexOutOfBoundsException();
            }
            Set<TChronology> chronos = TChronology.getAvailableChronologies();
            TChronology bestMatch = null;
            int matchLen = -1;
            for (TChronology chrono : chronos) {
                String id = chrono.getId();
                int idLen = id.length();
                if (idLen > matchLen && context.subSequenceEquals(text, position, id, 0, idLen)) {
                    bestMatch = chrono;
                    matchLen = idLen;
                }
            }
            if (bestMatch == null) {
                return ~position;
            }
            context.setParsed(bestMatch);
            return position + matchLen;
        }
    }

    static final class LocalizedPrinterParser implements DateTimePrinterParser {
        private final TFormatStyle dateStyle;

        private final TFormatStyle timeStyle;

        LocalizedPrinterParser(TFormatStyle dateStyle, TFormatStyle timeStyle) {

            // validated by caller
            this.dateStyle = dateStyle;
            this.timeStyle = timeStyle;
        }

        @Override
        public boolean print(TDateTimePrintContext context, StringBuilder buf) {

            TChronology chrono = TChronology.from(context.getTemporal());
            return formatter(context.getLocale(), chrono).toPrinterParser(false).print(context, buf);
        }

        @Override
        public int parse(TDateTimeParseContext context, CharSequence text, int position) {

            TChronology chrono = context.getEffectiveChronology();
            return formatter(context.getLocale(), chrono).toPrinterParser(false).parse(context, text, position);
        }

        private TDateTimeFormatter formatter(Locale locale, TChronology chrono) {

            return TDateTimeFormatStyleProvider.getInstance().getFormatter(this.dateStyle, this.timeStyle, chrono,
                    locale);
        }

        @Override
        public String toString() {

            return "Localized(" + (this.dateStyle != null ? this.dateStyle : "") + ","
                    + (this.timeStyle != null ? this.timeStyle : "") + ")";
        }
    }

    static final class WeekFieldsPrinterParser implements DateTimePrinterParser {
        private final char letter;

        private final int count;

        public WeekFieldsPrinterParser(char letter, int count) {

            this.letter = letter;
            this.count = count;
        }

        @Override
        public boolean print(TDateTimePrintContext context, StringBuilder buf) {

            TWeekFields weekFields = TWeekFields.of(context.getLocale());
            DateTimePrinterParser pp = evaluate(weekFields);
            return pp.print(context, buf);
        }

        @Override
        public int parse(TDateTimeParseContext context, CharSequence text, int position) {

            TWeekFields weekFields = TWeekFields.of(context.getLocale());
            DateTimePrinterParser pp = evaluate(weekFields);
            return pp.parse(context, text, position);
        }

        private DateTimePrinterParser evaluate(TWeekFields weekFields) {

            DateTimePrinterParser pp = null;
            switch (this.letter) {
                case 'e': // day-of-week
                    pp = new NumberPrinterParser(weekFields.dayOfWeek(), this.count, 2, TSignStyle.NOT_NEGATIVE);
                    break;
                case 'c': // day-of-week
                    pp = new NumberPrinterParser(weekFields.dayOfWeek(), this.count, 2, TSignStyle.NOT_NEGATIVE);
                    break;
                case 'w': // week-of-year
                    pp = new NumberPrinterParser(weekFields.weekOfWeekBasedYear(), this.count, 2,
                            TSignStyle.NOT_NEGATIVE);
                    break;
                case 'W': // week-of-month
                    pp = new NumberPrinterParser(weekFields.weekOfMonth(), 1, 2, TSignStyle.NOT_NEGATIVE);
                    break;
                case 'Y': // weekyear
                    if (this.count == 2) {
                        pp = new ReducedPrinterParser(weekFields.weekBasedYear(), 2, 2, 0,
                                ReducedPrinterParser.BASE_DATE);
                    } else {
                        pp = new NumberPrinterParser(weekFields.weekBasedYear(), this.count, 19,
                                (this.count < 4) ? TSignStyle.NORMAL : TSignStyle.EXCEEDS_PAD, -1);
                    }
                    break;
            }
            return pp;
        }

        @Override
        public String toString() {

            StringBuilder sb = new StringBuilder(30);
            sb.append("Localized(");
            if (this.letter == 'Y') {
                if (this.count == 1) {
                    sb.append("WeekBasedYear");
                } else if (this.count == 2) {
                    sb.append("ReducedValue(WeekBasedYear,2,2,2000-01-01)");
                } else {
                    sb.append("WeekBasedYear,").append(this.count).append(",").append(19).append(",")
                            .append((this.count < 4) ? TSignStyle.NORMAL : TSignStyle.EXCEEDS_PAD);
                }
            } else {
                if (this.letter == 'c' || this.letter == 'e') {
                    sb.append("TDayOfWeek");
                } else if (this.letter == 'w') {
                    sb.append("WeekOfWeekBasedYear");
                } else if (this.letter == 'W') {
                    sb.append("WeekOfMonth");
                }
                sb.append(",");
                sb.append(this.count);
            }
            sb.append(")");
            return sb.toString();
        }
    }

    static final Comparator<String> LENGTH_SORT = new Comparator<String>() {
        @Override
        public int compare(String str1, String str2) {

            return str1.length() == str2.length() ? str1.compareTo(str2) : str1.length() - str2.length();
        }
    };

}
