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

/**
 * Specifies the rounding behavior for operations whose results cannot be
 * represented exactly.
 */
public enum TRoundingMode {

    /**
     * Rounding mode where positive values are rounded towards positive infinity
     * and negative values towards negative infinity.
     * <br>
     * Rule: {@code x.round().abs() >= x.abs()}
     */
    UP(TBigDecimal.ROUND_UP),

    /**
     * Rounding mode where the values are rounded towards zero.
     * <br>
     * Rule: {@code x.round().abs() <= x.abs()}
     */
    DOWN(TBigDecimal.ROUND_DOWN),

    /**
     * Rounding mode to round towards positive infinity. For positive values
     * this rounding mode behaves as {@link #UP}, for negative values as
     * {@link #DOWN}.
     * <br>
     * Rule: {@code x.round() >= x}
     */
    CEILING(TBigDecimal.ROUND_CEILING),

    /**
     * Rounding mode to round towards negative infinity. For positive values
     * this rounding mode behaves as {@link #DOWN}, for negative values as
     * {@link #UP}.
     * <br>
     * Rule: {@code x.round() <= x}
     */
    FLOOR(TBigDecimal.ROUND_FLOOR),

    /**
     * Rounding mode where values are rounded towards the nearest neighbor. Ties
     * are broken by rounding up.
     */
    HALF_UP(TBigDecimal.ROUND_HALF_UP),

    /**
     * Rounding mode where values are rounded towards the nearest neighbor. Ties
     * are broken by rounding down.
     */
    HALF_DOWN(TBigDecimal.ROUND_HALF_DOWN),

    /**
     * Rounding mode where values are rounded towards the nearest neighbor. Ties
     * are broken by rounding to the even neighbor.
     */
    HALF_EVEN(TBigDecimal.ROUND_HALF_EVEN),

    /**
     * Rounding mode where the rounding operations throws an ArithmeticException
     * for the case that rounding is necessary, i.e. for the case that the value
     * cannot be represented exactly.
     */
    UNNECESSARY(TBigDecimal.ROUND_UNNECESSARY);

    /** The old constant of <code>BigDecimal</code>. */
    @SuppressWarnings("unused")
    private final int bigDecimalRM;

    /** It sets the old constant. */
    TRoundingMode(int rm) {
        bigDecimalRM = rm;
    }

    /**
     * Converts rounding mode constants from class {@code BigDecimal} into
     * {@code RoundingMode} values.
     *
     * @param mode
     *            rounding mode constant as defined in class {@code BigDecimal}
     * @return corresponding rounding mode object
     */
    public static TRoundingMode valueOf(int mode) {
        switch (mode) {
            case TBigDecimal.ROUND_CEILING:
                return CEILING;
            case TBigDecimal.ROUND_DOWN:
                return DOWN;
            case TBigDecimal.ROUND_FLOOR:
                return FLOOR;
            case TBigDecimal.ROUND_HALF_DOWN:
                return HALF_DOWN;
            case TBigDecimal.ROUND_HALF_EVEN:
                return HALF_EVEN;
            case TBigDecimal.ROUND_HALF_UP:
                return HALF_UP;
            case TBigDecimal.ROUND_UNNECESSARY:
                return UNNECESSARY;
            case TBigDecimal.ROUND_UP:
                return UP;
            default:
                throw new IllegalArgumentException("Invalid rounding mode");
        }
    }
}
