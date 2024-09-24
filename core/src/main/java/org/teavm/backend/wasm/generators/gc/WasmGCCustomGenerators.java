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
package org.teavm.backend.wasm.generators.gc;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.teavm.backend.wasm.generate.gc.methods.WasmGCCustomGeneratorProvider;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.runtime.gc.WasmGCResources;
import org.teavm.backend.wasm.runtime.gc.WasmGCSupport;
import org.teavm.common.ServiceRepository;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;

public class WasmGCCustomGenerators implements WasmGCCustomGeneratorProvider {
    private List<WasmGCCustomGeneratorFactory> factories;
    private Map<MethodReference, Container> generators = new HashMap<>();
    private ClassReaderSource classes;
    private ServiceRepository services;
    private WasmGCResourcesGenerator resourcesGenerator;

    public WasmGCCustomGenerators(ClassReaderSource classes, ServiceRepository services,
            List<WasmGCCustomGeneratorFactory> factories,
            Map<MethodReference, WasmGCCustomGenerator> generators,
            Properties properties) {
        this.factories = List.copyOf(factories);
        this.classes = classes;
        this.services = services;
        resourcesGenerator = new WasmGCResourcesGenerator(properties);
        fillClass();
        fillStringPool();
        fillSystem();
        fillArray();
        fillWeakReference();
        fillString();
        fillResources();
        for (var entry : generators.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
    }

    public void contributeToModule(WasmModule module) {
        resourcesGenerator.writeModule(module);
    }

    private void fillClass() {
        var classGenerators = new ClassGenerators();
        add(new MethodReference(Class.class, "isAssignableFrom", Class.class, boolean.class), classGenerators);
    }

    private void fillStringPool() {
        add(new MethodReference(WasmGCSupport.class, "nextByte", byte.class), new WasmGCStringPoolGenerator());
    }

    private void fillSystem() {
        add(
                new MethodReference(System.class, "doArrayCopy", Object.class, int.class, Object.class,
                        int.class, int.class, void.class),
                new SystemDoArrayCopyGenerator()
        );
    }

    private void fillArray() {
        var arrayGenerator = new ArrayGenerator();
        add(new MethodReference(Array.class, "newInstanceImpl", Class.class, int.class, Object.class), arrayGenerator);
    }

    private void fillWeakReference() {
        var generator = new WeakReferenceGenerator();
        add(new MethodReference(WeakReference.class, "<init>", Object.class, ReferenceQueue.class,
                void.class), generator);
        add(new MethodReference(WeakReference.class, "get", Object.class), generator);
        add(new MethodReference(WeakReference.class, "clear", void.class), generator);
    }

    private void fillString() {
        var generator = new StringGenerator();
        add(new MethodReference(String.class, "intern", String.class), generator);
    }

    private void fillResources() {
        add(new MethodReference(WasmGCResources.class, "acquireResources", WasmGCResources.Resource[].class),
                resourcesGenerator);
    }

    @Override
    public WasmGCCustomGenerator get(MethodReference method) {
        var result = generators.get(method);
        if (result == null) {
            WasmGCCustomGenerator generator = null;
            for (var factory : factories) {
                generator = factory.createGenerator(method, factoryContext);
            }
            result = new Container(generator);
            generators.put(method, result);
        }
        return result.generator;
    }

    private void add(MethodReference method, WasmGCCustomGenerator generator) {
        generators.put(method, new Container(generator));
    }

    private static class Container {
        final WasmGCCustomGenerator generator;

        Container(WasmGCCustomGenerator generator) {
            this.generator = generator;
        }
    }

    private WasmGCCustomGeneratorFactoryContext factoryContext = new WasmGCCustomGeneratorFactoryContext() {
        @Override
        public ClassReaderSource classes() {
            return classes;
        }

        @Override
        public ServiceRepository services() {
            return services;
        }
    };
}
