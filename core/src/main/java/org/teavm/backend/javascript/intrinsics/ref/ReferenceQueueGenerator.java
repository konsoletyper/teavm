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
package org.teavm.backend.javascript.intrinsics.ref;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;

public class ReferenceQueueGenerator implements Generator {
    private static final FieldReference INNER_FIELD = new FieldReference(ReferenceQueue.class.getName(), "inner");
    private static final FieldReference REGISTRY_FIELD = new FieldReference(ReferenceQueue.class.getName(),
            "registry");

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "<init>":
                generateInitMethod(context, writer);
                break;
            case "poll":
                generatePollMethod(context, writer);
                break;
        }
    }

    private void generateInitMethod(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append(context.getParameterName(0)).append(".").appendField(INNER_FIELD).ws().append("=")
                .ws().append("[];").softNewLine();
        writer.append(context.getParameterName(0)).append(".").appendField(REGISTRY_FIELD).ws().append("=")
                .ws().append("new $rt_globals.FinalizationRegistry(x").ws().append("=>").ws()
                .append(context.getParameterName(0)).appendField(INNER_FIELD)
                .append(".push(x));").softNewLine();
    }

    private void generatePollMethod(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append("var value").ws().append("=").ws().append(context.getParameterName(0))
                .append(".").appendField(INNER_FIELD).append(".shift();").softNewLine();
        writer.append("return typeof value").ws().append("!==").ws().append("'undefined'").ws()
                .append("?").ws().append("value").ws().append(":").ws().append("null;").softNewLine();
    }
}
