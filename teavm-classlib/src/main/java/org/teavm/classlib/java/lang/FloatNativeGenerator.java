/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.classlib.java.lang;

import java.io.IOException;
import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.spi.Generator;
import org.teavm.javascript.spi.GeneratorContext;
import org.teavm.javascript.spi.Injector;
import org.teavm.javascript.spi.InjectorContext;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class FloatNativeGenerator implements Generator, Injector {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "isNaN":
                generateIsNaN(context, writer);
                break;
            case "isInfinite":
                generateIsInfinite(context, writer);
                break;
        }
    }

    @Override
    public void generate(InjectorContext context, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "getNaN":
                context.getWriter().append("NaN");
                break;
        }
    }

    private void generateIsNaN(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append("return (isNaN(").append(context.getParameterName(1)).append(")").ws().append("?")
            .ws().append("1").ws().append(":").ws().append("0").ws().append(");").softNewLine();
    }

    private void generateIsInfinite(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append("return (isFinite(").append(context.getParameterName(1)).append(")").ws().append("?")
                .ws().append("0").ws().append(":").ws().append("1").append(");").softNewLine();
    }
}
