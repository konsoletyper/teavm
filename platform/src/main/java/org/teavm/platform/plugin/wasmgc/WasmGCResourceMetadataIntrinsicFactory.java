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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicFactory;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicFactoryContext;
import org.teavm.common.ServiceRepository;
import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.MetadataGenerator;

public class WasmGCResourceMetadataIntrinsicFactory implements WasmGCIntrinsicFactory {
    private Properties properties;
    private ServiceRepository services;
    private Map<MethodReference, MetadataGenerator> generators = new HashMap<>();

    public WasmGCResourceMetadataIntrinsicFactory(Properties properties, ServiceRepository services) {
        this.properties = properties;
        this.services = services;
    }

    public void addGenerator(MethodReference method, MetadataGenerator generator) {
        generators.put(method, generator);
    }

    @Override
    public WasmGCIntrinsic createIntrinsic(MethodReference methodRef, WasmGCIntrinsicFactoryContext context) {
        var generator = generators.get(methodRef);
        return generator != null
                ? new MetadataIntrinsic(properties, services, generator)
                : null;
    }
}
