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

class CurrentCharReader {
    private int offset;
    private int line;
    private int column;
    private int currentChar;
    private boolean wasCr;
    private CodePointReader reader;

    CurrentCharReader(CodePointReader reader) {
        this.reader = reader;
        currentChar = reader.read();
    }

    void next() {
        if (currentChar == -1) {
            return;
        }
        if (currentChar == '\r') {
            ++line;
            column = 0;
            wasCr = true;
        } else if (currentChar == '\n') {
            if (!wasCr) {
                ++line;
                column = 0;
            }
            wasCr = false;
        } else {
            ++column;
            wasCr = false;
        }
        ++offset;
        currentChar = reader.read();
    }

    public int offset() {
        return offset;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    public int currentChar() {
        return currentChar;
    }
}
