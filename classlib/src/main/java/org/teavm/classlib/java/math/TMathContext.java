/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.classlib.java.math;

import java.io.Serializable;

/**
 * Immutable objects describing settings such as rounding mode and digit
 * precision for the numerical operations provided by class {@link TBigDecimal}.
 */
public final class TMathContext implements Serializable {

    /**
     * A {@code MathContext} which corresponds to the IEEE 754r quadruple
     * decimal precision format: 34 digit precision and
     * {@link TRoundingMode#HALF_EVEN} rounding.
     */
    public static final TMathContext DECIMAL128 = new TMathContext(34,
            TRoundingMode.HALF_EVEN);

    /**
     * A {@code MathContext} which corresponds to the IEEE 754r single decimal
     * precision format: 7 digit precision and {@link TRoundingMode#HALF_EVEN}
     * rounding.
     */
    public static final TMathContext DECIMAL32 = new TMathContext(7,
            TRoundingMode.HALF_EVEN);

    /**
     * A {@code MathContext} which corresponds to the IEEE 754r double decimal
     * precision format: 16 digit precision and {@link TRoundingMode#HALF_EVEN}
     * rounding.
     */
    public static final TMathContext DECIMAL64 = new TMathContext(16,
            TRoundingMode.HALF_EVEN);

    /**
     * A {@code MathContext} for unlimited precision with
     * {@link TRoundingMode#HALF_UP} rounding.
     */
    public static final TMathContext UNLIMITED = new TMathContext(0,
            TRoundingMode.HALF_UP);

    /** This is the serialVersionUID used by the sun implementation */
    private static final long serialVersionUID = 5579720004786848255L;

    /**
     * The number of digits to be used for an operation; results are rounded to
     * this precision.
     */
    private int precision;

    /**
     * A {@code RoundingMode} object which specifies the algorithm to be used
     * for rounding.
     */
    private TRoundingMode roundingMode;

    /**
     * An array of {@code char} containing: {@code
     * 'p','r','e','c','i','s','i','o','n','='}. It's used to improve the
     * methods related to {@code String} conversion.
     *
     * @see #TMathContext(String)
     * @see #toString()
     */
    private final static char[] chPrecision = { 'p', 'r', 'e', 'c', 'i', 's',
            'i', 'o', 'n', '=' };

    /**
     * An array of {@code char} containing: {@code
     * 'r','o','u','n','d','i','n','g','M','o','d','e','='}. It's used to
     * improve the methods related to {@code String} conversion.
     *
     * @see #TMathContext(String)
     * @see #toString()
     */
    private final static char[] chRoundingMode = { 'r', 'o', 'u', 'n', 'd',
            'i', 'n', 'g', 'M', 'o', 'd', 'e', '=' };

    /**
     * Constructs a new {@code MathContext} with the specified precision and
     * with the rounding mode {@link TRoundingMode#HALF_UP HALF_UP}. If the
     * precision passed is zero, then this implies that the computations have to
     * be performed exact, the rounding mode in this case is irrelevant.
     *
     * @param precision
     *            the precision for the new {@code MathContext}.
     * @throws IllegalArgumentException
     *             if {@code precision < 0}.
     */
    public TMathContext(int precision) {
        this(precision, TRoundingMode.HALF_UP);
    }

    /**
     * Constructs a new {@code MathContext} with the specified precision and
     * with the specified rounding mode. If the precision passed is zero, then
     * this implies that the computations have to be performed exact, the
     * rounding mode in this case is irrelevant.
     *
     * @param precision
     *            the precision for the new {@code MathContext}.
     * @param roundingMode
     *            the rounding mode for the new {@code MathContext}.
     * @throws IllegalArgumentException
     *             if {@code precision < 0}.
     * @throws NullPointerException
     *             if {@code roundingMode} is {@code null}.
     */
    public TMathContext(int precision, TRoundingMode roundingMode) {
        if (precision < 0) {
            throw new IllegalArgumentException("Digits < 0");
        }
        if (roundingMode == null) {
            throw new NullPointerException("null RoundingMode");
        }
        this.precision = precision;
        this.roundingMode = roundingMode;
    }

