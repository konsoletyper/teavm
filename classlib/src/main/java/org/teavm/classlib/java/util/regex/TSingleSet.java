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
 * Group node over subexpression w/o alternations.
 *
 * @author Nikolay A. Kuznetsov
 */
class TSingleSet extends TJointSet {

    protected TAbstractSet kid;

    public TSingleSet(TAbstractSet child, TFSet fSet) {
        this.kid = child;
        this.fSet = fSet;
        this.groupIndex = fSet.getGroupIndex();
    }

    @Override
    public int matches(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        int start = matchResult.getStart(groupIndex);
        matchResult.setStart(groupIndex, stringIndex);
        int shift = kid.matches(stringIndex, testString, matchResult);
        if (shift >= 0) {
            return shift;
        }
        matchResult.setStart(groupIndex, start);
        return -1;
    }

    @Override
    public int find(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        int res = kid.find(stringIndex, testString, matchResult);
        if (res >= 0) {
            matchResult.setStart(groupIndex, res);
        }
        return res;
    }

    @Override
    public int findBack(int stringIndex, int lastIndex, CharSequence testString, TMatchResultImpl matchResult) {
        int res = kid.findBack(stringIndex, lastIndex, testString, matchResult);
        if (res >= 0) {
            matchResult.setStart(groupIndex, res);
        }
        return res;
    }

    @Override
    public boolean first(TAbstractSet set) {
        return kid.first(set);
    }

    /**
     * This method is used for replacement backreferenced sets.
     */
    @Override
    public TJointSet processBackRefReplacement() {
        TBackReferencedSingleSet set = new TBackReferencedSingleSet(this);

        /*
         * We will store a reference to created BackReferencedSingleSet in next
         * field. This is needed toprocess replacement of sets correctly since
         * sometimes we cannot renew all references to detachable set in the
         * current point of traverse. See QuantifierSet and AbstractSet
         * processSecondPass() methods for more details.
         */
        next = set;
        return set;
    }

    /**
     * This method is used for traversing nodes after the first stage of
     * compilation.
     */
    @Override
    public void processSecondPass() {
        this.isSecondPassVisited = true;

        if (fSet != null && !fSet.isSecondPassVisited) {

            /*
             * Add here code to do during the pass
             */

            /*
             * End code to do during the pass
             */
            fSet.processSecondPass();
        }

        if (kid != null && !kid.isSecondPassVisited) {

            /*
             * Add here code to do during the pass
             */
            TJointSet set = kid.processBackRefReplacement();

            if (set != null) {
                kid.isSecondPassVisited = true;
                kid = set;
            }

            /*
             * End code to do during the pass
             */

            kid.processSecondPass();
        }
    }
}
