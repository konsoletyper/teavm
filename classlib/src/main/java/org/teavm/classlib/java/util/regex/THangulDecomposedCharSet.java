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

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teavm.classlib.java.util.regex;

/**
 * Represents canonical decomposition of Hangul syllable. Is used when CANON_EQ
 * flag of Pattern class is specified.
 */
class THangulDecomposedCharSet extends TJointSet {

    /**
     * Decomposed Hangul syllable.
     */
    private char[] decomposedChar;

    /**
     * String representing syllable
     */
    private String decomposedCharUTF16;

    /**
     * Length of useful part of decomposedChar decomposedCharLength <=
     * decomposedChar.length
     */
    private int decomposedCharLength;

    public THangulDecomposedCharSet(char[] decomposedChar, int decomposedCharLength) {
        this.decomposedChar = decomposedChar;
        this.decomposedCharLength = decomposedCharLength;
    }

    /**
     * Returns the next.
     */
    @Override
    public TAbstractSet getNext() {
        return this.next;
    }

    /**
     * Sets next abstract set.
     *
     * @param next
     *            The next to set.
     */
    @Override
    public void setNext(TAbstractSet next) {
        this.next = next;
    }

    /**
     * Give string representation of this.
     *
     * @return - string representation.
     */
    private String getDecomposedChar() {
        if (decomposedCharUTF16 == null) {
            decomposedCharUTF16 = new String(decomposedChar);
        }
        return decomposedCharUTF16;
    }

    @Override
    protected String getName() {
        return "decomposed Hangul syllable:" + getDecomposedChar(); //$NON-NLS-1$
    }

    @Override
    public int matches(int strIndex, CharSequence testString, TMatchResultImpl matchResult) {

        /*
         * All decompositions for Hangul syllables have length that is less or
         * equal Lexer.MAX_DECOMPOSITION_LENGTH
         */
        int rightBound = matchResult.getRightBound();
        int syllIndex = 0;
        int[] decompSyllable = new int[TLexer.MAX_HANGUL_DECOMPOSITION_LENGTH];
        int[] decompCurSymb;
        char curSymb;

        /*
         * For details about Hangul composition and decomposition see
         * http://www.unicode.org/versions/Unicode4.0.0/ch03.pdf
         * "3.12 Conjoining Jamo Behavior"
         */
        int lIndex;
        int vIndex = -1;
        int tIndex = -1;

        if (strIndex >= rightBound) {
            return -1;
        }
        curSymb = testString.charAt(strIndex++);
        decompCurSymb = TLexer.getHangulDecomposition(curSymb);

        if (decompCurSymb == null) {

            /*
             * We deal with ordinary letter or sequence of jamos at strIndex at
             * testString.
             */
            decompSyllable[syllIndex++] = curSymb;
            lIndex = curSymb - TLexer.LBase;

            if ((lIndex < 0) || (lIndex >= TLexer.LCount)) {

                /*
                 * Ordinary letter, that doesn't match this
                 */
                return -1;
            }

            if (strIndex < rightBound) {
                curSymb = testString.charAt(strIndex);
                vIndex = curSymb - TLexer.VBase;
            }

            if ((vIndex < 0) || (vIndex >= TLexer.VCount)) {

                /*
                 * Single L jamo doesn't compose Hangul syllable, so doesn't
                 * match
                 */
                return -1;
            }
            strIndex++;
            decompSyllable[syllIndex++] = curSymb;

            if (strIndex < rightBound) {
                curSymb = testString.charAt(strIndex);
                tIndex = curSymb - TLexer.TBase;
            }

            if ((tIndex < 0) || (tIndex >= TLexer.TCount)) {

                /*
                 * We deal with LV syllable at testString, so compare it to this
                 */
                return decomposedCharLength == 2 && decompSyllable[0] == decomposedChar[0]
                        && decompSyllable[1] == decomposedChar[1]
                        ? next.matches(strIndex, testString, matchResult)
                        : -1;
            }
            strIndex++;
            decompSyllable[syllIndex++] = curSymb;

            /*
             * We deal with LVT syllable at testString, so compare it to this
             */
            return decomposedCharLength == 3 && decompSyllable[0] == decomposedChar[0]
                    && decompSyllable[1] == decomposedChar[1] && decompSyllable[2] == decomposedChar[2]
                    ? next.matches(strIndex, testString, matchResult)
                    : -1;
        } else {

            /*
             * We deal with Hangul syllable at strIndex at testString. So we
             * decomposed it to compare with this.
             */
            int i = 0;

            if (decompCurSymb.length != decomposedCharLength) {
                return -1;
            }

            for (; i < decomposedCharLength; i++) {
                if (decompCurSymb[i] != decomposedChar[i]) {
                    return -1;
                }
            }
            return next.matches(strIndex, testString, matchResult);
        }
    }

    @Override
    public boolean first(TAbstractSet set) {
        return !(set instanceof THangulDecomposedCharSet)
                || ((THangulDecomposedCharSet) set).getDecomposedChar().equals(getDecomposedChar());
    }

    @Override
    public boolean hasConsumed(TMatchResultImpl matchResult) {
        return true;
    }
}
