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

import java.util.ArrayList;

/**
 * This class represent atomic group (?>X), once X matches, this match become
 * unchangeable till the end of the match.
 *
 * @author Nikolay A. Kuznetsov
 */
class TAtomicJointSet extends TNonCapJointSet {
    public TAtomicJointSet(ArrayList<TAbstractSet> children, TFSet fSet) {
        super(children, fSet);
    }

    /**
     * Returns stringIndex+shift, the next position to match
     */
    @Override
    public int matches(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        int start = matchResult.getConsumed(groupIndex);
        matchResult.setConsumed(groupIndex, stringIndex);

        int size = children.size();
        for (int i = 0; i < size; i++) {
            TAbstractSet e = children.get(i);
            int shift = e.matches(stringIndex, testString, matchResult);
            if (shift >= 0) {
                // AtomicFset always returns true, but saves the index to run
                // this next.match() from;
                return next.matches(((TAtomicFSet) fSet).getIndex(), testString, matchResult);
            }
        }

        matchResult.setConsumed(groupIndex, start);
        return -1;
    }

    @Override
    public void setNext(TAbstractSet next) {
        this.next = next;
    }

    @Override
    public TAbstractSet getNext() {
        return next;
    }

    @Override
    protected String getName() {
        return "NonCapJointSet"; //$NON-NLS-1$
    }
}
