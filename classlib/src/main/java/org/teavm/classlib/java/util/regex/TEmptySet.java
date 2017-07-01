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
 * Valid constant zero character match.
 *
 * @author Nikolay A. Kuznetsov
 */
class TEmptySet extends TLeafSet {
    public TEmptySet(TAbstractSet next) {
        super(next);
        charCount = 0;
    }

    @Override
    public int accepts(int stringIndex, CharSequence testString) {
        return 0;
    }

    @Override
    public int find(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        int strLength = matchResult.getRightBound();
        int startStr = matchResult.getLeftBound();

        while (stringIndex <= strLength) {

            // check for supplementary codepoints
            if (stringIndex < strLength) {
                char low = testString.charAt(stringIndex);

                if (Character.isLowSurrogate(low)) {

                    if (stringIndex > startStr) {
                        char high = testString.charAt(stringIndex - 1);
                        if (Character.isHighSurrogate(high)) {
                            stringIndex++;
                            continue;
                        }
                    }
                }
            }

            if (next.matches(stringIndex, testString, matchResult) >= 0) {
                return stringIndex;
            }
            stringIndex++;
        }

        return -1;
    }

    @Override
    public int findBack(int stringIndex, int startSearch, CharSequence testString, TMatchResultImpl matchResult) {
        int strLength = matchResult.getRightBound();
        int startStr = matchResult.getLeftBound();

        while (startSearch >= stringIndex) {

            // check for supplementary codepoints
            if (startSearch < strLength) {
                char low = testString.charAt(startSearch);

                if (Character.isLowSurrogate(low)) {

                    if (startSearch > startStr) {
                        char high = testString.charAt(startSearch - 1);
                        if (Character.isHighSurrogate(high)) {
                            startSearch--;
                            continue;
                        }
                    }
                }
            }

            if (next.matches(startSearch, testString, matchResult) >= 0) {
                return startSearch;
            }
            startSearch--;
        }

        return -1;
    }

    @Override
    protected String getName() {
        return "<Empty set>";
    }

    @Override
    public boolean hasConsumed(TMatchResultImpl mr) {
        return false;
    }
}
