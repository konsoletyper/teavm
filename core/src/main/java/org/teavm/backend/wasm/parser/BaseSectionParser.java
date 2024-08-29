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

public abstract class BaseSectionParser {
    protected WasmBinaryReader reader;

    public void parse(AddressListener addressListener, byte[] data) {
        parse(new WasmBinaryReader(addressListener, data));
    }

    public void parse(WasmBinaryReader reader) {
        this.reader = reader;
        try {
            parseContent();
        } finally {
            this.reader = null;
        }
    }

    protected abstract void parseContent();

    protected void reportAddress() {
        reader.reportAddress();
    }

    protected int readSignedLEB() {
        return reader.readSignedLEB();
    }

    protected int readLEB() {
        return reader.readLEB();
    }

    protected long readSignedLongLEB() {
        return reader.readSignedLongLEB();
    }

    protected long readLongLEB() {
        return reader.readLongLEB();
    }

    protected int readFixedInt() {
        return reader.readFixedInt();
    }

    protected long readFixedLong() {
        return reader.readFixedLong();
    }

    protected String readString() {
        return reader.readString();
    }
}
