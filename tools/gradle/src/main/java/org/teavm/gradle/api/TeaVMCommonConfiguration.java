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

import java.io.File;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

public interface TeaVMCommonConfiguration {
    Property<String> getMainClass();

    Property<Boolean> getDebugInformation();

    Property<Boolean> getFastGlobalAnalysis();

    Property<OptimizationLevel> getOptimization();

    MapProperty<String, String> getProperties();

    ListProperty<String> getPreservedClasses();

    Property<Boolean> getOutOfProcess();

    Property<Integer> getProcessMemory();

    Property<File> getOutputDir();
}
