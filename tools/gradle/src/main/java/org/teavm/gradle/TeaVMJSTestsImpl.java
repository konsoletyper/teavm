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
import org.teavm.gradle.api.TeaVMJSTests;
import org.teavm.gradle.api.TeaVMWebTestRunner;

class TeaVMJSTestsImpl implements TeaVMJSTests {
    private Property<Boolean> enabled;
    private Property<TeaVMWebTestRunner> runner;
    private Property<Boolean> decodeStack;

    TeaVMJSTestsImpl(ObjectFactory objectFactory) {
        enabled = objectFactory.property(Boolean.class);
        runner = objectFactory.property(TeaVMWebTestRunner.class);
        decodeStack = objectFactory.property(Boolean.class);
    }

    @Override
    public Property<Boolean> getEnabled() {
        return enabled;
    }

    @Override
    public Property<TeaVMWebTestRunner> getRunner() {
        return runner;
    }

    @Override
    public Property<Boolean> getDecodeStack() {
        return decodeStack;
    }

    void configure(TeaVMBaseExtensionImpl extension) {
        enabled.convention(extension.property("tests.js.enabled").map(Boolean::parseBoolean).orElse(false));
        runner.convention(extension.property("tests.js.runner").map(s -> TeaVMWebTestRunner.valueOf(s.toUpperCase()))
                .orElse(TeaVMWebTestRunner.CHROME));
        decodeStack.convention(extension.property("tests.js.decodeStack").map(Boolean::parseBoolean).orElse(true));
    }
}
