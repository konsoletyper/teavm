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
 * Base class for nodes representing leaf tokens of the RE, those who consumes
 * fixed number of characters.
 *
 * @author Nikolay A. Kuznetsov
 */
abstract class TLeafSet extends TAbstractSet {

    protected int charCount = 1;

    public TLeafSet(TAbstractSet next) {
        super(next);
        setType(TAbstractSet.TYPE_LEAF);
    }

    public TLeafSet() {
    }

    public abstract int accepts(int stringIndex, CharSequence testString);

    /**
     * Checks if we can enter this state and pass the control to the next one.
     * Return positive value if match succeeds, negative otherwise.
     */
    @Override
    public int matches(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {

        if (stringIndex + charCount() > matchResult.getRightBound()) {
            matchResult.hitEnd = true;
            return -1;
        }

        int shift = accepts(stringIndex, testString);
        if (shift < 0) {
            return -1;
        }

        return next.matches(stringIndex + shift, testString, matchResult);
    }

    /**
     * Returns number of characters this node consumes.
     *
     * @return number of characters this node consumes.
     */
    public int charCount() {
        return charCount;
    }

    @Override
    public boolean hasConsumed(TMatchResultImpl mr) {
        return true;
    }
}
