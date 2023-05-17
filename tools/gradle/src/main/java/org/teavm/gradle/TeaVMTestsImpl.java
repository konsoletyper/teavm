/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.gradle;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.teavm.gradle.api.TeaVMJSTests;
import org.teavm.gradle.api.TeaVMTests;
import org.teavm.gradle.api.TeaVMWasmTests;

class TeaVMTestsImpl implements TeaVMTests {
    private TeaVMJSTestsImpl js;
    private TeaVMWasmTestsImpl wasm;

    TeaVMTestsImpl(ObjectFactory objectFactory) {
        js = new TeaVMJSTestsImpl(objectFactory);
        wasm = new TeaVMWasmTestsImpl(objectFactory);
    }

    @Override
    public TeaVMJSTests getJs() {
        return js;
    }

    @Override
    public void js(Action<TeaVMJSTests> config) {
        config.execute(js);
    }

    @Override
    public void js(Closure<?> config) {
        config.rehydrate(getJs(), config.getOwner(), config.getThisObject()).call();
    }

    @Override
    public TeaVMWasmTests getWasm() {
        return wasm;
    }

    @Override
    public void wasm(Action<TeaVMWasmTests> config) {
        config.execute(wasm);
    }

    @Override
    public void wasm(Closure<?> config) {
        config.rehydrate(getWasm(), config.getOwner(), config.getThisObject()).call();
    }

    void configure(TeaVMBaseExtensionImpl extension) {
        js.configure(extension);
        wasm.configure(extension);
    }
}
