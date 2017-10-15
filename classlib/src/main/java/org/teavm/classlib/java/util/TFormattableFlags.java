/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.classlib.java.util;

public final class TFormattableFlags {
    private TFormattableFlags() {
    }

    public static final int LEFT_JUSTIFY = 1;
    public static final int UPPERCASE = 2;
    public static final int ALTERNATE = 4;

    static final int SIGNED = 8;
    static final int LEADING_SPACE = 16;
    static final int ZERO_PADDED = 32;
    static final int GROUPING_SEPARATOR = 64;
    static final int PARENTHESIZED_NEGATIVE = 128;
    static final int PREVIOUS_ARGUMENT = 256;
}