    /**
     * Constructs a new {@code MathContext} from a string. The string has to
     * specify the precision and the rounding mode to be used and has to follow
     * the following syntax: "precision=&lt;precision&gt; roundingMode=&lt;roundingMode&gt;"
     * This is the same form as the one returned by the {@link #toString}
     * method.
     *
     * @param val
     *            a string describing the precision and rounding mode for the
     *            new {@code MathContext}.
     * @throws IllegalArgumentException
     *             if the string is not in the correct format or if the
     *             precision specified is < 0.
     */
    public TMathContext(String val) {
        char[] charVal = val.toCharArray();
        int i; // Index of charVal
        int j; // Index of chRoundingMode
        int digit; // It will contain the digit parsed

        if ((charVal.length < 27) || (charVal.length > 45)) {
            throw new IllegalArgumentException("bad string format");
        }
        // Parsing "precision=" String
        for (i = 0; (i < chPrecision.length) && (charVal[i] == chPrecision[i]); i++) {
            // do nothing
        }

        if (i < chPrecision.length) {
            throw new IllegalArgumentException("bad string format");
        }
        // Parsing the value for "precision="...
        digit = Character.digit(charVal[i], 10);
        if (digit == -1) {
            throw new IllegalArgumentException("bad string format");
        }
        this.precision = this.precision * 10 + digit;
        i++;

        do {
            digit = Character.digit(charVal[i], 10);
            if (digit == -1) {
                if (charVal[i] == ' ') {
                    // It parsed all the digits
                    i++;
                    break;
                }
                // It isn't  a valid digit, and isn't a white space
                throw new IllegalArgumentException("bad string format");
            }
            // Accumulating the value parsed
            this.precision = this.precision * 10 + digit;
            if (this.precision < 0) {
                throw new IllegalArgumentException("bad string format");
            }
            i++;
        } while (true);
        // Parsing "roundingMode="
        for (j = 0; (j < chRoundingMode.length) && (charVal[i] == chRoundingMode[j]); i++, j++) {
            // do nothing
        }

        if (j < chRoundingMode.length) {
            throw new IllegalArgumentException("bad string format");
        }
        // Parsing the value for "roundingMode"...
        this.roundingMode = TRoundingMode.valueOf(String.valueOf(charVal, i, charVal.length - i));
    }

    /* Public Methods */

    /**
     * Returns the precision. The precision is the number of digits used for an
     * operation. Results are rounded to this precision. The precision is
     * guaranteed to be non negative. If the precision is zero, then the
     * computations have to be performed exact, results are not rounded in this
     * case.
     *
     * @return the precision.
     */
    public int getPrecision() {
        return precision;
    }

    /**
     * Returns the rounding mode. The rounding mode is the strategy to be used
     * to round results.
     * <p>
     * The rounding mode is one of
     * {@link TRoundingMode#UP},
     * {@link TRoundingMode#DOWN},
     * {@link TRoundingMode#CEILING},
     * {@link TRoundingMode#FLOOR},
     * {@link TRoundingMode#HALF_UP},
     * {@link TRoundingMode#HALF_DOWN},
     * {@link TRoundingMode#HALF_EVEN}, or
     * {@link TRoundingMode#UNNECESSARY}.
     *
     * @return the rounding mode.
     */
    public TRoundingMode getRoundingMode() {
        return roundingMode;
    }

    /**
     * Returns true if x is a {@code MathContext} with the same precision
     * setting and the same rounding mode as this {@code MathContext} instance.
     *
     * @param x
     *            object to be compared.
     * @return {@code true} if this {@code MathContext} instance is equal to the
     *         {@code x} argument; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object x) {
        return x instanceof TMathContext
                && (((TMathContext) x).getPrecision() == precision) && (((TMathContext) x)
                .getRoundingMode() == roundingMode);
    }

    /**
     * Returns the hash code for this {@code MathContext} instance.
     *
     * @return the hash code for this {@code MathContext}.
     */
    @Override
    public int hashCode() {
        // Make place for the necessary bits to represent 8 rounding modes
        return (precision << 3) | roundingMode.ordinal();
    }

    /**
     * Returns the string representation for this {@code MathContext} instance.
     * The string has the form
     * {@code
     * "precision=&lt;precision&gt; roundingMode=&lt;roundingMode&gt;"
     * } where {@code &lt;precision&gt;} is an integer describing the number
     * of digits used for operations and {@code &lt;roundingMode&gt;} is the
     * string representation of the rounding mode.
     *
     * @return a string representation for this {@code MathContext} instance
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(45);

        sb.append(chPrecision);
        sb.append(precision);
        sb.append(' ');
        sb.append(chRoundingMode);
        sb.append(roundingMode);
        return sb.toString();
    }
}
