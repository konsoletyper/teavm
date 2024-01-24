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
import java.util.Random;

/**
 * This class represents immutable integer numbers of arbitrary length. Large
 * numbers are typically used in security applications and therefore BigIntegers
 * offer dedicated functionality like the generation of large prime numbers or
 * the computation of modular inverse.
 * <p>
 * Since the class was modeled to offer all the functionality as the
 * {@link Integer} class does, it provides even methods that operate bitwise on
 * a two's complement representation of large integers. Note however that the
 * implementations favors an internal representation where magnitude and sign
 * are treated separately. Hence such operations are inefficient and should be
 * discouraged. In simple words: Do NOT implement any bit fields based on
 * BigInteger.
 */
public class TBigInteger extends Number implements Comparable<TBigInteger>, Serializable {

    /** This is the serialVersionUID used by the sun implementation. */
    private static final long serialVersionUID = -8287574255936472291L;

    /* Fields used for the internal representation. */

    /**
     * The magnitude of this big integer. This array holds unsigned little
     * endian digits. For example: {@code 13} is represented as [ 13 ]
     * {@code -13} is represented as [ 13 ] {@code 2^32 + 13} is represented as
     * [ 13, 1 ] {@code 2^64 + 13} is represented as [ 13, 0, 1 ] {@code 2^31}
     * is represented as [ Integer.MIN_VALUE ] The magnitude array may be longer
     * than strictly necessary, which results in additional trailing zeros.
     */
    transient int[] digits;

    /**
     * The length of this in measured in ints. Can be less than digits.length().
     */
    transient int numberLength;

    /** The sign of this. */
    transient int sign;

    /**
     * The {@code BigInteger} constant 0.
     */
    public static final TBigInteger ZERO = new TBigInteger(0, 0);

    /**
     * The {@code BigInteger} constant 1.
     */
    public static final TBigInteger ONE = new TBigInteger(1, 1);

    /**
     * The {@code BigInteger} constant 2.
     */
    public static final TBigInteger TWO = new TBigInteger(1, 2);

    /**
     * The {@code BigInteger} constant 10.
     */
    public static final TBigInteger TEN = new TBigInteger(1, 10);

    /** The {@code BigInteger} constant -1. */
    static final TBigInteger MINUS_ONE = new TBigInteger(-1, 1);

    /** The {@code BigInteger} constant 0 used for comparison. */
    static final int EQUALS = 0;

    /** The {@code BigInteger} constant 1 used for comparison. */
    static final int GREATER = 1;

    /** The {@code BigInteger} constant -1 used for comparison. */
    static final int LESS = -1;

    /** All the {@code BigInteger} numbers in the range [0,10] are cached. */
    static final TBigInteger[] SMALL_VALUES = { ZERO, ONE, TWO, new TBigInteger(1, 3),
            new TBigInteger(1, 4), new TBigInteger(1, 5), new TBigInteger(1, 6), new TBigInteger(1, 7),
            new TBigInteger(1, 8), new TBigInteger(1, 9), TEN };

    static final TBigInteger[] TWO_POWS;

    static {
        TWO_POWS = new TBigInteger[32];
        for (int i = 0; i < TWO_POWS.length; i++) {
            TWO_POWS[i] = TBigInteger.valueOf(1L << i);
        }
    }

    private transient int firstNonzeroDigit = -2;

    /** Cache for the hash code. */
    private transient int hashCode;

    /**
     * Constructs a random non-negative {@code BigInteger} instance in the range
     * [0, 2^(numBits)-1].
     *
     * @param numBits
     *            maximum length of the new {@code BigInteger} in bits.
     * @param rnd
     *            is an optional random generator to be used.
     * @throws IllegalArgumentException
     *             if {@code numBits} < 0.
     */
    public TBigInteger(int numBits, Random rnd) {
        if (numBits < 0) {
            throw new IllegalArgumentException("numBits must be non-negative");
        }
        if (numBits == 0) {
            sign = 0;
            numberLength = 1;
            digits = new int[] { 0 };
        } else {
            sign = 1;
            numberLength = (numBits + 31) >> 5;
            digits = new int[numberLength];
            for (int i = 0; i < numberLength; i++) {
                digits[i] = rnd.nextInt();
            }
            // Using only the necessary bits
            digits[numberLength - 1] >>>= (-numBits) & 31;
            cutOffLeadingZeroes();
        }
    }

    /**
     * Constructs a random {@code BigInteger} instance in the range [0,
     * 2^(bitLength)-1] which is probably prime. The probability that the
     * returned {@code BigInteger} is prime is beyond (1-1/2^certainty).
     *
     * @param bitLength
     *            length of the new {@code BigInteger} in bits.
     * @param certainty
     *            tolerated primality uncertainty.
     * @param rnd
     *            is an optional random generator to be used.
     * @throws ArithmeticException
     *             if {@code bitLength} < 2.
     */
    public TBigInteger(int bitLength, int certainty, Random rnd) {
        if (bitLength < 2) {
            throw new ArithmeticException("bitLength < 2");
        }
        TBigInteger me = TPrimality.consBigInteger(bitLength, certainty, rnd);
        sign = me.sign;
        numberLength = me.numberLength;
        digits = me.digits;
    }

    /**
     * Constructs a new {@code BigInteger} instance from the string
     * representation. The string representation consists of an optional minus
     * sign followed by a non-empty sequence of decimal digits.
     *
     * @param val
     *            string representation of the new {@code BigInteger}.
     * @throws NullPointerException
     *             if {@code val == null}.
     * @throws NumberFormatException
     *             if {@code val} is not a valid representation of a
     *             {@code BigInteger}.
     */
    public TBigInteger(String val) {
        this(val, 10);
    }

    /**
     * Constructs a new {@code BigInteger} instance from the string
     * representation. The string representation consists of an optional minus
     * sign followed by a non-empty sequence of digits in the specified radix.
     * For the conversion the method {@code Character.digit(char, radix)} is
     * used.
     *
     * @param val
     *            string representation of the new {@code BigInteger}.
     * @param radix
     *            the base to be used for the conversion.
     * @throws NullPointerException
     *             if {@code val == null}.
     * @throws NumberFormatException
     *             if {@code val} is not a valid representation of a
     *             {@code BigInteger} or if {@code radix < Character.MIN_RADIX}
     *             or {@code radix > Character.MAX_RADIX}.
     */
    public TBigInteger(String val, int radix) {
        if (val == null) {
            throw new NullPointerException();
        }
        if ((radix < Character.MIN_RADIX) || (radix > Character.MAX_RADIX)) {
            throw new NumberFormatException("Radix out of range");
        }
        if (val.length() == 0) {
            throw new NumberFormatException("Zero length BigInteger");
        }
        setFromString(this, val, radix);
    }

