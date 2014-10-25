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
 * Represents node accepting single character from the given char class.
 *
 * @author Nikolay A. Kuznetsov
 */

class RangeSet extends LeafSet {

    private AbstractCharClass chars;

    private boolean alt = false;

    public RangeSet(AbstractCharClass cs, AbstractSet next) {
        super(next);
        this.chars = cs.getInstance();
        this.alt = cs.alt;
    }

    public RangeSet(AbstractCharClass cc) {
        this.chars = cc.getInstance();
        this.alt = cc.alt;
    }

    @Override
    public int accepts(int strIndex, CharSequence testString) {
        return chars.contains(testString.charAt(strIndex)) ? 1 : -1;
    }

    @Override
    protected String getName() {
        return "range:" + (alt ? "^ " : " ") + chars.toString();
    }

    @Override
    public boolean first(AbstractSet set) {
        if (set instanceof CharSet) {
            return AbstractCharClass.intersects(chars, ((CharSet)set).getChar());
        } else if (set instanceof RangeSet) {
            return AbstractCharClass.intersects(chars, ((RangeSet)set).chars);
        } else if (set instanceof SupplRangeSet) {
            return AbstractCharClass.intersects(chars, ((SupplRangeSet)set).getChars());
        } else if (set instanceof SupplCharSet) {
            return false;
        }
        return true;
    }

    protected AbstractCharClass getChars() {
        return chars;
    }
}
