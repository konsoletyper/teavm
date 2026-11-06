/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.junit.spi;

import java.io.File;
import java.util.List;
import java.util.Properties;
import org.teavm.model.MethodReference;
import org.teavm.vm.TeaVMOptimizationLevel;

public interface ExternalCompilationRequest {
    String getEntryPointClassName();

    String getTestClassName();

    List<MethodReference> getTestMethods();

    boolean isWholeClass();

    /**
     * Arguments of Jupiter parameterized tests among {@link #getTestMethods()}, serialized into an opaque string.
     * Pass it as is to {@link org.teavm.junit.TestCompilationPlugins#createEntryPointPlugin}.
     * Empty string if there are no parameterized tests.
     */
    String getParameterizedPlans();

    Properties getProperties();

    TeaVMOptimizationLevel getOptimizationLevel();

    File getOutputFile();

    ClassLoader getClassLoader();
}
