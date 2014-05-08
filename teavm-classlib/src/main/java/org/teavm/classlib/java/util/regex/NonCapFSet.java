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
 * Non-capturing group closing node.
 *
 * @author Nikolay A. Kuznetsov
 */
class NonCapFSet extends FSet {

    public NonCapFSet(int groupIndex) {
        super(groupIndex);
    }

    public int matches(int stringIndex, CharSequence testString,
            MatchResultImpl matchResult) {

        int gr = getGroupIndex();
        matchResult.setConsumed(gr, stringIndex - matchResult.getConsumed(gr));

        return next.matches(stringIndex, testString, matchResult);
    }

    protected String getName() {
        return "NonCapFSet"; //$NON-NLS-1$
    }

    public boolean hasConsumed(MatchResultImpl mr) {
        return false;
    }
}
