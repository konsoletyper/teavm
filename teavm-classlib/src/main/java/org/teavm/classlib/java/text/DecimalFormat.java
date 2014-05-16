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


public class DecimalFormat extends NumberFormat {

    private static final long serialVersionUID = 864413376551465018L;

    private transient boolean parseBigDecimal = false;

    private transient DecimalFormatSymbols symbols;

    private transient com.ibm.icu.text.DecimalFormat dform;

    private transient com.ibm.icu.text.DecimalFormatSymbols icuSymbols;

    private static final int CURRENT_SERIAL_VERTION = 3;

    private transient int serialVersionOnStream = 3;

    /**
     * Constructs a new {@code DecimalFormat} for formatting and parsing numbers
     * for the default locale.
     */
    public DecimalFormat() {
        TLocale locale = TLocale.getDefault();
        icuSymbols = new com.ibm.icu.text.DecimalFormatSymbols(locale);
        symbols = new DecimalFormatSymbols(locale);
        dform = new com.ibm.icu.text.DecimalFormat();

        super.setMaximumFractionDigits(dform.getMaximumFractionDigits());
        super.setMaximumIntegerDigits(dform.getMaximumIntegerDigits());
        super.setMinimumFractionDigits(dform.getMinimumFractionDigits());
        super.setMinimumIntegerDigits(dform.getMinimumIntegerDigits());
    }

    /**
     * Constructs a new {@code DecimalFormat} using the specified non-localized
     * pattern and the {@code DecimalFormatSymbols} for the default Locale.
     *
     * @param pattern
     *            the non-localized pattern.
     * @throws IllegalArgumentException
     *            if the pattern cannot be parsed.
     */
    public DecimalFormat(String pattern) {
        TLocale locale = TLocale.getDefault();
        icuSymbols = new com.ibm.icu.text.DecimalFormatSymbols(locale);
        symbols = new DecimalFormatSymbols(locale);
        dform = new com.ibm.icu.text.DecimalFormat(pattern, icuSymbols);

        super.setMaximumFractionDigits(dform.getMaximumFractionDigits());
        super.setMaximumIntegerDigits(dform.getMaximumIntegerDigits());
        super.setMinimumFractionDigits(dform.getMinimumFractionDigits());
        super.setMinimumIntegerDigits(dform.getMinimumIntegerDigits());
    }

    /**
     * Constructs a new {@code DecimalFormat} using the specified non-localized
     * pattern and {@code DecimalFormatSymbols}.
     *
     * @param pattern
     *            the non-localized pattern.
     * @param value
     *            the DecimalFormatSymbols.
     * @throws IllegalArgumentException
     *            if the pattern cannot be parsed.
     */
    public DecimalFormat(String pattern, DecimalFormatSymbols value) {
        symbols = (DecimalFormatSymbols) value.clone();
        TLocale locale = symbols.getLocale();
        icuSymbols = new com.ibm.icu.text.DecimalFormatSymbols(locale);
        copySymbols(icuSymbols, symbols);

        dform = new com.ibm.icu.text.DecimalFormat(pattern, icuSymbols);

        super.setMaximumFractionDigits(dform.getMaximumFractionDigits());
        super.setMaximumIntegerDigits(dform.getMaximumIntegerDigits());
        super.setMinimumFractionDigits(dform.getMinimumFractionDigits());
        super.setMinimumIntegerDigits(dform.getMinimumIntegerDigits());
    }

    DecimalFormat(String pattern, DecimalFormatSymbols value, com.ibm.icu.text.DecimalFormat icuFormat) {
        symbols = value;
        icuSymbols = value.getIcuSymbols();
        dform = icuFormat;

        super.setMaximumFractionDigits(dform.getMaximumFractionDigits());
        super.setMaximumIntegerDigits(dform.getMaximumIntegerDigits());
        super.setMinimumFractionDigits(dform.getMinimumFractionDigits());
        super.setMinimumIntegerDigits(dform.getMinimumIntegerDigits());
    }

    /**
     * Changes the pattern of this decimal format to the specified pattern which
     * uses localized pattern characters.
     *
     * @param pattern
     *            the localized pattern.
     * @throws IllegalArgumentException
     *            if the pattern cannot be parsed.
     */
    public void applyLocalizedPattern(String pattern) {
        dform.applyLocalizedPattern(pattern);
        super.setMaximumFractionDigits(dform.getMaximumFractionDigits());
        super.setMaximumIntegerDigits(dform.getMaximumIntegerDigits());
        super.setMinimumFractionDigits(dform.getMinimumFractionDigits());
        super.setMinimumIntegerDigits(dform.getMinimumIntegerDigits());
    }

