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
import org.teavm.classlib.java.util.TLocale;

/**
 *
 * @author Alexey Andreev
 */
public class TDecimalFormat extends TNumberFormat {
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

    @Override
    public StringBuffer format(long value, StringBuffer buffer, TFieldPosition field) {
        return null;
    }

    @Override
    public Number parse(String string, TParsePosition position) {
        return null;
    }

    @Override
    public StringBuffer format(double value, StringBuffer buffer, TFieldPosition field) {
        return null;
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
}
