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

import org.teavm.classlib.java.util.TLocale;

public abstract class NumberFormat extends Format {

    private static final long serialVersionUID = -2308460125733713944L;
    public static final int INTEGER_FIELD = 0;
    public static final int FRACTION_FIELD = 1;

    private boolean groupingUsed = true, parseIntegerOnly = false;

    private int maximumIntegerDigits = 40, minimumIntegerDigits = 1,
            maximumFractionDigits = 3, minimumFractionDigits = 0;

    public NumberFormat() {
    }

    @Override
    public Object clone() {
        return super.clone();
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof NumberFormat)) {
            return false;
        }
        NumberFormat obj = (NumberFormat) object;
        return groupingUsed == obj.groupingUsed
                && parseIntegerOnly == obj.parseIntegerOnly
                && maximumFractionDigits == obj.maximumFractionDigits
                && maximumIntegerDigits == obj.maximumIntegerDigits
                && minimumFractionDigits == obj.minimumFractionDigits
                && minimumIntegerDigits == obj.minimumIntegerDigits;
    }

    public final String format(double value) {
        return format(value, new StringBuffer(), new FieldPosition(0))
                .toString();
    }

    public abstract StringBuffer format(double value, StringBuffer buffer,
            FieldPosition field);

    public final String format(long value) {
        return format(value, new StringBuffer(), new FieldPosition(0))
                .toString();
    }
    public abstract StringBuffer format(long value, StringBuffer buffer,
            FieldPosition field);

    @Override
    public StringBuffer format(Object object, StringBuffer buffer,
            FieldPosition field) {
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

    public Currency getCurrency() {
        throw new UnsupportedOperationException();
    }

    public final static NumberFormat getCurrencyInstance() {
        return getCurrencyInstance(TLocale.getDefault());
    }

    public static NumberFormat getCurrencyInstance(TLocale locale) {
        com.ibm.icu.text.DecimalFormat icuFormat = (com.ibm.icu.text.DecimalFormat) com.ibm.icu.text.NumberFormat
                .getCurrencyInstance(locale);
        String pattern = icuFormat.toPattern();
        return new DecimalFormat(pattern, new DecimalFormatSymbols(locale));
    }

    public final static NumberFormat getIntegerInstance() {
        return getIntegerInstance(TLocale.getDefault());
    }

    public static NumberFormat getIntegerInstance(TLocale locale) {
        com.ibm.icu.text.DecimalFormat icuFormat = (com.ibm.icu.text.DecimalFormat) com.ibm.icu.text.NumberFormat
                .getIntegerInstance(locale);
        String pattern = icuFormat.toPattern();
        DecimalFormat format = new DecimalFormat(pattern, new DecimalFormatSymbols(locale));
        format.setParseIntegerOnly(true);
        return format;

    }

    public final static NumberFormat getInstance() {
        return getNumberInstance();
    }

    public static NumberFormat getInstance(TLocale locale) {
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

    public final static NumberFormat getNumberInstance() {
        return getNumberInstance(TLocale.getDefault());
    }

    public static NumberFormat getNumberInstance(TLocale locale) {
        com.ibm.icu.text.DecimalFormat icuFormat = (com.ibm.icu.text.DecimalFormat) com.ibm.icu.text.NumberFormat
                .getNumberInstance(locale);
        String pattern = icuFormat.toPattern();
        return new DecimalFormat(pattern, new DecimalFormatSymbols(locale, icuFormat.getDecimalFormatSymbols()), icuFormat);
    }

    public final static NumberFormat getPercentInstance() {
        return getPercentInstance(TLocale.getDefault());
    }

    public static NumberFormat getPercentInstance(TLocale locale) {
        com.ibm.icu.text.DecimalFormat icuFormat = (com.ibm.icu.text.DecimalFormat) com.ibm.icu.text.NumberFormat
                .getPercentInstance(locale);
        String pattern = icuFormat.toPattern();
        return new DecimalFormat(pattern, new DecimalFormatSymbols(locale));
    }

    @Override
    public int hashCode() {
        return (groupingUsed ? 1231 : 1237) + (parseIntegerOnly ? 1231 : 1237)
                + maximumFractionDigits + maximumIntegerDigits
                + minimumFractionDigits + minimumIntegerDigits;
    }

    public boolean isGroupingUsed() {
        return groupingUsed;
    }

    public boolean isParseIntegerOnly() {
        return parseIntegerOnly;
    }

    public Number parse(String string) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Number number = parse(string, pos);
        if (pos.getIndex() == 0) {
            throw new ParseException("Unparseable number: " + string, pos.getErrorIndex());
        }
        return number;
    }

    public abstract Number parse(String string, ParsePosition position);

    @Override
    public final Object parseObject(String string, ParsePosition position) {
        if (position == null) {
            throw new NullPointerException("position is null");
        }

        try {
            return parse(string, position);
        } catch (Exception e) {
            return null;
        }
    }

    public void setCurrency(Currency currency) {
        throw new UnsupportedOperationException();
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


    public static class Field extends Format.Field {
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
