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
 * Case Insensitive back reference node;
 *
 * @author Nikolay A. Kuznetsov
 */
class CIBackReferenceSet extends JointSet {

    protected int referencedGroup;

    protected int consCounter;

    /**
     * @param substring
     */
    public CIBackReferenceSet(int groupIndex, int consCounter) {
        this.referencedGroup = groupIndex;
        this.consCounter = consCounter;
    }

    public int accepts(int strIndex, CharSequence testString) {
        throw new TPatternSyntaxException("", "", 0);
    }

    public int matches(int stringIndex, CharSequence testString,
            MatchResultImpl matchResult) {
        String group = getString(matchResult);

        if (group == null
                || (stringIndex + group.length()) > matchResult.getRightBound())
            return -1;

        for (int i = 0; i < group.length(); i++) {
            if (group.charAt(i) != testString.charAt(stringIndex + i)
                    && TPattern.getSupplement(group.charAt(i)) != testString
                            .charAt(stringIndex + i)) {
                return -1;
            }
        }
        matchResult.setConsumed(consCounter, group.length());
        return next.matches(stringIndex + group.length(), testString,
                matchResult);
    }

    public AbstractSet getNext() {
        return this.next;
    }

    public void setNext(AbstractSet next) {
        this.next = next;
    }

    protected String getString(MatchResultImpl matchResult) {
        String res = matchResult.getGroupNoCheck(referencedGroup);
        return res;
        // return (res != null) ? res : "";
    }

    public String getName() {
        return "CI back reference: " + this.groupIndex; //$NON-NLS-1$
    }

    public boolean hasConsumed(MatchResultImpl matchResult) {
        int cons;
        boolean res = ((cons = matchResult.getConsumed(consCounter)) < 0 || cons > 0);
        matchResult.setConsumed(consCounter, -1);
        return res;
    }

}
