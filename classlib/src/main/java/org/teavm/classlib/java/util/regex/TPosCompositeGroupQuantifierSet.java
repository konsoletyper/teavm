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
 * Possessive composite (i.e. {n,m}) quantifier node over groups.
 *
 * @author Nikolay A. Kuznetsov
 */
class TPosCompositeGroupQuantifierSet extends TCompositeGroupQuantifierSet {

    public TPosCompositeGroupQuantifierSet(TQuantifier quant, TAbstractSet innerSet, TAbstractSet next, int type,
            int setCounter) {
        super(quant, innerSet, next, type, setCounter);
        innerSet.setNext(TFSet.posFSet);
    }

    @Override
    public int matches(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        int nextIndex;
        int counter = 0;
        int max = quantifier.max();

        while (true) {
            nextIndex = innerSet.matches(stringIndex, testString, matchResult);
            if (nextIndex <= stringIndex || counter >= max) {
                break;
            }
            counter++;
            stringIndex = nextIndex;
        }

        if (nextIndex < 0 && counter < quantifier.min()) {
            return -1;
        } else {
            return next.matches(stringIndex, testString, matchResult);
        }
    }
}