    /**
     * Constructs a new {@code BigInteger} instance with the given sign and the
     * given magnitude. The sign is given as an integer (-1 for negative, 0 for
     * zero, 1 for positive). The magnitude is specified as a byte array. The
     * most significant byte is the entry at index 0.
     *
     * @param signum
     *            sign of the new {@code BigInteger} (-1 for negative, 0 for
     *            zero, 1 for positive).
     * @param magnitude
     *            magnitude of the new {@code BigInteger} with the most
     *            significant byte first.
     * @throws NullPointerException
     *             if {@code magnitude == null}.
     * @throws NumberFormatException
     *             if the sign is not one of -1, 0, 1 or if the sign is zero and
     *             the magnitude contains non-zero entries.
     */
    public TBigInteger(int signum, byte[] magnitude) {
        if (magnitude == null) {
            throw new NullPointerException();
        }
        if ((signum < -1) || (signum > 1)) {
            throw new NumberFormatException("Invalid signum value");
        }
        if (signum == 0) {
            for (byte element : magnitude) {
                if (element != 0) {
                    throw new NumberFormatException("signum-magnitude mismatch");
                }
            }
        }
        if (magnitude.length == 0) {
            sign = 0;
            numberLength = 1;
            digits = new int[] { 0 };
        } else {
            sign = signum;
            putBytesPositiveToIntegers(magnitude);
            cutOffLeadingZeroes();
        }
    }

    /**
     * Constructs a new {@code BigInteger} from the given two's complement
     * representation. The most significant byte is the entry at index 0. The
     * most significant bit of this entry determines the sign of the new
     * {@code BigInteger} instance. The given array must not be empty.
     *
     * @param val
     *            two's complement representation of the new {@code BigInteger}.
     * @throws NullPointerException
     *             if {@code val == null}.
     * @throws NumberFormatException
     *             if the length of {@code val} is zero.
     */
    public TBigInteger(byte[] val) {
        if (val.length == 0) {
            throw new NumberFormatException("Zero length BigInteger");
        }
        if (val[0] < 0) {
            sign = -1;
            putBytesNegativeToIntegers(val);
        } else {
            sign = 1;
            putBytesPositiveToIntegers(val);
        }
        cutOffLeadingZeroes();
    }

    /**
     * Constructs a number which array is of size 1.
     *
     * @param sign
     *            the sign of the number
     * @param value
     *            the only one digit of array
     */
    TBigInteger(int sign, int value) {
        this.sign = sign;
        numberLength = 1;
        digits = new int[] { value };
    }

    /**
     * Constructs a number without to create new space. This construct should be
     * used only if the three fields of representation are known.
     *
     * @param sign
     *            the sign of the number
     * @param numberLength
     *            the length of the internal array
     * @param digits
     *            a reference of some array created before
     */
    TBigInteger(int sign, int numberLength, int[] digits) {
        this.sign = sign;
        this.numberLength = numberLength;
        this.digits = digits;
    }

    /**
     * Creates a new {@code BigInteger} whose value is equal to the specified
     * {@code long}.
     *
     * @param sign
     *            the sign of the number
     * @param val
     *            the value of the new {@code BigInteger}.
     */
    TBigInteger(int sign, long val) {
        // PRE: (val >= 0) && (sign >= -1) && (sign <= 1)
        this.sign = sign;
        if ((val & 0xFFFFFFFF00000000L) == 0) {
            // It fits in one 'int'
            numberLength = 1;
            digits = new int[] { (int) val };
        } else {
            numberLength = 2;
            digits = new int[] { (int) val, (int) (val >> 32) };
        }
    }

    /**
     * Creates a new {@code BigInteger} with the given sign and magnitude. This
     * constructor does not create a copy, so any changes to the reference will
     * affect the new number.
     *
     * @param signum
     *            The sign of the number represented by {@code digits}
     * @param digits
     *            The magnitude of the number
     */
    TBigInteger(int signum, int[] digits) {
        if (digits.length == 0) {
            sign = 0;
            numberLength = 1;
            this.digits = new int[] { 0 };
        } else {
            sign = signum;
            numberLength = digits.length;
            this.digits = digits;
            cutOffLeadingZeroes();
        }
    }

    public static TBigInteger valueOf(long val) {
        if (val < 0) {
            if (val != -1) {
                return new TBigInteger(-1, -val);
            }
            return MINUS_ONE;
        } else if (val <= 10) {
            return SMALL_VALUES[(int) val];
        } else {
            // (val > 10)
            return new TBigInteger(1, val);
        }
    }

    /**
     * Returns the two's complement representation of this BigInteger in a byte
     * array.
     *
     * @return two's complement representation of {@code this}.
     */
    public byte[] toByteArray() {
        if (this.sign == 0) {
            return new byte[] { 0 };
        }
        TBigInteger temp = this;
        int bitLen = bitLength();
        int iThis = getFirstNonzeroDigit();
        int bytesLen = (bitLen >> 3) + 1;
        /*
         * Puts the little-endian int array representing the magnitude of this
         * BigInteger into the big-endian byte array.
         */
        byte[] bytes = new byte[bytesLen];
        int firstByteNumber = 0;
        int highBytes;
        int digitIndex = 0;
        int bytesInInteger = 4;
        int digit;
        int hB;

        if (bytesLen - (numberLength << 2) == 1) {
            bytes[0] = (byte) ((sign < 0) ? -1 : 0);
            highBytes = 4;
            firstByteNumber++;
        } else {
            hB = bytesLen & 3;
            highBytes = (hB == 0) ? 4 : hB;
        }

        digitIndex = iThis;
        bytesLen -= iThis << 2;

        if (sign < 0) {
            digit = -temp.digits[digitIndex];
            digitIndex++;
            if (digitIndex == numberLength) {
                bytesInInteger = highBytes;
            }
            for (int i = 0; i < bytesInInteger; i++, digit >>= 8) {
                bytes[--bytesLen] = (byte) digit;
            }
            while (bytesLen > firstByteNumber) {
                digit = ~temp.digits[digitIndex];
                digitIndex++;
                if (digitIndex == numberLength) {
                    bytesInInteger = highBytes;
                }
                for (int i = 0; i < bytesInInteger; i++, digit >>= 8) {
                    bytes[--bytesLen] = (byte) digit;
                }
            }
        } else {
            while (bytesLen > firstByteNumber) {
                digit = temp.digits[digitIndex];
                digitIndex++;
                if (digitIndex == numberLength) {
                    bytesInInteger = highBytes;
                }
                for (int i = 0; i < bytesInInteger; i++, digit >>= 8) {
                    bytes[--bytesLen] = (byte) digit;
                }
            }
        }
        return bytes;
    }

