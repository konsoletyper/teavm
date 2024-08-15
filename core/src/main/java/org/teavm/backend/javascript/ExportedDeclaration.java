/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.javascript;

import java.util.function.Consumer;
import org.teavm.backend.javascript.codegen.NamingStrategy;
import org.teavm.backend.javascript.codegen.SourceWriter;

public class ExportedDeclaration {
    final Consumer<SourceWriter> name;
    final Consumer<NamingStrategy> nameFreq;
    final String alias;

    public ExportedDeclaration(Consumer<SourceWriter> name, Consumer<NamingStrategy> nameFreq, String alias) {
        this.name = name;
        this.nameFreq = nameFreq;
        this.alias = alias;
    }
}
