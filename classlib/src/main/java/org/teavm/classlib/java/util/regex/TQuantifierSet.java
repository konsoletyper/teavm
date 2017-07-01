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
 * Base class for quantifiers.
 *
 * @author Nikolay A. Kuznetsov
 */
abstract class TQuantifierSet extends TAbstractSet {

    protected TAbstractSet innerSet;

    public TQuantifierSet(TAbstractSet innerSet, TAbstractSet next, int type) {
        super(next);
        this.innerSet = innerSet;
        setType(type);
    }

    public TAbstractSet getInnerSet() {
        return innerSet;
    }

    /**
     * Sets an inner set.
     *
     * @param innerSet
     *            The innerSet to set.
     */
    public void setInnerSet(TAbstractSet innerSet) {
        this.innerSet = innerSet;
    }

    @Override
    public boolean first(TAbstractSet set) {
        return innerSet.first(set) || next.first(set);
    }

    @Override
    public boolean hasConsumed(TMatchResultImpl mr) {
        return true;
    }

    /**
     * This method is used for traversing nodes after the first stage of
     * compilation.
     */
    @Override
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
            }
        }

        if (innerSet != null) {

            if (!innerSet.isSecondPassVisited) {

                /*
                 * Add here code to do during the pass
                 */
                TJointSet set = innerSet.processBackRefReplacement();

                if (set != null) {
                    innerSet.isSecondPassVisited = true;
                    innerSet = set;
                }

                /*
                 * End code to do during the pass
                 */
                innerSet.processSecondPass();
            } else {

                /*
                 * We reach node through innerSet but it is already traversed.
                 * You can see this situation for GroupQuantifierSet.innerset if
                 * we compile smth like "(a)+ when GroupQuantifierSet ==
                 * GroupQuantifierSet.innerset.fSet.next
                 */

                /*
                 * Add here code to do during the pass
                 */
                if (innerSet instanceof TSingleSet && ((TFSet) ((TJointSet) innerSet).fSet).isBackReferenced) {
                    innerSet = innerSet.next;
                }

                /*
                 * End code to do during the pass
                 */
            }
        }
    }
}
