/*
 *  Copyright 2026 lemonlion.
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
package org.teavm.classlib.java.util.regex;

/**
 * Node that consumes a single code point, or a single char of a surrogate pair, and then passes control
 * to the next node. Unlike {@link TLeafSet}, such node may consume input in more than one way at the same
 * index: for example, a character class can match a high surrogate alone, and then the surrogate pair that
 * starts with it. {@link TAbstractSet#matches} of such node tries these ways in turn, passing control to the next
 * node after each of them. Such node always consumes input, its {@link TAbstractSet#hasConsumed} returns
 * {@code true}.
 *
 * <p>When such node is quantified, its next node is the quantifier, so matching recurses once per consumed
 * code point and long input overflows the stack. {@link TCodePointQuantifierSet} and
 * {@link TReluctantCodePointQuantifierSet} use this interface to try the same ways in the same order in a loop.
 */
interface TCodePointSet {
    /**
     * Returns the number of ways this node tries to consume input at a given index.
     */
    int ways();

    /**
     * Finds the first way of consuming input at the given index that matches, trying the ways in the order
     * {@link TAbstractSet#matches} tries them, from the given one on. Does not pass control to the next node, but
     * otherwise has the same effect on {@code matchResult} as {@code matches} has when it tries these ways.
     *
     * @param way the number of the way to start from; ways are numbered from 0.
     * @return {@code -1} if none of these ways matches, otherwise {@code 4 * w + n}, where {@code w} is the number
     * of the way that matches and {@code n} is the number of chars it consumes, 1 or 2.
     */
    int consume(int way, int stringIndex, CharSequence testString, TMatchResultImpl matchResult);
}
