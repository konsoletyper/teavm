/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.teavm.classlib.java.text;

import java.util.Arrays;
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TCloneable;
import org.teavm.classlib.java.util.TLocale;

public final class DecimalFormatSymbols implements TCloneable, TSerializable {

    private final int ZeroDigit = 0, Digit = 1, DecimalSeparator = 2,
            GroupingSeparator = 3, PatternSeparator = 4, Percent = 5,
            PerMill = 6, Exponent = 7, MonetaryDecimalSeparator = 8,
            MinusSign = 9;

    transient char[] patternChars;

    private transient Currency currency;

    private transient TLocale locale;

    private String infinity, NaN, currencySymbol, intlCurrencySymbol;

    /**
     * Constructs a new {@code DecimalFormatSymbols} containing the symbols for
     * the default locale. Best practice is to create a {@code DecimalFormat}
     * and then to get the {@code DecimalFormatSymbols} from that object by
     * calling {@link DecimalFormat#getDecimalFormatSymbols()}.
     */
    public DecimalFormatSymbols() {
        this(TLocale.getDefault());
    }

    /**
     * Constructs a new DecimalFormatSymbols containing the symbols for the
     * specified Locale. Best practice is to create a {@code DecimalFormat}
     * and then to get the {@code DecimalFormatSymbols} from that object by
     * calling {@link DecimalFormat#getDecimalFormatSymbols()}.
     *
     * @param locale
     *            the locale.
     */
    public DecimalFormatSymbols(TLocale locale) {
        this(locale, new com.ibm.icu.text.DecimalFormatSymbols(locale));
    }

    transient private com.ibm.icu.text.DecimalFormatSymbols icuSymbols;

    DecimalFormatSymbols(TLocale locale, com.ibm.icu.text.DecimalFormatSymbols icuSymbols) {
        this.icuSymbols = icuSymbols;
        infinity = icuSymbols.getInfinity();
        NaN = icuSymbols.getNaN();
        this.locale = locale;
        currencySymbol = icuSymbols.getCurrencySymbol();
        intlCurrencySymbol = icuSymbols.getInternationalCurrencySymbol();
        if (locale.getCountry().length() == 0) {
            currency = Currency.getInstance("XXX"); //$NON-NLS-1$
        } else {
            currency = Currency.getInstance(locale);
        }
        patternChars = new char[10];
        patternChars[ZeroDigit] = icuSymbols.getZeroDigit();
        patternChars[Digit] = icuSymbols.getDigit();
        patternChars[DecimalSeparator] = icuSymbols.getDecimalSeparator();
        patternChars[GroupingSeparator] = icuSymbols.getGroupingSeparator();
        patternChars[PatternSeparator] = icuSymbols.getPatternSeparator();
        patternChars[Percent] = icuSymbols.getPercent();
        patternChars[PerMill] = icuSymbols.getPerMill();
        patternChars[Exponent] = icuSymbols.getExponentSeparator().charAt(0);
        patternChars[MonetaryDecimalSeparator] = icuSymbols
                .getMonetaryDecimalSeparator();
        patternChars[MinusSign] = icuSymbols.getMinusSign();
    }

    @Override
    public Object clone() {
        try {
            DecimalFormatSymbols symbols = (DecimalFormatSymbols) super.clone();
            symbols.patternChars = patternChars.clone();
            return symbols;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DecimalFormatSymbols)) {
            return false;
        }
        DecimalFormatSymbols obj = (DecimalFormatSymbols) object;
        return Arrays.equals(patternChars, obj.patternChars)
                && infinity.equals(obj.infinity) && NaN.equals(obj.NaN)
                && currencySymbol.equals(obj.currencySymbol)
                && intlCurrencySymbol.equals(obj.intlCurrencySymbol);
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getInternationalCurrencySymbol() {
        return intlCurrencySymbol;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public char getDecimalSeparator() {
        return patternChars[DecimalSeparator];
    }

    public char getDigit() {
        return patternChars[Digit];
    }

    public char getGroupingSeparator() {
        return patternChars[GroupingSeparator];
    }

    public String getInfinity() {
        return infinity;
    }

    String getLocalPatternChars() {
        // Don't include the MonetaryDecimalSeparator or the MinusSign
        return new String(patternChars, 0, patternChars.length - 2);
    }

    public char getMinusSign() {
        return patternChars[MinusSign];
    }

    public char getMonetaryDecimalSeparator() {
        return patternChars[MonetaryDecimalSeparator];
    }

    public String getNaN() {
        return NaN;
    }

    public char getPatternSeparator() {
        return patternChars[PatternSeparator];
    }

    public char getPercent() {
        return patternChars[Percent];
    }

    public char getPerMill() {
        return patternChars[PerMill];
    }

    public char getZeroDigit() {
        return patternChars[ZeroDigit];
    }

    char getExponential() {
        return patternChars[Exponent];
    }

    @Override
    public int hashCode() {
        return new String(patternChars).hashCode() + infinity.hashCode()
                + NaN.hashCode() + currencySymbol.hashCode()
                + intlCurrencySymbol.hashCode();
    }

    public void setCurrency(Currency currency) {
        if (currency == null) {
            throw new NullPointerException();
        }
        if (currency == this.currency) {
            return;
        }
        this.currency = currency;
        intlCurrencySymbol = currency.getCurrencyCode();
        currencySymbol = currency.getSymbol(locale);
    }

    public void setInternationalCurrencySymbol(String value) {
        if (value == null) {
            currency = null;
            intlCurrencySymbol = null;
            return;
        }

        if (value.equals(intlCurrencySymbol)) {
            return;
        }

        try {
            currency = Currency.getInstance(value);
            currencySymbol = currency.getSymbol(locale);
        } catch (IllegalArgumentException e) {
            currency = null;
        }
        intlCurrencySymbol = value;
    }

    public void setCurrencySymbol(String value) {
        currencySymbol = value;
    }

    public void setDecimalSeparator(char value) {
        patternChars[DecimalSeparator] = value;
    }

    public void setDigit(char value) {
        patternChars[Digit] = value;
    }

    public void setGroupingSeparator(char value) {
        patternChars[GroupingSeparator] = value;
    }

    public void setInfinity(String value) {
        infinity = value;
    }

    public void setMinusSign(char value) {
        patternChars[MinusSign] = value;
    }

    public void setMonetaryDecimalSeparator(char value) {
        patternChars[MonetaryDecimalSeparator] = value;
    }

    public void setNaN(String value) {
        NaN = value;
    }

    public void setPatternSeparator(char value) {
        patternChars[PatternSeparator] = value;
    }

    public void setPercent(char value) {
        patternChars[Percent] = value;
    }

    public void setPerMill(char value) {
        patternChars[PerMill] = value;
    }

    public void setZeroDigit(char value) {
        patternChars[ZeroDigit] = value;
    }

    void setExponential(char value) {
        patternChars[Exponent] = value;
    }

    TLocale getLocale(){
        return locale;
    }

    com.ibm.icu.text.DecimalFormatSymbols getIcuSymbols() {
        return icuSymbols;
    }
}
