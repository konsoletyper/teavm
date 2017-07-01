/*
 *  Copyright 2017 Alexey Andreev.
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
 * Greedy quantifier node for the case where there is no intersection with next
 * node and normal quantifiers could be treated as greedy and possessive.
 *
 * @author Nikolay A. Kuznetsov
 */
class TUnifiedQuantifierSet extends TLeafQuantifierSet {

    public TUnifiedQuantifierSet(TLeafSet innerSet, TAbstractSet next, int type) {
        super(innerSet, next, type);
    }

    public TUnifiedQuantifierSet(TLeafQuantifierSet quant) {
        super((TLeafSet) quant.getInnerSet(), quant.getNext(), quant.getType());
        innerSet.setNext(this);

    }

    @Override
    public int matches(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        while (stringIndex + leaf.charCount() <= matchResult.getRightBound()
                && leaf.accepts(stringIndex, testString) > 0) {
            stringIndex += leaf.charCount();
        }

        return next.matches(stringIndex, testString, matchResult);
    }

    @Override
    public int find(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        int startSearch = next.find(stringIndex, testString, matchResult);
        if (startSearch < 0) {
            return -1;
        }
        int newSearch = startSearch - leaf.charCount();
        while (newSearch >= stringIndex && leaf.accepts(newSearch, testString) > 0) {
            startSearch = newSearch;
            newSearch -= leaf.charCount();
        }

        return startSearch;
    }
}