    /** @see TBigInteger#TBigInteger(String, int) */
    private static void setFromString(TBigInteger bi, String val, int radix) {
        int sign;
        int[] digits;
        int numberLength;
        int stringLength = val.length();
        int startChar;
        int endChar = stringLength;

        if (val.charAt(0) == '-') {
            sign = -1;
            startChar = 1;
            stringLength--;
        } else {
            sign = 1;
            startChar = 0;
        }
        /*
         * We use the following algorithm: split a string into portions of n
         * characters and convert each portion to an integer according to the
         * radix. Then convert an exp(radix, n) based number to binary using the
         * multiplication method. See D. Knuth, The Art of Computer Programming,
         * vol. 2.
         */

        int charsPerInt = TConversion.digitFitInInt[radix];
        int bigRadixDigitsLength = stringLength / charsPerInt;
        int topChars = stringLength % charsPerInt;

        if (topChars != 0) {
            bigRadixDigitsLength++;
        }
        digits = new int[bigRadixDigitsLength];
        // Get the maximal power of radix that fits in int
        int bigRadix = TConversion.bigRadices[radix - 2];
        // Parse an input string and accumulate the BigInteger's magnitude
        int digitIndex = 0; // index of digits array
        int substrEnd = startChar + ((topChars == 0) ? charsPerInt : topChars);
        int newDigit;

        for (int substrStart = startChar; substrStart < endChar; substrStart = substrEnd, substrEnd = substrStart
                + charsPerInt) {
            int bigRadixDigit = Integer.parseInt(val.substring(substrStart, substrEnd), radix);
            newDigit = TMultiplication.multiplyByInt(digits, digitIndex, bigRadix);
            newDigit += TElementary.inplaceAdd(digits, digitIndex, bigRadixDigit);
            digits[digitIndex++] = newDigit;
        }
        numberLength = digitIndex;
        bi.sign = sign;
        bi.numberLength = numberLength;
        bi.digits = digits;
        bi.cutOffLeadingZeroes();
    }

    /**
     * Returns a (new) {@code BigInteger} whose value is the absolute value of
     * {@code this}.
     *
     * @return {@code abs(this)}.
     */
    public TBigInteger abs() {
        return sign < 0 ? new TBigInteger(1, numberLength, digits) : this;
    }

    /**
     * Returns a new {@code BigInteger} whose value is the {@code -this}.
     *
     * @return {@code -this}.
     */
    public TBigInteger negate() {
        return sign == 0 ? this : new TBigInteger(-sign, numberLength, digits);
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this + val}.
     *
     * @param val
     *            value to be added to {@code this}.
     * @return {@code this + val}.
     * @throws NullPointerException
     *             if {@code val == null}.
     */
    public TBigInteger add(TBigInteger val) {
        return TElementary.add(this, val);
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this - val}.
     *
     * @param val
     *            value to be subtracted from {@code this}.
     * @return {@code this - val}.
     * @throws NullPointerException
     *             if {@code val == null}.
     */
    public TBigInteger subtract(TBigInteger val) {
        return TElementary.subtract(this, val);
    }

    /**
     * Returns the sign of this {@code BigInteger}.
     *
     * @return {@code -1} if {@code this < 0}, {@code 0} if {@code this == 0},
     *         {@code 1} if {@code this > 0}.
     */
    public int signum() {
        return sign;
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this >> n}. For
     * negative arguments, the result is also negative. The shift distance may
     * be negative which means that {@code this} is shifted left.
     * <p>
     * <b>Implementation Note:</b> Usage of this method on negative values is
     * not recommended as the current implementation is not efficient.
     *
     * @param n
     *            shift distance
     * @return {@code this >> n} if {@code n >= 0}; {@code this << (-n)}
     *         otherwise
     */
    public TBigInteger shiftRight(int n) {
        if ((n == 0) || (sign == 0)) {
            return this;
        }
        return n > 0 ? TBitLevel.shiftRight(this, n) : TBitLevel.shiftLeft(this, -n);
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this << n}. The
     * result is equivalent to {@code this * 2^n} if n >= 0. The shift distance
     * may be negative which means that {@code this} is shifted right. The
     * result then corresponds to {@code floor(this / 2^(-n))}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method on negative values is
     * not recommended as the current implementation is not efficient.
     *
     * @param n
     *            shift distance.
     * @return {@code this << n} if {@code n >= 0}; {@code this >> (-n)}.
     *         otherwise
     */
    public TBigInteger shiftLeft(int n) {
        if (n == 0 || sign == 0) {
            return this;
        }
        return n > 0 ? TBitLevel.shiftLeft(this, n) : TBitLevel.shiftRight(this, -n);
    }

    TBigInteger shiftLeftOneBit() {
        return (sign == 0) ? this : TBitLevel.shiftLeftOneBit(this);
    }

    /**
     * Returns the length of the value's two's complement representation without
     * leading zeros for positive numbers / without leading ones for negative
     * values.
     * <p>
     * The two's complement representation of {@code this} will be at least
     * {@code bitLength() + 1} bits long.
     * <p>
     * The value will fit into an {@code int} if {@code bitLength() < 32} or
     * into a {@code long} if {@code bitLength() < 64}.
     *
     * @return the length of the minimal two's complement representation for
     *         {@code this} without the sign bit.
     */
    public int bitLength() {
        return TBitLevel.bitLength(this);
    }

