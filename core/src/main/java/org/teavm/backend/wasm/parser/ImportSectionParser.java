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

public class ImportSectionParser extends BaseSectionParser {
    private final ImportSectionListener listener;

    public ImportSectionParser(ImportSectionListener listener) {
        this.listener = listener;
    }

    @Override
    protected void parseContent() {
        var count = readLEB();
        for (var i = 0; i < count; ++i) {
            readEntry();
        }
    }

    private void readEntry() {
        reportAddress();
        var module = readString();
        var name = readString();
        listener.startEntry(module, name);
        reportAddress();
        var type = reader.data[reader.ptr++];
        switch (type) {
            case 0: {
                var typeIndex = readLEB();
                listener.function(typeIndex);
                break;
            }
            case 2: {
                var limitsType = reader.data[reader.ptr++];
                if (limitsType == 0) {
                    listener.memory(readLEB(), -1);
                } else {
                    listener.memory(readLEB(), readLEB());
                }
                break;
            }
            case 3: {
                var valueType = reader.readType();
                listener.global(valueType, reader.readLEB() != 0);
                break;
            }
            default:
                throw new ParseException("Unsupported import type", reader.ptr);
        }
        listener.endEntry();
    }
}
