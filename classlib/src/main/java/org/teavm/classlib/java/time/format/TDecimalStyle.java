package org.teavm.classlib.java.time.format;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.teavm.classlib.java.text.TDecimalFormatSymbols;
import org.teavm.classlib.java.util.TLocale;
import org.teavm.classlib.java.util.TObjects;

public final class TDecimalStyle {
  public static final TDecimalStyle STANDARD = new TDecimalStyle('0', '+', '-', '.');

  private static final Map<TLocale, TDecimalStyle> CACHE = new HashMap<>(16, 0.75f);

  private final char zeroDigit;

  private final char positiveSign;

  private final char negativeSign;

  private final char decimalSeparator;

  public static Set<TLocale> getAvailableLocales() {

    TLocale[] l = TDecimalFormatSymbols.getAvailableLocales();
    Set<TLocale> locales = new HashSet<>(l.length);
    Collections.addAll(locales, l);
    return locales;
  }

  private TDecimalStyle(char zeroChar, char positiveSignChar, char negativeSignChar, char decimalPointChar) {

    this.zeroDigit = zeroChar;
    this.positiveSign = positiveSignChar;
    this.negativeSign = negativeSignChar;
    this.decimalSeparator = decimalPointChar;
  }

  public char getZeroDigit() {

    return this.zeroDigit;
  }

  public TDecimalStyle withZeroDigit(char zeroDigit) {

    if (zeroDigit == this.zeroDigit) {
      return this;
    }
    return new TDecimalStyle(zeroDigit, this.positiveSign, this.negativeSign, this.decimalSeparator);
  }

  public char getPositiveSign() {

    return this.positiveSign;
  }

  public TDecimalStyle withPositiveSign(char positiveSign) {

    if (positiveSign == this.positiveSign) {
      return this;
    }
    return new TDecimalStyle(this.zeroDigit, positiveSign, this.negativeSign, this.decimalSeparator);
  }

  public char getNegativeSign() {

    return this.negativeSign;
  }

  public TDecimalStyle withNegativeSign(char negativeSign) {

    if (negativeSign == this.negativeSign) {
      return this;
    }
    return new TDecimalStyle(this.zeroDigit, this.positiveSign, negativeSign, this.decimalSeparator);
  }

  public char getDecimalSeparator() {

    return this.decimalSeparator;
  }

  public TDecimalStyle withDecimalSeparator(char decimalSeparator) {

    if (decimalSeparator == this.decimalSeparator) {
      return this;
    }
    return new TDecimalStyle(this.zeroDigit, this.positiveSign, this.negativeSign, decimalSeparator);
  }

  int convertToDigit(char ch) {

    int val = ch - this.zeroDigit;
    return (val >= 0 && val <= 9) ? val : -1;
  }

  String convertNumberToI18N(String numericText) {

    if (this.zeroDigit == '0') {
      return numericText;
    }
    int diff = this.zeroDigit - '0';
    char[] array = numericText.toCharArray();
    for (int i = 0; i < array.length; i++) {
      array[i] = (char) (array[i] + diff);
    }
    return new String(array);
  }

  @Override
  public boolean equals(Object obj) {

    if (this == obj) {
      return true;
    }
    if (obj instanceof TDecimalStyle) {
      TDecimalStyle other = (TDecimalStyle) obj;
      return (this.zeroDigit == other.zeroDigit && this.positiveSign == other.positiveSign
          && this.negativeSign == other.negativeSign && this.decimalSeparator == other.decimalSeparator);
    }
    return false;
  }

  @Override
  public int hashCode() {

    return this.zeroDigit + this.positiveSign + this.negativeSign + this.decimalSeparator;
  }

  @Override
  public String toString() {

    return "DecimalStyle[" + this.zeroDigit + this.positiveSign + this.negativeSign + this.decimalSeparator + "]";
  }

  public static TDecimalStyle ofDefaultLocale() {

    return of(TLocale.getDefault());
  }

  public static TDecimalStyle of(TLocale locale) {

    TObjects.requireNonNull(locale, "locale");
    TDecimalStyle info = CACHE.get(locale);
    if (info == null) {
      info = create(locale);
      CACHE.putIfAbsent(locale, info);
      info = CACHE.get(locale);
    }
    return info;
  }

  private static TDecimalStyle create(TLocale locale) {

    TDecimalFormatSymbols oldSymbols = TDecimalFormatSymbols.getInstance(locale);
    char zeroDigit = oldSymbols.getZeroDigit();
    char positiveSign = '+';
    char negativeSign = oldSymbols.getMinusSign();
    char decimalSeparator = oldSymbols.getDecimalSeparator();
    if (zeroDigit == '0' && negativeSign == '-' && decimalSeparator == '.') {
      return STANDARD;
    }
    return new TDecimalStyle(zeroDigit, positiveSign, negativeSign, decimalSeparator);
  }

}
