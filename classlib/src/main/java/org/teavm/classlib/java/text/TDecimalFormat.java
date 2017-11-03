/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.classlib.java.text;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;

import org.teavm.classlib.impl.unicode.CLDRHelper;
import org.teavm.classlib.java.lang.TArithmeticException;
import org.teavm.classlib.java.lang.TDouble;
import org.teavm.classlib.java.lang.TString;
import org.teavm.classlib.java.util.TLocale;

public class TDecimalFormat extends TNumberFormat {
    private static final long[] POW10_ARRAY = { 1, 10, 100, 1000, 1_0000, 1_0_0000, 1_00_0000,
            1_000_0000, 1_0000_0000, 1_0_0000_0000, 1_00_0000_0000L, 1_000_0000_0000L, 1_0000_0000_0000L,
            1_0_0000_0000_0000L, 1_00_0000_0000_0000L, 1_000_0000_0000_0000L, 1_0000_0000_0000_0000L,
            1_0_0000_0000_0000_0000L, 1_00_0000_0000_0000_0000L };
    private static final int[] POW10_INT_ARRAY = { 1, 10, 100, 1000, 1_0000, 1_0_0000, 1_00_0000,
        1_000_0000, 1_0000_0000, 1_0_0000_0000 };
    private static final double[] POW10_FRAC_ARRAY = { 1E1, 1E2, 1E4, 1E8, 1E16, 1E32, 1E64, 1E128, 1E256 };
    private static final double[] POWM10_FRAC_ARRAY = { 1E-1, 1E-2, 1E-4, 1E-8, 1E-16, 1E-32, 1E-64, 1E-128, 1E-256 };
    private static final int DOUBLE_MAX_EXPONENT = 308;
    private static final long MAX_LONG_DIV_10 = Long.MAX_VALUE / 10;
    TDecimalFormatSymbols symbols;
    FormatField[] positivePrefix = {};
    FormatField[] negativePrefix = { new TextField("-") };
    FormatField[] positiveSuffix = {};
    FormatField[] negativeSuffix = {};
    private int multiplier = 1;
    private int groupingSize;
    private boolean decimalSeparatorAlwaysShown;
    private boolean parseBigDecimal;
    int exponentDigits;
    String pattern;

    public TDecimalFormat() {
        this(CLDRHelper.resolveNumberFormat(TLocale.getDefault().getLanguage(), TLocale.getDefault().getCountry()));
    }

    public TDecimalFormat(String pattern) {
        this(pattern, new TDecimalFormatSymbols());
    }

    public TDecimalFormat(String pattern, TDecimalFormatSymbols value) {
        symbols = (TDecimalFormatSymbols) value.clone();
        applyPattern(pattern);
    }

    public void applyPattern(String pattern) {
        TDecimalFormatParser parser = new TDecimalFormatParser();
        parser.parse(pattern);
        parser.apply(this);
        this.pattern = pattern;
    }

    String toPattern() {
        return pattern;
    }

    public DecimalFormatSymbols getDecimalFormatSymbols() {
        return (DecimalFormatSymbols) symbols.clone();
    }

    private StringBuffer fieldsToText(FormatField[] fields, StringBuffer buffer) {
        for (FormatField field : fields) {
            field.render(this, buffer);
        }
        return buffer;
    }

    private String fieldsToText(FormatField[] fields) {
        if (fields == null) {
            return null;
        }
        return fieldsToText(fields, new StringBuffer()).toString();
    }

    private FormatField[] textToFields(String text) {
        return new FormatField[] { new TextField(text) };
    }

    public String getPositivePrefix() {
        return fieldsToText(positivePrefix);
    }

    public void setPositivePrefix(String newValue) {
        positivePrefix = textToFields(newValue);
    }

    public String getNegativePrefix() {
        return fieldsToText(negativePrefix);
    }

    public void setNegativePrefix(String newValue) {
        negativePrefix = textToFields(newValue);
    }

    public String getPositiveSuffix() {
        return fieldsToText(positiveSuffix);
    }

    public void setPositiveSuffix(String newValue) {
        positiveSuffix = textToFields(newValue);
    }

    public String getNegativeSuffix() {
        return fieldsToText(negativeSuffix);
    }

    public void setNegativeSuffix(String newValue) {
        negativeSuffix = textToFields(newValue);
    }

    public int getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(int newValue) {
        multiplier = newValue;
    }

    public int getGroupingSize() {
        return groupingSize;
    }

    public void setGroupingSize(int newValue) {
        groupingSize = newValue;
    }

    public boolean isDecimalSeparatorAlwaysShown() {
        return decimalSeparatorAlwaysShown;
    }

    public void setDecimalSeparatorAlwaysShown(boolean newValue) {
        decimalSeparatorAlwaysShown = newValue;
    }

    public boolean isParseBigDecimal() {
        return parseBigDecimal;
    }

    public void setParseBigDecimal(boolean newValue) {
        parseBigDecimal = newValue;
    }