    /**
     * Tests whether the bit at position n in {@code this} is set. The result is
     * equivalent to {@code this & (2^n) != 0}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     *
     * @param n
     *            position where the bit in {@code this} has to be inspected.
     * @return {@code this & (2^n) != 0}.
     * @throws ArithmeticException
     *             if {@code n < 0}.
     */
    public boolean testBit(int n) {
        if (n == 0) {
            return (digits[0] & 1) != 0;
        }
        if (n < 0) {
            throw new ArithmeticException("Negative bit address");
        }
        int intCount = n >> 5;
        if (intCount >= numberLength) {
            return sign < 0;
        }
        int digit = digits[intCount];
        n = 1 << (n & 31); // int with 1 set to the needed position
        if (sign < 0) {
            int firstNonZeroDigit = getFirstNonzeroDigit();
            if (intCount < firstNonZeroDigit) {
                return false;
            } else if (firstNonZeroDigit == intCount) {
                digit = -digit;
            } else {
                digit = ~digit;
            }
        }
        return (digit & n) != 0;
    }

    /**
     * Returns a new {@code BigInteger} which has the same binary representation
     * as {@code this} but with the bit at position n set. The result is
     * equivalent to {@code this | 2^n}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     *
     * @param n
     *            position where the bit in {@code this} has to be set.
     * @return {@code this | 2^n}.
     * @throws ArithmeticException
     *             if {@code n < 0}.
     */
    public TBigInteger setBit(int n) {
        if (!testBit(n)) {
            return TBitLevel.flipBit(this, n);
        }
        return this;
    }

    /**
     * Returns a new {@code BigInteger} which has the same binary representation
     * as {@code this} but with the bit at position n cleared. The result is
     * equivalent to {@code this & ~(2^n)}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     *
     * @param n
     *            position where the bit in {@code this} has to be cleared.
     * @return {@code this & ~(2^n)}.
     * @throws ArithmeticException
     *             if {@code n < 0}.
     */
    public TBigInteger clearBit(int n) {
        if (testBit(n)) {
            return TBitLevel.flipBit(this, n);
        }
        return this;
    }

    /**
     * Returns a new {@code BigInteger} which has the same binary representation
     * as {@code this} but with the bit at position n flipped. The result is
     * equivalent to {@code this ^ 2^n}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     *
     * @param n
     *            position where the bit in {@code this} has to be flipped.
     * @return {@code this ^ 2^n}.
     * @throws ArithmeticException
     *             if {@code n < 0}.
     */
    public TBigInteger flipBit(int n) {
        if (n < 0) {
            throw new ArithmeticException("Negative bit address");
        }
        return TBitLevel.flipBit(this, n);
    }

    /**
     * Returns the position of the lowest set bit in the two's complement
     * representation of this {@code BigInteger}. If all bits are zero (this=0)
     * then -1 is returned as result.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     *
     * @return position of lowest bit if {@code this != 0}, {@code -1} otherwise
     */
    public int getLowestSetBit() {
        if (sign == 0) {
            return -1;
        }
        // (sign != 0) implies that exists some non zero digit
        int i = getFirstNonzeroDigit();
        return (i << 5) + Integer.numberOfTrailingZeros(digits[i]);
    }

    /**
     * Use {@code bitLength(0)} if you want to know the length of the binary
     * value in bits.
     * <p>
     * Returns the number of bits in the binary representation of {@code this}
     * which differ from the sign bit. If {@code this} is positive the result is
     * equivalent to the number of bits set in the binary representation of
     * {@code this}. If {@code this} is negative the result is equivalent to the
     * number of bits set in the binary representation of {@code -this-1}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     *
     * @return number of bits in the binary representation of {@code this} which
     *         differ from the sign bit
     */
    public int bitCount() {
        return TBitLevel.bitCount(this);
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code ~this}. The result
     * of this operation is {@code -this-1}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     *
     * @return {@code ~this}.
     */
    public TBigInteger not() {
        return TLogical.not(this);
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this & val}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     *
     * @param val
     *            value to be and'ed with {@code this}.
     * @return {@code this & val}.
     * @throws NullPointerException
     *             if {@code val == null}.
     */
    public TBigInteger and(TBigInteger val) {
        return TLogical.and(this, val);
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this | val}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     *
     * @param val
     *            value to be or'ed with {@code this}.
     * @return {@code this | val}.
     * @throws NullPointerException
     *             if {@code val == null}.
     */
    public TBigInteger or(TBigInteger val) {
        return TLogical.or(this, val);
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this ^ val}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     *
     * @param val
     *            value to be xor'ed with {@code this}
     * @return {@code this ^ val}
     * @throws NullPointerException
     *             if {@code val == null}
     */
    public TBigInteger xor(TBigInteger val) {
        return TLogical.xor(this, val);
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this & ~val}.
     * Evaluating {@code x.andNot(val)} returns the same result as
     * {@code x.and(val.not())}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     *
     * @param val
     *            value to be not'ed and then and'ed with {@code this}.
     * @return {@code this & ~val}.
     * @throws NullPointerException
     *             if {@code val == null}.
     */
    public TBigInteger andNot(TBigInteger val) {
        return TLogical.andNot(this, val);
    }

    public byte byteValueExact() {
        if (numberLength > 1 || bitLength() > 7) {
            throw new ArithmeticException("BigInteger out of byte range");
        }
        return byteValue();
    }

    public short shortValueExact() {
        if (numberLength > 1 || bitLength() > 15) {
            throw new ArithmeticException("BigInteger out of short range");
        }
        return shortValue();
    }

    /**
     * Returns this {@code BigInteger} as an int value. If {@code this} is too
     * big to be represented as an int, then {@code this} % 2^32 is returned.
     *
     * @return this {@code BigInteger} as an int value.
     */
    @Override
    public int intValue() {
        return sign * digits[0];
    }

    /**
     * Returns this {@code BigInter} as an int value.
     *
     * @return this {@code BigInteger} as an int value.
     * @see #intValue
     * @throws ArithmeticException
     *             if {@code this} is too big to be represented as an int.
     */
    public int intValueExact() {
        if (numberLength > 1 || bitLength() > 31) {
            throw new ArithmeticException("BigInteger out of int range");
        }
        return intValue();
    }

    /**
     * Returns this {@code BigInteger} as an long value. If {@code this} is too
     * big to be represented as an long, then {@code this} % 2^64 is returned.
     *
     * @return this {@code BigInteger} as a long value.
     */
    @Override
    public long longValue() {
        long value = (numberLength > 1) ? (((long) digits[1]) << 32) | (digits[0] & 0xFFFFFFFFL)
                : (digits[0] & 0xFFFFFFFFL);
        return sign * value;
    }

