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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.teavm.common.CollectionUtil;

public class ControlFlowInfo {
    private List<? extends FunctionControlFlow> functions;

    public ControlFlowInfo(FunctionControlFlow[] functions) {
        this.functions = Collections.unmodifiableList(Arrays.asList(functions));
    }

    public List<? extends FunctionControlFlow> functions() {
        return functions;
    }

    public FunctionControlFlow find(int address) {
        var index = CollectionUtil.binarySearch(functions, address, FunctionControlFlow::end);
        if (index < 0) {
            index = -index - 1;
        }
        if (index > functions.size()) {
            return null;
        }
        var fn = functions.get(index);
        if (fn.start() > address) {
            return fn;
        }
        return fn;
    }

    public void dump(PrintStream out) {
        for (int i = 0; i < functions.size(); ++i) {
            var range = functions.get(i);
            out.println("Range #" + i + ": [" + range.start() + ".." + range.end() + ")");
            for (var iter = range.iterator(0); iter.hasNext(); iter.next()) {
                out.print("  " + Integer.toHexString(iter.address()));
                if (iter.isCall()) {
                    out.print(" (call)");
                }
                out.print(" -> ");
                var followers = iter.targets();
                for (var j = 0; j < followers.length; ++j) {
                    if (j > 0) {
                        out.print(", ");
                    }
                    out.print(Integer.toHexString(followers[j]));
                }
                out.println();
            }
        }
    }
}
