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
package org.teavm.backend.wasm.debug.info;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class VariablesInfo {
    public abstract List<? extends VariableRangeInfo> ranges();

    public Collection<? extends VariableRangeInfo> find(int address) {
        var result = new ArrayList<VariableRangeInfo>();
        for (var range : ranges()) {
            if (address >= range.start() && address < range.end()) {
                result.add(range);
            }
        }
        return result;
    }

    public void dump(PrintStream out) {
        for (var range : ranges()) {
            out.println(range.variable().name() + ": " + range.variable().type() + " - "
                + Integer.toHexString(range.start()) + ".." + Integer.toHexString(range.end()) + " at "
                + range.index());
        }
    }
}
