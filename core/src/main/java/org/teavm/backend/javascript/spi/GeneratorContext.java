/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.backend.javascript.spi;

import java.util.Properties;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.common.ServiceRepository;
import org.teavm.dependency.DependencyInfo;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public interface GeneratorContext extends ServiceRepository {
    String getParameterName(int index);

    ClassReaderSource getInitialClassSource();

    ListableClassReaderSource getClassSource();

    ClassLoader getClassLoader();

    Properties getProperties();

    boolean isAsync();

    boolean isAsync(MethodReference method);

    boolean isAsyncFamily(MethodReference method);

    Diagnostics getDiagnostics();

    DependencyInfo getDependency();

    void typeToClassString(SourceWriter writer, ValueType type);

    void useLongLibrary();
}