    /**
     * Returns this {@code BigInter} as an long value.
     *
     * @return this {@code BigInteger} as a long value.
     * @throws ArithmeticException
     *             if {@code this} is too big to be represented as a long.
     * @see #longValue
     */
    public long longValueExact() {
        if (numberLength > 2 || bitLength() > 63) {
            throw new ArithmeticException("BigInteger out of long range");
        }
        return longValue();
    }

    /**
     * Returns this {@code BigInteger} as an float value. If {@code this} is too
     * big to be represented as an float, then {@code Float.POSITIVE_INFINITY}
     * or {@code Float.NEGATIVE_INFINITY} is returned. Note, that not all
     * integers x in the range [-Float.MAX_VALUE, Float.MAX_VALUE] can be
     * represented as a float. The float representation has a mantissa of length
     * 24. For example, 2^24+1 = 16777217 is returned as float 16777216.0.
     *
     * @return this {@code BigInteger} as a float value.
     */
    @Override
    public float floatValue() {
        return (float) doubleValue();
    }

    /**
     * Returns this {@code BigInteger} as an double value. If {@code this} is
     * too big to be represented as an double, then
     * {@code Double.POSITIVE_INFINITY} or {@code Double.NEGATIVE_INFINITY} is
     * returned. Note, that not all integers x in the range [-Double.MAX_VALUE,
     * Double.MAX_VALUE] can be represented as a double. The double
     * representation has a mantissa of length 53. For example, 2^53+1 =
     * 9007199254740993 is returned as double 9007199254740992.0.
     *
     * @return this {@code BigInteger} as a double value
     */
    @Override
    public double doubleValue() {
        return TConversion.bigInteger2Double(this);
    }

    /**
     * Compares this {@code BigInteger} with {@code val}. Returns one of the
     * three values 1, 0, or -1.
     *
     * @param val
     *            value to be compared with {@code this}.
     * @return {@code 1} if {@code this > val}, {@code -1} if {@code this < val}
     *         , {@code 0} if {@code this == val}.
     * @throws NullPointerException
     *             if {@code val == null}.
     */
    @Override
    public int compareTo(TBigInteger val) {
        if (sign > val.sign) {
            return GREATER;
        }
        if (sign < val.sign) {
            return LESS;
        }
        if (numberLength > val.numberLength) {
            return sign;
        }
        if (numberLength < val.numberLength) {
            return -val.sign;
        }
        // Equal sign and equal numberLength
        return sign * TElementary.compareArrays(digits, val.digits, numberLength);
    }

    /**
     * Returns the minimum of this {@code BigInteger} and {@code val}.
     *
     * @param val
     *            value to be used to compute the minimum with {@code this}.
     * @return {@code min(this, val)}.
     * @throws NullPointerException
     *             if {@code val == null}.
     */
    public TBigInteger min(TBigInteger val) {
        return (this.compareTo(val) == LESS) ? this : val;
    }

    /**
     * Returns the maximum of this {@code BigInteger} and {@code val}.
     *
     * @param val
     *            value to be used to compute the maximum with {@code this}
     * @return {@code max(this, val)}
     * @throws NullPointerException
     *             if {@code val == null}
     */
    public TBigInteger max(TBigInteger val) {
        return this.compareTo(val) == GREATER ? this : val;
    }

    /**
     * Returns a hash code for this {@code BigInteger}.
     *
     * @return hash code for {@code this}.
     */
    @Override
    public int hashCode() {
        if (hashCode != 0) {
            return hashCode;
        }
        for (int i = 0; i < digits.length; i++) {
            hashCode = hashCode * 33 + digits[i];
        }
        hashCode = hashCode * sign;
        return hashCode;
    }

    /**
     * Returns {@code true} if {@code x} is a BigInteger instance and if this
     * instance is equal to this {@code BigInteger}.
     *
     * @param x
     *            object to be compared with {@code this}.
     * @return true if {@code x} is a BigInteger and {@code this == x},
     *         {@code false} otherwise.
     */
    @Override
    public boolean equals(Object x) {
        if (this == x) {
            return true;
        }
        if (x instanceof TBigInteger) {
            TBigInteger x1 = (TBigInteger) x;
            return sign == x1.sign && numberLength == x1.numberLength && equalsArrays(x1.digits);
        }
        return false;
    }

    boolean equalsArrays(final int[] b) {
        int i;
        for (i = numberLength - 1; (i >= 0) && (digits[i] == b[i]); i--) {
            // Empty
        }
        return i < 0;
    }

    /**
     * Returns a string representation of this {@code BigInteger} in decimal
     * form.
     *
     * @return a string representation of {@code this} in decimal form.
     */
    @Override
    public String toString() {
        return TConversion.toDecimalScaledString(this, 0);
    }

    /**
     * Returns a string containing a string representation of this
     * {@code BigInteger} with base radix. If
     * {@code radix < Character.MIN_RADIX} or
     * {@code radix > Character.MAX_RADIX} then a decimal representation is
     * returned. The characters of the string representation are generated with
     * method {@code Character.forDigit}.
     *
     * @param radix
     *            base to be used for the string representation.
     * @return a string representation of this with radix 10.
     */
    public String toString(int radix) {
        return TConversion.bigInteger2String(this, radix);
    }

