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
package org.teavm.jso.impl.wasmgc;

import org.teavm.backend.wasm.generate.classes.WasmGCTypeMapper;
import org.teavm.backend.wasm.intrinsics.WasmGCBodyIntrinsic;
import org.teavm.jso.impl.JSMethods;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.vm.intrinsic.IntrinsicProvider;

class WasmGCMarshallMethodGeneratorFactory implements IntrinsicProvider<WasmGCBodyIntrinsic> {
    private WasmGCJsoCommonGenerator commonGen;
    private ClassReaderSource classes;
    private WasmGCTypeMapper typeMapper;

    WasmGCMarshallMethodGeneratorFactory(WasmGCJsoCommonGenerator commonGen, ClassReaderSource classes,
            WasmGCTypeMapper typeMapper) {
        this.commonGen = commonGen;
        this.classes = classes;
        this.typeMapper = typeMapper;
    }

    @Override
    public WasmGCBodyIntrinsic getIntrinsic(MethodReference method) {
        if (!method.getName().equals(JSMethods.MARSHALL_TO_JS.getName())) {
            return null;
        }
        var cls = classes.get(method.getClassName());
        return cls != null && cls.getInterfaces().contains(JSMethods.JS_MARSHALLABLE)
                ? new WasmGCMarshallMethodGenerator(commonGen, typeMapper)
                : null;
    }
}
