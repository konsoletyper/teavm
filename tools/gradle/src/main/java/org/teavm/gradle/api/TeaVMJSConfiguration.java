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
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;

public interface TeaVMJSConfiguration extends TeaVMWebConfiguration {
    Property<Boolean> getObfuscated();

    Property<Boolean> getStrict();

    Property<JSModuleType> getModuleType();

    Property<Boolean> getSourceMap();

    Property<String> getEntryPointName();

    Property<SourceFilePolicy> getSourceFilePolicy();

    Property<Integer> getMaxTopLevelNames();

    @Nested
    TeaVMJSDevServerConfiguration getDevServer();

    default void devServer(Action<TeaVMJSDevServerConfiguration> action) {
        action.execute(getDevServer());
    }

    default void devServer(@DelegatesTo(TeaVMJSDevServerConfiguration.class) Closure<?> action) {
        action.rehydrate(getDevServer(), action.getOwner(), action.getThisObject()).call();
    }
}
