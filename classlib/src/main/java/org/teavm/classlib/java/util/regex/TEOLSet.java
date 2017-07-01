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
final class TEOLSet extends TAbstractSet {
    private int consCounter;

    public TEOLSet(int counter) {
        this.consCounter = counter;
    }

    @Override
    public int matches(int strIndex, CharSequence testString, TMatchResultImpl matchResult) {
        int rightBound = matchResult.hasAnchoringBounds() ? matchResult.getRightBound() : testString.length();

        if (strIndex >= rightBound) {
            matchResult.setConsumed(consCounter, 0);
            return next.matches(strIndex, testString, matchResult);
        }

        // check final line terminator;
        if ((rightBound - strIndex) == 2 && testString.charAt(strIndex) == '\r'
                && testString.charAt(strIndex + 1) == '\n') {
            matchResult.setConsumed(consCounter, 0);
            return next.matches(strIndex, testString, matchResult);
        }
        char ch;

        if ((rightBound - strIndex) == 1) {
            ch = testString.charAt(strIndex);
            if (ch == '\n' || ch == '\r' || ch == '\u0085' || (ch | 1) == '\u2029') {
                matchResult.setConsumed(consCounter, 0);
                return next.matches(strIndex, testString, matchResult);
            }
        }

        return -1;
    }

    @Override
    public boolean hasConsumed(TMatchResultImpl matchResult) {
        boolean res = matchResult.getConsumed(consCounter) != 0;
        matchResult.setConsumed(consCounter, -1);
        return res;
    }

    @Override
    protected String getName() {
        return "<EOL>";
    }
}
