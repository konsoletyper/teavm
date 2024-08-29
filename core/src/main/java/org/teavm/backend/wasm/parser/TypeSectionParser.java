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

    public TypeSectionParser(TypeSectionListener listener) {
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
        if (reader.data[reader.ptr] == 0x4E) {
            parseRecType();
        } else {
            parseSubtype();
        }
    }

    private void parseRecType() {
        reportAddress();
        ++reader.ptr;
        var count = readLEB();
        listener.startRecType(count);
        for (var i = 0; i < count; ++i) {
            parseSubtype();
        }
        listener.endRecType();
    }

    private void parseSubtype() {
        switch (reader.data[reader.ptr]) {
            case 0x50:
                reportAddress();
                ++reader.ptr;
                parseCompositeType(true, readSupertypes());
                break;
            case 0x4F:
                reportAddress();
                ++reader.ptr;
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
        switch (reader.data[reader.ptr]) {
            case 0x5E:
                reportAddress();
                ++reader.ptr;
                listener.startArrayType();
                parseField();
                listener.endArrayType();
                break;
            case 0x5F: {
                reportAddress();
                ++reader.ptr;
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
                ++reader.ptr;
                var paramCount = readLEB();
                listener.funcType(paramCount);
                for (var i = 0; i < paramCount; ++i) {
                    reportAddress();
                    listener.resultType(reader.readType());
                }
                var resultCount = readLEB();
                listener.funcTypeResults(resultCount);
                for (var i = 0; i < resultCount; ++i) {
                    reportAddress();
                    listener.resultType(reader.readType());
                }
                listener.endFuncType();
                break;
            }
            default:
                throw new ParseException("Unknown type declaration", reader.ptr);
        }
        listener.endType();
    }

    private void parseField() {
        reportAddress();
        var type = reader.readStorageType();
        var mutable = reader.data[reader.ptr++] != 0;
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
