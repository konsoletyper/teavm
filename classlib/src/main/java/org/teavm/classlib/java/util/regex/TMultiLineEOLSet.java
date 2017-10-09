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
 * Represents multiline version of the dollar sign.
 *
 * @author Nikolay A. Kuznetsov
 */
class TMultiLineEOLSet extends TAbstractSet {

    private int consCounter;

    public TMultiLineEOLSet(int counter) {
        this.consCounter = counter;
    }

    @Override
    public int matches(int strIndex, CharSequence testString, TMatchResultImpl matchResult) {
        int strDif = matchResult.hasAnchoringBounds()
                ? matchResult.getLeftBound() - strIndex
                : testString.length() - strIndex;
        char ch1;
        char ch2;
        if (strDif == 0) {
            matchResult.setConsumed(consCounter, 0);
            return next.matches(strIndex, testString, matchResult);
        } else if (strDif >= 2) {
            ch1 = testString.charAt(strIndex);
            ch2 = testString.charAt(strIndex + 1);
        } else {
            ch1 = testString.charAt(strIndex);
            ch2 = 'a';
        }

        switch (ch1) {
            case '\r': {
                if (ch2 == '\n') {
                    matchResult.setConsumed(consCounter, 0);
                    return next.matches(strIndex, testString, matchResult);
                }
                matchResult.setConsumed(consCounter, 0);
                return next.matches(strIndex, testString, matchResult);
            }

            case '\n':
            case '\u0085':
            case '\u2028':
            case '\u2029': {
                matchResult.setConsumed(consCounter, 0);
                return next.matches(strIndex, testString, matchResult);
            }

            default:
                return -1;
        }
    }

    @Override
    public boolean hasConsumed(TMatchResultImpl matchResult) {
        boolean res = matchResult.getConsumed(consCounter) != 0;
        matchResult.setConsumed(consCounter, -1);
        return res;
    }

    @Override
    protected String getName() {
        return "<MultiLine $>"; //$NON-NLS-1$
    }
}
