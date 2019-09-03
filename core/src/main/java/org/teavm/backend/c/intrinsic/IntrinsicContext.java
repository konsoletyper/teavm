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
package org.teavm.backend.c.intrinsic;

import org.teavm.ast.Expr;
import org.teavm.backend.c.generate.CodeWriter;
import org.teavm.backend.c.generate.IncludeManager;
import org.teavm.backend.c.generate.StringPool;
import org.teavm.backend.lowlevel.generate.NameProvider;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;

public interface IntrinsicContext {
    CodeWriter writer();

    NameProvider names();

    void emit(Expr expr);

    Diagnostics diagnotics();

    MethodReference callingMethod();

    StringPool stringPool();

    IncludeManager includes();

    String escapeFileName(String name);

    ClassReaderSource classes();

    void importMethod(MethodReference method, boolean isStatic);

    boolean isIncremental();
}
