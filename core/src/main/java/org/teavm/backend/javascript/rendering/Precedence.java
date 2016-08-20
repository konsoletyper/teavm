/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.backend.javascript.rendering;

public enum Precedence {
    COMMA,
    ASSIGNMENT,
    CONDITIONAL,
    LOGICAL_OR,
    LOGICAL_AND,
    BITWISE_OR,
    BITWISE_XOR,
    BITWISE_AND,
    EQUALITY,
    COMPARISON,
    BITWISE_SHIFT,
    ADDITION,
    MULTIPLICATION,
    UNARY,
    FUNCTION_CALL,
    MEMBER_ACCESS,
    GROUPING;

    private static Precedence[] cache = Precedence.values();

    public Precedence next() {
        int index = ordinal();
        return index + 1 < cache.length ? cache[index + 1] : cache[index];
    }

    public Precedence previous() {
        int index = ordinal();
        return index > 0 ? cache[index - 1] : cache[index];
    }

    public static Precedence min() {
        return Precedence.COMMA;
    }
}
