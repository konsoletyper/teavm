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
 * Reluctant version of composite(i.e. {n,m}) quantifier set over leaf nodes.
 *
 * @author Nikolay A. Kuznetsov
 */
class TReluctantCompositeQuantifierSet extends TCompositeQuantifierSet {
    public TReluctantCompositeQuantifierSet(TQuantifier quant, TLeafSet innerSet, TAbstractSet next, int type) {
        super(quant, innerSet, next, type);
    }

    @Override
    public int matches(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        int min = quantifier.min();
        int max = quantifier.max();
        int i = 0;
        int shift = 0;

        for (; i < min; i++) {

            if (stringIndex + leaf.charCount() > matchResult.getRightBound()) {
                matchResult.hitEnd = true;
                return -1;
            }

            shift = leaf.accepts(stringIndex, testString);
            if (shift < 1) {
                return -1;
            }
            stringIndex += shift;
        }

        do {
            shift = next.matches(stringIndex, testString, matchResult);
            if (shift >= 0) {
                return shift;
            }

            if (stringIndex + leaf.charCount() <= matchResult.getRightBound()) {
                shift = leaf.accepts(stringIndex, testString);
                stringIndex += shift;
                i++;
            }

        } while (shift >= 1 && i <= max);

        return -1;
    }
}
