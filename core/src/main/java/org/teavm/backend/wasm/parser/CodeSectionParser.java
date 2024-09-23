/*
 *  Copyright 2022 Alexey Andreev.
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

public class CodeSectionParser extends BaseSectionParser {
    private CodeSectionListener listener;
    private int functionIndexOffset;
    private CodeParser codeParser = new CodeParser();

    public CodeSectionParser(CodeSectionListener listener) {
        this.listener = listener;
    }

    public void setFunctionIndexOffset(int functionIndexOffset) {
        this.functionIndexOffset = functionIndexOffset;
    }

    @Override
    protected void parseContent() {
        reportAddress();
        int count = readLEB();
        listener.sectionStart(count);
        for (var i = 0; i < count; ++i) {
            parseFunction(i);
        }
        reportAddress();
        listener.sectionEnd();
    }

    private void parseFunction(int index) {
        reportAddress();
        var functionSize = readLEB();
        var end = reader.ptr + functionSize;
        if (listener.functionStart(index + functionIndexOffset, functionSize)) {
            parseLocals();
            var codeListener = listener.code();
            if (codeListener != null) {
                codeParser.setCodeListener(codeListener);
                codeParser.parse(reader);
            }
        }
        reader.ptr = end;
        reportAddress();
        listener.functionEnd();
    }

    private void parseLocals() {
        reportAddress();
        var localEntries = readLEB();
        listener.localsStart(localEntries);
        var localIndex = 0;
        for (int i = 0; i < localEntries; ++i) {
            reportAddress();
            var countInGroup = readLEB();
            var type = reader.readType();
            listener.local(localIndex, countInGroup, type);
            localIndex += countInGroup;
        }
        reportAddress();
    }
}
