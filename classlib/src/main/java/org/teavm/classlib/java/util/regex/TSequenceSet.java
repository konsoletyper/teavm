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
 * This class represents nodes constructed with character sequences. For
 * example, lets consider regular expression: ".*word.*". During regular
 * expression compilation phase character sequence w-o-r-d, will be represented
 * with single node for the entire word.
 *
 * During the match phase, Moyer-Moore algorithm will be used for fast
 * searching.
 *
 * Please follow the next link for more details about mentioned algorithm:
 * http://portal.acm.org/citation.cfm?id=359859
 *
 * @author Nikolay A. Kuznetsov
 */
class TSequenceSet extends TLeafSet {

    private String string;

    private IntHash leftToRight;

    private IntHash rightToLeft;

    TSequenceSet(StringBuffer substring) {
        this.string = substring.toString();
        charCount = substring.length();

        leftToRight = new IntHash(charCount);
        rightToLeft = new IntHash(charCount);
        for (int j = 0; j < charCount - 1; j++) {
            leftToRight.put(string.charAt(j), charCount - j - 1);
            rightToLeft.put(string.charAt(charCount - j - 1), charCount - j - 1);
        }
    }

    @Override
    public int accepts(int strIndex, CharSequence testString) {
        return startsWith(testString, strIndex) ? charCount : -1;
    }

    @Override
    public int find(int strIndex, CharSequence testString,
            TMatchResultImpl matchResult) {

        int strLength = matchResult.getRightBound();

        while (strIndex <= strLength) {
            strIndex = indexOf(testString, strIndex, strLength);

            if (strIndex < 0) {
                return -1;
            }
            if (next.matches(strIndex + charCount, testString, matchResult) >= 0) {
                return strIndex;
            }

            strIndex++;
        }

        return -1;
    }

    @Override
    public int findBack(int strIndex, int lastIndex, CharSequence testString,
            TMatchResultImpl matchResult) {

        while (lastIndex >= strIndex) {
            lastIndex = lastIndexOf(testString, strIndex, lastIndex);

            if (lastIndex < 0) {
                return -1;
            }
            if (next.matches(lastIndex + charCount, testString, matchResult) >= 0) {
                return lastIndex;
            }

            lastIndex--;
        }

        return -1;
    }

    @Override
    public String getName() {
        return "sequence: " + string; //$NON-NLS-1$
    }

    @Override
    public boolean first(TAbstractSet set) {
        if (set instanceof TCharSet) {
            return ((TCharSet) set).getChar() == string.charAt(0);
        } else if (set instanceof TRangeSet) {
            return ((TRangeSet) set).accepts(0, string.substring(0, 1)) > 0;
        } else if (set instanceof TSupplRangeSet) {
            return ((TSupplRangeSet) set).contains(string.charAt(0))
                    || ((string.length() > 1) && ((TSupplRangeSet) set).contains(Character
                           .toCodePoint(string.charAt(0), string.charAt(1))));
        } else if (set instanceof TSupplCharSet) {
            return (string.length() > 1)
                    && ((TSupplCharSet) set).getCodePoint() == Character.toCodePoint(string.charAt(0),
                            string.charAt(1));
        }

        return true;
    }

    protected int indexOf(CharSequence str, int from, int to) {
        int last = string.charAt(charCount - 1);
        int i = from;

        while (i <= to - charCount) {
            char ch = str.charAt(i + charCount - 1);
            if (ch == last && startsWith(str, i)) {
                return i;
            }

            i += leftToRight.get(ch);
        }
        return -1;
    }

    protected int lastIndexOf(CharSequence str, int to, int from) {
        int first = string.charAt(0);
        int size = str.length();
        int delta = size - from - charCount;
        int i = delta > 0 ? from : from + delta;

        while (i >= to) {
            char ch = str.charAt(i);
            if (ch == first && startsWith(str, i)) {
                return i;
            }

            i -= rightToLeft.get(ch);
        }
        return -1;
    }

    protected boolean startsWith(CharSequence str, int from) {
        for (int i = 0; i < charCount; i++) {
            if (str.charAt(i + from) != string.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    static class IntHash {
        int[] table;
        int[] values;

        int mask;

        int size; // <-maximum shift

        public IntHash(int size) {
            while (size >= mask) {
                mask = (mask << 1) | 1;
            }
            mask = (mask << 1) | 1;
            table = new int[mask + 1];
            values = new int[mask + 1];
            this.size = size;
        }

        public void put(int key, int value) {
            int i = 0;
            int hashCode = key & mask;

            for (;;) {
                if (table[hashCode] == 0 // empty
                        || table[hashCode] == key) { // rewrite
                    table[hashCode] = key;
                    values[hashCode] = value;
                    return;
                }
                i++;
                i &= mask;

                hashCode += i;
                hashCode &= mask;
            }
        }

        public int get(int key) {

            int hashCode = key & mask;
            int i = 0;
            int storedKey;

            for (;;) {
                storedKey = table[hashCode];

                if (storedKey == 0) { // empty
                    return size;
                }

                if (storedKey == key) {
                    return values[hashCode];
                }

                i++;
                i &= mask;

                hashCode += i;
                hashCode &= mask;
            }
        }
    }
}
