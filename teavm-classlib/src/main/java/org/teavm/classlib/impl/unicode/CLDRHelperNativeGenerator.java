/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.classlib.impl.unicode;

import java.io.IOException;
import org.teavm.codegen.SourceWriter;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class CLDRHelperNativeGenerator implements Generator, DependencyPlugin {
    @Override
    public void methodAchieved(DependencyAgent agent, MethodDependency method) {
        switch (method.getMethod().getName()) {
            case "getLikelySubtagsImpl":
                method.getResult().propagate("java.lang.String");
                break;
            case "getEras":
                method.getResult().propagate("[java.lang.String;");
                method.getResult().getArrayItem().propagate("java.lang.String");
                break;
        }
    }

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "getLikelySubtagsImpl":
                writer.append("var data = ").appendClass("java.util.Locale").append(".$CLDR.likelySubtags[$rt_ustr(")
                        .append(context.getParameterName(1)).append(")];").softNewLine();
                writer.append("return data ? $rt_str(data) : null;").softNewLine();
                break;
            case "getEras":
                generateGetArray(context, writer, "eras");
                break;
        }
    }

    private void generateGetArray(GeneratorContext context, SourceWriter writer, String name) throws IOException {
        writer.append("var data = ").appendClass("java.util.Locale").append(".$CLDR.").append(name)
                .append("[$rt_ustr(").append(context.getParameterName(1)).append(")];").softNewLine();
        writer.append("if (!data) {").indent().softNewLine();
        writer.append("return null;");
        writer.outdent().append("}").softNewLine();
        writer.append("var result = $rt_createArray(").appendClass("java.lang.String)")
                .append(", data.length);").softNewLine();
        writer.append("for (var i = 0; i < data.length; ++i) {").indent().softNewLine();
        writer.append("result.data[i] = $rt_str(data[i])").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return result;").softNewLine();
    }
}
