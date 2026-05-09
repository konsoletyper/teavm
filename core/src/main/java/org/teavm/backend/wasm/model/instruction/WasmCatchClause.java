/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.model.instruction;

import java.util.Objects;
import org.teavm.backend.wasm.model.WasmTag;

public class WasmCatchClause {
    private WasmTag tag;
    private boolean ref;
    private WasmInstructionList target;

    public WasmCatchClause(WasmTag tag, boolean ref, WasmInstructionList target) {
        this.tag = tag;
        this.ref = ref;
        this.target = Objects.requireNonNull(target);
    }

    public WasmTag getTag() {
        return tag;
    }

    public void setTag(WasmTag tag) {
        this.tag = tag;
    }

    public boolean isRef() {
        return ref;
    }

    public void setRef(boolean ref) {
        this.ref = ref;
    }

    public WasmInstructionList getTarget() {
        return target;
    }

    public void setTarget(WasmInstructionList target) {
        this.target = Objects.requireNonNull(target);
    }
}
