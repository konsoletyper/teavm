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

public class NameSectionParser extends BaseSectionParser {
    private final NameSectionListener listener;

    public NameSectionParser(NameSectionListener listener) {
        super(AddressListener.EMPTY);
        this.listener = listener;
    }

    @Override
    protected void parseContent() {
        while (ptr < data.length) {
            var sectionType = data[ptr++];
            var sectionLength = readLEB();
            var next = ptr + sectionLength;
            switch (sectionType) {
                case 1:
                    parseNameMapSubsection(listener.functions());
                    break;
                case 2:
                    parseIndirectNameMap(listener.locals());
                    break;
                case 4:
                    parseNameMapSubsection(listener.types());
                    break;
                case 7:
                    parseNameMapSubsection(listener.globals());
                    break;
                case 10:
                    parseIndirectNameMap(listener.fields());
                    break;
            }
            ptr = next;
        }
    }

    private void parseNameMapSubsection(NameMapListener subsectionListener) {
        if (subsectionListener == null) {
            return;
        }
        parseNameMap(subsectionListener);
    }

    private void parseNameMap(NameMapListener subsectionListener) {
        var count = readLEB();
        for (var i = 0; i < count; ++i) {
            var index = readLEB();
            var name = readString();
            if (subsectionListener != null) {
                subsectionListener.name(index, name);
            }
        }
    }

    private void parseIndirectNameMap(NameIndirectMapListener subsectionListener) {
        var count = readLEB();
        for (var i = 0; i < count; ++i) {
            var index = readLEB();
            parseNameMap(subsectionListener.map(index));
        }
    }
}
