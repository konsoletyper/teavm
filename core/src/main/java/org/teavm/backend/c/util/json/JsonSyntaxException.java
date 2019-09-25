/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.backend.c.util.json;

public class JsonSyntaxException extends RuntimeException {
    private final int lineNumber;
    private final int columnNumber;
    private final String error;

    public JsonSyntaxException(int lineNumber, int columnNumber, String error) {
        super("JSON syntax error at " + (lineNumber + 1) + ":" + (columnNumber + 1) + ": " + error);
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.error = error;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public String getError() {
        return error;
    }
}
