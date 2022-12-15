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

import java.util.Arrays;

public class FunctionControlFlowIterator {
    private FunctionControlFlow controlFlow;
    private int index;
    private boolean valid;
    private int offset;
    private boolean isCall;

    FunctionControlFlowIterator(FunctionControlFlow controlFlow, int index) {
        this.controlFlow = controlFlow;
        this.index = index;
    }

    public boolean hasNext() {
        return index < controlFlow.offsets.length;
    }

    public void next() {
        ++index;
        valid = false;
    }

    public void rewind(int index) {
        if (this.index != index) {
            this.index = index;
            fill();
        }
    }

    private void fill() {
        if (!valid) {
            valid = true;
            var n = controlFlow.offsets[index];
            offset = n >>> 1;
            isCall = (n & 1) != 0;
        }
    }

    public int address() {
        fill();
        return controlFlow.data[offset];
    }

    public int[] targets() {
        fill();
        var nextOffset = index < controlFlow.offsets.length - 1
                ? controlFlow.offsets[index + 1] >>> 1
                : controlFlow.data.length;
        return Arrays.copyOfRange(controlFlow.data, offset + 1, nextOffset);
    }

    public boolean isCall() {
        fill();
        return isCall;
    }
}
