/*
 *  Copyright 2017 Alexey Andreev.
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
 * Represents word boundary, checks current character and previous one if
 * different types returns true;
 *
 * @author Nikolay A. Kuznetsov
 */
class TWordBoundary extends TAbstractSet {

    boolean positive;

    public TWordBoundary(boolean positive) {
        this.positive = positive;
    }

    @Override
    public int matches(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        boolean left;
        boolean right;

        char ch1 = stringIndex >= matchResult.getRightBound() ? ' ' : testString.charAt(stringIndex);
        char ch2 = stringIndex == 0 ? ' ' : testString.charAt(stringIndex - 1);

        int leftBound = matchResult.hasTransparentBounds() ? 0 : matchResult.getLeftBound();
        left = (ch1 == ' ') || isSpace(ch1, stringIndex, leftBound, testString);
        right = (ch2 == ' ') || isSpace(ch2, stringIndex - 1, leftBound, testString);
        return ((left ^ right) ^ positive) ? -1 : next.matches(stringIndex, testString, matchResult);
    }

    /**
     * Returns false, because word boundary does not consumes any characters and
     * do not move string index.
     */
    @Override
    public boolean hasConsumed(TMatchResultImpl matchResult) {
        // only checks boundary, do not consumes characters
        return false;
    }

    @Override
    protected String getName() {
        return "WordBoundary"; //$NON-NLS-1$
    }

    private boolean isSpace(char ch, int index, int leftBound, CharSequence testString) {
        if (Character.isLetterOrDigit(ch) || ch == '_') {
            return false;
        }
        if (Character.getType(ch) == Character.NON_SPACING_MARK) {
            for (; --index >= leftBound;) {
                ch = testString.charAt(index);
                if (Character.isLetterOrDigit(ch)) {
                    return false;
                }
                if (Character.getType(ch) != Character.NON_SPACING_MARK) {
                    return true;
                }
            }
        }
        return true;
    }
}
