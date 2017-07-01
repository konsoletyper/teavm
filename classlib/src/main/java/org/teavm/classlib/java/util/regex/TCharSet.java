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
 * Represents node accepting single character.
 *
 * @author Nikolay A. Kuznetsov
 */
class TCharSet extends TLeafSet {

    private char ch;

    public TCharSet(char ch) {
        this.ch = ch;
    }

    @Override
    public int charCount() {
        return 1;
    }

    @Override
    public int accepts(int strIndex, CharSequence testString) {
        return (this.ch == testString.charAt(strIndex)) ? 1 : -1;
    }

    @Override
    public int find(int strIndex, CharSequence testString, TMatchResultImpl matchResult) {
        if (testString instanceof String) {
            String testStr = (String) testString;
            int strLength = matchResult.getRightBound();

            while (strIndex < strLength) {
                strIndex = testStr.indexOf(ch, strIndex);
                if (strIndex < 0) {
                    return -1;
                }
                if (next.matches(strIndex + 1, testString, matchResult) >= 0) {
                    return strIndex;
                }
                strIndex++;
            }

            return -1;
        }

        return super.find(strIndex, testString, matchResult);
    }

    @Override
    public int findBack(int strIndex, int lastIndex, CharSequence testString, TMatchResultImpl matchResult) {
        if (testString instanceof String) {
            String testStr = (String) testString;

            while (lastIndex >= strIndex) {
                lastIndex = testStr.lastIndexOf(ch, lastIndex);
                if (lastIndex < 0 || lastIndex < strIndex) {
                    return -1;
                }

                if (next.matches(lastIndex + 1, testString, matchResult) >= 0) {
                    return lastIndex;
                }

                lastIndex--;
            }

            return -1;
        }

        return super.findBack(strIndex, lastIndex, testString, matchResult);
    }

    @Override
    protected String getName() {
        return "" + ch; //$NON-NLS-1$
    }

    protected char getChar() {
        return ch;
    }

    @Override
    public boolean first(TAbstractSet set) {
        if (set instanceof TCharSet) {
            return ((TCharSet) set).getChar() == ch;
        } else if (set instanceof TRangeSet) {
            return ((TRangeSet) set).accepts(0, Character.toString(ch)) > 0;
        } else if (set instanceof TSupplRangeSet) {
            return ((TSupplRangeSet) set).contains(ch);
        } else if (set instanceof TSupplCharSet) {
            return false;
        }

        return true;
    }
}