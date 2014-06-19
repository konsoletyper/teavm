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
import org.teavm.classlib.impl.unicode.CLDRDecimalData;
import org.teavm.classlib.impl.unicode.CLDRHelper;
import org.teavm.classlib.java.util.TLocale;

/**
 *
 * @author Alexey Andreev
 */
public class TDecimalFormat extends TNumberFormat {
    private TDecimalFormatSymbols symbols;

    public TDecimalFormat() {
        this(CLDRHelper.resolveDecimalFormat(TLocale.getDefault().getLanguage(), TLocale.getDefault().getCountry()));
    }

    public TDecimalFormat(String pattern) {
        this(pattern, new TDecimalFormatSymbols());
    }

    public TDecimalFormat(String pattern, TDecimalFormatSymbols value) {
        symbols = (TDecimalFormatSymbols)value.clone();
        TLocale locale = symbols.getLocale();
        applyPattern(pattern);

        CLDRDecimalData decimalData = CLDRHelper.resolveDecimalData(locale.getLanguage(), locale.getCountry());
        super.setMaximumFractionDigits(decimalData.getMaximumFractionDigits());
        super.setMaximumIntegerDigits(decimalData.getMaximumIntegerDigits());
        super.setMinimumFractionDigits(decimalData.getMinimumFractionDigits());
        super.setMinimumIntegerDigits(decimalData.getMinimumIntegerDigits());
    }

    public void applyPattern(String pattern) {

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
}
