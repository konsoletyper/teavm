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
package org.teavm.gradle.api;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.gradle.api.Action;

public interface TeaVMExtension extends TeaVMBaseExtension {
    TeaVMJSConfiguration getJs();

    void js(Action<TeaVMJSConfiguration> action);

    void js(@DelegatesTo(TeaVMJSConfiguration.class) Closure<?> action);

    TeaVMWasmConfiguration getWasm();

    void wasm(Action<TeaVMWasmConfiguration> action);

    void wasm(@DelegatesTo(TeaVMWasmConfiguration.class) Closure<?> action);

    TeaVMWasiConfiguration getWasi();

    void wasi(Action<TeaVMWasiConfiguration> action);

    void wasi(@DelegatesTo(TeaVMWasiConfiguration.class) Closure<?> action);

    TeaVMCConfiguration getC();

    void c(Action<TeaVMCConfiguration> action);

    void c(@DelegatesTo(TeaVMCConfiguration.class) Closure<?> action);

    TeaVMCommonConfiguration getAll();

    void all(Action<TeaVMCommonConfiguration> action);

    void all(@DelegatesTo(TeaVMCommonConfiguration.class) Closure<?> action);
}
