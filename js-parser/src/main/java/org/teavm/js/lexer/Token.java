/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.js.lexer;

public enum Token {
    EOF,
    LINE_TERMINATOR,
    COMMENT,
    IDENTIFER,

    LEFT_CURLY_BRACKET,
    RIGHT_CURLY_BRACKET,
    LEFT_PARENTHESIS,
    RIGHT_PARENTHESIS,
    LEFT_SQUARE_BRACKET,
    RIGHT_SQUARE_BRACKET,
    DOT,
    ELLIPSIS,
    SEMICOLON,
    COMMA,
    LESS,
    GREATER,
    LESS_OR_EQUAL,
    GREATER_OR_EQUAL,
    EQUAL,
    NOT_EQUAL,
    STRICT_EQUAL,
    STRICT_NOT_EQUAL,
    SHIFT_LEFT,
    SHIFT_RIGHT,
    SHIFT_RIGHT_UNSIGNED,
    PLUS,
    MINUS,
    MULTIPLY,
    DIVIDE,
    REMAINDER,
    AND,
    OR,
    XOR,
    NOT,
    INVERT,
    AND_AND,
    OR_OR,
    QUESTION,
    COLON,
    ASSIGN,
    PLUS_ASSIGN,
    MINUS_ASSIGN,
    MULTIPLY_ASSIGN,
    DIVIDE_ASSIGN,
    REMAINDER_ASSIGN,
    AND_ASSIGN,
    OR_ASSIGN,
    XOR_ASSIGN,
    SHIFT_LEFT_ASSIGN,
    SHIFT_RIGHT_ASSIGN,
    SHIFT_RIGHT_UNSIGNED_ASSIGN,
    ARROW,

    NUMERIC_LITERAL,
    REGEX_LITERAL
}
