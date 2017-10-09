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
 * @author Nikolay A. Kuznetsov
 */
class TLeafQuantifierSet extends TQuantifierSet {

    protected TLeafSet leaf;

    public TLeafQuantifierSet(TLeafSet innerSet, TAbstractSet next, int type) {
        super(innerSet, next, type);
        this.leaf = innerSet;
    }

    @Override
    public int matches(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        int i = 0;
        int shift = 0;

        while (stringIndex + leaf.charCount() <= matchResult.getRightBound()) {
            shift = leaf.accepts(stringIndex, testString);
            if (shift <= 0) {
                break;
            }
            stringIndex += shift;
            i++;
        }

        for (; i >= 0; i--) {
            shift = next.matches(stringIndex, testString, matchResult);
            if (shift >= 0) {
                return shift;
            }

            stringIndex -= leaf.charCount();
        }
        return -1;
    }

    @Override
    protected String getName() {
        return "<Quant>";
    }

    /**
     * Sets an inner set.
     *
     * @param innerSet
     *            The innerSet to set.
     */
    @Override
    public void setInnerSet(TAbstractSet innerSet) {
        if (!(innerSet instanceof TLeafSet)) {
            throw new RuntimeException("");
        }
        super.setInnerSet(innerSet);
        this.leaf = (TLeafSet) innerSet;
    }
}
