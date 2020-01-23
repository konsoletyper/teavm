package org.teavm.classlib.java.time.format;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Consumer;

import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.util.TLocale;
import org.teavm.classlib.java.util.TObjects;

public class TDateTimeParseContext {

  private TDateTimeFormatter formatter;

  private boolean caseSensitive = true;

  private boolean strict = true;

  private final ArrayList<TParsed> parsed = new ArrayList<>();

  private ArrayList<Consumer<TChronology>> chronoListeners = null;

  TDateTimeParseContext(TDateTimeFormatter formatter) {

    super();
    this.formatter = formatter;
    this.parsed.add(new TParsed());
  }

  TDateTimeParseContext copy() {

    TDateTimeParseContext newContext = new TDateTimeParseContext(this.formatter);
    newContext.caseSensitive = this.caseSensitive;
    newContext.strict = this.strict;
    return newContext;
  }

  TLocale getLocale() {

    return this.formatter.getLocale();
  }

  TDecimalStyle getDecimalStyle() {

    return this.formatter.getDecimalStyle();
  }

  TChronology getEffectiveChronology() {

    TChronology chrono = currentParsed().chrono;
    if (chrono == null) {
      chrono = this.formatter.getChronology();
      if (chrono == null) {
        chrono = TIsoChronology.INSTANCE;
      }
    }
    return chrono;
  }

  boolean isCaseSensitive() {

    return this.caseSensitive;
  }

  void setCaseSensitive(boolean caseSensitive) {

    this.caseSensitive = caseSensitive;
  }

  boolean subSequenceEquals(CharSequence cs1, int offset1, CharSequence cs2, int offset2, int length) {

    if (offset1 + length > cs1.length() || offset2 + length > cs2.length()) {
      return false;
    }
    if (isCaseSensitive()) {
      for (int i = 0; i < length; i++) {
        char ch1 = cs1.charAt(offset1 + i);
        char ch2 = cs2.charAt(offset2 + i);
        if (ch1 != ch2) {
          return false;
        }
      }
    } else {
      for (int i = 0; i < length; i++) {
        char ch1 = cs1.charAt(offset1 + i);
        char ch2 = cs2.charAt(offset2 + i);
        if (ch1 != ch2 && Character.toUpperCase(ch1) != Character.toUpperCase(ch2)
            && Character.toLowerCase(ch1) != Character.toLowerCase(ch2)) {
          return false;
        }
      }
    }
    return true;
  }

  boolean charEquals(char ch1, char ch2) {

    if (isCaseSensitive()) {
      return ch1 == ch2;
    }
    return charEqualsIgnoreCase(ch1, ch2);
  }

  static boolean charEqualsIgnoreCase(char c1, char c2) {

    return c1 == c2 || Character.toUpperCase(c1) == Character.toUpperCase(c2)
        || Character.toLowerCase(c1) == Character.toLowerCase(c2);
  }

  boolean isStrict() {

    return this.strict;
  }

  void setStrict(boolean strict) {

    this.strict = strict;
  }

  void startOptional() {

    this.parsed.add(currentParsed().copy());
  }

  void endOptional(boolean successful) {

    if (successful) {
      this.parsed.remove(this.parsed.size() - 2);
    } else {
      this.parsed.remove(this.parsed.size() - 1);
    }
  }

  private TParsed currentParsed() {

    return this.parsed.get(this.parsed.size() - 1);
  }

  TParsed toUnresolved() {

    return currentParsed();
  }

  TTemporalAccessor toResolved(TResolverStyle resolverStyle, Set<TTemporalField> resolverFields) {

    TParsed parsed = currentParsed();
    parsed.chrono = getEffectiveChronology();
    parsed.zone = (parsed.zone != null ? parsed.zone : this.formatter.getZone());
    return parsed.resolve(resolverStyle, resolverFields);
  }

  Long getParsed(TTemporalField field) {

    return currentParsed().fieldValues.get(field);
  }

  int setParsedField(TTemporalField field, long value, int errorPos, int successPos) {

    TObjects.requireNonNull(field, "field");
    Long old = currentParsed().fieldValues.put(field, value);
    return (old != null && old.longValue() != value) ? ~errorPos : successPos;
  }

  void setParsed(TChronology chrono) {

    TObjects.requireNonNull(chrono, "chrono");
    currentParsed().chrono = chrono;
    if (this.chronoListeners != null && !this.chronoListeners.isEmpty()) {
      @SuppressWarnings({ "rawtypes", "unchecked" })
      Consumer<TChronology>[] tmp = new Consumer[1];
      Consumer<TChronology>[] listeners = this.chronoListeners.toArray(tmp);
      this.chronoListeners.clear();
      for (Consumer<TChronology> l : listeners) {
        l.accept(chrono);
      }
    }
  }

  void addChronoChangedListener(Consumer<TChronology> listener) {

    if (this.chronoListeners == null) {
      this.chronoListeners = new ArrayList<>();
    }
    this.chronoListeners.add(listener);
  }

  void setParsed(TZoneId zone) {

    TObjects.requireNonNull(zone, "zone");
    currentParsed().zone = zone;
  }

  void setParsedLeapSecond() {

    currentParsed().leapSecond = true;
  }

  @Override
  public String toString() {

    return currentParsed().toString();
  }

}
