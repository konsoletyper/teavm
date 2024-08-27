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

public class TypeSectionParser extends BaseSectionParser {
    private TypeSectionListener listener;
    private int typeIndex;

    public TypeSectionParser(AddressListener addressListener, TypeSectionListener listener) {
        super(addressListener);
        this.listener = listener;
    }

    @Override
    protected void parseContent() {
        reportAddress();
        int count = readLEB();
        listener.sectionStart(count);
        for (var i = 0; i < count; ++i) {
            parseType();
        }
        reportAddress();
        listener.sectionEnd();
        typeIndex = 0;
    }

    private void parseType() {
        if (data[ptr] == 0x4E) {
            parseRecType();
        } else {
            parseSubtype();
        }
    }

    private void parseRecType() {
        reportAddress();
        ++ptr;
        var count = readLEB();
        listener.startRecType(count);
        for (var i = 0; i < count; ++i) {
            parseSubtype();
        }
        listener.endRecType();
    }

    private void parseSubtype() {
        switch (data[ptr]) {
            case 0x50:
                reportAddress();
                ++ptr;
                parseCompositeType(true, readSupertypes());
                break;
            case 0x4F:
                reportAddress();
                ++ptr;
                parseCompositeType(false, readSupertypes());
                break;
            default:
                parseCompositeType(true, new int[0]);
                break;
        }
    }

    private void parseCompositeType(boolean open, int[] supertypes) {
        reportAddress();
        listener.startType(typeIndex++, open, supertypes);
        switch (data[ptr]) {
            case 0x5E:
                reportAddress();
                ++ptr;
                listener.startArrayType();
                parseField();
                listener.endArrayType();
                break;
            case 0x5F: {
                reportAddress();
                ++ptr;
                var fieldCount = readLEB();
                listener.startStructType(fieldCount);
                for (var i = 0; i < fieldCount; ++i) {
                    parseField();
                }
                listener.endStructType();
                break;
            }
            case 0x60: {
                reportAddress();
                ++ptr;
                var paramCount = readLEB();
                listener.funcType(paramCount);
                for (var i = 0; i < paramCount; ++i) {
                    reportAddress();
                    listener.resultType(readType());
                }
                var resultCount = readLEB();
                listener.funcTypeResults(resultCount);
                for (var i = 0; i < resultCount; ++i) {
                    reportAddress();
                    listener.resultType(readType());
                }
                listener.endFuncType();
                break;
            }
            default:
                throw new ParseException("Unknown type declaration", ptr);
        }
        listener.endType();
    }

    private void parseField() {
        reportAddress();
        var type = readStorageType();
        var mutable = data[ptr++] != 0;
        listener.field(type, mutable);
    }

    private int[] readSupertypes() {
        var count = readLEB();
        var result = new int[count];
        for (var i = 0; i < count; ++i) {
            result[i] = readLEB();
        }
        return result;
    }
}
