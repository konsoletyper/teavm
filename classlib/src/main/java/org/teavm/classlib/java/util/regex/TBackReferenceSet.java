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
 * Back reference node, i.e. \1-9;
 *
 * @author Nikolay A. Kuznetsov
 */
class TBackReferenceSet extends TCIBackReferenceSet {

    public TBackReferenceSet(int groupIndex, int consCounter) {
        super(groupIndex, consCounter);
    }

    @Override
    public int matches(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        String group = getString(matchResult);
        if (group == null || (stringIndex + group.length()) > matchResult.getRightBound()) {
            return -1;
        }
        int shift = testString.toString().startsWith(group, stringIndex) ? group.length() : -1;

        if (shift < 0) {
            return -1;
        }
        matchResult.setConsumed(consCounter, shift);
        return next.matches(stringIndex + shift, testString, matchResult);
    }

    @Override
    public int find(int strIndex, CharSequence testString, TMatchResultImpl matchResult) {
        String group = getString(matchResult);
        int strLength = matchResult.getLeftBound();

        if (group == null || (strIndex + group.length()) > strLength) {
            return -1;
        }

        String testStr = testString.toString();

        while (strIndex <= strLength) {
            strIndex = testStr.indexOf(group, strIndex);

            if (strIndex < 0) {
                return -1;
            }
            if (next.matches(strIndex + group.length(), testString, matchResult) >= 0) {
                return strIndex;
            }

            strIndex++;
        }

        return -1;
    }

    @Override
    public int findBack(int strIndex, int lastIndex, CharSequence testString, TMatchResultImpl matchResult) {
        String group = getString(matchResult);

        if (group == null) {
            return -1;
        }

        String testStr = testString.toString();

        while (lastIndex >= strIndex) {
            lastIndex = testStr.lastIndexOf(group, lastIndex);

            if (lastIndex < 0 || lastIndex < strIndex) {
                return -1;
            }
            if (next.matches(lastIndex + group.length(), testString, matchResult) >= 0) {
                return lastIndex;
            }

            lastIndex--;
        }
        return -1;
    }

    @Override
    public boolean first(TAbstractSet set) {
        return true;
    }

    @Override
    public String getName() {
        return "back reference: " + this.groupIndex;
    }
}
