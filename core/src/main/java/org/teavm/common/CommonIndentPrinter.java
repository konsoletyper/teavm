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
package org.teavm.common;

public class CommonIndentPrinter {
    private StringBuilder sb = new StringBuilder();
    private int indentLevel;
    private int lastLineIndex;

    public void clear() {
        sb.setLength(0);
        indentLevel = 0;
        lastLineIndex = 0;
    }

    public CommonIndentPrinter newLine() {
        sb.append("\n");
        for (int i = 0; i < indentLevel; ++i) {
            sb.append("    ");
        }
        lastLineIndex = sb.length();
        return this;
    }

    public CommonIndentPrinter indent() {
        indentLevel++;
        if (lastLineIndex == sb.length()) {
            sb.append("    ");
            lastLineIndex = sb.length();
        }
        return this;
    }

    public CommonIndentPrinter outdent() {
        indentLevel--;
        if (lastLineIndex == sb.length()) {
            sb.setLength(sb.length() - 4);
            lastLineIndex = sb.length();
        }
        return this;
    }

    public CommonIndentPrinter append(String text) {
        sb.append(text);
        return this;
    }

    public CommonIndentPrinter append(String text, int start, int end) {
        sb.append(text, start, end);
        return this;
    }

    public CommonIndentPrinter append(boolean value) {
        sb.append(value);
        return this;
    }

    public CommonIndentPrinter append(int value) {
        sb.append(value);
        return this;
    }

    public CommonIndentPrinter append(float value) {
        sb.append(value);
        return this;
    }

    public CommonIndentPrinter append(double value) {
        sb.append(value);
        return this;
    }

    public String toString() {
        return sb.toString();
    }
}
