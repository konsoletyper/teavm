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
 * Reluctant version of composite (i.e. {n,m}) quantifier node over group.
 *
 * @author Nikolay A. Kuznetsov
 */
class TRelCompositeGroupQuantifierSet extends TCompositeGroupQuantifierSet {

    public TRelCompositeGroupQuantifierSet(TQuantifier quant, TAbstractSet innerSet, TAbstractSet next, int type,
            int setCounter) {
        super(quant, innerSet, next, type, setCounter);
    }

    @Override
    public int matches(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        int enterCounter = matchResult.getEnterCounter(setCounter);

        if (!innerSet.hasConsumed(matchResult)) {
            return next.matches(stringIndex, testString, matchResult);
        }

        // can't go inner set;
        if (enterCounter >= quantifier.max()) {
            matchResult.setEnterCounter(setCounter, 0);
            return next.matches(stringIndex, testString, matchResult);
        }

        int nextIndex;

        if (enterCounter >= quantifier.min()) {
            nextIndex = next.matches(stringIndex, testString, matchResult);
            if (nextIndex < 0) {
                matchResult.setEnterCounter(setCounter, ++enterCounter);
                nextIndex = innerSet.matches(stringIndex, testString, matchResult);
            } else {
                matchResult.setEnterCounter(setCounter, 0);
                return nextIndex;
            }
        } else {
            matchResult.setEnterCounter(setCounter, ++enterCounter);
            nextIndex = innerSet.matches(stringIndex, testString, matchResult);
        }

        return nextIndex;
    }
}
