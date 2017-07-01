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
 * Basic class for nodes, representing given regular expression. Note: All the
 * classes representing nodes has set prefix;
 *
 * @author Nikolay A. Kuznetsov
 */
abstract class TAbstractSet {

    public static final int TYPE_LEAF = 1 << 0;

    public static final int TYPE_FSET = 1 << 1;

    public static final int TYPE_QUANT = 1 << 3;

    public static final int TYPE_DOTSET = 0x80000000 | '.';

    /**
     * Next node to visit
     */
    protected TAbstractSet next;

    /**
     * Counter for debugging purposes, represent unique node index;
     */
    static int counter = 1;

    protected boolean isSecondPassVisited;

    protected String index = new Integer(TAbstractSet.counter++).toString();

    private int type;

    public TAbstractSet() {
    }

    public TAbstractSet(TAbstractSet n) {
        next = n;
    }

    /**
     * Checks if this node matches in given position and recursively call next
     * node matches on positive self match. Returns positive integer if entire
     * match succeed, negative otherwise
     *
     * @param stringIndex
     *            - string index to start from;
     * @param testString
     *            - input string
     * @param matchResult
     *            - MatchResult to sore result into
     * @return -1 if match fails or n > 0;
     */
    public abstract int matches(int stringIndex, CharSequence testString, TMatchResultImpl matchResult);

    /**
     * Attempts to apply pattern starting from this set/stringIndex; returns
     * index this search was started from, if value is negative, this means that
     * this search didn't succeed, additional information could be obtained via
     * matchResult;
     *
     * Note: this is default implementation for find method, it's based on
     * matches, subclasses do not have to override find method unless more
     * effective find method exists for a particular node type (sequence, i.e.
     * substring, for example). Same applies for find back method.
     *
     * @param stringIndex
     *            starting index
     * @param testString
     *            string to search in
     * @param matchResult
     *            result of the match
     * @return last searched index
     */
    public int find(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        int length = matchResult.getRightBound();
        while (stringIndex <= length) {
            if (matches(stringIndex, testString, matchResult) >= 0) {
                return stringIndex;
            } else {
                stringIndex++;
            }
        }
        return -1;
    }

    /**
     * @param stringIndex
     *            - an index, to finish search back (left limit)
     * @param startSearch
     *            - an index to start search from (right limit)
     * @param testString
     *            - test string;
     * @param matchResult
     *            match result
     * @return an index to start back search next time if this search fails(new
     *         left bound); if this search fails the value is negative;
     */
    public int findBack(int stringIndex, int startSearch, CharSequence testString, TMatchResultImpl matchResult) {
        while (startSearch >= stringIndex) {
            if (matches(startSearch, testString, matchResult) >= 0) {
                return startSearch;
            } else {
                startSearch--;
            }
        }
        return -1;
    }

    public abstract boolean hasConsumed(TMatchResultImpl matchResult);

    /**
     * Returns name for the particular node type. Used for debugging purposes.
     */
    protected abstract String getName();

    protected void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return this.type;
    }

    protected String getQualifiedName() {
        return "<" + index + ":" + getName() + ">";
    }

    @Override
    public String toString() {
        return getQualifiedName();
    }

    public TAbstractSet getNext() {
        return next;
    }

    /**
     * Sets next abstract set
     *
     * @param next
     *            The next to set.
     */
    public void setNext(TAbstractSet next) {
        this.next = next;
    }

    /**
     * Returns true if the given node intersects with this one, false otherwise.
     * This method is being used for quantifiers construction, lets consider the
     * following regular expression (a|b)*ccc.
     *
     * (a|b) does not intersects with "ccc" and thus can be quantified greedily
     * (w/o kickbacks), like *+ instead of *.
     *
     * @param set
     *            - usually previous node
     *
     * @return true if the given node intersects with this one
     */
    public boolean first(TAbstractSet set) {
        return true;
    }

    public TJointSet processBackRefReplacement() {
        return null;
    }

    /**
     * This method is used for traversing nodes after the first stage of
     * compilation.
     */
    public void processSecondPass() {
        this.isSecondPassVisited = true;

        if (next != null) {

            if (!next.isSecondPassVisited) {

                /*
                 * Add here code to do during the pass
                 */
                TJointSet set = next.processBackRefReplacement();

                if (set != null) {
                    next.isSecondPassVisited = true;
                    next = set;
                }

                /*
                 * End code to do during the pass
                 */
                next.processSecondPass();
            } else {

                /*
                 * We reach node through next but it is already traversed. You
                 * can see this situation for AltGroupQuantifierSet.next when we
                 * reach this node through AltGroupQuantifierSet.innerset. ...
                 * .next
                 */

                /*
                 * Add here code to do during the pass
                 */
                if (next instanceof TSingleSet && ((TFSet) ((TJointSet) next).fSet).isBackReferenced) {
                    next = next.next;
                }

                /*
                 * End code to do during the pass
                 */
            }
        }
    }
}