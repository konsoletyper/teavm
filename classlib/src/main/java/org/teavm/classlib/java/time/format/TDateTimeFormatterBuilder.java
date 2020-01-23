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
import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.format.SignStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.chrono.TChronoLocalDate;
import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TValueRange;
import org.teavm.classlib.java.util.TLocale;
import org.teavm.classlib.java.util.TObjects;

public final class TDateTimeFormatterBuilder {

  private static final TTemporalQuery<TZoneId> QUERY_REGION_ONLY = (temporal) -> {
    TZoneId zone = temporal.query(TTemporalQueries.zoneId());
    return (zone != null && zone instanceof TZoneOffset == false ? zone : null);
  };

  private static final String[] LOCALIZED_PATTERN_DATE = { "y MMMM d, EEEE ", "y MMMM d ", "y MMM d ", "y-MM-dd " };

  private static final String[] LOCALIZED_PATTERN_TIME = { "HH:mm:ss zzzz", "HH:mm:ss", "HH:mm:ss", "HH:mm" };

  private TDateTimeFormatterBuilder active = this;

  private final TDateTimeFormatterBuilder parent;

  private final List<TDateTimePrinterParser> printerParsers = new ArrayList<>();

  private final boolean optional;

  private int padNextWidth;

  private char padNextChar;

  private int valueParserIndex = -1;

  public static String getLocalizedDateTimePattern(TFormatStyle dateStyle, TFormatStyle timeStyle, TChronology chrono,
      TLocale locale) {

    TObjects.requireNonNull(locale, "locale");
    TObjects.requireNonNull(chrono, "chrono");

    if (dateStyle == null && timeStyle == null) {
      throw new IllegalArgumentException("Either dateStyle or timeStyle must be non-null");
    }
    String datePattern = "";
    if (dateStyle != null) {
      datePattern = LOCALIZED_PATTERN_DATE[dateStyle.ordinal()];
    }
    String timePattern = "";
    if (timeStyle != null) {
      timePattern = LOCALIZED_PATTERN_TIME[timeStyle.ordinal()];
    }
    String infix = "";
    if (!datePattern.isEmpty() && timePattern.isEmpty()) {
      // TODO consider adding localized 'at' if date style is full or long
      infix = " ";
    }
    return datePattern + infix + timePattern;
  }

  private static int convertStyle(TFormatStyle style) {

    if (style == null) {
      return -1;
    }
    return style.ordinal();
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

    appendInternal(TSettingsParser.SENSITIVE);
    return this;
  }

  public TDateTimeFormatterBuilder parseCaseInsensitive() {

    appendInternal(TSettingsParser.INSENSITIVE);
    return this;
  }

  public TDateTimeFormatterBuilder parseStrict() {

    appendInternal(TSettingsParser.STRICT);
    return this;
  }

  public TDateTimeFormatterBuilder parseLenient() {

    appendInternal(TSettingsParser.LENIENT);
    return this;
  }

  public TDateTimeFormatterBuilder parseDefaulting(TTemporalField field, long value) {

    Objects.requireNonNull(field, "field");
    appendInternal(new TDefaultValueParser(field, value));
    return this;
  }

  public TDateTimeFormatterBuilder appendValue(TTemporalField field) {

    TObjects.requireNonNull(field, "field");
    appendValue(new TNumberPrinterParser(field, 1, 19, TSignStyle.NORMAL));
    return this;
  }

  public TDateTimeFormatterBuilder appendValue(TTemporalField field, int width) {

    TObjects.requireNonNull(field, "field");
    if (width < 1 || width > 19) {
      throw new IllegalArgumentException("The width must be from 1 to 19 inclusive but was " + width);
    }
    TNumberPrinterParser pp = new TNumberPrinterParser(field, width, width, TSignStyle.NOT_NEGATIVE);
    appendValue(pp);
    return this;
  }

  public TDateTimeFormatterBuilder appendValue(TTemporalField field, int minWidth, int maxWidth, TSignStyle signStyle) {

    if (minWidth == maxWidth && signStyle == TSignStyle.NOT_NEGATIVE) {
      return appendValue(field, maxWidth);
    }
    Objects.requireNonNull(field, "field");
    Objects.requireNonNull(signStyle, "signStyle");
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
    TNumberPrinterParser pp = new TNumberPrinterParser(field, minWidth, maxWidth, signStyle);
    appendValue(pp);
    return this;
  }

  public TDateTimeFormatterBuilder appendValueReduced(TTemporalField field, int width, int maxWidth, int baseValue) {

    TObjects.requireNonNull(field, "field");
    TReducedPrinterParser pp = new TReducedPrinterParser(field, width, maxWidth, baseValue, null);
    appendValue(pp);
    return this;
  }

  public TDateTimeFormatterBuilder appendValueReduced(TTemporalField field, int width, int maxWidth,
      TChronoLocalDate baseDate) {

    TObjects.requireNonNull(field, "field");
    TObjects.requireNonNull(baseDate, "baseDate");
    TReducedPrinterParser pp = new TReducedPrinterParser(field, width, maxWidth, 0, baseDate);
    appendValue(pp);
    return this;
  }

  private TDateTimeFormatterBuilder appendValue(TNumberPrinterParser pp) {

    if (this.active.valueParserIndex >= 0) {
      final int activeValueParser = this.active.valueParserIndex;

      TNumberPrinterParser basePP = (TNumberPrinterParser) this.active.printerParsers.get(activeValueParser);
      if (pp.minWidth == pp.maxWidth && pp.signStyle == TSignStyle.NOT_NEGATIVE) {
        basePP = basePP.withSubsequentWidth(pp.maxWidth);
        appendInternal(pp.withFixedWidth());
        this.active.valueParserIndex = activeValueParser;
      } else {
        basePP = basePP.withFixedWidth();
        this.active.valueParserIndex = appendInternal(pp);
      }
      this.active.printerParsers.set(activeValueParser, basePP);
    } else {
      this.active.valueParserIndex = appendInternal(pp);
    }
    return this;
  }

  public TDateTimeFormatterBuilder appendFraction(TTemporalField field, int minWidth, int maxWidth,
      boolean decimalPoint) {

    if (minWidth == maxWidth && decimalPoint == false) {
      // adjacent parsing
      appendValue(new TFractionPrinterParser(field, minWidth, maxWidth, decimalPoint));
    } else {
      appendInternal(new TFractionPrinterParser(field, minWidth, maxWidth, decimalPoint));
    }
    return this;
  }

  public TDateTimeFormatterBuilder appendText(TTemporalField field) {

    return appendText(field, TTextStyle.FULL);
  }

  public TDateTimeFormatterBuilder appendText(TTemporalField field, TTextStyle textStyle) {

    TObjects.requireNonNull(field, "field");
    TObjects.requireNonNull(textStyle, "textStyle");
    appendInternal(new TTextPrinterParser(field, textStyle));
    return this;
  }

  public TDateTimeFormatterBuilder appendText(TTemporalField field, Map<Long, String> textLookup) {

    TObjects.requireNonNull(field, "field");
    TObjects.requireNonNull(textLookup, "textLookup");
    appendInternal(new TTextPrinterParser(field, TTextStyle.FULL));
    return this;
  }

  public TDateTimeFormatterBuilder appendInstant() {

    appendInternal(new TInstantPrinterParser(-2));
    return this;
  }

  public TDateTimeFormatterBuilder appendInstant(int fractionalDigits) {

    if (fractionalDigits < -1 || fractionalDigits > 9) {
      throw new IllegalArgumentException(
          "The fractional digits must be from -1 to 9 inclusive but was " + fractionalDigits);
    }
    appendInternal(new TInstantPrinterParser(fractionalDigits));
    return this;
  }

  public TDateTimeFormatterBuilder appendOffsetId() {

    appendInternal(TOffsetIdPrinterParser.INSTANCE_ID_Z);
    return this;
  }

  public TDateTimeFormatterBuilder appendOffset(String pattern, String noOffsetText) {

    appendInternal(new TOffsetIdPrinterParser(pattern, noOffsetText));
    return this;
  }

  public TDateTimeFormatterBuilder appendLocalizedOffset(TTextStyle style) {

    Objects.requireNonNull(style, "style");
    if (style != TTextStyle.FULL && style != TTextStyle.SHORT) {
      throw new IllegalArgumentException("Style must be either full or short");
    }
    appendInternal(new TLocalizedOffsetIdPrinterParser(style));
    return this;
  }

