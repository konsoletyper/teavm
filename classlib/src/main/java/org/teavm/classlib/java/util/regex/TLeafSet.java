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

/**
 * @author Nikolay A. Kuznetsov
 */
package org.teavm.classlib.java.util.regex;

/**
 * Base class for nodes representing leaf tokens of the RE, those who consume
 * characters without passing control to other nodes. Most of them consume
 * fixed number of characters, but some consume either one char or a surrogate pair;
 * such nodes override {@link #stepBack(int, int, CharSequence)}.
 *
 * @author Nikolay A. Kuznetsov
 */
abstract class TLeafSet extends TAbstractSet {

    protected int charCount = 1;

    public TLeafSet(TAbstractSet next) {
        super(next);
        setType(TAbstractSet.TYPE_LEAF);
    }

    public TLeafSet() {
    }

    /**
     * Checks whether this node matches at given index, without passing control to the next node.
     * Returns number of consumed characters or negative value if match fails. Leaves that look at
     * neighbouring characters (to tell a surrogate pair from a lone surrogate) take bounds of
     * the matched region from {@code matchResult}.
     */
    public abstract int accepts(int stringIndex, CharSequence testString, TMatchResultImpl matchResult);

    /**
     * Given that consecutive matches of this leaf started at {@code leftLimit} and ended at
     * {@code stringIndex}, returns the index where the last of these matches started.
     * Quantifiers use it to backtrack.
     */
    public int stepBack(int stringIndex, int leftLimit, CharSequence testString) {
        return stringIndex - charCount();
    }

    /**
     * Checks whether given index points to the low surrogate of a surrogate pair. Character classes
     * never match there, so that a match never starts in the middle of a code point.
     */
    static boolean isInsidePair(int stringIndex, CharSequence testString, int leftBound) {
        return stringIndex > leftBound && Character.isLowSurrogate(testString.charAt(stringIndex))
                && Character.isHighSurrogate(testString.charAt(stringIndex - 1));
    }

    /**
     * Implementation of {@link #stepBack(int, int, CharSequence)} for leaves that consume
     * either a single char or a surrogate pair, and never consume a part of a surrogate pair.
     */
    static int stepBackCodePoint(int stringIndex, int leftLimit, CharSequence testString) {
        if (stringIndex - 2 >= leftLimit && Character.isLowSurrogate(testString.charAt(stringIndex - 1))
                && Character.isHighSurrogate(testString.charAt(stringIndex - 2))) {
            return stringIndex - 2;
        }
        return stringIndex - 1;
    }

    /**
     * Checks if we can enter this state and pass the control to the next one.
     * Return positive value if match succeeds, negative otherwise.
     */
    @Override
    public int matches(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {

        if (stringIndex + charCount() > matchResult.getRightBound()) {
            matchResult.hitEnd = true;
            return -1;
        }

        int shift = accepts(stringIndex, testString, matchResult);
        if (shift < 0) {
            return -1;
        }

        return next.matches(stringIndex + shift, testString, matchResult);
    }

    /**
     * Returns number of characters this node consumes.
     *
     * @return number of characters this node consumes.
     */
    public int charCount() {
        return charCount;
    }

    @Override
    public boolean hasConsumed(TMatchResultImpl mr) {
        return true;
    }
}
