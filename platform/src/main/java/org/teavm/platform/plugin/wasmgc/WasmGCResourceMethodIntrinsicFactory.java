/*
 *  Copyright 2024 konsoletyper.
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
package org.teavm.platform.plugin.wasmgc;

import org.teavm.backend.wasm.intrinsics.WasmGCCodeGenContext;
import org.teavm.backend.wasm.intrinsics.WasmGCCodeGenContributor;
import org.teavm.backend.wasm.intrinsics.WasmGCCodeGenRegistry;

public class WasmGCResourceMethodIntrinsicFactory implements WasmGCCodeGenContributor {
    @Override
    public void contribute(WasmGCCodeGenContext context, WasmGCCodeGenRegistry registry) {
        registry.inlineIntrinsics().registerIntrinsic(methodRef -> {
            var cls = context.classes().get(methodRef.getClassName());
            if (cls == null) {
                return null;
            }
            var method = cls.getMethod(methodRef.getDescriptor());
            if (method == null) {
                return null;
            }
            var annot = method.getAnnotations().get(FieldMarker.class.getName());
            if (annot == null) {
                return null;
            }
            var index = annot.getValue("index").getInt();
            return new ResourceMethodIntrinsic(context.typeMapper(), index);
        });
    }
}