  public TDateTimeFormatterBuilder appendZoneId() {

    appendInternal(new TZoneIdPrinterParser(TTemporalQueries.zoneId(), "ZoneId()"));
    return this;
  }

  public TDateTimeFormatterBuilder appendZoneRegionId() {

    appendInternal(new TZoneIdPrinterParser(QUERY_REGION_ONLY, "ZoneRegionId()"));
    return this;
  }

  public TDateTimeFormatterBuilder appendZoneOrOffsetId() {

    appendInternal(new TZoneIdPrinterParser(TTemporalQueries.zone(), "ZoneOrOffsetId()"));
    return this;
  }

  public TDateTimeFormatterBuilder appendZoneText(TTextStyle textStyle) {

    appendInternal(new TZoneTextPrinterParser(textStyle, false));
    return this;
  }

  public TDateTimeFormatterBuilder appendGenericZoneText(TTextStyle textStyle) {

    appendInternal(new TZoneTextPrinterParser(textStyle, true));
    return this;
  }

  public TDateTimeFormatterBuilder appendChronologyId() {

    appendInternal(new TChronoPrinterParser(null));
    return this;
  }

  public TDateTimeFormatterBuilder appendChronologyText(TTextStyle textStyle) {

    TObjects.requireNonNull(textStyle, "textStyle");
    appendInternal(new TChronoPrinterParser(textStyle));
    return this;
  }

  public TDateTimeFormatterBuilder appendLocalized(TFormatStyle dateStyle, TFormatStyle timeStyle) {

    if (dateStyle == null && timeStyle == null) {
      throw new IllegalArgumentException("Either the date or time style must be non-null");
    }
    appendInternal(new TLocalizedPrinterParser(dateStyle, timeStyle));
    return this;
  }

  public TDateTimeFormatterBuilder appendLiteral(char literal) {

    appendInternal(new TCharLiteralPrinterParser(literal));
    return this;
  }

  public TDateTimeFormatterBuilder appendLiteral(String literal) {

    TObjects.requireNonNull(literal, "literal");
    if (!literal.isEmpty()) {
      if (literal.length() == 1) {
        appendInternal(new TCharLiteralPrinterParser(literal.charAt(0)));
      } else {
        appendInternal(new TStringLiteralPrinterParser(literal));
      }
    }
    return this;
  }

  public TDateTimeFormatterBuilder append(TDateTimeFormatter formatter) {

    TObjects.requireNonNull(formatter, "formatter");
    appendInternal(formatter.toPrinterParser(false));
    return this;
  }

  public TDateTimeFormatterBuilder appendOptional(TDateTimeFormatter formatter) {

    TObjects.requireNonNull(formatter, "formatter");
    appendInternal(formatter.toPrinterParser(true));
    return this;
  }

  public TDateTimeFormatterBuilder appendPattern(String pattern) {

    TObjects.requireNonNull(pattern, "pattern");
    parsePattern(pattern);
    return this;
  }

