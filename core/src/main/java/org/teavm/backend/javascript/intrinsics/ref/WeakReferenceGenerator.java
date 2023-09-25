/*
 *  Copyright 2023 konsoletyper.
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
import java.lang.ref.WeakReference;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;

public class WeakReferenceGenerator implements Generator {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "<init>":
                generateConstructor(context, writer);
                break;
            case "get":
                generateGet(context, writer);
                break;
            case "clear":
                generateClear(context, writer);
                break;
        }
    }

    private void generateConstructor(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append("var supported").ws().append("=").ws();
        isSupported(writer).append(";").softNewLine();
        writer.append("var value").ws().append("=").ws().append("supported").ws()
                .append("?").ws().append("new $rt_globals.WeakRef(")
                .append(context.getParameterName(1)).append(")").ws();
        writer.append(":").ws().append(context.getParameterName(0)).append(";").softNewLine();

        writer.append(context.getParameterName(0)).append(".")
                .appendField(new FieldReference(WeakReference.class.getName(), "value"))
                .ws().append("=").ws().append("value;").softNewLine();

        writer.appendIf().append(context.getParameterName(2)).ws().append("!==").ws().append("null")
                .ws().append("&&").ws().append("supported)")
                .appendBlockStart();

        writer.append("var registry").ws().append("=").ws()
                .append(context.getParameterName(2)).append(".")
                .appendField(new FieldReference(ReferenceQueue.class.getName(), "registry")).append(";")
                .softNewLine();
        writer.appendIf().append("registry").ws().append("!==").ws().append("null)").ws();
        writer.append("registry.register(").append(context.getParameterName(1))
                .append(",").ws().append(context.getParameterName(0)).append(");").softNewLine();

        writer.appendBlockEnd();
    }

    private void generateGet(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append("var value").ws().append("=").ws().append(context.getParameterName(0)).append(".")
                .appendField(new FieldReference(WeakReference.class.getName(), "value"))
                .append(";").softNewLine();

        writer.appendIf();
        isSupported(writer).append(")").appendBlockStart();
        writer.appendIf().append("value").ws().append("===").ws().append("null)")
                .ws().append("return null;").softNewLine();
        writer.append("var result").ws().append("=").ws().append("value.deref();").softNewLine();
        writer.append("return typeof result").ws().append("!==").ws().append("'undefined'")
                .ws().append("?").ws().append("result").ws().append(":").ws().append("null;").softNewLine();
        writer.appendElse();
        writer.append("return value;").softNewLine();
        writer.appendBlockEnd();
    }

    private void generateClear(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append(context.getParameterName(0)).append(".")
                .appendField(new FieldReference(WeakReference.class.getName(), "value")).ws();
        writer.append("=").ws().append("null;").softNewLine();
    }

    private SourceWriter isSupported(SourceWriter writer) throws IOException {
        return writer.append("typeof ").append("$rt_globals.WeakRef").ws().append("!==").ws()
                .append("'undefined'");
    }
}
