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
package org.teavm.incremental;

import java.io.IOException;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;

public class EntryPointGenerator implements Injector {
    @Override
    public void generate(InjectorContext context, MethodReference methodRef) throws IOException {
        context.getWriter().append("main.result = (");
        context.writeExpr(context.getArgument(0));
        context.getWriter().append(").").appendField(new FieldReference("java.lang.String", "characters"));
        context.getWriter().append(".data");
    }
}
