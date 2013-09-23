/*
 *  Copyright 2012 Alexey Andreev.
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
package org.teavm.javascript.ast;

/**
 *
 * @author Alexey Andreev
 */
public enum BinaryOperation {
    ADD,
    ADD_LONG,
    SUBTRACT,
    SUBTRACT_LONG,
    MULTIPLY,
    MULTIPLY_LONG,
    DIVIDE,
    DIVIDE_LONG,
    MODULO,
    MODULO_LONG,
    EQUALS,
    NOT_EQUALS,
    STRICT_EQUALS,
    STRICT_NOT_EQUALS,
    LESS,
    LESS_OR_EQUALS,
    GREATER,
    GREATER_OR_EQUALS,
    AND,
    OR,
    COMPARE,
    COMPARE_LONG,
    BITWISE_AND,
    BITWISE_AND_LONG,
    BITWISE_OR,
    BITWISE_OR_LONG,
    BITWISE_XOR,
    BITWISE_XOR_LONG,
    LEFT_SHIFT,
    LEFT_SHIFT_LONG,
    RIGHT_SHIFT,
    RIGHT_SHIFT_LONG,
    UNSIGNED_RIGHT_SHIFT,
    UNSIGNED_RIGHT_SHIFT_LONG
}