    /**
     * Returns a new {@code BigInteger} whose value is greatest common divisor
     * of {@code this} and {@code val}. If {@code this==0} and {@code val==0}
     * then zero is returned, otherwise the result is positive.
     *
     * @param val
     *            value with which the greatest common divisor is computed.
     * @return {@code gcd(this, val)}.
     * @throws NullPointerException
     *             if {@code val == null}.
     */
    public TBigInteger gcd(TBigInteger val) {
        TBigInteger val1 = this.abs();
        TBigInteger val2 = val.abs();
        // To avoid a possible division by zero
        if (val1.signum() == 0) {
            return val2;
        } else if (val2.signum() == 0) {
            return val1;
        }

        // Optimization for small operands
        // (op2.bitLength() < 64) and (op1.bitLength() < 64)
        if (((val1.numberLength == 1) || ((val1.numberLength == 2) && (val1.digits[1] > 0)))
                && (val2.numberLength == 1 || (val2.numberLength == 2 && val2.digits[1] > 0))) {
            return TBigInteger.valueOf(TDivision.gcdBinary(val1.longValue(), val2.longValue()));
        }

        return TDivision.gcdBinary(val1.copy(), val2.copy());

    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this * val}.
     *
     * @param val
     *            value to be multiplied with {@code this}.
     * @return {@code this * val}.
     * @throws NullPointerException
     *             if {@code val == null}.
     */
    public TBigInteger multiply(TBigInteger val) {
        // This let us to throw NullPointerException when val == null
        if (val.sign == 0) {
            return ZERO;
        }
        if (sign == 0) {
            return ZERO;
        }
        return TMultiplication.multiply(this, val);
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this ^ exp}.
     *
     * @param exp
     *            exponent to which {@code this} is raised.
     * @return {@code this ^ exp}.
     * @throws ArithmeticException
     *             if {@code exp < 0}.
     */
    public TBigInteger pow(int exp) {
        if (exp < 0) {
            throw new ArithmeticException("Negative exponent");
        }
        if (exp == 0) {
            return ONE;
        } else if (exp == 1 || equals(ONE) || equals(ZERO)) {
            return this;
        }

        // if even take out 2^x factor which we can
        // calculate by shifting.
        if (!testBit(0)) {
            int x = 1;
            while (!testBit(x)) {
                x++;
            }
            return getPowerOfTwo(x * exp).multiply(this.shiftRight(x).pow(exp));
        }
        return TMultiplication.pow(this, exp);
    }

    /**
     * Returns a new {@code BigInteger} whose value is the biggest integer
     * {@code n} such that {@code n * n <= this}.
     *
     * @implNote This implementation follows the ideas in Henry S. Warren, Jr.,
     * Hacker's Delight (2nd ed.) (Addison Wesley, 2013), 279-282.
     *
     * @return {@code floor(sqrt(this))}
     * @throws ArithmeticException if {@code this} is negative.
     */
    public TBigInteger sqrt() {
        if (sign < 0) {
            throw new ArithmeticException("Negative BigInteger");
        }

        // Trivial cases
        if (equals(ZERO)) {
            return ZERO;
        } else if (compareTo(valueOf(4)) < 0) {
            return ONE;
        }

        // BigInteger fits into long, so do calculation directly
        if (bitLength() < 64) {
            // Estimate using existing sqrt implementation for double
            long val = longValueExact();
            long candidate = (long) Math.floor(Math.sqrt(val));

            // Improve estimate using Newton's method
            do {
                long next = (candidate + val / candidate) >> 1;
                if (next >= candidate) {
                    // found convergence candidate if stopped to decrease
                    return valueOf(candidate);
                }

                candidate = next;
            } while (true);
        }

        // Shift BigInteger into long range to use existing sqrt implementation
        // and then shift back into the initial range for a rough estimate

        long shiftCount = bitLength() - 63;
        if (shiftCount % 2 == 1) {
            shiftCount += 1;
        }

        if ((shiftCount & 0xFFFFFFFF00000000L) > 0) {
            throw new ArithmeticException("integer overflow");
        }

        double shiftedVal = shiftRight((int) shiftCount).doubleValue();
        TBigInteger candidate = valueOf((long) Math.ceil(Math.sqrt(shiftedVal)));

        candidate = candidate.shiftLeft((int) shiftCount >> 1);

        // Improve estimate using Newton's method
        do {
            // next = (candidate + this/candidate) >> 1;
            TBigInteger next = candidate.add(this.divide(candidate)).shiftRight(1);
            if (next.compareTo(candidate) >= 0) {
                // found convergence candidate if stopped to decrease
                return candidate;
            }

            candidate = next;
        } while (true);
    }

