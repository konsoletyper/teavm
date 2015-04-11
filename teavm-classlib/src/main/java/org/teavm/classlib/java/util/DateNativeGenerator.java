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
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.javascript.spi.Generator;
import org.teavm.javascript.spi.GeneratorContext;
import org.teavm.model.CallLocation;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class DateNativeGenerator implements Generator, DependencyPlugin {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "buildNumericTime":
                generateBuildNumericTime(context, writer);
                break;
            case "parseNumericTime":
                generateParseNumericTime(context, writer);
                break;
            case "buildNumericUTC":
                generateBuildNumericUTC(context, writer);
                break;
            case "getFullYear":
            case "getMonth":
            case "getDate":
            case "getDay":
            case "getHours":
            case "getMinutes":
            case "getSeconds":
            case "getTimezoneOffset":
                generateGetMethod(context, writer, methodRef.getName());
                break;
            case "setFullYear":
            case "setMonth":
            case "setDate":
            case "setHours":
            case "setMinutes":
            case "setSeconds":
                generateSetMethod(context, writer, methodRef.getName());
                break;
            case "toString":
            case "toGMTString":
                generateToString(context, writer, methodRef.getName());
                break;
            case "toLocaleFormat":
                generateToLocaleFormat(context, writer);
                break;
        }
    }

    @Override
    public void methodAchieved(DependencyAgent agent, MethodDependency method, CallLocation location) {
        switch (method.getMethod().getName()) {
            case "toString":
            case "toLocaleFormat":
            case "toGMTString":
                method.getResult().propagate(agent.getType("java.lang.String"));
                break;
        }
    }

    private void generateBuildNumericTime(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append("return new Date(").append(context.getParameterName(1));
        for (int i = 2; i <= 6; ++i) {
            writer.append(',').ws().append(context.getParameterName(i));
        }
        writer.append(").getTime();").softNewLine();
    }

    private void generateParseNumericTime(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append("return Date.parse(").append(context.getParameterName(1)).append(");").softNewLine();
    }

    private void generateBuildNumericUTC(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append("return Date.UTC(").append(context.getParameterName(1));
        for (int i = 2; i <= 6; ++i) {
            writer.append(',').ws().append(context.getParameterName(i));
        }
        writer.append(").getTime();").softNewLine();
    }

    private void generateGetMethod(GeneratorContext context, SourceWriter writer, String methodName)
            throws IOException {
        writer.append("return new Date(").append(context.getParameterName(1)).append(").").append(methodName)
                .append("();").softNewLine();
    }

    private void generateSetMethod(GeneratorContext context, SourceWriter writer, String methodName)
            throws IOException {
        writer.append("var date = new Date(").append(context.getParameterName(1)).append(");").softNewLine();
        writer.append("return date.").append(methodName).append("(").append(context.getParameterName(2)).append(");")
                .softNewLine();
    }

    private void generateToString(GeneratorContext context, SourceWriter writer, String method) throws IOException {
        writer.append("return $rt_str(new Date(").append(context.getParameterName(1)).append(").").append(method)
                .append("());").softNewLine();
    }

    private void generateToLocaleFormat(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append("return $rt_str(new Date(").append(context.getParameterName(1))
                .append(").toLocaleFormat($rt_ustr(").append(context.getParameterName(2)).append(")));")
                .softNewLine();
    }
}