  private void parsePattern(String pattern) {

    for (int pos = 0; pos < pattern.length(); pos++) {
      char cur = pattern.charAt(pos);
      if ((cur >= 'A' && cur <= 'Z') || (cur >= 'a' && cur <= 'z')) {
        int start = pos++;
        for (; pos < pattern.length() && pattern.charAt(pos) == cur; pos++)
          ;
        int count = pos - start;
        if (cur == 'p') {
          int pad = 0;
          if (pos < pattern.length()) {
            cur = pattern.charAt(pos);
            if ((cur >= 'A' && cur <= 'Z') || (cur >= 'a' && cur <= 'z')) {
              pad = count;
              start = pos++;
              for (; pos < pattern.length() && pattern.charAt(pos) == cur; pos++)
                ;
              count = pos - start;
            }
          }
          if (pad == 0) {
            throw new IllegalArgumentException("Pad letter 'p' must be followed by valid pad pattern: " + pattern);
          }
          padNext(pad);
        }
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
        } else if (cur == 'v') {
          if (count == 1) {
            appendGenericZoneText(TTextStyle.SHORT);
          } else if (count == 4) {
            appendGenericZoneText(TTextStyle.FULL);
          } else {
            throw new IllegalArgumentException("Wrong number of  pattern letters: " + cur);
          }
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
          appendOffset(TOffsetIdPrinterParser.PATTERNS[count + (count == 1 ? 0 : 1)], "Z");
        } else if (cur == 'x') {
          if (count > 5) {
            throw new IllegalArgumentException("Too many pattern letters: " + cur);
          }
          String zero = (count == 1 ? "+00" : (count % 2 == 0 ? "+0000" : "+00:00"));
          appendOffset(TOffsetIdPrinterParser.PATTERNS[count + (count == 1 ? 0 : 1)], zero);
        } else if (cur == 'W') {
          if (count > 1) {
            throw new IllegalArgumentException("Too many pattern letters: " + cur);
          }
          appendValue(new TWeekBasedFieldPrinterParser(cur, count, count, count));
        } else if (cur == 'w') {
          if (count > 2) {
            throw new IllegalArgumentException("Too many pattern letters: " + cur);
          }
          appendValue(new TWeekBasedFieldPrinterParser(cur, count, count, 2));
        } else if (cur == 'Y') {
          if (count == 2) {
            appendValue(new TWeekBasedFieldPrinterParser(cur, count, count, 2));
          } else {
            appendValue(new TWeekBasedFieldPrinterParser(cur, count, count, 19));
          }
        } else {
          throw new IllegalArgumentException("Unknown pattern letter: " + cur);
        }
        pos--;

      } else if (cur == '\'') {
        int start = pos++;
        for (; pos < pattern.length(); pos++) {
          if (pattern.charAt(pos) == '\'') {
            if (pos + 1 < pattern.length() && pattern.charAt(pos + 1) == '\'') {
              pos++;
            } else {
              break;
            }
          }
        }
        if (pos >= pattern.length()) {
          throw new IllegalArgumentException("Pattern ends with an incomplete string literal: " + pattern);
        }
        String str = pattern.substring(start + 1, pos);
        if (str.isEmpty()) {
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

  @SuppressWarnings("fallthrough")
  private void parseField(char cur, int count, TTemporalField field) {

    boolean standalone = false;
    switch (cur) {
      case 'u':
      case 'y':
        if (count == 2) {
          appendValueReduced(field, 2, 2, TReducedPrinterParser.BASE_DATE);
        } else if (count < 4) {
          appendValue(field, count, 19, TSignStyle.NORMAL);
        } else {
          appendValue(field, count, 19, TSignStyle.EXCEEDS_PAD);
        }
        break;
      case 'c':
        if (count == 1) {
          appendValue(new TWeekBasedFieldPrinterParser(cur, count, count, count));
          break;
        } else if (count == 2) {
          throw new IllegalArgumentException("Invalid pattern \"cc\"");
        }
      case 'L':
      case 'q':
        standalone = true;
      case 'M':
      case 'Q':
      case 'E':
      case 'e':
        switch (count) {
          case 1:
          case 2:
            if (cur == 'e') {
              appendValue(new TWeekBasedFieldPrinterParser(cur, count, count, count));
            } else if (cur == 'E') {
              appendText(field, TTextStyle.SHORT);
            } else {
              if (count == 1) {
                appendValue(field);
              } else {
                appendValue(field, 2);
              }
            }
            break;
          case 3:
            appendText(field, standalone ? TTextStyle.SHORT_STANDALONE : TTextStyle.SHORT);
            break;
          case 4:
            appendText(field, standalone ? TTextStyle.FULL_STANDALONE : TTextStyle.FULL);
            break;
          case 5:
            appendText(field, standalone ? TTextStyle.NARROW_STANDALONE : TTextStyle.NARROW);
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
        } else if (count == 2 || count == 3) {
          appendValue(field, count, 3, TSignStyle.NOT_NEGATIVE);
        } else {
          throw new IllegalArgumentException("Too many pattern letters: " + cur);
        }
        break;
      case 'g':
        appendValue(field, count, 19, TSignStyle.NORMAL);
        break;
      case 'A':
      case 'n':
      case 'N':
        appendValue(field, count, 19, TSignStyle.NOT_NEGATIVE);
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

  private static final Map<Character, TTemporalField> FIELD_MAP = new HashMap<>();
  static {
    // SDF = SimpleDateFormat
    FIELD_MAP.put('G', TChronoField.ERA);
    FIELD_MAP.put('y', TChronoField.YEAR_OF_ERA);
    FIELD_MAP.put('u', TChronoField.YEAR);
    // FIELD_MAP.put('Q', IsoFields.QUARTER_OF_YEAR);
    // FIELD_MAP.put('q', IsoFields.QUARTER_OF_YEAR);
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
    // FIELD_MAP.put('g', JulianFields.MODIFIED_JULIAN_DAY);
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
      throw new IllegalStateException("Cannot call optionalEnd() as there was no previous call to optionalStart()");
    }
    if (this.active.printerParsers.size() > 0) {
      TCompositePrinterParser cpp = new TCompositePrinterParser(this.active.printerParsers, this.active.optional);
      this.active = this.active.parent;
      appendInternal(cpp);
    } else {
      this.active = this.active.parent;
    }
    return this;
  }

  private int appendInternal(TDateTimePrinterParser pp) {

    TObjects.requireNonNull(pp, "pp");
    if (this.active.padNextWidth > 0) {
      if (pp != null) {
        pp = new TPadPrinterParserDecorator(pp, this.active.padNextWidth, this.active.padNextChar);
      }
      this.active.padNextWidth = 0;
      this.active.padNextChar = 0;
    }
    this.active.printerParsers.add(pp);
    this.active.valueParserIndex = -1;
    return this.active.printerParsers.size() - 1;
  }

  public TDateTimeFormatter toFormatter() {

    return toFormatter(TLocale.getDefault());
  }

  public TDateTimeFormatter toFormatter(TLocale locale) {

    return toFormatter(locale, TResolverStyle.SMART, null);
  }

  TDateTimeFormatter toFormatter(TResolverStyle resolverStyle, TChronology chrono) {

    return toFormatter(TLocale.getDefault(), resolverStyle, chrono);
  }

  private TDateTimeFormatter toFormatter(TLocale locale, TResolverStyle resolverStyle, TChronology chrono) {

    TObjects.requireNonNull(locale, "locale");
    while (this.active.parent != null) {
      optionalEnd();
    }
    TCompositePrinterParser pp = new TCompositePrinterParser(this.printerParsers, false);
    return new TDateTimeFormatter(pp, locale, TDecimalStyle.STANDARD, resolverStyle, null, chrono, null);
  }

  interface TDateTimePrinterParser {

    boolean format(TDateTimePrintContext context, StringBuilder buf);

    int parse(TDateTimeParseContext context, CharSequence text, int position);
  }

  static final class TCompositePrinterParser implements TDateTimePrinterParser {
    private final TDateTimePrinterParser[] printerParsers;

    private final boolean optional;

    TCompositePrinterParser(List<TDateTimePrinterParser> printerParsers, boolean optional) {

      this(printerParsers.toArray(new TDateTimePrinterParser[printerParsers.size()]), optional);
    }

    TCompositePrinterParser(TDateTimePrinterParser[] printerParsers, boolean optional) {

      this.printerParsers = printerParsers;
      this.optional = optional;
    }

    public TCompositePrinterParser withOptional(boolean optional) {

      if (optional == this.optional) {
        return this;
      }
      return new TCompositePrinterParser(this.printerParsers, optional);
    }

    @Override
    public boolean format(TDateTimePrintContext context, StringBuilder buf) {

      int length = buf.length();
      if (this.optional) {
        context.startOptional();
      }
      try {
        for (TDateTimePrinterParser pp : this.printerParsers) {
          if (pp.format(context, buf) == false) {
            buf.setLength(length);
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
        for (TDateTimePrinterParser pp : this.printerParsers) {
          pos = pp.parse(context, text, pos);
          if (pos < 0) {
            context.endOptional(false);
            return position;
          }
        }
        context.endOptional(true);
        return pos;
      } else {
        for (TDateTimePrinterParser pp : this.printerParsers) {
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
        for (TDateTimePrinterParser pp : this.printerParsers) {
          buf.append(pp);
        }
        buf.append(this.optional ? "]" : ")");
      }
      return buf.toString();
    }
  }

  static final class TPadPrinterParserDecorator implements TDateTimePrinterParser {
    private final TDateTimePrinterParser printerParser;

    private final int padWidth;

    private final char padChar;

    TPadPrinterParserDecorator(TDateTimePrinterParser printerParser, int padWidth, char padChar) {

      this.printerParser = printerParser;
      this.padWidth = padWidth;
      this.padChar = padChar;
    }

    @Override
    public boolean format(TDateTimePrintContext context, StringBuilder buf) {

      int preLen = buf.length();
      if (this.printerParser.format(context, buf) == false) {
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

      final boolean strict = context.isStrict();
      if (position > text.length()) {
        throw new IndexOutOfBoundsException();
      }
      if (position == text.length()) {
        return ~position;
      }
      int endPos = position + this.padWidth;
      if (endPos > text.length()) {
        if (strict) {
          return ~position;
        }
        endPos = text.length();
      }
      int pos = position;
      while (pos < endPos && context.charEquals(text.charAt(pos), this.padChar)) {
        pos++;
      }
      text = text.subSequence(0, endPos);
      int resultPos = this.printerParser.parse(context, text, pos);
      if (resultPos != endPos && strict) {
        return ~(position + pos);
      }
      return resultPos;
    }

    @Override
    public String toString() {

      return "Pad(" + this.printerParser + "," + this.padWidth
          + (this.padChar == ' ' ? ")" : ",'" + this.padChar + "')");
    }
  }

  static enum TSettingsParser implements TDateTimePrinterParser {
    SENSITIVE, INSENSITIVE, STRICT, LENIENT;

    @Override
    public boolean format(TDateTimePrintContext context, StringBuilder buf) {

      return true;
    }

    @Override
    public int parse(TDateTimeParseContext context, CharSequence text, int position) {

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

  static class TDefaultValueParser implements TDateTimePrinterParser {
    private final TTemporalField field;

    private final long value;

    TDefaultValueParser(TTemporalField field, long value) {

      this.field = field;
      this.value = value;
    }

    @Override
    public boolean format(TDateTimePrintContext context, StringBuilder buf) {

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

  static final class TCharLiteralPrinterParser implements TDateTimePrinterParser {
    private final char literal;

    TCharLiteralPrinterParser(char literal) {

      this.literal = literal;
    }

    @Override
    public boolean format(TDateTimePrintContext context, StringBuilder buf) {

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
      if (ch != this.literal) {
        if (context.isCaseSensitive() || (Character.toUpperCase(ch) != Character.toUpperCase(this.literal)
            && Character.toLowerCase(ch) != Character.toLowerCase(this.literal))) {
          return ~position;
        }
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

  static final class TStringLiteralPrinterParser implements TDateTimePrinterParser {
    private final String literal;

    TStringLiteralPrinterParser(String literal) {

      this.literal = literal;
    }

    @Override
    public boolean format(TDateTimePrintContext context, StringBuilder buf) {

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

  static class TNumberPrinterParser implements TDateTimePrinterParser {

    static final long[] EXCEED_POINTS = new long[] { 0L, 10L, 100L, 1000L, 10000L, 100000L, 1000000L, 10000000L,
    100000000L, 1000000000L, 10000000000L, };

    final TTemporalField field;

    final int minWidth;

    final int maxWidth;

    private final TSignStyle signStyle;

    final int subsequentWidth;

    TNumberPrinterParser(TTemporalField field, int minWidth, int maxWidth, TSignStyle signStyle) {

      this.field = field;
      this.minWidth = minWidth;
      this.maxWidth = maxWidth;
      this.signStyle = signStyle;
      this.subsequentWidth = 0;
    }

    protected TNumberPrinterParser(TTemporalField field, int minWidth, int maxWidth, TSignStyle signStyle,
        int subsequentWidth) {

      this.field = field;
      this.minWidth = minWidth;
      this.maxWidth = maxWidth;
      this.signStyle = signStyle;
      this.subsequentWidth = subsequentWidth;
    }

    TNumberPrinterParser withFixedWidth() {

      if (this.subsequentWidth == -1) {
        return this;
      }
      return new TNumberPrinterParser(this.field, this.minWidth, this.maxWidth, this.signStyle, -1);
    }

    TNumberPrinterParser withSubsequentWidth(int subsequentWidth) {

      return new TNumberPrinterParser(this.field, this.minWidth, this.maxWidth, this.signStyle,
          this.subsequentWidth + subsequentWidth);
    }

    @Override
    public boolean format(TDateTimePrintContext context, StringBuilder buf) {

      Long valueLong = context.getValue(this.field);
      if (valueLong == null) {
        return false;
      }
      long value = getValue(context, valueLong);
      TDecimalStyle decimalStyle = context.getDecimalStyle();
      String str = (value == Long.MIN_VALUE ? "9223372036854775808" : Long.toString(Math.abs(value)));
      if (str.length() > this.maxWidth) {
        throw new TDateTimeException("Field " + this.field + " cannot be printed as the value " + value
            + " exceeds the maximum print width of " + this.maxWidth);
      }
      // str = decimalStyle.convertNumberToI18N(str);

      if (value >= 0) {
        switch (this.signStyle) {
          case EXCEEDS_PAD:
            if (this.minWidth < 19 && value >= EXCEED_POINTS[this.minWidth]) {
              buf.append(decimalStyle.getPositiveSign());
            }
            break;
          case ALWAYS:
            buf.append(decimalStyle.getPositiveSign());
            break;
        }
      } else {
        switch (this.signStyle) {
          case NORMAL:
          case EXCEEDS_PAD:
          case ALWAYS:
            buf.append(decimalStyle.getNegativeSign());
            break;
          case NOT_NEGATIVE:
            throw new TDateTimeException("Field " + this.field + " cannot be printed as the value " + value
                + " cannot be negative according to the SignStyle");
        }
      }
      for (int i = 0; i < this.minWidth - str.length(); i++) {
        buf.append(decimalStyle.getZeroDigit());
      }
      buf.append(str);
      return true;
    }

    long getValue(TDateTimePrintContext context, long value) {

      return value;
    }

    boolean isFixedWidth(TDateTimeParseContext context) {

      return this.subsequentWidth == -1
          || (this.subsequentWidth > 0 && this.minWidth == this.maxWidth && this.signStyle == TSignStyle.NOT_NEGATIVE);
    }

    @Override
    public int parse(TDateTimeParseContext context, CharSequence text, int position) {

      int length = text.length();
      if (position == length) {
        return ~position;
      }
      char sign = text.charAt(position);
      boolean negative = false;
      boolean positive = false;
      if (sign == context.getDecimalStyle().getPositiveSign()) {
        if (this.signStyle.parse(true, context.isStrict(), this.minWidth == this.maxWidth) == false) {
          return ~position;
        }
        positive = true;
        position++;
      } else if (sign == context.getDecimalStyle().getNegativeSign()) {
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
          int digit = ch - '0';
          if ((digit < 0) || (digit > 9)) {
            pos--;
            if (pos < minEndPos) {
              return ~position;
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
            return ~(position - 1);
          }
          totalBig = totalBig.negate();
        } else {
          if (total == 0 && context.isStrict()) {
            return ~(position - 1);
          }
          total = -total;
        }
      } else if (this.signStyle == TSignStyle.EXCEEDS_PAD && context.isStrict()) {
        int parseLen = pos - position;
        if (positive) {
          if (parseLen <= this.minWidth) {
            return ~(position - 1);
          }
        } else {
          if (parseLen > this.minWidth) {
            return ~position;
          }
        }
      }
      if (totalBig != null) {
        if (totalBig.bitLength() > 63) {
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

  static final class TReducedPrinterParser extends TNumberPrinterParser {

    static final TLocalDate BASE_DATE = TLocalDate.of(2000, 1, 1);

    private final int baseValue;

    private final TChronoLocalDate baseDate;

    TReducedPrinterParser(TTemporalField field, int minWidth, int maxWidth, int baseValue, TChronoLocalDate baseDate) {

      this(field, minWidth, maxWidth, baseValue, baseDate, 0);
      if (minWidth < 1 || minWidth > 10) {
        throw new IllegalArgumentException("The minWidth must be from 1 to 10 inclusive but was " + minWidth);
      }
      if (maxWidth < 1 || maxWidth > 10) {
        throw new IllegalArgumentException("The maxWidth must be from 1 to 10 inclusive but was " + minWidth);
      }
      if (maxWidth < minWidth) {
        throw new IllegalArgumentException(
            "Maximum width must exceed or equal the minimum width but " + maxWidth + " < " + minWidth);
      }
      if (baseDate == null) {
        if (field.range().isValidValue(baseValue) == false) {
          throw new IllegalArgumentException("The base value must be within the range of the field");
        }
        if (((baseValue) + EXCEED_POINTS[maxWidth]) > Integer.MAX_VALUE) {
          throw new DateTimeException("Unable to add printer-parser as the range exceeds the capacity of an int");
        }
      }
    }

    private TReducedPrinterParser(TTemporalField field, int minWidth, int maxWidth, int baseValue,
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

        final long initialValue = value;
        context.addChronoChangedListener((_unused) -> {
          setValue(context, initialValue, errorPos, successPos);
        });
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
    TReducedPrinterParser withFixedWidth() {

      if (this.subsequentWidth == -1) {
        return this;
      }
      return new TReducedPrinterParser(this.field, this.minWidth, this.maxWidth, this.baseValue, this.baseDate, -1);
    }

    @Override
    TReducedPrinterParser withSubsequentWidth(int subsequentWidth) {

      return new TReducedPrinterParser(this.field, this.minWidth, this.maxWidth, this.baseValue, this.baseDate,
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
          + Objects.requireNonNullElse(this.baseDate, this.baseValue) + ")";
    }
  }

  static final class TFractionPrinterParser extends TNumberPrinterParser {
    private final boolean decimalPoint;

    TFractionPrinterParser(TTemporalField field, int minWidth, int maxWidth, boolean decimalPoint) {

      this(field, minWidth, maxWidth, decimalPoint, 0);
      TObjects.requireNonNull(field, "field");
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
    }

    TFractionPrinterParser(TTemporalField field, int minWidth, int maxWidth, boolean decimalPoint, int subsequentWidth) {

      super(field, minWidth, maxWidth, TSignStyle.NOT_NEGATIVE, subsequentWidth);
      this.decimalPoint = decimalPoint;
    }

    @Override
    TFractionPrinterParser withFixedWidth() {

      if (this.subsequentWidth == -1) {
        return this;
      }
      return new TFractionPrinterParser(this.field, this.minWidth, this.maxWidth, this.decimalPoint, -1);
    }

    @Override
    TFractionPrinterParser withSubsequentWidth(int subsequentWidth) {

      return new TFractionPrinterParser(this.field, this.minWidth, this.maxWidth, this.decimalPoint,
          this.subsequentWidth + subsequentWidth);
    }

    @Override
    boolean isFixedWidth(TDateTimeParseContext context) {

      if (context.isStrict() && this.minWidth == this.maxWidth && this.decimalPoint == false) {
        return true;
      }
      return false;
    }

    @Override
    public boolean format(TDateTimePrintContext context, StringBuilder buf) {

      Long value = context.getValue(this.field);
      if (value == null) {
        return false;
      }
      TDecimalStyle decimalStyle = context.getDecimalStyle();
      BigDecimal fraction = convertToFraction(value);
      if (fraction.scale() == 0) {
        if (this.minWidth > 0) {
          if (this.decimalPoint) {
            buf.append(decimalStyle.getDecimalSeparator());
          }
          for (int i = 0; i < this.minWidth; i++) {
            buf.append(decimalStyle.getZeroDigit());
          }
        }
      } else {
        int outputScale = Math.min(Math.max(fraction.scale(), this.minWidth), this.maxWidth);
        fraction = fraction.setScale(outputScale, RoundingMode.FLOOR);
        String str = fraction.toPlainString().substring(2);
        str = decimalStyle.convertNumberToI18N(str);
        if (this.decimalPoint) {
          buf.append(decimalStyle.getDecimalSeparator());
        }
        buf.append(str);
      }
      return true;
    }

    @Override
    public int parse(TDateTimeParseContext context, CharSequence text, int position) {

      int effectiveMin = (context.isStrict() || isFixedWidth(context) ? this.minWidth : 0);
      int effectiveMax = (context.isStrict() || isFixedWidth(context) ? this.maxWidth : 9);
      int length = text.length();
      if (position == length) {
        return (effectiveMin > 0 ? ~position : position);
      }
      if (this.decimalPoint) {
        if (text.charAt(position) != context.getDecimalStyle().getDecimalSeparator()) {
          return (effectiveMin > 0 ? ~position : position);
        }
        position++;
      }
      int minEndPos = position + effectiveMin;
      if (minEndPos > length) {
        return ~position;
      }
      int maxEndPos = Math.min(position + effectiveMax, length);
      int total = 0;
      int pos = position;
      while (pos < maxEndPos) {
        char ch = text.charAt(pos++);
        int digit = ch - '0';
        if ((digit < 0) || (digit > 9)) {
          if (pos < minEndPos) {
            return ~position;
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

  static final class TTextPrinterParser implements TDateTimePrinterParser {

    private final TTemporalField field;

    private final TTextStyle textStyle;

    private volatile TNumberPrinterParser numberPrinterParser;

    TTextPrinterParser(TTemporalField field, TTextStyle textStyle) {

      this.field = field;
      this.textStyle = textStyle;
    }

    @Override
    public boolean format(TDateTimePrintContext context, StringBuilder buf) {

      Long value = context.getValue(this.field);
      if (value == null) {
        return false;
      }
      String text;
      text = this.field.getDisplayName(context.getLocale());
      if (text == null) {
        return numberPrinterParser().format(context, buf);
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
      // Iterator<Entry<String, Long>> it;
      // it = this.provider.getTextIterator(this.field, style, context.getLocale());
      // if (it != null) {
      // while (it.hasNext()) {
      // Entry<String, Long> entry = it.next();
      // String itText = entry.getKey();
      // if (context.subSequenceEquals(itText, 0, parseText, position, itText.length())) {
      // return context.setParsedField(this.field, entry.getValue(), position, position + itText.length());
      // }
      // }
      // if (this.field == ERA && !context.isStrict()) {
      // for (TEra era : TIsoEra.values()) {
      // String name = era.toString();
      // if (context.subSequenceEquals(name, 0, parseText, position, name.length())) {
      // return context.setParsedField(this.field, era.getValue(), position, position + name.length());
      // }
      // }
      // }
      // if (context.isStrict()) {
      // return ~position;
      // }
      // }
      return numberPrinterParser().parse(context, parseText, position);
    }

    private TNumberPrinterParser numberPrinterParser() {

      if (this.numberPrinterParser == null) {
        this.numberPrinterParser = new TNumberPrinterParser(this.field, 1, 19, TSignStyle.NORMAL);
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

  static final class TInstantPrinterParser implements TDateTimePrinterParser {

    private static final long SECONDS_PER_10000_YEARS = 146097L * 25L * 86400L;

    private static final long SECONDS_0000_TO_1970 = ((146097L * 5L) - (30L * 365L + 7L)) * 86400L;

    private final int fractionalDigits;

    TInstantPrinterParser(int fractionalDigits) {

      this.fractionalDigits = fractionalDigits;
    }

    @Override
    public boolean format(TDateTimePrintContext context, StringBuilder buf) {

      Long inSecs = context.getValue(INSTANT_SECONDS);
      Long inNanos = null;
      if (context.getTemporal().isSupported(NANO_OF_SECOND)) {
        inNanos = context.getTemporal().getLong(NANO_OF_SECOND);
      }
      if (inSecs == null) {
        return false;
      }
      long inSec = inSecs;
      int inNano = NANO_OF_SECOND.checkValidIntValue(inNanos != null ? inNanos : 0);
      if (inSec >= -SECONDS_0000_TO_1970) {
        long zeroSecs = inSec - SECONDS_PER_10000_YEARS + SECONDS_0000_TO_1970;
        long hi = Math.floorDiv(zeroSecs, SECONDS_PER_10000_YEARS) + 1;
        long lo = Math.floorMod(zeroSecs, SECONDS_PER_10000_YEARS);
        TLocalDateTime ldt = TLocalDateTime.ofEpochSecond(lo - SECONDS_0000_TO_1970, 0, TZoneOffset.UTC);
        if (hi > 0) {
          buf.append('+').append(hi);
        }
        buf.append(ldt);
        if (ldt.getSecond() == 0) {
          buf.append(":00");
        }
      } else {
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
          if (ldt.getYear() == -10_000) {
            buf.replace(pos, pos + 2, Long.toString(hi - 1));
          } else if (lo == 0) {
            buf.insert(pos, hi);
          } else {
            buf.insert(pos + 1, Math.abs(hi));
          }
        }
      }
      if ((this.fractionalDigits < 0 && inNano > 0) || this.fractionalDigits > 0) {
        buf.append('.');
        int div = 100_000_000;
        for (int i = 0; ((this.fractionalDigits == -1 && inNano > 0)
            || (this.fractionalDigits == -2 && (inNano > 0 || (i % 3) != 0)) || i < this.fractionalDigits); i++) {
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

      int minDigits = (this.fractionalDigits < 0 ? 0 : this.fractionalDigits);
      int maxDigits = (this.fractionalDigits < 0 ? 9 : this.fractionalDigits);
      TCompositePrinterParser parser = new TDateTimeFormatterBuilder().append(TDateTimeFormatter.ISO_LOCAL_DATE)
          .appendLiteral('T').appendValue(HOUR_OF_DAY, 2).appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2)
          .appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2)
          .appendFraction(NANO_OF_SECOND, minDigits, maxDigits, true).appendLiteral('Z').toFormatter()
          .toPrinterParser(false);
      TDateTimeParseContext newContext = context.copy();
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
      int days = 0;
      if (hour == 24 && min == 0 && sec == 0 && nano == 0) {
        hour = 0;
        days = 1;
      } else if (hour == 23 && min == 59 && sec == 60) {
        context.setParsedLeapSecond();
        sec = 59;
      }
      int year = (int) yearParsed % 10_000;
      long instantSecs;
      try {
        TLocalDateTime ldt = TLocalDateTime.of(year, month, day, hour, min, sec, 0).plusDays(days);
        instantSecs = ldt.toEpochSecond(TZoneOffset.UTC);
        instantSecs += Math.multiplyExact(yearParsed / 10_000L, SECONDS_PER_10000_YEARS);
      } catch (RuntimeException ex) {
        return ~position;
      }
      int successPos = pos;
      successPos = context.setParsedField(INSTANT_SECONDS, instantSecs, position, successPos);
      return context.setParsedField(NANO_OF_SECOND, nano, position, successPos);
    }

    @Override
    public String toString() {

      return "Instant()";
    }
  }

  static final class TOffsetIdPrinterParser implements TDateTimePrinterParser {

    static final String[] PATTERNS = new String[] { "+HH", "+HHmm", "+HH:mm", "+HHMM", "+HH:MM", "+HHMMss", "+HH:MM:ss",
    "+HHMMSS", "+HH:MM:SS", "+HHmmss", "+HH:mm:ss", "+H", "+Hmm", "+H:mm", "+HMM", "+H:MM", "+HMMss", "+H:MM:ss",
    "+HMMSS", "+H:MM:SS", "+Hmmss", "+H:mm:ss", };

    static final TOffsetIdPrinterParser INSTANCE_ID_Z = new TOffsetIdPrinterParser("+HH:MM:ss", "Z");

    static final TOffsetIdPrinterParser INSTANCE_ID_ZERO = new TOffsetIdPrinterParser("+HH:MM:ss", "0");

    private final String noOffsetText;

    private final int type;

    private final int style;

    TOffsetIdPrinterParser(String pattern, String noOffsetText) {

      Objects.requireNonNull(pattern, "pattern");
      Objects.requireNonNull(noOffsetText, "noOffsetText");
      this.type = checkPattern(pattern);
      this.style = this.type % 11;
      this.noOffsetText = noOffsetText;
    }

    private int checkPattern(String pattern) {

      for (int i = 0; i < PATTERNS.length; i++) {
        if (PATTERNS[i].equals(pattern)) {
          return i;
        }
      }
      throw new IllegalArgumentException("Invalid zone offset pattern: " + pattern);
    }

    private boolean isPaddedHour() {

      return this.type < 11;
    }

    private boolean isColon() {

      return this.style > 0 && (this.style % 2) == 0;
    }

    @Override
    public boolean format(TDateTimePrintContext context, StringBuilder buf) {

      Long offsetSecs = context.getValue(OFFSET_SECONDS);
      if (offsetSecs == null) {
        return false;
      }
      int totalSecs = Math.toIntExact(offsetSecs);
      if (totalSecs == 0) {
        buf.append(this.noOffsetText);
      } else {
        int absHours = Math.abs((totalSecs / 3600) % 100);
        int absMinutes = Math.abs((totalSecs / 60) % 60);
        int absSeconds = Math.abs(totalSecs % 60);
        int bufPos = buf.length();
        int output = absHours;
        buf.append(totalSecs < 0 ? "-" : "+");
        if (isPaddedHour() || absHours >= 10) {
          formatZeroPad(false, absHours, buf);
        } else {
          buf.append((char) (absHours + '0'));
        }
        if ((this.style >= 3 && this.style <= 8) || (this.style >= 9 && absSeconds > 0)
            || (this.style >= 1 && absMinutes > 0)) {
          formatZeroPad(isColon(), absMinutes, buf);
          output += absMinutes;
          if (this.style == 7 || this.style == 8 || (this.style >= 5 && absSeconds > 0)) {
            formatZeroPad(isColon(), absSeconds, buf);
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

    private void formatZeroPad(boolean colon, int value, StringBuilder buf) {

      buf.append(colon ? ":" : "").append((char) (value / 10 + '0')).append((char) (value % 10 + '0'));
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

      char sign = text.charAt(position);
      if (sign == '+' || sign == '-') {
        int negative = (sign == '-' ? -1 : 1);
        boolean isColon = isColon();
        boolean paddedHour = isPaddedHour();
        int[] array = new int[4];
        array[0] = position + 1;
        int parseType = this.type;
        if (!context.isStrict()) {
          if (paddedHour) {
            if (isColon || (parseType == 0 && length > position + 3 && text.charAt(position + 3) == ':')) {
              isColon = true;
              parseType = 10;
            } else {
              parseType = 9;
            }
          } else {
            if (isColon || (parseType == 11 && length > position + 3
                && (text.charAt(position + 2) == ':' || text.charAt(position + 3) == ':'))) {
              isColon = true;
              parseType = 21;
            } else {
              parseType = 20;
            }
          }
        }
        switch (parseType) {
          case 0:
          case 11:
            parseHour(text, paddedHour, array);
            break;
          case 1:
          case 2:
          case 13:
            parseHour(text, paddedHour, array);
            parseMinute(text, isColon, false, array);
            break;
          case 3:
          case 4:
          case 15:
            parseHour(text, paddedHour, array);
            parseMinute(text, isColon, true, array);
            break;
          case 5:
          case 6:
          case 17:
            parseHour(text, paddedHour, array);
            parseMinute(text, isColon, true, array);
            parseSecond(text, isColon, false, array);
            break;
          case 7:
          case 8:
          case 19:
            parseHour(text, paddedHour, array);
            parseMinute(text, isColon, true, array);
            parseSecond(text, isColon, true, array);
            break;
          case 9:
          case 10:
          case 21:
            parseHour(text, paddedHour, array);
            parseOptionalMinuteSecond(text, isColon, array);
            break;
          case 12:
            parseVariableWidthDigits(text, 1, 4, array);
            break;
          case 14:
            parseVariableWidthDigits(text, 3, 4, array);
            break;
          case 16:
            parseVariableWidthDigits(text, 3, 6, array);
            break;
          case 18:
            parseVariableWidthDigits(text, 5, 6, array);
            break;
          case 20:
            parseVariableWidthDigits(text, 1, 6, array);
            break;
        }
        if (array[0] > 0) {
          if (array[1] > 23 || array[2] > 59 || array[3] > 59) {
            throw new DateTimeException("Value out of range: Hour[0-23], Minute[0-59], Second[0-59]");
          }
          long offsetSecs = negative * (array[1] * 3600L + array[2] * 60L + array[3]);
          return context.setParsedField(OFFSET_SECONDS, offsetSecs, position, array[0]);
        }
      }
      if (noOffsetLen == 0) {
        return context.setParsedField(OFFSET_SECONDS, 0, position, position);
      }
      return ~position;
    }

    private void parseHour(CharSequence parseText, boolean paddedHour, int[] array) {

      if (paddedHour) {
        if (!parseDigits(parseText, false, 1, array)) {
          array[0] = ~array[0];
        }
      } else {
        parseVariableWidthDigits(parseText, 1, 2, array);
      }
    }

    private void parseMinute(CharSequence parseText, boolean isColon, boolean mandatory, int[] array) {

      if (!parseDigits(parseText, isColon, 2, array)) {
        if (mandatory) {
          array[0] = ~array[0];
        }
      }
    }

    private void parseSecond(CharSequence parseText, boolean isColon, boolean mandatory, int[] array) {

      if (!parseDigits(parseText, isColon, 3, array)) {
        if (mandatory) {
          array[0] = ~array[0];
        }
      }
    }

    private void parseOptionalMinuteSecond(CharSequence parseText, boolean isColon, int[] array) {

      if (parseDigits(parseText, isColon, 2, array)) {
        parseDigits(parseText, isColon, 3, array);
      }
    }

    private boolean parseDigits(CharSequence parseText, boolean isColon, int arrayIndex, int[] array) {

      int pos = array[0];
      if (pos < 0) {
        return true;
      }
      if (isColon && arrayIndex != 1) {
        if (pos + 1 > parseText.length() || parseText.charAt(pos) != ':') {
          return false;
        }
        pos++;
      }
      if (pos + 2 > parseText.length()) {
        return false;
      }
      char ch1 = parseText.charAt(pos++);
      char ch2 = parseText.charAt(pos++);
      if (ch1 < '0' || ch1 > '9' || ch2 < '0' || ch2 > '9') {
        return false;
      }
      int value = (ch1 - 48) * 10 + (ch2 - 48);
      if (value < 0 || value > 59) {
        return false;
      }
      array[arrayIndex] = value;
      array[0] = pos;
      return true;
    }

    private void parseVariableWidthDigits(CharSequence parseText, int minDigits, int maxDigits, int[] array) {

      int pos = array[0];
      int available = 0;
      char[] chars = new char[maxDigits];
      for (int i = 0; i < maxDigits; i++) {
        if (pos + 1 > parseText.length()) {
          break;
        }
        char ch = parseText.charAt(pos++);
        if (ch < '0' || ch > '9') {
          pos--;
          break;
        }
        chars[i] = ch;
        available++;
      }
      if (available < minDigits) {
        array[0] = ~array[0];
        return;
      }
      switch (available) {
        case 1:
          array[1] = (chars[0] - 48);
          break;
        case 2:
          array[1] = ((chars[0] - 48) * 10 + (chars[1] - 48));
          break;
        case 3:
          array[1] = (chars[0] - 48);
          array[2] = ((chars[1] - 48) * 10 + (chars[2] - 48));
          break;
        case 4:
          array[1] = ((chars[0] - 48) * 10 + (chars[1] - 48));
          array[2] = ((chars[2] - 48) * 10 + (chars[3] - 48));
          break;
        case 5:
          array[1] = (chars[0] - 48);
          array[2] = ((chars[1] - 48) * 10 + (chars[2] - 48));
          array[3] = ((chars[3] - 48) * 10 + (chars[4] - 48));
          break;
        case 6:
          array[1] = ((chars[0] - 48) * 10 + (chars[1] - 48));
          array[2] = ((chars[2] - 48) * 10 + (chars[3] - 48));
          array[3] = ((chars[4] - 48) * 10 + (chars[5] - 48));
          break;
      }
      array[0] = pos;
    }

    @Override
    public String toString() {

      String converted = this.noOffsetText.replace("'", "''");
      return "Offset(" + PATTERNS[this.type] + ",'" + converted + "')";
    }
  }

  static final class TLocalizedOffsetIdPrinterParser implements TDateTimePrinterParser {
    private final TTextStyle style;

    TLocalizedOffsetIdPrinterParser(TTextStyle style) {

      this.style = style;
    }

    private static StringBuilder appendHMS(StringBuilder buf, int t) {

      return buf.append((char) (t / 10 + '0')).append((char) (t % 10 + '0'));
    }

    @Override
    public boolean format(TDateTimePrintContext context, StringBuilder buf) {

      Long offsetSecs = context.getValue(OFFSET_SECONDS);
      if (offsetSecs == null) {
        return false;
      }
      String gmtText = "GMT";
      buf.append(gmtText);
      int totalSecs = Math.toIntExact(offsetSecs);
      if (totalSecs != 0) {
        int absHours = Math.abs((totalSecs / 3600) % 100);
        int absMinutes = Math.abs((totalSecs / 60) % 60);
        int absSeconds = Math.abs(totalSecs % 60);
        buf.append(totalSecs < 0 ? "-" : "+");
        if (this.style == TTextStyle.FULL) {
          appendHMS(buf, absHours);
          buf.append(':');
          appendHMS(buf, absMinutes);
          if (absSeconds != 0) {
            buf.append(':');
            appendHMS(buf, absSeconds);
          }
        } else {
          if (absHours >= 10) {
            buf.append((char) (absHours / 10 + '0'));
          }
          buf.append((char) (absHours % 10 + '0'));
          if (absMinutes != 0 || absSeconds != 0) {
            buf.append(':');
            appendHMS(buf, absMinutes);
            if (absSeconds != 0) {
              buf.append(':');
              appendHMS(buf, absSeconds);
            }
          }
        }
      }
      return true;
    }

    int getDigit(CharSequence text, int position) {

      char c = text.charAt(position);
      if (c < '0' || c > '9') {
        return -1;
      }
      return c - '0';
    }

    @Override
    public int parse(TDateTimeParseContext context, CharSequence text, int position) {

      int pos = position;
      int end = text.length();
      String gmtText = "GMT";
      if (!context.subSequenceEquals(text, pos, gmtText, 0, gmtText.length())) {
        return ~position;
      }
      pos += gmtText.length();
      int negative = 0;
      if (pos == end) {
        return context.setParsedField(OFFSET_SECONDS, 0, position, pos);
      }
      char sign = text.charAt(pos);
      if (sign == '+') {
        negative = 1;
      } else if (sign == '-') {
        negative = -1;
      } else {
        return context.setParsedField(OFFSET_SECONDS, 0, position, pos);
      }
      pos++;
      int h = 0;
      int m = 0;
      int s = 0;
      if (this.style == TTextStyle.FULL) {
        int h1 = getDigit(text, pos++);
        int h2 = getDigit(text, pos++);
        if (h1 < 0 || h2 < 0 || text.charAt(pos++) != ':') {
          return ~position;
        }
        h = h1 * 10 + h2;
        int m1 = getDigit(text, pos++);
        int m2 = getDigit(text, pos++);
        if (m1 < 0 || m2 < 0) {
          return ~position;
        }
        m = m1 * 10 + m2;
        if (pos + 2 < end && text.charAt(pos) == ':') {
          int s1 = getDigit(text, pos + 1);
          int s2 = getDigit(text, pos + 2);
          if (s1 >= 0 && s2 >= 0) {
            s = s1 * 10 + s2;
            pos += 3;
          }
        }
      } else {
        h = getDigit(text, pos++);
        if (h < 0) {
          return ~position;
        }
        if (pos < end) {
          int h2 = getDigit(text, pos);
          if (h2 >= 0) {
            h = h * 10 + h2;
            pos++;
          }
          if (pos + 2 < end && text.charAt(pos) == ':') {
            if (pos + 2 < end && text.charAt(pos) == ':') {
              int m1 = getDigit(text, pos + 1);
              int m2 = getDigit(text, pos + 2);
              if (m1 >= 0 && m2 >= 0) {
                m = m1 * 10 + m2;
                pos += 3;
                if (pos + 2 < end && text.charAt(pos) == ':') {
                  int s1 = getDigit(text, pos + 1);
                  int s2 = getDigit(text, pos + 2);
                  if (s1 >= 0 && s2 >= 0) {
                    s = s1 * 10 + s2;
                    pos += 3;
                  }
                }
              }
            }
          }
        }
      }
      long offsetSecs = negative * (h * 3600L + m * 60L + s);
      return context.setParsedField(OFFSET_SECONDS, offsetSecs, position, pos);
    }

    @Override
    public String toString() {

      return "LocalizedOffset(" + this.style + ")";
    }
  }

  static final class TZoneTextPrinterParser extends TZoneIdPrinterParser {

    private final TTextStyle textStyle;

    private final boolean isGeneric;

    TZoneTextPrinterParser(TTextStyle textStyle, boolean isGeneric) {

      super(TTemporalQueries.zone(), "ZoneText(" + textStyle + ")");
      this.textStyle = TObjects.requireNonNull(textStyle, "textStyle");
      this.isGeneric = isGeneric;
    }

    private static final int STD = 0;

    private static final int DST = 1;

    private static final int GENERIC = 2;

    private String getDisplayName(String id, int type, TLocale locale) {

      if (this.textStyle == TTextStyle.NARROW) {
        return null;
      }
      // TODO use i18n
      return id;
    }

    @Override
    public boolean format(TDateTimePrintContext context, StringBuilder buf) {

      TZoneId zone = context.getValue(TTemporalQueries.zoneId());
      if (zone == null) {
        return false;
      }
      String zname = zone.getId();
      if (!(zone instanceof TZoneOffset)) {
        TTemporalAccessor dt = context.getTemporal();
        int type = GENERIC;
        if (!this.isGeneric) {
          if (dt.isSupported(TChronoField.INSTANT_SECONDS)) {
            type = zone.getRules().isDaylightSavings(TInstant.from(dt)) ? DST : STD;
          } else if (dt.isSupported(TChronoField.EPOCH_DAY) && dt.isSupported(TChronoField.NANO_OF_DAY)) {
            TLocalDate date = TLocalDate.ofEpochDay(dt.getLong(TChronoField.EPOCH_DAY));
            TLocalTime time = TLocalTime.ofNanoOfDay(dt.getLong(TChronoField.NANO_OF_DAY));
            TLocalDateTime ldt = date.atTime(time);
            if (zone.getRules().getTransition(ldt) == null) {
              type = zone.getRules().isDaylightSavings(ldt.atZone(zone).toInstant()) ? DST : STD;
            }
          }
        }
        String name = getDisplayName(zname, type, context.getLocale());
        if (name != null) {
          zname = name;
        }
      }
      buf.append(zname);
      return true;
    }

  }

  static class TZoneIdPrinterParser implements TDateTimePrinterParser {
    private final TTemporalQuery<TZoneId> query;

    private final String description;

    TZoneIdPrinterParser(TTemporalQuery<TZoneId> query, String description) {

      this.query = query;
      this.description = description;
    }

    @Override
    public boolean format(TDateTimePrintContext context, StringBuilder buf) {

      TZoneId zone = context.getValue(this.query);
      if (zone == null) {
        return false;
      }
      buf.append(zone.getId());
      return true;
    }

    @Override
    public int parse(TDateTimeParseContext context, CharSequence text, int position) {

      int length = text.length();
      if (position > length) {
        throw new IndexOutOfBoundsException();
      }
      if (position == length) {
        return ~position;
      }

      char nextChar = text.charAt(position);
      if (nextChar == '+' || nextChar == '-') {
        return parseOffsetBased(context, text, position, position, TOffsetIdPrinterParser.INSTANCE_ID_Z);
      } else if (length >= position + 2) {
        char nextNextChar = text.charAt(position + 1);
        if (context.charEquals(nextChar, 'U') && context.charEquals(nextNextChar, 'T')) {
          if (length >= position + 3 && context.charEquals(text.charAt(position + 2), 'C')) {
            return parseOffsetBased(context, text, position, position + 3, TOffsetIdPrinterParser.INSTANCE_ID_ZERO);
          }
          return parseOffsetBased(context, text, position, position + 2, TOffsetIdPrinterParser.INSTANCE_ID_ZERO);
        } else if (context.charEquals(nextChar, 'G') && length >= position + 3 && context.charEquals(nextNextChar, 'M')
            && context.charEquals(text.charAt(position + 2), 'T')) {
          if (length >= position + 4 && context.charEquals(text.charAt(position + 3), '0')) {
            context.setParsed(TZoneId.of("GMT0"));
            return position + 4;
          }
          return parseOffsetBased(context, text, position, position + 3, TOffsetIdPrinterParser.INSTANCE_ID_ZERO);
        }
      }

      ParsePosition ppos = new ParsePosition(position);
      String parsedZoneId = null; // tree.match(text, ppos);
      if (parsedZoneId == null) {
        if (context.charEquals(nextChar, 'Z')) {
          context.setParsed(TZoneOffset.UTC);
          return position + 1;
        }
        return ~position;
      }
      context.setParsed(TZoneId.of(parsedZoneId));
      return ppos.getIndex();
    }

    private int parseOffsetBased(TDateTimeParseContext context, CharSequence text, int prefixPos, int position,
        TOffsetIdPrinterParser parser) {

      String prefix = text.subSequence(prefixPos, position).toString().toUpperCase();
      if (position >= text.length()) {
        context.setParsed(TZoneId.of(prefix));
        return position;
      }

      // '0' or 'Z' after prefix is not part of a valid ZoneId; use bare prefix
      if (text.charAt(position) == '0' || context.charEquals(text.charAt(position), 'Z')) {
        context.setParsed(TZoneId.of(prefix));
        return position;
      }

      TDateTimeParseContext newContext = context.copy();
      int endPos = parser.parse(newContext, text, position);
      try {
        if (endPos < 0) {
          if (parser == TOffsetIdPrinterParser.INSTANCE_ID_Z) {
            return ~prefixPos;
          }
          context.setParsed(TZoneId.of(prefix));
          return position;
        }
        int offset = (int) newContext.getParsed(OFFSET_SECONDS).longValue();
        TZoneOffset zoneOffset = TZoneOffset.ofTotalSeconds(offset);
        context.setParsed(TZoneId.ofOffset(prefix, zoneOffset));
        return endPos;
      } catch (TDateTimeException dte) {
        return ~prefixPos;
      }
    }

    @Override
    public String toString() {

      return this.description;
    }
  }

  static final class TChronoPrinterParser implements TDateTimePrinterParser {
    private final TTextStyle textStyle;

    TChronoPrinterParser(TTextStyle textStyle) {

      this.textStyle = textStyle;
    }

    @Override
    public boolean format(TDateTimePrintContext context, StringBuilder buf) {

      TChronology chrono = context.getValue(TTemporalQueries.chronology());
      if (chrono == null) {
        return false;
      }
      if (this.textStyle == null) {
        buf.append(chrono.getId());
      } else {
        buf.append(getChronologyName(chrono, context.getLocale()));
      }
      return true;
    }

    @Override
    public int parse(TDateTimeParseContext context, CharSequence text, int position) {

      if (position < 0 || position > text.length()) {
        throw new IndexOutOfBoundsException();
      }
      Set<TChronology> chronos = TChronology.getAvailableChronologies();
      TChronology bestMatch = null;
      int matchLen = -1;
      for (TChronology chrono : chronos) {
        String name;
        if (this.textStyle == null) {
          name = chrono.getId();
        } else {
          name = getChronologyName(chrono, context.getLocale());
        }
        int nameLen = name.length();
        if (nameLen > matchLen && context.subSequenceEquals(text, position, name, 0, nameLen)) {
          bestMatch = chrono;
          matchLen = nameLen;
        }
      }
      if (bestMatch == null) {
        return ~position;
      }
      context.setParsed(bestMatch);
      return position + matchLen;
    }

    private String getChronologyName(TChronology chrono, TLocale locale) {

      return chrono.getDisplayName(TTextStyle.FULL, locale);
    }
  }

  static final class TLocalizedPrinterParser implements TDateTimePrinterParser {
    private static final Map<String, TDateTimeFormatter> FORMATTER_CACHE = new HashMap<>(16, 0.75f);

    private final TFormatStyle dateStyle;

    private final TFormatStyle timeStyle;

    TLocalizedPrinterParser(TFormatStyle dateStyle, TFormatStyle timeStyle) {

      this.dateStyle = dateStyle;
      this.timeStyle = timeStyle;
    }

    @Override
    public boolean format(TDateTimePrintContext context, StringBuilder buf) {

      TChronology chrono = TChronology.from(context.getTemporal());
      return formatter(context.getLocale(), chrono).toPrinterParser(false).format(context, buf);
    }

    @Override
    public int parse(TDateTimeParseContext context, CharSequence text, int position) {

      TChronology chrono = context.getEffectiveChronology();
      return formatter(context.getLocale(), chrono).toPrinterParser(false).parse(context, text, position);
    }

    private TDateTimeFormatter formatter(TLocale locale, TChronology chrono) {

      TDateTimeFormatter formatter = null;
      if (formatter == null) {
        String pattern = getLocalizedDateTimePattern(this.dateStyle, this.timeStyle, chrono, locale);
        formatter = new TDateTimeFormatterBuilder().appendPattern(pattern).toFormatter(locale);
      }
      return formatter;
    }

    @Override
    public String toString() {

      return "Localized(" + (this.dateStyle != null ? this.dateStyle : "") + ","
          + (this.timeStyle != null ? this.timeStyle : "") + ")";
    }
  }

  static final class TWeekBasedFieldPrinterParser extends TNumberPrinterParser {
    private char chr;

    private int count;

    TWeekBasedFieldPrinterParser(char chr, int count, int minWidth, int maxWidth) {

      this(chr, count, minWidth, maxWidth, 0);
    }

    TWeekBasedFieldPrinterParser(char chr, int count, int minWidth, int maxWidth, int subsequentWidth) {

      super(null, minWidth, maxWidth, TSignStyle.NOT_NEGATIVE, subsequentWidth);
      this.chr = chr;
      this.count = count;
    }

    @Override
    TWeekBasedFieldPrinterParser withFixedWidth() {

      if (this.subsequentWidth == -1) {
        return this;
      }
      return new TWeekBasedFieldPrinterParser(this.chr, this.count, this.minWidth, this.maxWidth, -1);
    }

    @Override
    TWeekBasedFieldPrinterParser withSubsequentWidth(int subsequentWidth) {

      return new TWeekBasedFieldPrinterParser(this.chr, this.count, this.minWidth, this.maxWidth,
          this.subsequentWidth + subsequentWidth);
    }

    @Override
    public boolean format(TDateTimePrintContext context, StringBuilder buf) {

      return printerParser(context.getLocale()).format(context, buf);
    }

    @Override
    public int parse(TDateTimeParseContext context, CharSequence text, int position) {

      return printerParser(context.getLocale()).parse(context, text, position);
    }

    private TDateTimePrinterParser printerParser(TLocale locale) {

      // WeekFields weekDef = WeekFields.of(locale);
      // TTemporalField field = null;
      // switch (this.chr) {
      // case 'Y':
      // field = weekDef.weekBasedYear();
      // if (this.count == 2) {
      // return new ReducedPrinterParser(field, 2, 2, 0, ReducedPrinterParser.BASE_DATE, this.subsequentWidth);
      // } else {
      // return new NumberPrinterParser(field, this.count, 19,
      // (this.count < 4) ? SignStyle.NORMAL : SignStyle.EXCEEDS_PAD, this.subsequentWidth);
      // }
      // case 'e':
      // case 'c':
      // field = weekDef.dayOfWeek();
      // break;
      // case 'w':
      // field = weekDef.weekOfWeekBasedYear();
      // break;
      // case 'W':
      // field = weekDef.weekOfMonth();
      // break;
      // default:
      // throw new IllegalStateException("unreachable");
      // }
      // TODO Currently unsupported / totally broken
      return new TNumberPrinterParser(this.field, this.minWidth, this.maxWidth, TSignStyle.NOT_NEGATIVE,
          this.subsequentWidth);
    }

    @Override
    public String toString() {

      StringBuilder sb = new StringBuilder(30);
      sb.append("Localized(");
      if (this.chr == 'Y') {
        if (this.count == 1) {
          sb.append("WeekBasedYear");
        } else if (this.count == 2) {
          sb.append("ReducedValue(WeekBasedYear,2,2,2000-01-01)");
        } else {
          sb.append("WeekBasedYear,").append(this.count).append(",").append(19).append(",")
              .append((this.count < 4) ? SignStyle.NORMAL : SignStyle.EXCEEDS_PAD);
        }
      } else {
        switch (this.chr) {
          case 'c':
          case 'e':
            sb.append("DayOfWeek");
            break;
          case 'w':
            sb.append("WeekOfWeekBasedYear");
            break;
          case 'W':
            sb.append("WeekOfMonth");
            break;
          default:
            break;
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
