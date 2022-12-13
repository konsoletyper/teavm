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
package org.teavm.backend.wasm.debug;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.teavm.backend.wasm.debug.info.VariableType;

public class DebugVariablesBuilder extends DebugSectionBuilder implements DebugVariables {
    private DebugStrings strings;
    private int sequenceStart;
    private int lastSequenceStart;
    private Map<String, VarInfo> variables = new LinkedHashMap<>();

    public DebugVariablesBuilder(DebugStrings strings) {
        super(DebugConstants.SECTION_VARIABLES);
        this.strings = strings;
    }

    @Override
    public void startSequence(int pointer) {
        sequenceStart = pointer;
    }

    @Override
    public void type(String name, VariableType type) {
        getInfo(name).type = type;
    }

    @Override
    public void range(String name, int start, int end, int pointer) {
        getInfo(name).ranges.add(new Range(start, end, pointer));
    }

    private VarInfo getInfo(String name) {
        var info = variables.get(name);
        if (info == null) {
            info = new VarInfo();
            variables.put(name, info);
        }
        return info;
    }

    @Override
    public void endSequence() {
        if (variables.isEmpty()) {
            return;
        }
        blob.writeLEB(sequenceStart - lastSequenceStart);
        lastSequenceStart = sequenceStart;
        blob.writeLEB(variables.size());
        for (var variable : variables.entrySet()) {
            blob.writeLEB(strings.stringPtr(variable.getKey()));
            var info = variable.getValue();
            blob.writeLEB(info.type.ordinal());
            blob.writeLEB(info.ranges.size());
            var lastPtr = sequenceStart;
            var lastPointer = 0;
            for (var range : info.ranges) {
                blob.writeSLEB(range.start - lastPtr);
                blob.writeLEB(range.end - range.start);
                blob.writeSLEB(range.pointer - lastPointer);
                lastPointer = range.pointer;
                lastPointer = range.end;
            }
        }
        variables.clear();
    }

    private static class VarInfo {
        VariableType type = VariableType.UNDEFINED;
        List<Range> ranges = new ArrayList<>();
    }

    private static class Range {
        int start;
        int end;
        int pointer;

        Range(int start, int end, int pointer) {
            this.start = start;
            this.end = end;
            this.pointer = pointer;
        }
    }
}
