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

import com.carrotsearch.hppc.IntArrayList;

public class FunctionControlFlowBuilder {
    private int start;
    private int end;
    private IntArrayList offsets = new IntArrayList();
    private IntArrayList data = new IntArrayList();

    public FunctionControlFlowBuilder(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public void addBranch(int position, int[] targets) {
        offsets.add(data.size() << 1);
        data.add(position);
        data.add(targets);
    }

    public void addCall(int position, int[] targets) {
        offsets.add((data.size() << 1) | 1);
        data.add(position);
        data.add(targets);
    }

    public boolean isEmpty() {
        return offsets.isEmpty();
    }

    public FunctionControlFlow build() {
        return new FunctionControlFlow(start, end, offsets.toArray(), data.toArray());
    }
}
