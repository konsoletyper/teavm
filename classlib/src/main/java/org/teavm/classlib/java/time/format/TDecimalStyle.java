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

import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class TDecimalStyle {

    public static final TDecimalStyle STANDARD = new TDecimalStyle('0', '+', '-', '.');

    private static final Map<Locale, TDecimalStyle> CACHE = new HashMap<>(16, 0.75f);

    private final char zeroDigit;

    private final char positiveSign;

    private final char negativeSign;

    private final char decimalSeparator;

    public static Set<Locale> getAvailableLocales() {

        Locale[] l = DecimalFormatSymbols.getAvailableLocales();
        return new HashSet<>(Arrays.asList(l));
    }

    public static TDecimalStyle ofDefaultLocale() {

        return of(Locale.getDefault());
    }

    public static TDecimalStyle of(Locale locale) {

        Objects.requireNonNull(locale, "locale");
        TDecimalStyle info = CACHE.get(locale);
        if (info == null) {
            info = create(locale);
            CACHE.putIfAbsent(locale, info);
            info = CACHE.get(locale);
        }
        return info;
    }

    private static TDecimalStyle create(Locale locale) {

        DecimalFormatSymbols oldSymbols = DecimalFormatSymbols.getInstance(locale);
        char zeroDigit = oldSymbols.getZeroDigit();
        char positiveSign = '+';
        char negativeSign = oldSymbols.getMinusSign();
        char decimalSeparator = oldSymbols.getDecimalSeparator();
        if (zeroDigit == '0' && negativeSign == '-' && decimalSeparator == '.') {
            return STANDARD;
        }
        return new TDecimalStyle(zeroDigit, positiveSign, negativeSign, decimalSeparator);
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

        return "TDecimalStyle[" + this.zeroDigit + this.positiveSign + this.negativeSign + this.decimalSeparator + "]";
    }

}
