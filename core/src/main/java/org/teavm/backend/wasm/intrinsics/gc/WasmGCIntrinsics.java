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
package org.teavm.backend.wasm.intrinsics.gc;

import java.util.HashMap;
import java.util.Map;
import org.teavm.backend.wasm.generate.gc.methods.WasmGCIntrinsicProvider;
import org.teavm.model.MethodReference;

public class WasmGCIntrinsics implements WasmGCIntrinsicProvider {
    private Map<MethodReference, WasmGCIntrinsic> intrinsics = new HashMap<>();

    public WasmGCIntrinsics() {
        fillObject();
        fillClass();
        fillSystem();
    }

    private void fillObject() {
        var objectIntrinsics = new ObjectIntrinsics();
        intrinsics.put(new MethodReference(Object.class, "getClass", Class.class), objectIntrinsics);
    }

    private void fillClass() {
        var classIntrinsics = new ClassIntrinsics();
        intrinsics.put(new MethodReference(Class.class, "getComponentType", Class.class), classIntrinsics);
    }

    private void fillSystem() {
        intrinsics.put(new MethodReference(System.class, "arraycopy", Object.class, int.class, Object.class,
                int.class, int.class, void.class), new SystemArrayCopyIntrinsic());
    }

    @Override
    public WasmGCIntrinsic get(MethodReference method) {
        return intrinsics.get(method);
    }
}
