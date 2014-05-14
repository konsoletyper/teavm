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
package org.teavm.classlib.java.util;

import java.io.IOException;
import org.teavm.codegen.SourceWriter;
import org.teavm.dependency.DependencyChecker;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class LocaleNativeGenerator implements Generator, DependencyPlugin {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "getDisplayCountry":
                writer.append("var result = ").appendClass("java.util.Locale").append(".$CLDR[$rt_ustr(")
                        .append(context.getParameterName(1)).append(")];").softNewLine();
                writer.append("result = result ? result.territories[$rt_ustr(")
                        .append(context.getParameterName(2)).append(")] : undefined;").softNewLine();
                writer.append("return result ? $rt_str(result) : null").softNewLine();
                break;
            case "getDisplayLanguage":
                writer.append("var result = ").appendClass("java.util.Locale").append(".$CLDR[$rt_ustr(")
                        .append(context.getParameterName(1)).append(")];").softNewLine();
                writer.append("result = result ? result.languages[$rt_ustr(")
                        .append(context.getParameterName(2)).append(")] : undefined;").softNewLine();
                writer.append("return result ? $rt_str(result) : null;").softNewLine();
                break;
            case "getAvailableLocaleStrings":
                generateAvailableLocales(writer);
                break;
        }
    }

    private void generateAvailableLocales(SourceWriter writer) throws IOException {
        writer.append("var locales = Object.keys(").appendClass("java.util.Locale").append(".$CLDR);").softNewLine();
        writer.append("var array = $rt_createArray(").appendClass("java.lang.String").append(", locales);")
                .softNewLine();
        writer.append("for (var i = 0; i < locales.length; ++i) {").indent().softNewLine();
        writer.append("array.data[i] = $rt_str(locales[i]);").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return array;").softNewLine();
    }

    @Override
    public void methodAchieved(DependencyChecker checker, MethodDependency method) {
        switch (method.getMethod().getName()) {
            case "getDefaultLocale":
            case "getDisplayCountry":
            case "getDisplayLanguage":
                method.getResult().propagate("java.lang.String");
                break;
            case "getAvailableLocaleStrings":
                method.getResult().propagate("[java.lang.String");
                method.getResult().getArrayItem().propagate("java.lang.String");
                break;
        }
    }
}
