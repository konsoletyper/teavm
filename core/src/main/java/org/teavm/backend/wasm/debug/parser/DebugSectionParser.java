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

import java.util.ArrayList;
import java.util.List;
import org.teavm.common.binary.BinaryParser;

public abstract class DebugSectionParser extends BinaryParser {
    private String name;
    private List<DebugSectionParser> dependantSections = new ArrayList<>();
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
}
