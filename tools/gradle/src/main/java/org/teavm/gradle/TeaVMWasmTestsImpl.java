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

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.teavm.gradle.api.TeaVMWasmTests;
import org.teavm.gradle.api.TeaVMWebTestRunner;

class TeaVMWasmTestsImpl implements TeaVMWasmTests {
    private Property<Boolean> enabled;
    private Property<TeaVMWebTestRunner> runner;

    TeaVMWasmTestsImpl(ObjectFactory objectFactory) {
        enabled = objectFactory.property(Boolean.class);
        runner = objectFactory.property(TeaVMWebTestRunner.class);
    }

    @Override
    public Property<Boolean> getEnabled() {
        return enabled;
    }

    @Override
    public Property<TeaVMWebTestRunner> getRunner() {
        return runner;
    }

    void configure(TeaVMBaseExtensionImpl extension) {
        enabled.convention(extension.property("tests.wasm.enabled").map(Boolean::parseBoolean).orElse(false));
        runner.convention(extension.property("tests.wasm.runner").map(s -> TeaVMWebTestRunner.valueOf(s.toUpperCase()))
                .orElse(TeaVMWebTestRunner.CHROME));
    }
}
