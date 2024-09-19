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
package org.teavm.gradle.tasks;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.builder.BuildStrategy;

public abstract class GenerateWasmGCTask extends TeaVMTask {
    private static final int MB = 1024 * 1024;

    public GenerateWasmGCTask() {
        getStrict().convention(true);
        getObfuscated().convention(true);
    }

    @Input
    public abstract Property<Boolean> getStrict();

    @Input
    public abstract Property<Boolean> getObfuscated();

    @Override
    protected void setupBuilder(BuildStrategy builder) {
        builder.setStrict(getStrict().get());
        builder.setObfuscated(getObfuscated().get());
        builder.setTargetType(TeaVMTargetType.WEBASSEMBLY_GC);
    }
}
