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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.teavm.classlib.java.text.TFieldPosition;
import org.teavm.classlib.java.text.TFormat;
import org.teavm.classlib.java.text.TParseException;
import org.teavm.classlib.java.text.TParsePosition;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TPeriod;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder.TCompositePrinterParser;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.util.TLocale;
import org.teavm.classlib.java.util.TObjects;

public final class TDateTimeFormatter {

  private final TCompositePrinterParser printerParser;

  private final TLocale locale;

  private final TDecimalStyle decimalStyle;

  private final TResolverStyle resolverStyle;

  private final Set<TTemporalField> resolverFields;

  private final TChronology chrono;

  private final TZoneId zone;

  public static TDateTimeFormatter ofPattern(String pattern) {

    return new TDateTimeFormatterBuilder().appendPattern(pattern).toFormatter();
  }

  public static TDateTimeFormatter ofPattern(String pattern, TLocale locale) {

    return new TDateTimeFormatterBuilder().appendPattern(pattern).toFormatter(locale);
  }

  public static TDateTimeFormatter ofLocalizedDate(TFormatStyle dateStyle) {

    TObjects.requireNonNull(dateStyle, "dateStyle");
    return new TDateTimeFormatterBuilder().appendLocalized(dateStyle, null).toFormatter(TResolverStyle.SMART,
        TIsoChronology.INSTANCE);
  }

  public static TDateTimeFormatter ofLocalizedTime(TFormatStyle timeStyle) {

    TObjects.requireNonNull(timeStyle, "timeStyle");
    return new TDateTimeFormatterBuilder().appendLocalized(null, timeStyle).toFormatter(TResolverStyle.SMART,
        TIsoChronology.INSTANCE);
  }

  public static TDateTimeFormatter ofLocalizedDateTime(TFormatStyle dateTimeStyle) {

    TObjects.requireNonNull(dateTimeStyle, "dateTimeStyle");
    return new TDateTimeFormatterBuilder().appendLocalized(dateTimeStyle, dateTimeStyle)
        .toFormatter(TResolverStyle.SMART, TIsoChronology.INSTANCE);
  }

  public static TDateTimeFormatter ofLocalizedDateTime(TFormatStyle dateStyle, TFormatStyle timeStyle) {

    TObjects.requireNonNull(dateStyle, "dateStyle");
    TObjects.requireNonNull(timeStyle, "timeStyle");
    return new TDateTimeFormatterBuilder().appendLocalized(dateStyle, timeStyle).toFormatter(TResolverStyle.SMART,
        TIsoChronology.INSTANCE);
  }

  public static final TDateTimeFormatter ISO_LOCAL_DATE;
  static {
    ISO_LOCAL_DATE = new TDateTimeFormatterBuilder().appendValue(YEAR, 4, 10, TSignStyle.EXCEEDS_PAD).appendLiteral('-')
        .appendValue(MONTH_OF_YEAR, 2).appendLiteral('-').appendValue(DAY_OF_MONTH, 2)
        .toFormatter(TResolverStyle.STRICT, TIsoChronology.INSTANCE);
  }

  public static final TDateTimeFormatter ISO_OFFSET_DATE;
  static {
    ISO_OFFSET_DATE = new TDateTimeFormatterBuilder().parseCaseInsensitive().append(ISO_LOCAL_DATE).appendOffsetId()
        .toFormatter(TResolverStyle.STRICT, TIsoChronology.INSTANCE);
  }

  public static final TDateTimeFormatter ISO_DATE;
  static {
    ISO_DATE = new TDateTimeFormatterBuilder().parseCaseInsensitive().append(ISO_LOCAL_DATE).optionalStart()
        .appendOffsetId().toFormatter(TResolverStyle.STRICT, TIsoChronology.INSTANCE);
  }

