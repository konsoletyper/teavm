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
 * @author Nikolay Kuznetsov
 */

package org.teavm.classlib.java.util.regex;

/**
 * Group node over subexpression w/o alternations. This node is used if current
 * group is referenced via backreference.
 */

class TBackReferencedSingleSet extends TSingleSet {

    /*
     * This class is needed only for overwriting find() and findBack() methods
     * of SingleSet class, which is being back referenced. The following example
     * explains the need for such substitution: Let's consider the pattern
     * ".*(.)\\1". Leading .* works as follows: finds line terminator and runs
     * findBack from that point. findBack method in its turn (in contrast to
     * matches) sets group boundaries on the back trace. Thus at the point we
     * try to match back reference(\\1) groups are not yet set.
     *
     * To fix this problem we replace backreferenced groups with instances of
     * this class, which will use matches instead of find; this will affect
     * performance, but ensure correctness of the match.
     */

    public TBackReferencedSingleSet(TAbstractSet child, TFSet fSet) {
        super(child, fSet);
    }

    public TBackReferencedSingleSet(TSingleSet node) {
        super(node.kid, (TFSet) node.fSet);
    }

    @Override
    public int find(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        int res = 0;
        int lastIndex = matchResult.getRightBound();
        int startSearch = stringIndex;

        for (; startSearch <= lastIndex; startSearch++) {
            int saveStart = matchResult.getStart(groupIndex);

            matchResult.setStart(groupIndex, startSearch);
            res = kid.matches(startSearch, testString, matchResult);
            if (res >= 0) {
                res = startSearch;
                break;
            } else {
                matchResult.setStart(groupIndex, saveStart);
            }
        }

        return res;
    }

    @Override
    public int findBack(int stringIndex, int lastIndex, CharSequence testString, TMatchResultImpl matchResult) {
        int res = 0;
        int startSearch = lastIndex;

        for (; startSearch >= stringIndex; startSearch--) {
            int saveStart = matchResult.getStart(groupIndex);

            matchResult.setStart(groupIndex, startSearch);
            res = kid.matches(startSearch, testString, matchResult);
            if (res >= 0) {
                res = startSearch;
                break;
            } else {
                matchResult.setStart(groupIndex, saveStart);
            }
        }

        return res;
    }

    @Override
    public TJointSet processBackRefReplacement() {
        return null;
    }
}