    /**
     * Returns a {@code BigInteger} array which contains {@code this / divisor}
     * at index 0 and {@code this % divisor} at index 1.
     *
     * @param divisor
     *            value by which {@code this} is divided.
     * @return {@code [this / divisor, this % divisor]}.
     * @throws NullPointerException
     *             if {@code divisor == null}.
     * @throws ArithmeticException
     *             if {@code divisor == 0}.
     * @see #divide
     * @see #remainder
     */
    public TBigInteger[] divideAndRemainder(TBigInteger divisor) {
        int divisorSign = divisor.sign;
        if (divisorSign == 0) {
            throw new ArithmeticException("BigInteger divide by zero");
        }
        int divisorLen = divisor.numberLength;
        int[] divisorDigits = divisor.digits;
        if (divisorLen == 1) {
            return TDivision.divideAndRemainderByInteger(this, divisorDigits[0], divisorSign);
        }
        // res[0] is a quotient and res[1] is a remainder:
        int[] thisDigits = digits;
        int thisLen = numberLength;
        int cmp = (thisLen != divisorLen) ? ((thisLen > divisorLen) ? 1 : -1) : TElementary.compareArrays(thisDigits,
                divisorDigits, thisLen);
        if (cmp < 0) {
            return new TBigInteger[] { ZERO, this };
        }
        int thisSign = sign;
        int quotientLength = thisLen - divisorLen + 1;
        int remainderLength = divisorLen;
        int quotientSign = thisSign == divisorSign ? 1 : -1;
        int[] quotientDigits = new int[quotientLength];
        int[] remainderDigits = TDivision.divide(quotientDigits, quotientLength, thisDigits, thisLen, divisorDigits,
                divisorLen);
        TBigInteger result0 = new TBigInteger(quotientSign, quotientLength, quotientDigits);
        TBigInteger result1 = new TBigInteger(thisSign, remainderLength, remainderDigits);
        result0.cutOffLeadingZeroes();
        result1.cutOffLeadingZeroes();
        return new TBigInteger[] { result0, result1 };
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this / divisor}.
     *
     * @param divisor
     *            value by which {@code this} is divided.
     * @return {@code this / divisor}.
     * @throws NullPointerException
     *             if {@code divisor == null}.
     * @throws ArithmeticException
     *             if {@code divisor == 0}.
     */
    public TBigInteger divide(TBigInteger divisor) {
        if (divisor.sign == 0) {
            throw new ArithmeticException("BigInteger divide by zero");
        }
        int divisorSign = divisor.sign;
        if (divisor.isOne()) {
            return divisor.sign > 0 ? this : this.negate();
        }
        int thisSign = sign;
        int thisLen = numberLength;
        int divisorLen = divisor.numberLength;
        if (thisLen + divisorLen == 2) {
            long val = (digits[0] & 0xFFFFFFFFL) / (divisor.digits[0] & 0xFFFFFFFFL);
            if (thisSign != divisorSign) {
                val = -val;
            }
            return valueOf(val);
        }
        int cmp = thisLen != divisorLen ? (thisLen > divisorLen ? 1 : -1) : TElementary.compareArrays(digits,
                divisor.digits, thisLen);
        if (cmp == EQUALS) {
            return thisSign == divisorSign ? ONE : MINUS_ONE;
        }
        if (cmp == LESS) {
            return ZERO;
        }
        int resLength = thisLen - divisorLen + 1;
        int[] resDigits = new int[resLength];
        int resSign = thisSign == divisorSign ? 1 : -1;
        if (divisorLen == 1) {
            TDivision.divideArrayByInt(resDigits, digits, thisLen, divisor.digits[0]);
        } else {
            TDivision.divide(resDigits, resLength, digits, thisLen, divisor.digits, divisorLen);
        }
        TBigInteger result = new TBigInteger(resSign, resLength, resDigits);
        result.cutOffLeadingZeroes();
        return result;
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this % divisor}.
     * Regarding signs this methods has the same behavior as the % operator on
     * int's, i.e. the sign of the remainder is the same as the sign of this.
     *
     * @param divisor
     *            value by which {@code this} is divided.
     * @return {@code this % divisor}.
     * @throws NullPointerException
     *             if {@code divisor == null}.
     * @throws ArithmeticException
     *             if {@code divisor == 0}.
     */
    public TBigInteger remainder(TBigInteger divisor) {
        if (divisor.sign == 0) {
            throw new ArithmeticException("BigInteger divide by zero");
        }
        int thisLen = numberLength;
        int divisorLen = divisor.numberLength;
        if (((thisLen != divisorLen) ? ((thisLen > divisorLen) ? 1 : -1) : TElementary.compareArrays(digits,
                divisor.digits, thisLen)) == LESS) {
            return this;
        }
        int resLength = divisorLen;
        int[] resDigits = new int[resLength];
        if (resLength == 1) {
            resDigits[0] = TDivision.remainderArrayByInt(digits, thisLen, divisor.digits[0]);
        } else {
            int qLen = thisLen - divisorLen + 1;
            resDigits = TDivision.divide(null, qLen, digits, thisLen, divisor.digits, divisorLen);
        }
        TBigInteger result = new TBigInteger(sign, resLength, resDigits);
        result.cutOffLeadingZeroes();
        return result;
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code 1/this mod m}. The
     * modulus {@code m} must be positive. The result is guaranteed to be in the
     * interval {@code [0, m)} (0 inclusive, m exclusive). If {@code this} is
     * not relatively prime to m, then an exception is thrown.
     *
     * @param m
     *            the modulus.
     * @return {@code 1/this mod m}.
     * @throws NullPointerException
     *             if {@code m == null}
     * @throws ArithmeticException
     *             if {@code m < 0 or} if {@code this} is not relatively prime
     *             to {@code m}
     */
    public TBigInteger modInverse(TBigInteger m) {
        if (m.sign <= 0) {
            throw new ArithmeticException("BigInteger: modulus not positive");
        }
        // If both are even, no inverse exists
        if (!(testBit(0) || m.testBit(0))) {
            throw new ArithmeticException("BigInteger not invertible.");
        }
        if (m.isOne()) {
            return ZERO;
        }

        // From now on: (m > 1)
        TBigInteger res = TDivision.modInverseMontgomery(abs().mod(m), m);
        if (res.sign == 0) {
            throw new ArithmeticException("BigInteger not invertible.");
        }

        res = sign < 0 ? m.subtract(res) : res;
        return res;

    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this^exponent mod
     * m}. The modulus {@code m} must be positive. The result is guaranteed to
     * be in the interval {@code [0, m)} (0 inclusive, m exclusive). If the
     * exponent is negative, then {@code this.modInverse(m)^(-exponent) mod m)}
     * is computed. The inverse of this only exists if {@code this} is
     * relatively prime to m, otherwise an exception is thrown.
     *
     * @param exponent
     *            the exponent.
     * @param m
     *            the modulus.
     * @return {@code this^exponent mod val}.
     * @throws NullPointerException
     *             if {@code m == null} or {@code exponent == null}.
     * @throws ArithmeticException
     *             if {@code m < 0} or if {@code exponent<0} and this is not
     *             relatively prime to {@code m}.
     */
    public TBigInteger modPow(TBigInteger exponent, TBigInteger m) {
        if (m.sign <= 0) {
            throw new ArithmeticException("BigInteger: modulus not positive");
        }
        TBigInteger base = this;

        if (m.isOne() | (exponent.sign > 0 & base.sign == 0)) {
            return TBigInteger.ZERO;
        }
        if (exponent.sign == 0) {
            return TBigInteger.ONE.mod(m);
        }
        if (exponent.sign < 0) {
            base = modInverse(m);
            exponent = exponent.negate();
        }
        // From now on: (m > 0) and (exponent >= 0)
        TBigInteger res = (m.testBit(0)) ? TDivision.oddModPow(base.abs(), exponent, m) : TDivision.evenModPow(
                base.abs(), exponent, m);
        if ((base.sign < 0) && exponent.testBit(0)) {
            // -b^e mod m == ((-1 mod m) * (b^e mod m)) mod m
            res = m.subtract(TBigInteger.ONE).multiply(res).mod(m);
        }
        // else exponent is even, so base^exp is positive
        return res;
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this mod m}. The
     * modulus {@code m} must be positive. The result is guaranteed to be in the
     * interval {@code [0, m)} (0 inclusive, m exclusive). The behavior of this
     * function is not equivalent to the behavior of the % operator defined for
     * the built-in {@code int}'s.
     *
     * @param m
     *            the modulus.
     * @return {@code this mod m}.
     * @throws NullPointerException
     *             if {@code m == null}.
     * @throws ArithmeticException
     *             if {@code m < 0}.
     */
    public TBigInteger mod(TBigInteger m) {
        if (m.sign <= 0) {
            throw new ArithmeticException("BigInteger: modulus not positive");
        }
        TBigInteger rem = remainder(m);
        return rem.sign < 0 ? rem.add(m) : rem;
    }

    /**
     * Tests whether this {@code BigInteger} is probably prime. If {@code true}
     * is returned, then this is prime with a probability beyond
     * (1-1/2^certainty). If {@code false} is returned, then this is definitely
     * composite. If the argument {@code certainty} <= 0, then this method
     * returns true.
     *
     * @param certainty
     *            tolerated primality uncertainty.
     * @return {@code true}, if {@code this} is probably prime, {@code false}
     *         otherwise.
     */
    public boolean isProbablePrime(int certainty) {
        return TPrimality.isProbablePrime(abs(), certainty);
    }

    /**
     * Returns the smallest integer x > {@code this} which is probably prime as
     * a {@code BigInteger} instance. The probability that the returned
     * {@code BigInteger} is prime is beyond (1-1/2^80).
     *
     * @return smallest integer > {@code this} which is robably prime.
     * @throws ArithmeticException
     *             if {@code this < 0}.
     */
    public TBigInteger nextProbablePrime() {
        if (sign < 0) {
            throw new ArithmeticException("start < 0: " + this);
        }
        return TPrimality.nextProbablePrime(this);
    }

    /**
     * Returns a random positive {@code BigInteger} instance in the range [0,
     * 2^(bitLength)-1] which is probably prime. The probability that the
     * returned {@code BigInteger} is prime is beyond (1-1/2^80).
     * <p>
     * <b>Implementation Note:</b> Currently {@code rnd} is ignored.
     *
     * @param bitLength
     *            length of the new {@code BigInteger} in bits.
     * @param rnd
     *            random generator used to generate the new {@code BigInteger}.
     * @return probably prime random {@code BigInteger} instance.
     * @throws IllegalArgumentException
     *             if {@code bitLength < 2}.
     */
    public static TBigInteger probablePrime(int bitLength, Random rnd) {
        return new TBigInteger(bitLength, 100, rnd);
    }

    /* Private Methods */

    /** Decreases {@code numberLength} if there are zero high elements. */
    final void cutOffLeadingZeroes() {
        while ((numberLength > 0) && (digits[--numberLength] == 0)) {
            // Empty
        }
        if (digits[numberLength++] == 0) {
            sign = 0;
        }
    }

    /** Tests if {@code this.abs()} is equals to {@code ONE} */
    boolean isOne() {
        return numberLength == 1 && digits[0] == 1;
    }

    /**
     * Puts a big-endian byte array into a little-endian int array.
     */
    private void putBytesPositiveToIntegers(byte[] byteValues) {
        int bytesLen = byteValues.length;
        int highBytes = bytesLen & 3;
        numberLength = (bytesLen >> 2) + ((highBytes == 0) ? 0 : 1);
        digits = new int[numberLength];
        int i = 0;
        // Put bytes to the int array starting from the end of the byte array
        while (bytesLen > highBytes) {
            digits[i++] = (byteValues[--bytesLen] & 0xFF) | (byteValues[--bytesLen] & 0xFF) << 8
                    | (byteValues[--bytesLen] & 0xFF) << 16 | (byteValues[--bytesLen] & 0xFF) << 24;
        }
        // Put the first bytes in the highest element of the int array
        for (int j = 0; j < bytesLen; j++) {
            digits[i] = (digits[i] << 8) | (byteValues[j] & 0xFF);
        }
    }

    /**
     * Puts a big-endian byte array into a little-endian applying two
     * complement.
     */
    private void putBytesNegativeToIntegers(byte[] byteValues) {
        int bytesLen = byteValues.length;
        int highBytes = bytesLen & 3;
        numberLength = (bytesLen >> 2) + ((highBytes == 0) ? 0 : 1);
        digits = new int[numberLength];
        int i = 0;
        // Setting the sign
        digits[numberLength - 1] = -1;
        // Put bytes to the int array starting from the end of the byte array
        while (bytesLen > highBytes) {
            digits[i] = (byteValues[--bytesLen] & 0xFF) | (byteValues[--bytesLen] & 0xFF) << 8
                    | (byteValues[--bytesLen] & 0xFF) << 16 | (byteValues[--bytesLen] & 0xFF) << 24;
            if (digits[i] != 0) {
                digits[i] = -digits[i];
                firstNonzeroDigit = i;
                i++;
                while (bytesLen > highBytes) {
                    digits[i] = (byteValues[--bytesLen] & 0xFF) | (byteValues[--bytesLen] & 0xFF) << 8
                            | (byteValues[--bytesLen] & 0xFF) << 16 | (byteValues[--bytesLen] & 0xFF) << 24;
                    digits[i] = ~digits[i];
                    i++;
                }
                break;
            }
            i++;
        }
        if (highBytes != 0) {
            // Put the first bytes in the highest element of the int array
            if (firstNonzeroDigit != -2) {
                for (int j = 0; j < bytesLen; j++) {
                    digits[i] = (digits[i] << 8) | (byteValues[j] & 0xFF);
                }
                digits[i] = ~digits[i];
            } else {
                for (int j = 0; j < bytesLen; j++) {
                    digits[i] = (digits[i] << 8) | (byteValues[j] & 0xFF);
                }
                digits[i] = -digits[i];
            }
        }
    }

    int getFirstNonzeroDigit() {
        if (firstNonzeroDigit == -2) {
            int i;
            if (this.sign == 0) {
                i = -1;
            } else {
                for (i = 0; digits[i] == 0; i++) {
                    // Empty
                }
            }
            firstNonzeroDigit = i;
        }
        return firstNonzeroDigit;
    }

    /*
     * Returns a copy of the current instance to achieve immutability
     */
    TBigInteger copy() {
        int[] copyDigits = new int[numberLength];
        System.arraycopy(digits, 0, copyDigits, 0, numberLength);
        return new TBigInteger(sign, numberLength, copyDigits);
    }

    void unCache() {
        firstNonzeroDigit = -2;
    }

    static TBigInteger getPowerOfTwo(int exp) {
        if (exp < TWO_POWS.length) {
            return TWO_POWS[exp];
        }
        int intCount = exp >> 5;
        int bitN = exp & 31;
        int[] resDigits = new int[intCount + 1];
        resDigits[intCount] = 1 << bitN;
        return new TBigInteger(1, intCount + 1, resDigits);
    }
}
