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
 * This class represents ASCII case insensitive character sequences.
 *
 * @author Nikolay A. Kuznetsov
 */
class TCISequenceSet extends TLeafSet {
    private String string;

    TCISequenceSet(StringBuffer substring) {
        this.string = substring.toString();
        this.charCount = substring.length();
    }

    @Override
    public int accepts(int strIndex, CharSequence testString) {
        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) != testString.charAt(strIndex + i)
                    && TPattern.getSupplement(string.charAt(i)) != testString.charAt(strIndex + i)) {
                return -1;
            }
        }

        return string.length();

    }

    @Override
    public String getName() {
        return "CI sequence: " + string;
    }
}
