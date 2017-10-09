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
 * Represents RE quantifier; contains two fields responsible for min and max
 * number of repetitions. Negative value for maximum number of repetition
 * represents infinity(i.e. +,*)
 *
 * @author Nikolay A. Kuznetsov
 */
class TQuantifier extends TSpecialToken implements Cloneable {

    private int min;

    private int max;

    private int counter;

    public TQuantifier(int min) {
        this.min = min;
        this.max = min;
    }

    public TQuantifier(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public void resetCounter() {
        counter = 0;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    @Override
    public String toString() {
        return "{" + min + "," + ((max == Integer.MAX_VALUE) ? "" : new Integer(max).toString()) + "}";
    }

    @Override
    public int getType() {
        return TSpecialToken.TOK_QUANTIFIER;
    }

    @Override
    public Object clone() {
        return new TQuantifier(min, max);
    }
}
