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
 * Represents node accepting single character in case insensitive manner.
 *
 * @author Nikolay A. Kuznetsov
 */
class TCICharSet extends TLeafSet {

    private char ch;

    private char supplement;

    public TCICharSet(char ch) {
        this.ch = ch;
        this.supplement = TPattern.getSupplement(ch);
    }

    @Override
    public int accepts(int strIndex, CharSequence testString) {
        return (this.ch == testString.charAt(strIndex) || this.supplement == testString.charAt(strIndex)) ? 1 : -1;
    }

    @Override
    protected String getName() {
        return "CI " + ch;
    }

    protected char getChar() {
        return ch;
    }
}