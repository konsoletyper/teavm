/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.backend.c.generators;

import org.teavm.backend.c.generate.CodeWriter;
import org.teavm.backend.c.generate.FileGenerator;
import org.teavm.backend.c.generate.IncludeManager;
import org.teavm.backend.c.generate.StringPool;
import org.teavm.backend.lowlevel.generate.NameProvider;
import org.teavm.dependency.DependencyInfo;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;

public interface GeneratorContext {
    CodeWriter writer();

    NameProvider names();

    Diagnostics diagnotics();

    ClassReaderSource classSource();

    DependencyInfo dependencies();

    String parameterName(int index);

    StringPool stringPool();

    CodeWriter writerBefore();

    CodeWriter writerAfter();

    IncludeManager includes();

    FileGenerator createSourceFile(String path);

    FileGenerator createHeaderFile(String path);

    String escapeFileName(String name);

    void importMethod(MethodReference method, boolean isStatic);
}
