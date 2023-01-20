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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.teavm.backend.wasm.debug.DebugConstants;
import org.teavm.backend.wasm.debug.info.VariableInfo;
import org.teavm.backend.wasm.debug.info.VariableRangeInfo;
import org.teavm.backend.wasm.debug.info.VariableType;
import org.teavm.backend.wasm.debug.info.VariablesInfo;

public class DebugVariablesParser extends DebugSectionParser {
    private static final VariableType[] typeByOrdinal = VariableType.values();
    private DebugStringParser strings;
    private VariablesInfoImpl variablesInfo;

    public DebugVariablesParser(DebugStringParser strings) {
        super(DebugConstants.SECTION_VARIABLES, strings);
        this.strings = strings;
    }

    public VariablesInfoImpl getVariablesInfo() {
        return variablesInfo;
    }

    @Override
    protected void doParse() {
        var lastAddress = 0;
        var ranges = new ArrayList<VariableRangeInfo>();
        var localRanges = new ArrayList<VariableRangeInfoImpl>();
        while (ptr < data.length) {
            var baseAddress = lastAddress + readLEB();
            lastAddress = baseAddress;
            var variableCount = readLEB();
            for (var i = 0; i < variableCount; ++i) {
                var name = strings.getString(readLEB());
                var type = typeByOrdinal[readLEB()];
                var rangeCount = readLEB();
                var varInfo = new VariableInfoImpl(name, type);
                var address = baseAddress;
                var lastLocation = 0;
                for (var j = 0; j < rangeCount; ++j) {
                    var start = address + readSignedLEB();
                    var size = readLEB();
                    var end = start + size;
                    address = end;
                    var location = lastLocation + readSignedLEB();
                    lastLocation = location;
                    var rangeInfo = new VariableRangeInfoImpl(varInfo, start, end, location);
                    ranges.add(rangeInfo);
                    localRanges.add(rangeInfo);
                }
                localRanges.clear();
                varInfo.ranges = Collections.unmodifiableList(Arrays.asList(
                        localRanges.toArray(new VariableRangeInfoImpl[0])));
            }
        }
        ranges.sort(Comparator.comparing(VariableRangeInfo::start));

        this.variablesInfo = new VariablesInfoImpl(Collections.unmodifiableList(Arrays.asList(
                ranges.toArray(new VariableRangeInfoImpl[0]))));
    }

    private static class VariablesInfoImpl extends VariablesInfo {
        private List<VariableRangeInfoImpl> ranges;

        VariablesInfoImpl(List<VariableRangeInfoImpl> ranges) {
            this.ranges = ranges;
        }

        @Override
        public List<? extends VariableRangeInfo> ranges() {
            return ranges;
        }
    }

    private static class VariableInfoImpl extends VariableInfo {
        private String name;
        private VariableType type;
        private List<VariableRangeInfoImpl> ranges;

        VariableInfoImpl(String name, VariableType type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public VariableType type() {
            return type;
        }

        @Override
        public Collection<? extends VariableRangeInfo> ranges() {
            return ranges;
        }
    }

    private static class VariableRangeInfoImpl extends VariableRangeInfo {
        private VariableInfoImpl variableInfo;
        private int start;
        private int end;
        private int index;

        VariableRangeInfoImpl(VariableInfoImpl variableInfo, int start, int end, int index) {
            this.variableInfo = variableInfo;
            this.start = start;
            this.end = end;
            this.index = index;
        }

        @Override
        public VariableInfo variable() {
            return variableInfo;
        }

        @Override
        public int start() {
            return start;
        }

        @Override
        public int end() {
            return end;
        }

        @Override
        public int index() {
            return index;
        }
    }
}
