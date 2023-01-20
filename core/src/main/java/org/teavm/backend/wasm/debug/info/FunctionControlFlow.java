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

public class FunctionControlFlow {
    private int start;
    private int end;
    int[] offsets;
    int[] data;

    FunctionControlFlow(int start, int end, int[] offsets, int[] data) {
        this.start = start;
        this.end = end;
        this.offsets = offsets;
        this.data = data;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    public FunctionControlFlowIterator iterator(int index) {
        return new FunctionControlFlowIterator(this, index);
    }

    public int count() {
        return offsets.length;
    }

    public int findIndex(int address) {
        var l = 0;
        var u = offsets.length;
        while (true) {
            var i = (l + u) / 2;
            var t = data[offsets[i]];
            if (address == t) {
                return i;
            } else if (address > t) {
                l = i + 1;
                if (l > u) {
                    return i + 1;
                }
            } else {
                u = i - 1;
                if (u < l) {
                    return i;
                }
            }
        }
    }
}
