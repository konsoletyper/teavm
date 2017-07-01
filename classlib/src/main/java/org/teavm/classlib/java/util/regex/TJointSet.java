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
import java.util.Iterator;

/**
 * Represents group, which is alternation of other subexpression. One should
 * think about "group" in this model as JointSet opening group and corresponding
 * FSet closing group.
 */
class TJointSet extends TAbstractSet {

    protected ArrayList<TAbstractSet> children;

    protected TAbstractSet fSet;

    protected int groupIndex;

    protected TJointSet() {
    }

    public TJointSet(ArrayList<TAbstractSet> children, TFSet fSet) {
        this.children = children;
        this.fSet = fSet;
        this.groupIndex = fSet.getGroupIndex();
    }

    /**
     * Returns stringIndex+shift, the next position to match
     */
    @Override
    public int matches(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        if (children == null) {
            return -1;
        }
        int start = matchResult.getStart(groupIndex);
        matchResult.setStart(groupIndex, stringIndex);
        int size = children.size();
        for (int i = 0; i < size; i++) {
            TAbstractSet e = children.get(i);
            int shift = e.matches(stringIndex, testString, matchResult);
            if (shift >= 0) {
                return shift;
            }
        }
        matchResult.setStart(groupIndex, start);
        return -1;
    }

    @Override
    public void setNext(TAbstractSet next) {
        fSet.setNext(next);
    }

    @Override
    public TAbstractSet getNext() {
        return fSet.getNext();
    }

    @Override
    protected String getName() {
        return "JointSet"; //$NON-NLS-1$
    }

    public int getGroup() {
        return groupIndex;
    }

    @Override
    public boolean first(TAbstractSet set) {
        if (children != null) {
            for (Iterator<TAbstractSet> i = children.iterator(); i.hasNext();) {
                if ((i.next()).first(set)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean hasConsumed(TMatchResultImpl matchResult) {
        return !(matchResult.getEnd(groupIndex) >= 0 && matchResult.getStart(groupIndex) == matchResult
                .getEnd(groupIndex));
    }

    /**
     * This method is used for traversing nodes after the first stage of
     * compilation.
     */
    @Override
    public void processSecondPass() {
        this.isSecondPassVisited = true;

        if (fSet != null && !fSet.isSecondPassVisited) {
            fSet.processSecondPass();
        }

        if (children != null) {
            int childrenSize = children.size();

            for (int i = 0; i < childrenSize; i++) {
                TAbstractSet child = children.get(i);
                TJointSet set = child.processBackRefReplacement();

                if (set != null) {
                    child.isSecondPassVisited = true;
                    children.remove(i);
                    children.add(i, set);
                    child = set;
                }

                if (!child.isSecondPassVisited) {
                    child.processSecondPass();
                }
            }
        }

        if (next != null) {
            super.processSecondPass();
        }
    }
}
