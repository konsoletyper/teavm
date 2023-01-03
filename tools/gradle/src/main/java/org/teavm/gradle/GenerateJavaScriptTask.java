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

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.builder.BuildStrategy;

public abstract class GenerateJavaScriptTask extends TeaVMTask {
    public GenerateJavaScriptTask() {
        getObfuscated().convention(true);
        getStrict().convention(false);
        getSourceMap().convention(false);
        getEntryPointName().convention("main");
    }

    @Input
    @Optional
    public abstract Property<Boolean> getObfuscated();

    @Input
    @Optional
    public abstract Property<Boolean> getStrict();

    @Input
    @Optional
    public abstract Property<Boolean> getSourceMap();

    @Input
    @Optional
    public abstract Property<String> getEntryPointName();

    @Override
    protected void setupBuilder(BuildStrategy builder) {
        builder.setTargetType(TeaVMTargetType.JAVASCRIPT);
        builder.setObfuscated(getObfuscated().get());
        builder.setStrict(getStrict().get());
        builder.setSourceMapsFileGenerated(getSourceMap().get());
        builder.setEntryPointName(getEntryPointName().get());
    }
}
