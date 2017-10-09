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
 * Special node for ".*" construction. The main idea here is to find line
 * terminator and try to find the rest of the construction from this point.
 *
 * @author Nikolay A. Kuznetsov
 */
class TDotQuantifierSet extends TQuantifierSet {

    TAbstractLineTerminator lt;

    public TDotQuantifierSet(TAbstractSet innerSet, TAbstractSet next, int type, TAbstractLineTerminator lt) {
        super(innerSet, next, type);
        this.lt = lt;
    }

    @Override
    public int matches(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {

        int strLength = matchResult.getRightBound();

        int startSearch = /* testString.toString().indexOf('\n', stringIndex); */
        findLineTerminator(stringIndex, strLength, testString);

        if (startSearch < 0) {
            startSearch = strLength;
        }

        if (startSearch <= stringIndex) {
            return next.matches(stringIndex, testString, matchResult);
        }
        return next.findBack(stringIndex, startSearch, testString, matchResult);
    }

    @Override
    public int find(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        // String testStr = testString.toString();
        int strLength = matchResult.getRightBound();
        // 1. skip line terminators ???
        // //
        // we don't skip line terminators here, but return zero match instead
        // //

        // 2. find first occurrence of the searched pattern
        // //
        int res = next.find(stringIndex, testString, matchResult);

        // 3. Check if we have other occurrences till the end of line
        // (because .* is greedy and we need last one)
        // //
        if (res >= 0) {
            int nextSearch = findLineTerminator(res, strLength, testString);
            // testStr.indexOf('\n', res);
            if (nextSearch < 0) {
                nextSearch = strLength;
            }
            nextSearch = next.findBack(res, nextSearch, testString, matchResult);
            res = (res < nextSearch) ? nextSearch : res;
        } else {
            return -1;
        }

        // 4. find left boundary of this search
        // //
        int leftBound = (res > 0) ? findBackLineTerminator(stringIndex, res - 1, testString)/*
                                                                                             * testStr
                                                                                             * .
                                                                                             * lastIndexOf
                                                                                             * (
                                                                                             * '\n'
                                                                                             * ,
                                                                                             * res
                                                                                             * -
                                                                                             * 1
                                                                                             * )
                                                                                             */
        : (res == 0) ? 0 : -1;
        res = (leftBound >= stringIndex) ? ((leftBound < res) ? leftBound + 1 : leftBound) : stringIndex;

        return res;
    }

    /*
     * All line terminators are from Basic Multilingual Pane
     */
    private int findLineTerminator(int from, int to, CharSequence testString) {
        for (int i = from; i < to; i++) {
            if (lt.isLineTerminator(testString.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private int findBackLineTerminator(int from, int to, CharSequence testString) {
        for (int i = to; i >= from; i--) {
            if (lt.isLineTerminator(testString.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected String getName() {
        return "<DotQuant>";
    }
}
