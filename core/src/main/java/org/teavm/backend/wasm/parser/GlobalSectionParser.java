/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.parser;

public class GlobalSectionParser extends BaseSectionParser {
    private final GlobalSectionListener listener;
    private CodeParser codeParser;

    public GlobalSectionParser(GlobalSectionListener listener) {
        this.listener = listener;
        codeParser = new CodeParser();
    }

    @Override
    protected void parseContent() {
        var count = readLEB();
        for (var i = 0; i < count; ++i) {
            reportAddress();
            var type = reader.readType();
            var mutable = reader.data[reader.ptr++] != 0;
            var codeListener = listener.startGlobal(i, type, mutable);
            if (codeListener == null) {
                codeListener = CodeListener.EMPTY;
            }
            codeParser.setCodeListener(codeListener);
            if (!codeParser.parseSingleExpression(reader)) {
                throw new ParseException("Error parsing global initializer", reader.ptr);
            }
            reader.ptr++;
            listener.endGlobal();
        }
    }
}
