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
import java.util.List;
import java.util.Map;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.generate.gc.methods.WasmGCIntrinsicProvider;
import org.teavm.model.MethodReference;

public class WasmGCIntrinsics implements WasmGCIntrinsicProvider {
    private Map<MethodReference, WasmGCIntrinsic> intrinsics = new HashMap<>();

    public WasmGCIntrinsics() {
        fillWasmRuntime();
        fillObject();
        fillClass();
        fillSystem();
        fillLong();
    }

    private void fillWasmRuntime() {
        var intrinsic = new WasmRuntimeIntrinsic();
        for (var cls : List.of(int.class, long.class, float.class, double.class)) {
            intrinsics.put(new MethodReference(WasmRuntime.class, "lt", cls, cls, boolean.class), intrinsic);
            intrinsics.put(new MethodReference(WasmRuntime.class, "gt", cls, cls, boolean.class), intrinsic);
        }
        for (var cls : List.of(int.class, long.class)) {
            intrinsics.put(new MethodReference(WasmRuntime.class, "ltu", cls, cls, boolean.class), intrinsic);
            intrinsics.put(new MethodReference(WasmRuntime.class, "gtu", cls, cls, boolean.class), intrinsic);
        }
        for (var cls : List.of(float.class, double.class)) {
            intrinsics.put(new MethodReference(WasmRuntime.class, "min", cls, cls, cls), intrinsic);
            intrinsics.put(new MethodReference(WasmRuntime.class, "max", cls, cls, cls), intrinsic);
        }
    }

    private void fillObject() {
        var objectIntrinsics = new ObjectIntrinsics();
        intrinsics.put(new MethodReference(Object.class, "getClass", Class.class), objectIntrinsics);
    }

    private void fillClass() {
        var intrinsic = new ClassIntrinsics();
        intrinsics.put(new MethodReference(Class.class, "getComponentType", Class.class), intrinsic);
        intrinsics.put(new MethodReference(Class.class, "getNameImpl", String.class), intrinsic);
        intrinsics.put(new MethodReference(Class.class, "setNameImpl", String.class, void.class), intrinsic);
    }

    private void fillSystem() {
        intrinsics.put(new MethodReference(System.class, "arraycopy", Object.class, int.class, Object.class,
                int.class, int.class, void.class), new SystemArrayCopyIntrinsic());
    }

    private void fillLong() {
        var intrinsic = new LongIntrinsic();
        intrinsics.put(new MethodReference(Long.class, "divideUnsigned", long.class, long.class, long.class),
                intrinsic);
        intrinsics.put(new MethodReference(Long.class, "remainderUnsigned", long.class, long.class, long.class),
                intrinsic);
        intrinsics.put(new MethodReference(Long.class, "compareUnsigned", long.class, long.class, int.class),
                intrinsic);
    }

    @Override
    public WasmGCIntrinsic get(MethodReference method) {
        return intrinsics.get(method);
    }
}
