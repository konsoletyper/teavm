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

import java.util.Objects;
import org.teavm.classlib.impl.unicode.CLDRHelper;
import org.teavm.classlib.java.math.TRoundingMode;
import org.teavm.classlib.java.util.TCurrency;
import org.teavm.classlib.java.util.TLocale;

public abstract class TNumberFormat extends TFormat {
    public static final int INTEGER_FIELD = 0;
    public static final int FRACTION_FIELD = 1;
    private boolean groupingUsed = true;
    private boolean parseIntegerOnly;
    private int maximumIntegerDigits = 40;
    private int minimumIntegerDigits = 1;
    private int maximumFractionDigits = 3;
    private int minimumFractionDigits;
    private TRoundingMode roundingMode = TRoundingMode.HALF_EVEN;
    TCurrency currency = TCurrency.getInstance(TLocale.getDefault());

    public TNumberFormat() {
    }

    @Override
    public Object clone() {
        return super.clone();
    }

    public TCurrency getCurrency() {
        return currency;
    }

    public void setCurrency(TCurrency currency) {
        this.currency = currency;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof TNumberFormat)) {
            return false;
        }
        TNumberFormat obj = (TNumberFormat) object;
        return groupingUsed == obj.groupingUsed
                && parseIntegerOnly == obj.parseIntegerOnly
                && maximumFractionDigits == obj.maximumFractionDigits
                && maximumIntegerDigits == obj.maximumIntegerDigits
                && minimumFractionDigits == obj.minimumFractionDigits
                && minimumIntegerDigits == obj.minimumIntegerDigits
                && roundingMode == obj.roundingMode
                && currency == obj.currency;
    }

    public final String format(double value) {
        return format(value, new StringBuffer(), new TFieldPosition(0)).toString();
    }

    public abstract StringBuffer format(double value, StringBuffer buffer, TFieldPosition field);

    public final String format(long value) {
        return format(value, new StringBuffer(), new TFieldPosition(0)).toString();
    }

    public abstract StringBuffer format(long value, StringBuffer buffer, TFieldPosition field);

    @Override
    public StringBuffer format(Object object, StringBuffer buffer, TFieldPosition field) {
        if (object instanceof Number) {
            double dv = ((Number) object).doubleValue();
            long lv = ((Number) object).longValue();
            if (dv == lv) {
                return format(lv, buffer, field);
            }
            return format(dv, buffer, field);
        }
        throw new IllegalArgumentException();
    }

    public static TLocale[] getAvailableLocales() {
        return TLocale.getAvailableLocales();
    }

    public static TNumberFormat getIntegerInstance() {
        return getIntegerInstance(TLocale.getDefault());
    }

    public static TNumberFormat getIntegerInstance(TLocale locale) {
        String pattern = CLDRHelper.resolveNumberFormat(locale.getLanguage(), locale.getCountry());
        TDecimalFormat format = new TDecimalFormat(pattern, new TDecimalFormatSymbols(locale));
        format.setParseIntegerOnly(true);
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(0);
        format.setDecimalSeparatorAlwaysShown(false);
        return format;
    }

    public static TNumberFormat getInstance() {
        return getNumberInstance();
    }

    public static TNumberFormat getInstance(TLocale locale) {
        return getNumberInstance(locale);
    }

    public int getMaximumFractionDigits() {
        return maximumFractionDigits;
    }

    public int getMaximumIntegerDigits() {
        return maximumIntegerDigits;
    }

    public int getMinimumFractionDigits() {
        return minimumFractionDigits;
    }

    public int getMinimumIntegerDigits() {
        return minimumIntegerDigits;
    }

    public static TNumberFormat getNumberInstance() {
        return getNumberInstance(TLocale.getDefault());
    }

    public static TNumberFormat getNumberInstance(TLocale locale) {
        String pattern = CLDRHelper.resolveNumberFormat(locale.getLanguage(), locale.getCountry());
        return new TDecimalFormat(pattern, new TDecimalFormatSymbols(locale));
    }

    public static TNumberFormat getPercentInstance() {
        return getPercentInstance(TLocale.getDefault());
    }

    public static TNumberFormat getPercentInstance(TLocale locale) {
        String pattern = CLDRHelper.resolvePercentFormat(locale.getLanguage(), locale.getCountry());
        return new TDecimalFormat(pattern, new TDecimalFormatSymbols(locale));
    }

    public static TNumberFormat getCurrencyInstance() {
        return getCurrencyInstance(TLocale.getDefault());
    }

    public static TNumberFormat getCurrencyInstance(TLocale locale) {
        String pattern = CLDRHelper.resolveCurrencyFormat(locale.getLanguage(), locale.getCountry());
        return new TDecimalFormat(pattern, new TDecimalFormatSymbols(locale));
    }

    @Override
    public int hashCode() {
        return (groupingUsed ? 1231 : 1237) + (parseIntegerOnly ? 1231 : 1237)
                + maximumFractionDigits + maximumIntegerDigits
                + minimumFractionDigits + minimumIntegerDigits
                + roundingMode.hashCode() + Objects.hashCode(currency);
    }

    public boolean isGroupingUsed() {
        return groupingUsed;
    }

    public boolean isParseIntegerOnly() {
        return parseIntegerOnly;
    }

    public Number parse(String string) throws TParseException {
        TParsePosition pos = new TParsePosition(0);
        Number number = parse(string, pos);
        if (pos.getIndex() == 0) {
            throw new TParseException("Unparseable number: " + string, pos.getErrorIndex());
        }
        return number;
    }

    public abstract Number parse(String string, TParsePosition position);

    @Override
    public final Object parseObject(String string, TParsePosition position) {
        if (position == null) {
            throw new NullPointerException("position is null");
        }

        try {
            return parse(string, position);
        } catch (Exception e) {
            return null;
        }
    }

    public void setGroupingUsed(boolean value) {
        groupingUsed = value;
    }

    public void setMaximumFractionDigits(int value) {
        maximumFractionDigits = value < 0 ? 0 : value;
        if (maximumFractionDigits < minimumFractionDigits) {
            minimumFractionDigits = maximumFractionDigits;
        }
    }

    public void setMaximumIntegerDigits(int value) {
        maximumIntegerDigits = value < 0 ? 0 : value;
        if (maximumIntegerDigits < minimumIntegerDigits) {
            minimumIntegerDigits = maximumIntegerDigits;
        }
    }

    public void setMinimumFractionDigits(int value) {
        minimumFractionDigits = value < 0 ? 0 : value;
        if (maximumFractionDigits < minimumFractionDigits) {
            maximumFractionDigits = minimumFractionDigits;
        }
    }

    public void setMinimumIntegerDigits(int value) {
        minimumIntegerDigits = value < 0 ? 0 : value;
        if (maximumIntegerDigits < minimumIntegerDigits) {
            maximumIntegerDigits = minimumIntegerDigits;
        }
    }

    public void setParseIntegerOnly(boolean value) {
        parseIntegerOnly = value;
    }

    public TRoundingMode getRoundingMode() {
        return roundingMode;
    }

    public void setRoundingMode(TRoundingMode roundingMode) {
        this.roundingMode = roundingMode;
    }

    public static class Field extends TFormat.Field {
        public static final Field SIGN = new Field("sign");
        public static final Field INTEGER = new Field("integer");
        public static final Field FRACTION = new Field("fraction");
        public static final Field EXPONENT = new Field("exponent");
        public static final Field EXPONENT_SIGN = new Field("exponent sign");
        public static final Field EXPONENT_SYMBOL = new Field("exponent symbol");
        public static final Field DECIMAL_SEPARATOR = new Field("decimal separator");
        public static final Field GROUPING_SEPARATOR = new Field("grouping separator");
        public static final Field PERCENT = new Field("percent");
        public static final Field PERMILLE = new Field("per mille");
        public static final Field CURRENCY = new Field("currency");

        protected Field(String fieldName) {
            super(fieldName);
        }
    }
}
