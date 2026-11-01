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
package org.teavm.compiler.bootstrap.junit;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface CompileRequest extends JSObject {
    @JSProperty
    String getOutputDirectory();

    @JSProperty
    String getOutputFileName();

    @JSProperty
    String getEntryPoint();

    @JSProperty
    String getTestClass();

    @JSProperty
    String[] getTestMethods();

    @JSProperty
    boolean isWholeClass();

    /**
     * Serialized arguments of Jupiter parameterized tests,
     * see {@link org.teavm.junit.spi.ExternalCompilationRequest#getParameterizedPlans()}.
     */
    @JSProperty
    String getParameterizedPlans();

    /**
     * Properties as a flat array: key1, value1, key2, value2, ...
     */
    @JSProperty
    String[] getProperties();

    @JSProperty
    String getOptimizationLevel();
}
