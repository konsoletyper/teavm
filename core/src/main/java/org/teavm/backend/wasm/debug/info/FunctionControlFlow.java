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

    FunctionControlFlow(int[] offsets, int[] data) {
        this.offsets = offsets;
        this.data = data;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    public FunctionControlFlowIterator iterator() {
        return new FunctionControlFlowIterator(this);
    }
}
