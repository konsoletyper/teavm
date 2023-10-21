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

final class CharacterClassifier {
    static final int EOF = -1;

    private CharacterClassifier() {
    }

    static boolean isEof(int codePoint) {
        return codePoint == -EOF;
    }

    static boolean isWhiteSpace(int codePoint) {
        switch (codePoint) {
            case 0x0009:
            case 0x000B:
            case 0x000C:
            case 0x0020:
            case 0x00A0:
            case 0xFEFF:
                return true;
            default:
                return Character.getType(codePoint) == Character.SPACE_SEPARATOR;
        }
    }

    static boolean isLineTerminator(int codePoint) {
        switch (codePoint) {
            case EOF:
            case 0x000A:
            case 0x000D:
            case 0x2028:
            case 0x2029:
                return true;
            default:
                return false;
        }
    }

    static boolean isIdentifierStart(int codePoint) {
        return Character.isUnicodeIdentifierStart(codePoint);
    }
}
