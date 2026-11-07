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

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

public interface TeaVMCommonConfiguration {
    Property<String> getMainClass();

    Property<Boolean> getDebugInformation();

    Property<Boolean> getFastGlobalAnalysis();

    /**
     * Whether coroutine transformation is enabled, i.e. whether async methods (like {@code Thread.sleep}
     * or {@code Object.wait}) are allowed. When disabled, TeaVM reports error for each reachable async method.
     */
    Property<Boolean> getAsyncEnabled();

    /**
     * Whether Java assertions ({@code assert} statements) should be removed from generated code.
     * When enabled, classes and methods reachable only from assertions are not included into
     * the output either.
     */
    Property<Boolean> getAssertionsRemoved();

    Property<OptimizationLevel> getOptimization();

    MapProperty<String, String> getProperties();

    ListProperty<String> getPreservedClasses();

    Property<Boolean> getOutOfProcess();

    Property<Integer> getProcessMemory();

    DirectoryProperty getOutputDir();
}
