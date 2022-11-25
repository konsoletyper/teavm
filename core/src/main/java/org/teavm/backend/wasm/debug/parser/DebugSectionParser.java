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
package org.teavm.backend.wasm.debug.parser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public abstract class DebugSectionParser {
    private String name;
    private List<DebugSectionParser> dependantSections = new ArrayList<>();
    byte[] data;
    int ptr;
    private boolean ready;
    private int unsatisfiedDependencies;

    protected DebugSectionParser(String name, DebugSectionParser... dependencies) {
        this.name = name;
        for (var dependency : dependencies) {
            dependency.dependantSections.add(this);
        }
        unsatisfiedDependencies = dependencies.length;
    }

    public String name() {
        return name;
    }

    public boolean ready() {
        return ready;
    }

    public void parse(byte[] data) {
        this.data = data;
        if (unsatisfiedDependencies == 0) {
            parse();
        }
    }

    private void parse() {
        doParse();
        ready = true;
        data = null;
        for (var dependant : dependantSections) {
            if (--dependant.unsatisfiedDependencies == 0 && dependant.data != null) {
                dependant.parse();
            }
        }
    }

    protected abstract void doParse();

    protected int readLEB() {
        var result = 0;
        var shift = 0;
        while (true) {
            var b = data[ptr++];
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                break;
            }
            shift += 7;
        }
        return result;
    }

    protected int readSignedLEB() {
        var result = 0;
        var shift = 0;
        while (true) {
            var b = data[ptr++];
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                if ((b & 0x40) != 0) {
                    result |= -1 << (shift + 7);
                }
                break;
            }
            shift += 7;
        }
        return result;
    }

    protected String readString() {
        var length = readLEB();
        var result = new String(data, ptr, length, StandardCharsets.UTF_8);
        ptr += length;
        return result;
    }
}
