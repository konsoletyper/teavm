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
package org.teavm.classlib.impl.string;

import java.io.IOException;
import java.util.function.Function;
import org.teavm.backend.javascript.ProviderContext;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;

public class JSStringInjector implements Injector, Function<ProviderContext, Injector> {
    static final FieldReference NATIVE_FIELD = new FieldReference("java.lang.String", "nativeString");

    @Override
    public Injector apply(ProviderContext providerContext) {
        switch (providerContext.getMethod().getName()) {
            case "initWithEmptyChars":
            case "borrowChars":
            case "initWithCharArray":
            case "takeCharArray":
            case "charactersLength":
            case "charactersGet":
            case "copyCharsToArray":
            case "fastCharArray":
            case "nativeString":
            case "substringJS":
            case "toLowerCaseJS":
            case "toUpperCaseJS":
            case "intern":
            case "stripJS":
            case "stripLeadingJS":
            case "stripTrailingJS":
                return this;
        }
        return null;
    }

    @Override
    public void generate(InjectorContext context, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "initWithEmptyChars":
                initWithEmptyChars(context);
                break;
            case "borrowChars":
                borrowChars(context);
                break;
            case "initWithCharArray":
                initWithCharArray(context);
                break;
            case "takeCharArray":
                takeCharArray(context);
                break;
            case "charactersLength":
                charactersLength(context);
                break;
            case "charactersGet":
                charactersGet(context);
                break;
            case "copyCharsToArray":
                copyCharsToArray(context);
                break;
            case "fastCharArray":
                fastCharArray(context);
                break;
            case "nativeString":
                nativeString(context);
                break;
            case "substringJS":
                substringJS(context);
                break;
            case "toLowerCaseJS":
                toLowerCaseJS(context);
                break;
            case "toUpperCaseJS":
                toUpperCaseJS(context);
                break;
            case "intern":
                intern(context);
                break;
            case "stripJS":
                stripJS(context);
                break;
            case "stripLeadingJS":
                stripLeadingJS(context);
                break;
            case "stripTrailingJS":
                stripTrailingJS(context);
                break;
        }
    }

    private void initWithEmptyChars(InjectorContext context) throws IOException {
        var writer = context.getWriter();
        context.writeExpr(context.getArgument(0));
        writer.append(".").appendField(NATIVE_FIELD).ws().append("=").ws().append("\"\"");
    }

    private void borrowChars(InjectorContext context) throws IOException {
        var writer = context.getWriter();
        context.writeExpr(context.getArgument(0));
        writer.append(".").appendField(NATIVE_FIELD).ws().append("=").ws();
        context.writeExpr(context.getArgument(1));
        writer.append(".").appendField(NATIVE_FIELD);
    }

    private void initWithCharArray(InjectorContext context) throws IOException {
        var writer = context.getWriter();
        context.writeExpr(context.getArgument(0));
        writer.append(".").appendField(NATIVE_FIELD).ws().append("=").ws();
        writer.appendFunction("$rt_charArrayToString").append("(");
        context.writeExpr(context.getArgument(1));
        writer.append(".data,").ws();
        context.writeExpr(context.getArgument(2));
        writer.append(",").ws();
        context.writeExpr(context.getArgument(3));
        writer.append(")");
    }

    private void takeCharArray(InjectorContext context) throws IOException {
        var writer = context.getWriter();
        context.writeExpr(context.getArgument(0));
        writer.append(".").appendField(NATIVE_FIELD).ws().append("=").ws();
        writer.appendFunction("$rt_fullArrayToString").append("(");
        context.writeExpr(context.getArgument(1));
        writer.append(".data)");
    }

    private void charactersLength(InjectorContext context) throws IOException {
        var writer = context.getWriter();
        context.writeExpr(context.getArgument(0));
        writer.append(".").appendField(NATIVE_FIELD).append(".length");
    }

    private void charactersGet(InjectorContext context) throws IOException {
        var writer = context.getWriter();
        context.writeExpr(context.getArgument(0));
        writer.append(".").appendField(NATIVE_FIELD).append(".charCodeAt(");
        context.writeExpr(context.getArgument(1));
        writer.append(")");
    }

    private void copyCharsToArray(InjectorContext context) throws IOException {
        var writer = context.getWriter();
        writer.appendFunction("$rt_stringToCharArray").append("(");
        context.writeExpr(context.getArgument(0));
        writer.append(".").appendField(NATIVE_FIELD);
        writer.append(",").ws();
        context.writeExpr(context.getArgument(1));
        writer.append(",").ws();
        context.writeExpr(context.getArgument(2));
        writer.append(".data");
        writer.append(",").ws();
        context.writeExpr(context.getArgument(3));
        writer.append(",").ws();
        context.writeExpr(context.getArgument(4));
        writer.append(")");
    }

    private void substringJS(InjectorContext context) throws IOException {
        var writer = context.getWriter();
        context.writeExpr(context.getArgument(0));
        writer.append(".substring(");
        context.writeExpr(context.getArgument(1));
        writer.append(",").ws();
        context.writeExpr(context.getArgument(2));
        writer.append(")");
    }

    private void fastCharArray(InjectorContext context) throws IOException {
        var writer = context.getWriter();
        writer.appendFunction("$rt_fastStringToCharArray").append("(");
        context.writeExpr(context.getArgument(0));
        writer.append(".").appendField(NATIVE_FIELD);
        writer.append(")");
    }

    private void nativeString(InjectorContext context) throws IOException {
        var writer = context.getWriter();
        context.writeExpr(context.getArgument(0));
        writer.append(".").appendField(NATIVE_FIELD);
    }

    private void toLowerCaseJS(InjectorContext context) throws IOException {
        var writer = context.getWriter();
        context.writeExpr(context.getArgument(0));
        writer.append(".toLowerCase()");
    }

    private void toUpperCaseJS(InjectorContext context) throws IOException {
        var writer = context.getWriter();
        context.writeExpr(context.getArgument(0));
        writer.append(".toUpperCase()");
    }

    private void intern(InjectorContext context) throws IOException {
        var writer = context.getWriter();
        writer.appendFunction("$rt_intern").append("(");
        context.writeExpr(context.getArgument(0));
        writer.append(")");
    }

    private void stripJS(InjectorContext context) throws IOException {
        var writer = context.getWriter();
        context.writeExpr(context.getArgument(0));
        writer.append(".trim()");
    }

    private void stripLeadingJS(InjectorContext context) throws IOException {
        var writer = context.getWriter();
        context.writeExpr(context.getArgument(0));
        writer.append(".trimStart()");
    }

    private void stripTrailingJS(InjectorContext context) throws IOException {
        var writer = context.getWriter();
        context.writeExpr(context.getArgument(0));
        writer.append(".trimEnd()");
    }
}
