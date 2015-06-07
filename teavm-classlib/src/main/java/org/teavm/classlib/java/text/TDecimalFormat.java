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

import java.text.DecimalFormatSymbols;
import org.teavm.classlib.impl.unicode.CLDRHelper;
import org.teavm.classlib.java.lang.TArithmeticException;
import org.teavm.classlib.java.lang.TString;
import org.teavm.classlib.java.util.TLocale;

/**
 *
 * @author Alexey Andreev
 */
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
    TDecimalFormatSymbols symbols;
    private String positivePrefix;
    private String negativePrefix;
    private String positiveSuffix;
    private String negativeSuffix;
    private int multiplier;
    private int groupingSize;
    private boolean decimalSeparatorAlwaysShown;
    private boolean parseBigDecimal;
    int exponentDigits;

    public TDecimalFormat() {
        this(CLDRHelper.resolveDecimalFormat(TLocale.getDefault().getLanguage(), TLocale.getDefault().getCountry()));
    }

    public TDecimalFormat(String pattern) {
        this(pattern, new TDecimalFormatSymbols());
    }

    public TDecimalFormat(String pattern, TDecimalFormatSymbols value) {
        symbols = (TDecimalFormatSymbols)value.clone();
        applyPattern(pattern);
    }

    public void applyPattern(String pattern) {
        TDecimalFormatParser parser = new TDecimalFormatParser();
        parser.parse(pattern);
        parser.apply(this);
    }

    public DecimalFormatSymbols getDecimalFormatSymbols() {
        return (DecimalFormatSymbols)symbols.clone();
    }

    public String getPositivePrefix() {
        return positivePrefix;
    }

    public void setPositivePrefix(String newValue) {
        positivePrefix = newValue;
    }

    public String getNegativePrefix() {
        return negativePrefix;
    }

    public void setNegativePrefix(String newValue) {
        negativePrefix = newValue;
    }

    public String getPositiveSuffix() {
        return positiveSuffix;
    }

    public void setPositiveSuffix(String newValue) {
        positiveSuffix = newValue;
    }

    public String getNegativeSuffix() {
        return negativeSuffix;
    }

    public void setNegativeSuffix(String newValue) {
        negativeSuffix = newValue;
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
        TDecimalFormat other = (TDecimalFormat)obj;
        if (!super.equals(obj)) {
            return false;
        }
        return positivePrefix.equals(other.positivePrefix) &&
                positiveSuffix.equals(other.positiveSuffix) &&
                negativePrefix.equals(other.negativePrefix) &&
                negativeSuffix.equals(other.negativeSuffix) &&
                multiplier == other.multiplier &&
                groupingSize == other.groupingSize &&
                decimalSeparatorAlwaysShown == other.decimalSeparatorAlwaysShown &&
                parseBigDecimal == other.parseBigDecimal &&
                exponentDigits == other.exponentDigits;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = result * 31 + positivePrefix.hashCode();
        result = result * 31 + positiveSuffix.hashCode();
        result = result * 31 + negativePrefix.hashCode();
        result = result * 31 + negativeSuffix.hashCode();
        result = result * 31 + multiplier;
        result = result * 31 + groupingSize;
        result = result * 31 + (decimalSeparatorAlwaysShown ? 1 : 0);
        result = result * 31 + (parseBigDecimal ? 1 : 0);
        result = result * 31 + exponentDigits;
        return result;
    }

    @Override
    public Number parse(String string, TParsePosition position) {
        return null;
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
        MantissaAndExponent me = getMantissaAndExponent(value);
        if (exponentDigits > 0) {
            formatExponent(me.mantissa, me.exponent, buffer);
        } else {
            formatRegular(me.mantissa, me.exponent, buffer);
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
        buffer.append(positive ? positivePrefix : negativePrefix);

        int exponentPos = Math.max(visibleExponent, 0);
        for (int i = mantissaLength - 1; i >= exponentPos; --i) {
            long mantissaDigitMask = POW10_ARRAY[i];
            buffer.append(Character.forDigit(Math.abs((int)(mantissa / mantissaDigitMask)), 10));
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
                buffer.append(Character.forDigit(Math.abs((int)(mantissa / mantissaDigitMask)), 10));
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
            buffer.append(Character.forDigit(exponent / exponentDigit, 10));
            exponent %= exponentDigit;
        }

        // Add suffix
        if (positive) {
            buffer.append(positiveSuffix != null ? positiveSuffix : "");
        } else {
            buffer.append(negativeSuffix != null ? negativeSuffix : positiveSuffix != null ? positiveSuffix : "");
        }
    }

    private void formatRegular(long mantissa, int exponent, StringBuffer buffer) {
        boolean positive = mantissa >= 0;
        int mantissaLength = fastLn10(mantissa) + 1;
        ++exponent;

        // Apply rounding if necessary
        int roundingPos = exponent + getMaximumFractionDigits();
        if (roundingPos < 0) {
            mantissa = 0;
        } else if (roundingPos < mantissaLength) {
            mantissa = applyRounding(mantissa, mantissaLength, roundingPos);
        }

        // Append pattern prefix
        buffer.append(positive ? positivePrefix : negativePrefix);

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
            buffer.append(Character.forDigit(Math.abs((int)(mantissa / mantissaDigitMask)), 10));
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
                buffer.append(Character.forDigit(Math.abs((int)(mantissa / mantissaDigitMask)), 10));
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
        if (positive) {
            buffer.append(positiveSuffix != null ? positiveSuffix : "");
        } else {
            buffer.append(negativeSuffix != null ? negativeSuffix : positiveSuffix != null ? positiveSuffix : "");
        }
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
            mantissa = (long)(((value / digit) * mantissaPattern) + 0.5);
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
            mantissa = (long)(((value * mantissaPattern) / digit) + 0.5);
        }
        mantissa = ((mantissa + 500) / 1000) * 1000;
        return new MantissaAndExponent(positive ? mantissa : -mantissa, exp);
    }

    static class MantissaAndExponent {
        long mantissa;
        int exponent;

        public MantissaAndExponent(long mantissa, int exponent) {
            this.mantissa = mantissa;
            this.exponent = exponent;
        }
    }
}