    @Override
    public Object clone() {
        return super.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof TDecimalFormat)) {
            return false;
        }
        TDecimalFormat other = (TDecimalFormat) obj;
        if (!super.equals(obj)) {
            return false;
        }
        return Arrays.equals(positivePrefix, other.positivePrefix)
                && Arrays.equals(positiveSuffix, other.positiveSuffix)
                && Arrays.equals(negativePrefix, other.negativePrefix)
                && Arrays.equals(negativeSuffix, other.negativeSuffix)
                && multiplier == other.multiplier
                && groupingSize == other.groupingSize
                && decimalSeparatorAlwaysShown == other.decimalSeparatorAlwaysShown
                && parseBigDecimal == other.parseBigDecimal
                && exponentDigits == other.exponentDigits;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = result * 31 + Arrays.hashCode(positivePrefix);
        result = result * 31 + Arrays.hashCode(positiveSuffix);
        result = result * 31 + Arrays.hashCode(negativePrefix);
        result = result * 31 + Arrays.hashCode(negativeSuffix);
        result = result * 31 + multiplier;
        result = result * 31 + groupingSize;
        result = result * 31 + (decimalSeparatorAlwaysShown ? 1 : 0);
        result = result * 31 + (parseBigDecimal ? 1 : 0);
        result = result * 31 + exponentDigits;
        return result;
    }

    @Override
    public Number parse(String string, TParsePosition position) {
        return isParseBigDecimal() ? parseBigDecimal(string, position) : parseNumber(string, position);
    }

    private BigDecimal parseBigDecimal(String string, TParsePosition position) {
        BigInteger mantissa = BigInteger.ZERO;
        int exponent = 0;
        int index = position.getIndex();
        boolean allowGroupSeparator = false;
        String exponentSeparator = symbols.getExponentSeparator();
        int intSize = 0;
        int fracSize = 0;
        boolean fractionalPart = false;
        boolean positive = true;

        // Find prefix
        String negPrefix = getNegativePrefix();
        String posPrefix = getPositivePrefix();
        if (string.regionMatches(index, negPrefix, 0, negPrefix.length())) {
            positive = false;
            index += negPrefix.length();
        } else if (string.regionMatches(index, posPrefix, 0, posPrefix.length())) {
            index += posPrefix.length();
        } else {
            position.setErrorIndex(index);
            return null;
        }

        // Find suffix
        String suffix = positive ? getPositiveSuffix() : getNegativeSuffix();
        if (suffix == null) {
            suffix = getPositiveSuffix();
        }

        // Parse mantissa and exponent
        while (index < string.length()) {
            char c = string.charAt(index);
            int digit = c - symbols.getZeroDigit();
            if (digit >= 0 && digit <= 9) {
                if (!fractionalPart) {
                    ++intSize;
                    allowGroupSeparator = groupingSize > 1;
                } else {
                    ++fracSize;
                }
                mantissa = mantissa.multiply(BigInteger.TEN).add(BigInteger.valueOf(digit));
                ++index;
            } else if (c == symbols.getDecimalSeparator()) {
                if (fractionalPart) {
                    break;
                }
                if (intSize < 1) {
                    position.setErrorIndex(index);
                    return null;
                }
                fractionalPart = true;
                allowGroupSeparator = false;
                ++index;
            } else if (c == symbols.getGroupingSeparator()) {
                if (!allowGroupSeparator) {
                    break;
                }
                allowGroupSeparator = false;
                ++index;
            } else if (string.regionMatches(index, exponentSeparator, 0, exponentSeparator.length())) {
                if (exponentDigits == 0) {
                    break;
                }
                index += exponentSeparator.length();
                if (index == string.length()) {
                    position.setErrorIndex(index);
                    return null;
                }
                boolean positiveExponent = true;
                if (string.charAt(index) == symbols.getMinusSign()) {
                    positiveExponent = false;
                    ++index;
                }
                int exponentLength = 0;
                while (index < string.length()) {
                    digit = string.charAt(index) - symbols.getZeroDigit();
                    if (digit < 0 || digit > 9) {
                        break;
                    }
                    exponent = exponent * 10 + digit;
                    ++exponentLength;
                    ++index;
                }
                if (exponentLength == 0) {
                    position.setErrorIndex(index);
                    return null;
                }
                if (!positiveExponent) {
                    exponent = -exponent;
                }
                break;
            } else {
                break;
            }
        }

        // If decimal separator without fractional part is not allowed, report error
        if (fracSize == 0 && fractionalPart && !isDecimalSeparatorAlwaysShown()) {
            position.setErrorIndex(index);
            return null;
        }

        // Check suffix
        if (suffix != null && !string.regionMatches(index, suffix, 0, suffix.length())) {
            position.setErrorIndex(index);
            return null;
        }

        // Advance parse position
        position.setIndex(index);

        // Apply exponent
        exponent -= fracSize;

        // Expose result
        BigDecimal result = new BigDecimal(mantissa, -exponent);
        if (multiplier != 1) {
            result = result.divide(BigDecimal.valueOf(multiplier));
        }
        if (!positive) {
            result = result.negate();
        }
        return result;
    }

    private Number parseNumber(String string, TParsePosition position) {
        long mantissa = 0;
        int shift = 0;
        int exponent = 0;
        int index = position.getIndex();
        boolean allowGroupSeparator = false;
        String exponentSeparator = symbols.getExponentSeparator();
        int intSize = 0;
        int fracSize = 0;
        boolean fractionalPart = false;
        boolean positive = true;

        // Find prefix
        String negPrefix = getNegativePrefix();
        String posPrefix = getPositivePrefix();
        if (string.regionMatches(index, negPrefix, 0, negPrefix.length())) {
            positive = false;
            index += negPrefix.length();
        } else if (string.regionMatches(index, posPrefix, 0, posPrefix.length())) {
            index += posPrefix.length();
        } else {
            position.setErrorIndex(index);
            return null;
        }

        // Find suffix
        String suffix = positive ? getPositiveSuffix() : getNegativeSuffix();
        if (suffix == null) {
            suffix = getPositiveSuffix();
        }

        // Check for infinity
        if (string.regionMatches(index, symbols.getInfinity(), 0, symbols.getInfinity().length())) {
            index += symbols.getInfinity().length();
            if (suffix != null && !string.regionMatches(index, suffix, 0, suffix.length())) {
                position.setErrorIndex(index);
                return null;
            }
            position.setIndex(index);
            return positive ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }

        // Parse mantissa and exponent
        while (index < string.length()) {
            char c = string.charAt(index);
            int digit = c - symbols.getZeroDigit();
            if (digit >= 0 && digit <= 9) {
                if (!fractionalPart) {
                    ++intSize;
                    allowGroupSeparator = groupingSize > 1;
                } else {
                    ++fracSize;
                }
                if (mantissa > MAX_LONG_DIV_10) {
                    if (shift == 0 && digit > 5) {
                        ++mantissa;
                    }
                    ++shift;
                } else {
                    long next = mantissa * 10;
                    if (next > Long.MAX_VALUE - digit) {
                        ++shift;
                    } else {
                        mantissa = next + digit;
                    }
                }
                ++index;
            } else if (c == symbols.getDecimalSeparator()) {
                if (fractionalPart) {
                    break;
                }
                if (intSize < 1) {
                    position.setErrorIndex(index);
                    return null;
                }
                fractionalPart = true;
                allowGroupSeparator = false;
                ++index;
            } else if (c == symbols.getGroupingSeparator()) {
                if (!allowGroupSeparator) {
                    break;
                }
                allowGroupSeparator = false;
                ++index;
            } else if (string.regionMatches(index, exponentSeparator, 0, exponentSeparator.length())) {
                if (exponentDigits == 0) {
                    break;
                }
                index += exponentSeparator.length();
                if (index == string.length()) {
                    position.setErrorIndex(index);
                    return null;
                }
                boolean positiveExponent = true;
                if (string.charAt(index) == symbols.getMinusSign()) {
                    positiveExponent = false;
                    ++index;
                }
                int exponentLength = 0;
                while (index < string.length()) {
                    digit = string.charAt(index) - symbols.getZeroDigit();
                    if (digit < 0 || digit > 9) {
                        break;
                    }
                    exponent = exponent * 10 + digit;
                    ++exponentLength;
                    ++index;
                }
                if (exponentLength == 0) {
                    position.setErrorIndex(index);
                    return null;
                }
                if (!positiveExponent) {
                    exponent = -exponent;
                }
                break;
            } else {
                break;
            }
        }

        // If decimal separator without fractional part is not allowed, report error
        if (fracSize == 0 && fractionalPart && !isDecimalSeparatorAlwaysShown()) {
            position.setErrorIndex(index);
            return null;
        }

        // Check suffix
        if (suffix != null && !string.regionMatches(index, suffix, 0, suffix.length())) {
            position.setErrorIndex(index);
            return null;
        }

        // Advance parse position
        position.setIndex(index);

        // Apply multiplier
        if (multiplier != 1) {
            int multiplierDigits = fastLn10(multiplier);
            if (POW10_INT_ARRAY[multiplierDigits] != multiplier) {
                mantissa /= multiplier;
            } else {
                exponent -= multiplierDigits;
            }
        }

        // Apply exponent
        exponent += shift - fracSize;
        if (exponent > 0 && exponent < POW10_ARRAY.length) {
            if (mantissa < Long.MAX_VALUE / POW10_ARRAY[exponent]) {
                mantissa *= POW10_ARRAY[exponent];
                exponent = 0;
            }
        } else if (exponent < 0 && -exponent < POW10_ARRAY.length) {
            if (mantissa % POW10_ARRAY[-exponent] == 0) {
                mantissa /= POW10_ARRAY[-exponent];
                exponent = 0;
            }
        }

        // Expose result
        if (mantissa == 0 && !positive) {
            return -0.0;
        }
        if (exponent == 0) {
            return positive ? mantissa : -mantissa;
        }
        double result = TDouble.decimalExponent(exponent) * mantissa;
        return positive ? result : -result;
    }

    @Override
    public StringBuffer format(Object object, StringBuffer buffer, TFieldPosition field) {
        if (object instanceof BigDecimal) {
            return format((BigDecimal) object, buffer, field);
        } else if (object instanceof BigInteger) {
            return format((BigInteger) object, buffer, field);
        } else {
            return super.format(object, buffer, field);
        }
    }

    private StringBuffer format(BigInteger value, StringBuffer buffer, TFieldPosition field) {
        return format(new BigDecimal(value), buffer, field);
    }

    private StringBuffer format(BigDecimal value, StringBuffer buffer,
            @SuppressWarnings("unused") TFieldPosition field) {
        if (exponentDigits > 0) {
            formatExponent(value, buffer);
        } else {
            formatRegular(value, buffer);
        }
        return buffer;
    }

    @Override
    public StringBuffer format(long value, StringBuffer buffer, TFieldPosition field) {
        if (exponentDigits > 0) {
            formatExponent(value, buffer);
        } else {
            formatRegular(value, buffer);
        }
        return buffer;
    }

    @Override
    public StringBuffer format(double value, StringBuffer buffer, TFieldPosition field) {
        if (Double.isNaN(value)) {
            fieldsToText(positivePrefix, buffer).append(symbols.getNaN());
            appendSuffix(true, buffer);
        } else if (Double.isInfinite(value)) {
            fieldsToText(value > 0 ? positivePrefix : negativePrefix, buffer).append(symbols.getInfinity());
            appendSuffix(value > 0, buffer);
        } else {
            MantissaAndExponent me = getMantissaAndExponent(value);
            if (exponentDigits > 0) {
                formatExponent(me.mantissa, me.exponent, buffer);
            } else {
                formatRegular(me.mantissa, me.exponent, buffer);
            }
        }
        return buffer;
    }

    private void formatExponent(long value, StringBuffer buffer) {
        int exponent = fastLn10(Math.abs(value));
        formatExponent(value, exponent, buffer);
    }

    private void formatRegular(long value, StringBuffer buffer) {
        int exponent = fastLn10(Math.abs(value));
        formatRegular(value, exponent, buffer);
    }

    private void formatExponent(long mantissa, int exponent, StringBuffer buffer) {
        boolean positive = mantissa >= 0;
        int visibleExponent = fastLn10(mantissa);
        int mantissaLength = visibleExponent + 1;

        if (multiplier != 1) {
            int multiplierDigits = fastLn10(multiplier);
            int tenMultiplier = POW10_INT_ARRAY[multiplierDigits];
            if (tenMultiplier == multiplier) {
                exponent += multiplierDigits;
            } else if (mantissa >= Long.MAX_VALUE / multiplier || mantissa <= Long.MIN_VALUE / multiplier)  {
                formatExponent(new BigDecimal(BigInteger.valueOf(mantissa), visibleExponent - exponent), buffer);
                return;
            } else {
                mantissa *= multiplier;
                positive = mantissa >= 0;
                visibleExponent = fastLn10(mantissa);
                mantissaLength = visibleExponent + 1;
            }
        }

        int significantSize = getMinimumIntegerDigits() + getMaximumFractionDigits();
        int exponentMultiplier = getMaximumIntegerDigits() - getMinimumIntegerDigits() + 1;
        if (exponentMultiplier > 1) {
            int delta = exponent - (exponent / exponentMultiplier) * exponentMultiplier;
            exponent -= delta;
            visibleExponent -= delta;
        } else {
            exponent -= getMinimumIntegerDigits() - 1;
            visibleExponent -= getMinimumIntegerDigits() - 1;
        }

        if (significantSize < 0) {
            mantissa = 0;
        } else if (significantSize < mantissaLength) {
            mantissa = applyRounding(mantissa, mantissaLength, significantSize);
        }

        // Append pattern prefix
        fieldsToText(positive ? positivePrefix : negativePrefix, buffer);

        int exponentPos = Math.max(visibleExponent, 0);
        for (int i = mantissaLength - 1; i >= exponentPos; --i) {
            long mantissaDigitMask = POW10_ARRAY[i];
            buffer.append(forDigit(Math.abs((int) (mantissa / mantissaDigitMask))));
            mantissa %= mantissaDigitMask;
        }
        for (int i = exponentPos - 1; i >= visibleExponent; --i) {
            buffer.append('0');
        }

        significantSize -= mantissaLength - visibleExponent;
        int requiredSize = significantSize - (getMaximumFractionDigits() - getMinimumFractionDigits());
        if (requiredSize > 0 || (mantissa != 0 && significantSize > 0)) {
            buffer.append(symbols.getDecimalSeparator());

            int limit = Math.max(0, visibleExponent - significantSize);
            int count = 0;
            for (int i = visibleExponent - 1; i >= limit; --i) {
                long mantissaDigitMask = POW10_ARRAY[i];
                buffer.append(forDigit(Math.abs((int) (mantissa / mantissaDigitMask))));
                mantissa %= mantissaDigitMask;
                ++count;
                if (mantissa == 0) {
                    break;
                }
            }
            while (count++ < requiredSize) {
                buffer.append('0');
            }
        }

        buffer.append(symbols.getExponentSeparator());
        if (exponent < 0) {
            exponent = -exponent;
            buffer.append(symbols.getMinusSign());
        }
        int exponentLength = Math.max(exponentDigits, fastLn10(exponent) + 1);
        for (int i = exponentLength - 1; i >= 0; --i) {
            int exponentDigit = POW10_INT_ARRAY[i];
            buffer.append(forDigit(exponent / exponentDigit));
            exponent %= exponentDigit;
        }

        // Add suffix
        appendSuffix(positive, buffer);
    }

    private void formatRegular(long mantissa, int exponent, StringBuffer buffer) {
        boolean positive = mantissa >= 0;
        int mantissaLength = fastLn10(mantissa) + 1;
        ++exponent;

        if (multiplier != 1) {
            int multiplierDigits = fastLn10(multiplier);
            int tenMultiplier = POW10_INT_ARRAY[multiplierDigits];
            if (tenMultiplier == multiplier) {
                exponent += multiplierDigits;
            } else if (mantissa >= Long.MAX_VALUE / multiplier || mantissa <= Long.MIN_VALUE / multiplier)  {
                formatRegular(new BigDecimal(BigInteger.valueOf(mantissa), mantissaLength - exponent), buffer);
                return;
            } else {
                mantissa *= multiplier;
                mantissaLength = fastLn10(mantissa) + 1;
            }
        }

        // Apply rounding if necessary
        int roundingPos = exponent + getMaximumFractionDigits();
        if (roundingPos < 0) {
            mantissa = 0;
        } else if (roundingPos < mantissaLength) {
            mantissa = applyRounding(mantissa, mantissaLength, roundingPos);
        }

        // Append pattern prefix
        fieldsToText(positive ? positivePrefix : negativePrefix, buffer);

        // Add insignificant integer zeros
        int intLength = Math.max(0, exponent);
        int digitPos = Math.max(intLength, getMinimumIntegerDigits()) - 1;
        for (int i = getMinimumIntegerDigits() - 1; i >= intLength; --i) {
            buffer.append('0');
            if (groupingSize > 0 && digitPos % groupingSize == 0 && digitPos > 0) {
                buffer.append(symbols.getGroupingSeparator());
            }
            --digitPos;
        }

        // Add significant integer digits
        int significantIntDigits = Math.min(mantissaLength, intLength);
        int mantissaDigit = mantissaLength - 1;
        for (int i = 0; i < significantIntDigits; ++i) {
            long mantissaDigitMask = POW10_ARRAY[mantissaDigit--];
            buffer.append(forDigit(Math.abs((int) (mantissa / mantissaDigitMask))));
            mantissa %= mantissaDigitMask;
            if (groupingSize > 0 && digitPos % groupingSize == 0 && digitPos > 0) {
                buffer.append(symbols.getGroupingSeparator());
            }
            --digitPos;
        }

        // Add significant integer zeroes
        intLength -= significantIntDigits;
        for (int i = 0; i < intLength; ++i) {
            buffer.append('0');
            if (groupingSize > 0 && digitPos % groupingSize == 0 && digitPos > 0) {
                buffer.append(symbols.getGroupingSeparator());
            }
            --digitPos;
        }

        if (mantissa == 0) {
            if (getMinimumFractionDigits() == 0) {
                if (isDecimalSeparatorAlwaysShown()) {
                    buffer.append(symbols.getDecimalSeparator());
                }
            } else {
                buffer.append(symbols.getDecimalSeparator());
                for (int i = 0; i < getMinimumFractionDigits(); ++i) {
                    buffer.append('0');
                }
            }
        } else {
            buffer.append(symbols.getDecimalSeparator());

            // Add significant fractional zeros
            int fracZeros = Math.min(getMaximumFractionDigits(), Math.max(0, -exponent));
            digitPos = 0;
            for (int i = 0; i < fracZeros; ++i) {
                ++digitPos;
                buffer.append('0');
            }

            // Add significant fractional digits
            int significantFracDigits = Math.min(getMaximumFractionDigits() - digitPos, mantissaDigit);
            for (int i = 0; i < significantFracDigits; ++i) {
                if (mantissa == 0) {
                    break;
                }
                ++digitPos;
                long mantissaDigitMask = POW10_ARRAY[mantissaDigit];
                buffer.append(forDigit(Math.abs((int) (mantissa / mantissaDigitMask))));
                mantissa %= mantissaDigitMask;
                mantissaDigit--;
            }

            // Add insignificant fractional zeros
            for (int i = digitPos; i < getMinimumFractionDigits(); ++i) {
                ++digitPos;
                buffer.append('0');
            }
        }

        // Add suffix
        appendSuffix(positive, buffer);
    }

    private void formatExponent(BigDecimal value, StringBuffer buffer) {
        if (multiplier != 1) {
            value = value.multiply(BigDecimal.valueOf(multiplier));
        }
        boolean positive = value.compareTo(BigDecimal.ZERO) >= 0;
        int mantissaLength = value.precision();
        int visibleExponent = mantissaLength - 1;
        int exponent = visibleExponent - value.scale();
        BigInteger mantissa = value.unscaledValue();

        int significantSize = getMinimumIntegerDigits() + getMaximumFractionDigits();
        int exponentMultiplier = getMaximumIntegerDigits() - getMinimumIntegerDigits() + 1;
        if (exponentMultiplier > 1) {
            int delta = exponent - (exponent / exponentMultiplier) * exponentMultiplier;
            exponent -= delta;
            visibleExponent -= delta;
        } else {
            exponent -= getMinimumIntegerDigits() - 1;
            visibleExponent -= getMinimumIntegerDigits() - 1;
        }

        if (significantSize < 0) {
            mantissa = BigInteger.ZERO;
        } else if (significantSize < mantissaLength) {
            mantissa = applyRounding(mantissa, mantissaLength, significantSize);
        }

        // Append pattern prefix
        fieldsToText(positive ? positivePrefix : negativePrefix, buffer);

        int exponentPos = Math.max(visibleExponent, 0);
        BigInteger mantissaDigitMask = pow10(BigInteger.ONE, mantissaLength - 1);
        for (int i = mantissaLength - 1; i >= exponentPos; --i) {
            BigInteger[] parts = mantissa.divideAndRemainder(mantissaDigitMask);
            buffer.append(forDigit(Math.abs(parts[0].intValue())));
            mantissa = parts[1];
            mantissaDigitMask = mantissaDigitMask.divide(BigInteger.TEN);
        }
        for (int i = exponentPos - 1; i >= visibleExponent; --i) {
            buffer.append('0');
        }

        significantSize -= mantissaLength - visibleExponent;
        int requiredSize = significantSize - (getMaximumFractionDigits() - getMinimumFractionDigits());
        if (requiredSize > 0 || (!mantissa.equals(BigInteger.ZERO) && significantSize > 0)) {
            buffer.append(symbols.getDecimalSeparator());

            int limit = Math.max(0, visibleExponent - significantSize);
            int count = 0;
            for (int i = visibleExponent - 1; i >= limit; --i) {
                BigInteger[] parts = mantissa.divideAndRemainder(mantissaDigitMask);
                buffer.append(forDigit(Math.abs(parts[0].intValue())));
                mantissa = parts[1];
                ++count;
                if (mantissa.equals(BigInteger.ZERO)) {
                    break;
                }
                mantissaDigitMask = mantissaDigitMask.divide(BigInteger.TEN);
            }
            while (count++ < requiredSize) {
                buffer.append('0');
            }
        }

        buffer.append(symbols.getExponentSeparator());
        if (exponent < 0) {
            exponent = -exponent;
            buffer.append(symbols.getMinusSign());
        }
        int exponentLength = Math.max(exponentDigits, fastLn10(exponent) + 1);
        for (int i = exponentLength - 1; i >= 0; --i) {
            int exponentDigit = POW10_INT_ARRAY[i];
            buffer.append(forDigit(exponent / exponentDigit));
            exponent %= exponentDigit;
        }

        // Add suffix
        appendSuffix(positive, buffer);
    }

    private void appendSuffix(boolean positive, StringBuffer buffer) {
        if (positive) {
            if (positiveSuffix != null) {
                fieldsToText(positiveSuffix, buffer);
            }
        } else {
            fieldsToText(negativeSuffix != null ? negativeSuffix
                    : positiveSuffix != null ? positiveSuffix : new FormatField[0], buffer);
        }
    }

    private void formatRegular(BigDecimal value, StringBuffer buffer) {
        if (multiplier != 1) {
            value = value.multiply(BigDecimal.valueOf(multiplier));
        }
        BigInteger mantissa = value.unscaledValue();
        boolean positive = mantissa.compareTo(BigInteger.ZERO) >= 0;
        int mantissaLength = value.precision();
        int exponent = value.precision() - value.scale();

        // Apply rounding if necessary
        int roundingPos = exponent + getMaximumFractionDigits();
        if (roundingPos < 0) {
            mantissa = BigInteger.ZERO;
        } else if (roundingPos < mantissaLength) {
            mantissa = applyRounding(mantissa, mantissaLength, roundingPos);
        }

        // Append pattern prefix
        fieldsToText(positive ? positivePrefix : negativePrefix, buffer);

        // Add insignificant integer zeros
        int intLength = Math.max(0, exponent);
        int digitPos = Math.max(intLength, getMinimumIntegerDigits()) - 1;
        for (int i = getMinimumIntegerDigits() - 1; i >= intLength; --i) {
            buffer.append('0');
            if (groupingSize > 0 && digitPos % groupingSize == 0 && digitPos > 0) {
                buffer.append(symbols.getGroupingSeparator());
            }
            --digitPos;
        }

        // Add significant integer digits
        int significantIntDigits = Math.min(mantissaLength, intLength);
        BigInteger mantissaDigitMask = pow10(BigInteger.ONE, mantissaLength - 1);
        for (int i = 0; i < significantIntDigits; ++i) {
            BigInteger[] parts = mantissa.divideAndRemainder(mantissaDigitMask);
            buffer.append(forDigit(Math.abs(parts[0].intValue())));
            mantissa = parts[1];
            if (groupingSize > 0 && digitPos % groupingSize == 0 && digitPos > 0) {
                buffer.append(symbols.getGroupingSeparator());
            }
            --digitPos;
            --mantissaLength;
            mantissaDigitMask = mantissaDigitMask.divide(BigInteger.TEN);
        }

        // Add significant integer zeros
        intLength -= significantIntDigits;
        for (int i = 0; i < intLength; ++i) {
            buffer.append('0');
            if (groupingSize > 0 && digitPos % groupingSize == 0 && digitPos > 0) {
                buffer.append(symbols.getGroupingSeparator());
            }
            --digitPos;
        }

        if (mantissa.equals(BigInteger.ZERO)) {
            if (getMinimumFractionDigits() == 0) {
                if (isDecimalSeparatorAlwaysShown()) {
                    buffer.append(symbols.getDecimalSeparator());
                }
            } else {
                buffer.append(symbols.getDecimalSeparator());
                for (int i = 0; i < getMinimumFractionDigits(); ++i) {
                    buffer.append('0');
                }
            }
        } else {
            buffer.append(symbols.getDecimalSeparator());

            // Add significant fractional zeros
            int fracZeros = Math.min(getMaximumFractionDigits(), Math.max(0, -exponent));
            digitPos = 0;
            for (int i = 0; i < fracZeros; ++i) {
                ++digitPos;
                buffer.append('0');
            }

            // Add significant fractional digits
            int significantFracDigits = Math.min(getMaximumFractionDigits() - digitPos, mantissaLength);
            for (int i = 0; i < significantFracDigits; ++i) {
                if (mantissa.equals(BigInteger.ZERO)) {
                    break;
                }
                ++digitPos;
                BigInteger[] parts = mantissa.divideAndRemainder(mantissaDigitMask);
                buffer.append(forDigit(Math.abs(parts[0].intValue())));
                mantissa = parts[1];
                --mantissaLength;
                mantissaDigitMask = mantissaDigitMask.divide(BigInteger.TEN);
            }

            // Add insignificant fractional zeros
            for (int i = digitPos; i < getMinimumFractionDigits(); ++i) {
                ++digitPos;
                buffer.append('0');
            }
        }

        // Add suffix
        appendSuffix(positive, buffer);
    }

    private long applyRounding(long mantissa, int mantissaLength, int exponent) {
        long rounding = POW10_ARRAY[mantissaLength - exponent];
        long signedRounding = mantissa > 0 ? rounding : -rounding;
        switch (getRoundingMode()) {
            case CEILING:
                mantissa = (mantissa / rounding) * rounding;
                if (mantissa >= 0) {
                    mantissa += rounding;
                }
                break;
            case FLOOR:
                mantissa = (mantissa / rounding) * rounding;
                if (mantissa <= 0) {
                    mantissa -= rounding;
                }
                break;
            case UP:
                mantissa = (mantissa / rounding) * rounding + signedRounding;
                break;
            case DOWN:
                mantissa = (mantissa / rounding) * rounding;
                break;
            case UNNECESSARY:
                if (mantissa % rounding != 0) {
                    throw new TArithmeticException(TString.wrap("Can't avoid rounding"));
                }
                break;
            case HALF_DOWN:
                if (mantissa % rounding == signedRounding / 2) {
                    mantissa = (mantissa / rounding) * rounding;
                } else {
                    mantissa = ((mantissa + signedRounding / 2) / rounding) * rounding;
                }
                break;
            case HALF_UP:
                if (mantissa % rounding == signedRounding / 2) {
                    mantissa = (mantissa / rounding) * rounding + signedRounding;
                } else {
                    mantissa = ((mantissa + signedRounding / 2) / rounding) * rounding;
                }
                break;
            case HALF_EVEN: {
                if (mantissa % rounding == signedRounding / 2) {
                    mantissa = (mantissa / rounding) * rounding;
                    if ((mantissa / rounding) % 2 != 0) {
                        mantissa += signedRounding;
                    }
                } else {
                    mantissa = ((mantissa + signedRounding / 2) / rounding) * rounding;
                }
                break;
            }
        }
        return mantissa;
    }

    private BigInteger applyRounding(BigInteger mantissa, int mantissaLength, int exponent) {
        BigInteger rounding = pow10(BigInteger.ONE, mantissaLength - exponent);
        BigInteger signedRounding = mantissa.compareTo(BigInteger.ZERO) >= 0 ? rounding : rounding.negate();
        switch (getRoundingMode()) {
            case CEILING:
                mantissa = mantissa.divide(rounding).multiply(rounding);
                if (mantissa.compareTo(BigInteger.ZERO) >= 0) {
                    mantissa = mantissa.add(rounding);
                }
                break;
            case FLOOR:
                mantissa = mantissa.divide(rounding).multiply(rounding);
                if (mantissa.compareTo(BigInteger.ZERO) <= 0) {
                    mantissa = mantissa.subtract(rounding);
                }
                break;
            case UP:
                mantissa = mantissa.divide(rounding).multiply(rounding).add(signedRounding);
                break;
            case DOWN:
                mantissa = mantissa.divide(rounding).multiply(rounding);
                break;
            case UNNECESSARY:
                if (mantissa.remainder(rounding).equals(BigInteger.ZERO)) {
                    throw new TArithmeticException(TString.wrap("Can't avoid rounding"));
                }
                break;
            case HALF_DOWN:
                if (mantissa.remainder(rounding).equals(signedRounding.divide(BigInteger.valueOf(2)))) {
                    mantissa = mantissa.divide(rounding).multiply(rounding);
                } else {
                    mantissa = mantissa.add(signedRounding.divide(BigInteger.valueOf(2)))
                            .divide(rounding).multiply(rounding);
                }
                break;
            case HALF_UP:
                if (mantissa.remainder(rounding).equals(signedRounding.divide(BigInteger.valueOf(2)))) {
                    mantissa = mantissa.divide(rounding).multiply(rounding).add(signedRounding);
                } else {
                    mantissa = mantissa.add(signedRounding.divide(BigInteger.valueOf(2)))
                            .divide(rounding).multiply(rounding);
                }
                break;
            case HALF_EVEN: {
                if (mantissa.remainder(rounding).equals(signedRounding.divide(BigInteger.valueOf(2)))) {
                    mantissa = mantissa.divide(rounding).multiply(rounding);
                    if (!mantissa.divide(rounding).remainder(BigInteger.valueOf(2)).equals(BigInteger.ZERO)) {
                        mantissa = mantissa.add(signedRounding);
                    }
                } else {
                    mantissa = mantissa.add(signedRounding.divide(BigInteger.valueOf(2)))
                            .divide(rounding).multiply(rounding);
                }
                break;
            }
        }
        return mantissa;
    }

    private int fastLn10(long value) {
        int result = 0;
        if (value >= 0) {
            if (value >= 1_0000_0000_0000_0000L) {
                result += 16;
                value /= 1_0000_0000_0000_0000L;
            }
            if (value >= 1_0000_0000L) {
                result += 8;
                value /= 1_0000_0000L;
            }
            if (value >= 1_0000L) {
                result += 4;
                value /= 1_0000L;
            }
            if (value >= 100L) {
                result += 2;
                value /= 100L;
            }
            if (value >= 10L) {
                result += 1;
                value /= 10L;
            }
        } else {
            if (value <= -1_0000_0000_0000_0000L) {
                result += 16;
                value /= 1_0000_0000_0000_0000L;
            }
            if (value <= -1_0000_0000L) {
                result += 8;
                value /= 1_0000_0000L;
            }
            if (value <= -1_0000L) {
                result += 4;
                value /= 1_0000L;
            }
            if (value <= -100L) {
                result += 2;
                value /= 100L;
            }
            if (value <= -10L) {
                result += 1;
                value /= 10L;
            }
        }
        return result;
    }

    private int fastLn10(int value) {
        int result = 0;
        if (value >= 1_0000_0000) {
            result += 8;
            value /= 1_0000_0000;
        }
        if (value >= 1_0000) {
            result += 4;
            value /= 1_0000;
        }
        if (value >= 100) {
            result += 2;
            value /= 100;
        }
        if (value >= 10) {
            result += 1;
            value /= 10;
        }
        return result;
    }

    private BigInteger pow10(BigInteger value, int power) {
        BigInteger digit = BigInteger.TEN;
        while (power != 0) {
            if ((power & 1) != 0) {
                value = value.multiply(digit);
            }
            digit = digit.multiply(digit);
            power >>>= 1;
        }
        return value;
    }

    private MantissaAndExponent getMantissaAndExponent(double value) {
        long mantissaPattern = POW10_ARRAY[17];
        int exp = 0;
        long mantissa = 0;
        boolean positive;
        if (value >= 0) {
            positive = true;
        } else {
            positive = false;
            value = -value;
        }
        if (value >= 1) {
            int bit = 256;
            exp = 0;
            double digit = 1;
            for (int i = POW10_FRAC_ARRAY.length - 1; i >= 0; --i) {
                if ((exp | bit) <= DOUBLE_MAX_EXPONENT && POW10_FRAC_ARRAY[i] * digit <= value) {
                    digit *= POW10_FRAC_ARRAY[i];
                    exp |= bit;
                }
                bit >>= 1;
            }
            mantissa = (long) (((value / digit) * mantissaPattern) + 0.5);
        } else {
            int bit = 256;
            exp = 0;
            double digit = 1;
            for (int i = POWM10_FRAC_ARRAY.length - 1; i >= 0; --i) {
                if ((exp | bit) <= DOUBLE_MAX_EXPONENT && POWM10_FRAC_ARRAY[i] * digit * 10 > value) {
                    digit *= POWM10_FRAC_ARRAY[i];
                    exp |= bit;
                }
                bit >>= 1;
            }
            exp = -exp;
            mantissa = (long) (((value * mantissaPattern) / digit) + 0.5);
        }
        mantissa = ((mantissa + 500) / 1000) * 1000;
        return new MantissaAndExponent(positive ? mantissa : -mantissa, exp);
    }

    private char forDigit(int n) {
        return (char) (symbols.getZeroDigit() + n);
    }

    static class MantissaAndExponent {
        long mantissa;
        int exponent;

        public MantissaAndExponent(long mantissa, int exponent) {
            this.mantissa = mantissa;
            this.exponent = exponent;
        }
    }

    interface FormatField {
        void render(TDecimalFormat format, StringBuffer buffer);
    }

    static class TextField implements FormatField {
        private String text;

        public TextField(String text) {
            this.text = text;
        }

        @Override
        public void render(TDecimalFormat format, StringBuffer buffer) {
            buffer.append(text);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof TextField)) {
                return false;
            }
            TextField other = (TextField) obj;
            return text.equals(other.text);
        }

        @Override
        public int hashCode() {
            return text.hashCode();
        }
    }

    static class CurrencyField implements FormatField {
        @Override
        public void render(TDecimalFormat format, StringBuffer buffer) {
            if (format.getCurrency() == null) {
                buffer.append('¤');
            } else {
                buffer.append(format.getCurrency().getSymbol(format.symbols.getLocale()));
            }
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof CurrencyField;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    static class PercentField implements FormatField {
        @Override
        public void render(TDecimalFormat format, StringBuffer buffer) {
            buffer.append(format.symbols.getPercent());
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof PercentField;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    static class PerMillField implements FormatField {
        @Override
        public void render(TDecimalFormat format, StringBuffer buffer) {
            buffer.append(format.symbols.getPerMill());
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof PerMillField;
        }

        @Override
        public int hashCode() {
            return 2;
        }
    }

    static class MinusField implements FormatField {
        @Override
        public void render(TDecimalFormat format, StringBuffer buffer) {
            buffer.append(format.symbols.getMinusSign());
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof MinusField;
        }

        @Override
        public int hashCode() {
            return 3;
        }
    }
}
