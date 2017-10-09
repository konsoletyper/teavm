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
 * The node which marks end of the particular group.
 *
 * @author Nikolay A. Kuznetsov
 */
class TFSet extends TAbstractSet {

    static PossessiveFSet posFSet = new PossessiveFSet();

    boolean isBackReferenced;

    private int groupIndex;

    public TFSet(int groupIndex) {
        this.groupIndex = groupIndex;
    }

    @Override
    public int matches(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        int end = matchResult.getEnd(groupIndex);
        matchResult.setEnd(groupIndex, stringIndex);
        int shift = next.matches(stringIndex, testString, matchResult);
        /*
         * if(shift >=0 && matchResult.getEnd(groupIndex) == -1) {
         * matchResult.setEnd(groupIndex, stringIndex); }
         */
        if (shift < 0) {
            matchResult.setEnd(groupIndex, end);
        }
        return shift;
    }

    public int getGroupIndex() {
        return groupIndex;
    }

    @Override
    protected String getName() {
        return "fSet"; //$NON-NLS-1$
    }

    @Override
    public boolean hasConsumed(TMatchResultImpl mr) {
        return false;
    }

    /**
     * Marks the end of the particular group and not take into account possible
     * kickbacks(required for atomic groups, for instance)
     *
     */
    static class PossessiveFSet extends TAbstractSet {

        @Override
        public int matches(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
            return stringIndex;
        }

        @Override
        protected String getName() {
            return "posFSet"; //$NON-NLS-1$
        }

        @Override
        public boolean hasConsumed(TMatchResultImpl mr) {
            return false;
        }
    }
}