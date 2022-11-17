/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.backend.javascript.rendering;

import java.util.function.Function;
import org.teavm.backend.javascript.codegen.SourceWriter;

public class DefaultGlobalNameWriter implements Function<String, NameEmitter> {
    private SourceWriter writer;

    public DefaultGlobalNameWriter(SourceWriter writer) {
        this.writer = writer;
    }

    @Override
    public NameEmitter apply(String s) {
        if (s.startsWith("$rt_") || s.startsWith("Long_") || s.equals("Long")) {
            return prec -> writer.append(s);
        }
        return prec -> writer.append("$rt_globals").append('.').append(s);
    }
}
