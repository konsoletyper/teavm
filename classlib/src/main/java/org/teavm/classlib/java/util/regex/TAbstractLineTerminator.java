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
 * Line terminator factory
 *
 * @author Nikolay A. Kuznetsov
 */
abstract class TAbstractLineTerminator {
    static TAbstractLineTerminator unixLT;

    static TAbstractLineTerminator unicodeLT;

    public abstract boolean isLineTerminator(int ch);

    public abstract boolean isAfterLineTerminator(int ch1, int ch2);

    public static TAbstractLineTerminator getInstance(int flag) {
        if ((flag & TPattern.UNIX_LINES) != 0) {
            if (unixLT != null) {
                return unixLT;
            }
            unixLT = new TAbstractLineTerminator() {
                @Override
                public boolean isLineTerminator(int ch) {
                    return ch == '\n';
                }

                @Override
                public boolean isAfterLineTerminator(int ch, int ch2) {
                    return ch == '\n';
                }
            };
            return unixLT;
        } else {
            if (unicodeLT != null) {
                return unicodeLT;
            }
            unicodeLT = new TAbstractLineTerminator() {
                @Override
                public boolean isLineTerminator(int ch) {
                    return ch == '\n' || ch == '\r' || ch == '\u0085' || (ch | 1) == '\u2029';
                }

                @Override
                public boolean isAfterLineTerminator(int ch, int ch2) {
                    return (ch == '\n' || ch == '\u0085' || (ch | 1) == '\u2029') || (ch == '\r' && ch2 != '\n');
                }
            };
            return unicodeLT;
        }
    }
}