    /**
     * Changes the pattern of this decimal format to the specified pattern which
     * uses non-localized pattern characters.
     *
     * @param pattern
     *            the non-localized pattern.
     * @throws IllegalArgumentException
     *            if the pattern cannot be parsed.
     */
    public void applyPattern(String pattern) {

        dform.applyPattern(pattern);
        super.setMaximumFractionDigits(dform.getMaximumFractionDigits());
        super.setMaximumIntegerDigits(dform.getMaximumIntegerDigits());
        super.setMinimumFractionDigits(dform.getMinimumFractionDigits());
        super.setMinimumIntegerDigits(dform.getMinimumIntegerDigits());
    }

    /**
     * Returns a new instance of {@code DecimalFormat} with the same pattern and
     * properties as this decimal format.
     *
     * @return a shallow copy of this decimal format.
     * @see java.lang.Cloneable
     */
    @Override
    public Object clone() {
        DecimalFormat clone = (DecimalFormat) super.clone();
        clone.dform = (com.ibm.icu.text.DecimalFormat) dform.clone();
        clone.symbols = (DecimalFormatSymbols) symbols.clone();
        return clone;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DecimalFormat)) {
            return false;
        }
        DecimalFormat format = (DecimalFormat) object;
        return (this.dform == null ? format.dform == null : this.dform
                .equals(format.dform));
    }

    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object object) {
        if (object == null) {
            throw new NullPointerException();
        }
        return dform.formatToCharacterIterator(object);
    }

    @Override
    public StringBuffer format(double value, StringBuffer buffer,
            FieldPosition position) {
        return dform.format(value, buffer, position);
    }

    @Override
    public StringBuffer format(long value, StringBuffer buffer,
            FieldPosition position) {
        return dform.format(value, buffer, position);
    }

    @Override
    public final StringBuffer format(Object number, StringBuffer toAppendTo,
            FieldPosition pos) {
        if (!(number instanceof Number)) {
            throw new IllegalArgumentException();
        }
        if (toAppendTo == null || pos == null) {
            throw new NullPointerException();
        }
        if (number instanceof BigInteger || number instanceof BigDecimal) {
            return dform.format(number, toAppendTo, pos);
        }
        return super.format(number, toAppendTo, pos);
    }

    public DecimalFormatSymbols getDecimalFormatSymbols() {
        return (DecimalFormatSymbols) symbols.clone();
    }

    @Override
    public Currency getCurrency() {
        final com.ibm.icu.util.Currency cur = dform.getCurrency();
        final String code = (cur == null) ? "XXX" : cur.getCurrencyCode(); //$NON-NLS-1$

        return Currency.getInstance(code);
    }

    public int getGroupingSize() {
        return dform.getGroupingSize();
    }

    public int getMultiplier() {
        return dform.getMultiplier();
    }

    public String getNegativePrefix() {
        return dform.getNegativePrefix();
    }

    public String getNegativeSuffix() {
        return dform.getNegativeSuffix();
    }

    public String getPositivePrefix() {
        return dform.getPositivePrefix();
    }

    public String getPositiveSuffix() {
        return dform.getPositiveSuffix();
    }

    @Override
    public int hashCode() {
        return dform.hashCode();
    }

    public boolean isDecimalSeparatorAlwaysShown() {
        return dform.isDecimalSeparatorAlwaysShown();
    }

    public boolean isParseBigDecimal() {
        return this.parseBigDecimal;
    }

    @Override
    public void setParseIntegerOnly(boolean value) {
        // In this implementation, com.ibm.icu.text.DecimalFormat is wrapped to
        // fulfill most of the format and parse feature. And this method is
        // delegated to the wrapped instance of com.ibm.icu.text.DecimalFormat.

        dform.setParseIntegerOnly(value);
    }

    @Override
    public boolean isParseIntegerOnly() {
        return dform.isParseIntegerOnly();
    }

    private static final Double NEGATIVE_ZERO_DOUBLE = new Double(-0.0);

    @Override
    public Number parse(String string, ParsePosition position) {
        Number number = dform.parse(string, position);
        if (null == number) {
            return null;
        }
        if (this.isParseBigDecimal()) {
            if (number instanceof Long) {
                return new BigDecimal(number.longValue());
            }
            if ((number instanceof Double) && !((Double) number).isInfinite()
                    && !((Double) number).isNaN()) {

                return new BigDecimal(number.doubleValue());
            }
            if (number instanceof BigInteger) {
                return new BigDecimal(number.doubleValue());
            }
            if (number instanceof com.ibm.icu.math.BigDecimal) {
                return new BigDecimal(number.toString());
            }
            return number;
        }
        if ((number instanceof com.ibm.icu.math.BigDecimal)
                || (number instanceof BigInteger)) {
            return new Double(number.doubleValue());
        }

        if (this.isParseIntegerOnly() && number.equals(NEGATIVE_ZERO_DOUBLE)) {
            return new Long(0);
        }
        return number;

    }

    public void setDecimalFormatSymbols(DecimalFormatSymbols value) {
        if (value != null) {
            symbols = (DecimalFormatSymbols) value.clone();
            icuSymbols = dform.getDecimalFormatSymbols();
            copySymbols(icuSymbols, symbols);
            dform.setDecimalFormatSymbols(icuSymbols);
        }
    }

    @Override
    public void setCurrency(Currency currency) {
        dform.setCurrency(com.ibm.icu.util.Currency.getInstance(currency
                .getCurrencyCode()));
        symbols.setCurrency(currency);
    }

    public void setDecimalSeparatorAlwaysShown(boolean value) {
        dform.setDecimalSeparatorAlwaysShown(value);
    }

    public void setGroupingSize(int value) {
        dform.setGroupingSize(value);
    }

    @Override
    public void setGroupingUsed(boolean value) {
        dform.setGroupingUsed(value);
    }

    @Override
    public boolean isGroupingUsed() {
        return dform.isGroupingUsed();
    }

    @Override
    public void setMaximumFractionDigits(int value) {
        super.setMaximumFractionDigits(value);
        dform.setMaximumFractionDigits(value);
    }

    @Override
    public void setMaximumIntegerDigits(int value) {
        super.setMaximumIntegerDigits(value);
        dform.setMaximumIntegerDigits(value);
    }

    @Override
    public void setMinimumFractionDigits(int value) {
        super.setMinimumFractionDigits(value);
        dform.setMinimumFractionDigits(value);
    }

    @Override
    public void setMinimumIntegerDigits(int value) {
        super.setMinimumIntegerDigits(value);
        dform.setMinimumIntegerDigits(value);
    }

    public void setMultiplier(int value) {
        dform.setMultiplier(value);
    }

    public void setNegativePrefix(String value) {
        dform.setNegativePrefix(value);
    }

    public void setNegativeSuffix(String value) {
        dform.setNegativeSuffix(value);
    }

    public void setPositivePrefix(String value) {
        dform.setPositivePrefix(value);
    }

    public void setPositiveSuffix(String value) {
        dform.setPositiveSuffix(value);
    }

    public void setParseBigDecimal(boolean newValue) {
        this.parseBigDecimal = newValue;
    }

    public String toLocalizedPattern() {
        return dform.toLocalizedPattern();
    }

    public String toPattern() {
        return dform.toPattern();
    }

    /*
     * Copies decimal format symbols from text object to ICU one.
     *
     * @param icu the object which receives the new values. @param dfs the
     * object which contains the new values.
     */
    private void copySymbols(final com.ibm.icu.text.DecimalFormatSymbols icu,
            final DecimalFormatSymbols dfs) {
        Currency currency = dfs.getCurrency();
        if (currency == null) {
            icu.setCurrency(com.ibm.icu.util.Currency.getInstance("XXX")); //$NON-NLS-1$
        } else {
            icu.setCurrency(com.ibm.icu.util.Currency.getInstance(dfs
                    .getCurrency().getCurrencyCode()));
        }

        icu.setCurrencySymbol(dfs.getCurrencySymbol());
        icu.setDecimalSeparator(dfs.getDecimalSeparator());
        icu.setDigit(dfs.getDigit());
        icu.setGroupingSeparator(dfs.getGroupingSeparator());
        icu.setInfinity(dfs.getInfinity());
        icu
                .setInternationalCurrencySymbol(dfs
                        .getInternationalCurrencySymbol());
        icu.setMinusSign(dfs.getMinusSign());
        icu.setMonetaryDecimalSeparator(dfs.getMonetaryDecimalSeparator());
        icu.setNaN(dfs.getNaN());
        icu.setPatternSeparator(dfs.getPatternSeparator());
        icu.setPercent(dfs.getPercent());
        icu.setPerMill(dfs.getPerMill());
        icu.setZeroDigit(dfs.getZeroDigit());
    }
}