  public static final TDateTimeFormatter ISO_LOCAL_TIME;
  static {
    ISO_LOCAL_TIME = new TDateTimeFormatterBuilder().appendValue(HOUR_OF_DAY, 2).appendLiteral(':')
        .appendValue(MINUTE_OF_HOUR, 2).optionalStart().appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2)
        .optionalStart().appendFraction(NANO_OF_SECOND, 0, 9, true).toFormatter(TResolverStyle.STRICT, null);
  }

  public static final TDateTimeFormatter ISO_OFFSET_TIME;
  static {
    ISO_OFFSET_TIME = new TDateTimeFormatterBuilder().parseCaseInsensitive().append(ISO_LOCAL_TIME).appendOffsetId()
        .toFormatter(TResolverStyle.STRICT, null);
  }

  public static final TDateTimeFormatter ISO_TIME;
  static {
    ISO_TIME = new TDateTimeFormatterBuilder().parseCaseInsensitive().append(ISO_LOCAL_TIME).optionalStart()
        .appendOffsetId().toFormatter(TResolverStyle.STRICT, null);
  }

  public static final TDateTimeFormatter ISO_LOCAL_DATE_TIME;
  static {
    ISO_LOCAL_DATE_TIME = new TDateTimeFormatterBuilder().parseCaseInsensitive().append(ISO_LOCAL_DATE)
        .appendLiteral('T').append(ISO_LOCAL_TIME).toFormatter(TResolverStyle.STRICT, TIsoChronology.INSTANCE);
  }

  public static final TDateTimeFormatter ISO_OFFSET_DATE_TIME;
  static {
    ISO_OFFSET_DATE_TIME = new TDateTimeFormatterBuilder().parseCaseInsensitive().append(ISO_LOCAL_DATE_TIME)
        .parseLenient().appendOffsetId().parseStrict().toFormatter(TResolverStyle.STRICT, TIsoChronology.INSTANCE);
  }

  public static final TDateTimeFormatter ISO_ZONED_DATE_TIME;
  static {
    ISO_ZONED_DATE_TIME = new TDateTimeFormatterBuilder().append(ISO_OFFSET_DATE_TIME).optionalStart()
        .appendLiteral('[').parseCaseSensitive().appendZoneRegionId().appendLiteral(']')
        .toFormatter(TResolverStyle.STRICT, TIsoChronology.INSTANCE);
  }

  public static final TDateTimeFormatter ISO_DATE_TIME;
  static {
    ISO_DATE_TIME = new TDateTimeFormatterBuilder().append(ISO_LOCAL_DATE_TIME).optionalStart().appendOffsetId()
        .optionalStart().appendLiteral('[').parseCaseSensitive().appendZoneRegionId().appendLiteral(']')
        .toFormatter(TResolverStyle.STRICT, TIsoChronology.INSTANCE);
  }

  public static final TDateTimeFormatter ISO_ORDINAL_DATE;
  static {
    ISO_ORDINAL_DATE = new TDateTimeFormatterBuilder().parseCaseInsensitive()
        .appendValue(YEAR, 4, 10, TSignStyle.EXCEEDS_PAD).appendLiteral('-').appendValue(DAY_OF_YEAR, 3).optionalStart()
        .appendOffsetId().toFormatter(TResolverStyle.STRICT, TIsoChronology.INSTANCE);
  }

  // public static final TDateTimeFormatter ISO_WEEK_DATE;
  // static {
  // ISO_WEEK_DATE = new TDateTimeFormatterBuilder().parseCaseInsensitive()
  // .appendValue(IsoFields.WEEK_BASED_YEAR, 4, 10, SignStyle.EXCEEDS_PAD).appendLiteral("-W")
  // .appendValue(IsoFields.WEEK_OF_WEEK_BASED_YEAR, 2).appendLiteral('-').appendValue(DAY_OF_WEEK, 1)
  // .optionalStart().appendOffsetId().toFormatter(TResolverStyle.STRICT, TIsoChronology.INSTANCE);
  // }

  public static final TDateTimeFormatter ISO_INSTANT;
  static {
    ISO_INSTANT = new TDateTimeFormatterBuilder().parseCaseInsensitive().appendInstant()
        .toFormatter(TResolverStyle.STRICT, null);
  }

  public static final TDateTimeFormatter BASIC_ISO_DATE;
  static {
    BASIC_ISO_DATE = new TDateTimeFormatterBuilder().parseCaseInsensitive().appendValue(YEAR, 4)
        .appendValue(MONTH_OF_YEAR, 2).appendValue(DAY_OF_MONTH, 2).optionalStart().parseLenient()
        .appendOffset("+HHMMss", "Z").parseStrict().toFormatter(TResolverStyle.STRICT, TIsoChronology.INSTANCE);
  }

  public static final TDateTimeFormatter RFC_1123_DATE_TIME;
  static {
    Map<Long, String> dow = new HashMap<>();
    dow.put(1L, "Mon");
    dow.put(2L, "Tue");
    dow.put(3L, "Wed");
    dow.put(4L, "Thu");
    dow.put(5L, "Fri");
    dow.put(6L, "Sat");
    dow.put(7L, "Sun");
    Map<Long, String> moy = new HashMap<>();
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
        .appendValue(DAY_OF_MONTH, 1, 2, TSignStyle.NOT_NEGATIVE).appendLiteral(' ').appendText(MONTH_OF_YEAR, moy)
        .appendLiteral(' ').appendValue(YEAR, 4).appendLiteral(' ').appendValue(HOUR_OF_DAY, 2).appendLiteral(':')
        .appendValue(MINUTE_OF_HOUR, 2).optionalStart().appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2)
        .optionalEnd().appendLiteral(' ').appendOffset("+HHMM", "GMT")
        .toFormatter(TResolverStyle.SMART, TIsoChronology.INSTANCE);
  }

  public static final TTemporalQuery<TPeriod> parsedExcessDays() {

    return PARSED_EXCESS_DAYS;
  }

  private static final TTemporalQuery<TPeriod> PARSED_EXCESS_DAYS = t -> {
    if (t instanceof TParsed) {
      return ((TParsed) t).excessDays;
    } else {
      return TPeriod.ZERO;
    }
  };

  public static final TTemporalQuery<Boolean> parsedLeapSecond() {

    return PARSED_LEAP_SECOND;
  }

  private static final TTemporalQuery<Boolean> PARSED_LEAP_SECOND = t -> {
    if (t instanceof TParsed) {
      return ((TParsed) t).leapSecond;
    } else {
      return Boolean.FALSE;
    }
  };

  TDateTimeFormatter(TCompositePrinterParser printerParser, TLocale locale, TDecimalStyle decimalStyle,
      TResolverStyle resolverStyle, Set<TTemporalField> resolverFields, TChronology chrono, TZoneId zone) {

    this.printerParser = TObjects.requireNonNull(printerParser, "printerParser");
    this.resolverFields = resolverFields;
    this.locale = TObjects.requireNonNull(locale, "locale");
    this.decimalStyle = TObjects.requireNonNull(decimalStyle, "decimalStyle");
    this.resolverStyle = TObjects.requireNonNull(resolverStyle, "resolverStyle");
    this.chrono = chrono;
    this.zone = zone;
  }

  public TLocale getLocale() {

    return this.locale;
  }

  public TDateTimeFormatter withLocale(TLocale locale) {

    if (this.locale.equals(locale)) {
      return this;
    }
    return new TDateTimeFormatter(this.printerParser, locale, this.decimalStyle, this.resolverStyle,
        this.resolverFields, this.chrono, this.zone);
  }

  public TDateTimeFormatter localizedBy(TLocale locale) {

    if (this.locale.equals(locale)) {
      return this;
    }

    TChronology c = this.chrono;
    TDecimalStyle ds = this.decimalStyle;
    TZoneId z = this.zone;
    return new TDateTimeFormatter(this.printerParser, locale, ds, this.resolverStyle, this.resolverFields, c, z);
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

    if (TObjects.equals(this.chrono, chrono)) {
      return this;
    }
    return new TDateTimeFormatter(this.printerParser, this.locale, this.decimalStyle, this.resolverStyle,
        this.resolverFields, chrono, this.zone);
  }

  public TZoneId getZone() {

    return this.zone;
  }

  public TDateTimeFormatter withZone(TZoneId zone) {

    if (TObjects.equals(this.zone, zone)) {
      return this;
    }
    return new TDateTimeFormatter(this.printerParser, this.locale, this.decimalStyle, this.resolverStyle,
        this.resolverFields, this.chrono, zone);
  }

  public TResolverStyle getResolverStyle() {

    return this.resolverStyle;
  }

  public TDateTimeFormatter withResolverStyle(TResolverStyle resolverStyle) {

    TObjects.requireNonNull(resolverStyle, "resolverStyle");
    if (TObjects.equals(this.resolverStyle, resolverStyle)) {
      return this;
    }
    return new TDateTimeFormatter(this.printerParser, this.locale, this.decimalStyle, resolverStyle,
        this.resolverFields, this.chrono, this.zone);
  }

  public Set<TTemporalField> getResolverFields() {

    return this.resolverFields;
  }

  public TDateTimeFormatter withResolverFields(TTemporalField... resolverFields) {

    Set<TTemporalField> fields = null;
    if (resolverFields != null) {
      // Set.of cannot be used because it is hostile to nulls and duplicate elements
      fields = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(resolverFields)));
    }
    if (TObjects.equals(this.resolverFields, fields)) {
      return this;
    }
    return new TDateTimeFormatter(this.printerParser, this.locale, this.decimalStyle, this.resolverStyle, fields,
        this.chrono, this.zone);
  }

  public TDateTimeFormatter withResolverFields(Set<TTemporalField> resolverFields) {

    if (TObjects.equals(this.resolverFields, resolverFields)) {
      return this;
    }
    if (resolverFields != null) {
      resolverFields = Collections.unmodifiableSet(new HashSet<>(resolverFields));
    }
    return new TDateTimeFormatter(this.printerParser, this.locale, this.decimalStyle, this.resolverStyle,
        resolverFields, this.chrono, this.zone);
  }

  public String format(TTemporalAccessor temporal) {

    StringBuilder buf = new StringBuilder(32);
    formatTo(temporal, buf);
    return buf.toString();
  }

  public void formatTo(TTemporalAccessor temporal, Appendable appendable) {

    TObjects.requireNonNull(temporal, "temporal");
    TObjects.requireNonNull(appendable, "appendable");
    try {
      TDateTimePrintContext context = new TDateTimePrintContext(temporal, this);
      if (appendable instanceof StringBuilder) {
        this.printerParser.format(context, (StringBuilder) appendable);
      } else {
        StringBuilder buf = new StringBuilder(32);
        this.printerParser.format(context, buf);
        appendable.append(buf);
      }
    } catch (IOException ex) {
      throw new TDateTimeException(ex.getMessage(), ex);
    }
  }

  public TTemporalAccessor parse(CharSequence text) {

    TObjects.requireNonNull(text, "text");
    try {
      return parseResolved0(text, null);
    } catch (TDateTimeParseException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      throw createError(text, ex);
    }
  }

  public TTemporalAccessor parse(CharSequence text, TParsePosition position) {

    TObjects.requireNonNull(text, "text");
    TObjects.requireNonNull(position, "position");
    try {
      return parseResolved0(text, position);
    } catch (TDateTimeParseException | IndexOutOfBoundsException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      throw createError(text, ex);
    }
  }

  public <T> T parse(CharSequence text, TTemporalQuery<T> query) {

    Objects.requireNonNull(text, "text");
    Objects.requireNonNull(query, "query");
    try {
      return parseResolved0(text, null).query(query);
    } catch (TDateTimeParseException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      throw createError(text, ex);
    }
  }

  public TTemporalAccessor parseBest(CharSequence text, TTemporalQuery<?>... queries) {

    TObjects.requireNonNull(text, "text");
    TObjects.requireNonNull(queries, "queries");
    if (queries.length < 2) {
      throw new IllegalArgumentException("At least two queries must be specified");
    }
    try {
      TTemporalAccessor resolved = parseResolved0(text, null);
      for (TTemporalQuery<?> query : queries) {
        try {
          return (TTemporalAccessor) resolved.query(query);
        } catch (RuntimeException ex) {
        }
      }
      throw new TDateTimeException("Unable to convert parsed text using any of the specified queries");
    } catch (TDateTimeParseException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      throw createError(text, ex);
    }
  }

  private TDateTimeParseException createError(CharSequence text, RuntimeException ex) {

    String abbr;
    if (text.length() > 64) {
      abbr = text.subSequence(0, 64).toString() + "...";
    } else {
      abbr = text.toString();
    }
    return new TDateTimeParseException("Text '" + abbr + "' could not be parsed: " + ex.getMessage(), text, 0, ex);
  }

  private TTemporalAccessor parseResolved0(final CharSequence text, final TParsePosition position) {

    TParsePosition pos = (position != null ? position : new TParsePosition(0));
    TDateTimeParseContext context = parseUnresolved0(text, pos);
    if (context == null || pos.getErrorIndex() >= 0 || (position == null && pos.getIndex() < text.length())) {
      String abbr;
      if (text.length() > 64) {
        abbr = text.subSequence(0, 64).toString() + "...";
      } else {
        abbr = text.toString();
      }
      if (pos.getErrorIndex() >= 0) {
        throw new TDateTimeParseException("Text '" + abbr + "' could not be parsed at index " + pos.getErrorIndex(),
            text, pos.getErrorIndex());
      } else {
        throw new TDateTimeParseException(
            "Text '" + abbr + "' could not be parsed, unparsed text found at index " + pos.getIndex(), text,
            pos.getIndex());
      }
    }
    return context.toResolved(this.resolverStyle, this.resolverFields);
  }

  public TTemporalAccessor parseUnresolved(CharSequence text, TParsePosition position) {

    TDateTimeParseContext context = parseUnresolved0(text, position);
    if (context == null) {
      return null;
    }
    return context.toUnresolved();
  }

  private TDateTimeParseContext parseUnresolved0(CharSequence text, TParsePosition position) {

    TObjects.requireNonNull(text, "text");
    TObjects.requireNonNull(position, "position");
    TDateTimeParseContext context = new TDateTimeParseContext(this);
    int pos = position.getIndex();
    pos = this.printerParser.parse(context, text, pos);
    if (pos < 0) {
      position.setErrorIndex(~pos);
      return null;
    }
    position.setIndex(pos);
    return context;
  }

  TCompositePrinterParser toPrinterParser(boolean optional) {

    return this.printerParser.withOptional(optional);
  }

  public TFormat toFormat() {

    return new TClassicFormat(this, null);
  }

  public TFormat toFormat(TTemporalQuery<?> parseQuery) {

    TObjects.requireNonNull(parseQuery, "parseQuery");
    return new TClassicFormat(this, parseQuery);
  }

  @Override
  public String toString() {

    String pattern = this.printerParser.toString();
    pattern = pattern.startsWith("[") ? pattern : pattern.substring(1, pattern.length() - 1);
    return pattern;
  }

  static class TClassicFormat extends TFormat {
    private final TDateTimeFormatter formatter;

    private final TTemporalQuery<?> parseType;

    public TClassicFormat(TDateTimeFormatter formatter, TTemporalQuery<?> parseType) {

      this.formatter = formatter;
      this.parseType = parseType;
    }

    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, TFieldPosition pos) {

      TObjects.requireNonNull(obj, "obj");
      TObjects.requireNonNull(toAppendTo, "toAppendTo");
      TObjects.requireNonNull(pos, "pos");
      if (obj instanceof TTemporalAccessor == false) {
        throw new IllegalArgumentException("Format target must implement TemporalAccessor");
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
    public Object parseObject(String text) throws TParseException {

      Objects.requireNonNull(text, "text");
      try {
        if (this.parseType == null) {
          return this.formatter.parseResolved0(text, null);
        }
        return this.formatter.parse(text, this.parseType);
      } catch (TDateTimeParseException ex) {
        throw new TParseException(ex.getMessage(), ex.getErrorIndex());
      } catch (RuntimeException ex) {
        throw (TParseException) new TParseException(ex.getMessage(), 0).initCause(ex);
      }
    }

    @Override
    public Object parseObject(String text, TParsePosition pos) {

      Objects.requireNonNull(text, "text");
      TDateTimeParseContext context;
      try {
        context = this.formatter.parseUnresolved0(text, pos);
      } catch (IndexOutOfBoundsException ex) {
        if (pos.getErrorIndex() < 0) {
          pos.setErrorIndex(0);
        }
        return null;
      }
      if (context == null) {
        if (pos.getErrorIndex() < 0) {
          pos.setErrorIndex(0);
        }
        return null;
      }
      try {
        TTemporalAccessor resolved = context.toResolved(this.formatter.resolverStyle, this.formatter.resolverFields);
        if (this.parseType == null) {
          return resolved;
        }
        return resolved.query(this.parseType);
      } catch (RuntimeException ex) {
        pos.setErrorIndex(0);
        return null;
      }
    }
  }

}
