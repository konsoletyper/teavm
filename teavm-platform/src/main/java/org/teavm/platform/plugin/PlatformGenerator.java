/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.platform.plugin;

import java.io.IOException;
import org.teavm.codegen.SourceWriter;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.javascript.spi.Generator;
import org.teavm.javascript.spi.GeneratorContext;
import org.teavm.javascript.spi.Injector;
import org.teavm.javascript.spi.InjectorContext;
import org.teavm.model.*;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformRunnable;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class PlatformGenerator implements Generator, Injector, DependencyPlugin {
    @Override
    public void methodAchieved(DependencyAgent agent, MethodDependency method, CallLocation location) {
        switch (method.getReference().getName()) {
            case "asJavaClass":
                method.getResult().propagate(agent.getType("java.lang.Class"));
                return;
            case "clone":
                method.getVariable(1).connect(method.getResult());
                break;
            case "startThread":
            case "schedule": {
                MethodDependency launchMethod = agent.linkMethod(new MethodReference(Platform.class,
                        "launchThread", PlatformRunnable.class, void.class), null);
                method.getVariable(1).connect(launchMethod.getVariable(1));
                launchMethod.use();
                break;
            }
        }
    }

    @Override
    public void generate(InjectorContext context, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "asJavaClass":
            case "classFromResource":
                context.writeExpr(context.getArgument(0));
                return;
            case "getEnumConstants":
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(".values()");
                break;
        }
    }

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "newInstance":
                generateNewInstance(context, writer, methodRef);
                break;
            case "lookupClass":
                generateLookup(context, writer);
                break;
            case "clone":
                generateClone(context, writer);
                break;
            case "startThread":
                generateSchedule(context, writer, false);
                break;
            case "schedule":
                generateSchedule(context, writer, true);
                break;
        }
    }

    private void generateNewInstance(GeneratorContext context, SourceWriter writer, MethodReference methodRef)
            throws IOException {
        writer.append("var c").ws().append("=").ws().append("'$$constructor$$';").softNewLine();
        if (context.isAsync()) {
            writer.append("function async(cls, init) {").indent().softNewLine();
            writer.append("return function($return) {").indent().softNewLine();
            writer.append("var r = new cls;").softNewLine();
            writer.append("init(r, $rt_guardAsync(function($restore) {").indent().softNewLine();
            writer.append("$restore();").softNewLine();
            writer.append("$return($rt_asyncResult(r))").softNewLine();
            writer.outdent().append("}));").softNewLine();
            writer.outdent().append("};").softNewLine();
            writer.outdent().append("}").softNewLine();

            writer.append("function sync(cls, init) {").indent().softNewLine();
            writer.append("return function($return) {").indent().softNewLine();
            writer.append("var r = new cls;").softNewLine();
            writer.append("try {").indent().softNewLine();
            writer.append("init(r);").softNewLine();
            writer.append("$return($rt_asyncResult(r));").softNewLine();
            writer.outdent().append("} catch (e) {").indent().softNewLine();
            writer.append("$return($rt_asyncError(e));").softNewLine();
            writer.outdent().append("}").softNewLine();
            writer.outdent().append("};").softNewLine();
            writer.outdent().append("}").softNewLine();
        }
        for (String clsName : context.getClassSource().getClassNames()) {
            ClassReader cls = context.getClassSource().get(clsName);
            MethodReader method = cls.getMethod(new MethodDescriptor("<init>", void.class));
            if (method != null) {
                writer.appendClass(clsName).append("[c]").ws().append("=").ws();
                if (!context.isAsync()) {
                    writer.append(writer.getNaming().getNameForInit(method.getReference()));
                } else {
                    String function = context.isAsync(method.getReference()) ? "async" : "sync";
                    String methodName = context.isAsync(method.getReference()) ?
                            writer.getNaming().getFullNameForAsync(method.getReference()) :
                            writer.getNaming().getFullNameFor(method.getReference());
                    writer.append(function).append("(").appendClass(clsName).append(',').ws()
                            .append(methodName).append(")");
                }
                writer.append(";").softNewLine();
            }
        }
        String selfName = context.isAsync() ? writer.getNaming().getFullNameForAsync(methodRef) :
                writer.getNaming().getFullNameFor(methodRef);
        writer.append(selfName).ws().append("=").ws().append("function(cls");
        if (context.isAsync()) {
            writer.append(',').ws().append("$return");
        }
        writer.append(")").ws().append("{").softNewLine().indent();
        writer.append("if").ws().append("(!cls.hasOwnProperty(c))").ws().append("{").indent().softNewLine();
        if (!context.isAsync()) {
            writer.append("return null;").softNewLine();
        } else {
            writer.append("return $return($rt_asyncResult(null));").softNewLine();
        }
        writer.outdent().append("}").softNewLine();
        if (!context.isAsync()) {
            writer.append("return cls[c]();").softNewLine();
        } else {
            writer.append("return cls[c]($return);").softNewLine();
        }
        writer.outdent().append("}").softNewLine();

        writer.append("return ").append(selfName).append("(").append(context.getParameterName(1));
        if (context.isAsync()) {
            writer.append(',').ws().append("$return");
        }
        writer.append(");").softNewLine();
    }

    private void generateLookup(GeneratorContext context, SourceWriter writer) throws IOException {
        String param = context.getParameterName(1);
        writer.append("switch ($rt_ustr(" + param + ")) {").softNewLine().indent();
        for (String name : context.getClassSource().getClassNames()) {
            writer.append("case \"" + name + "\": ").appendClass(name).append(".$clinit(); ")
                    .append("return ").appendClass(name).append(";").softNewLine();
        }
        writer.append("default: return null;").softNewLine();
        writer.outdent().append("}").softNewLine();
    }

    private void generateClone(GeneratorContext context, SourceWriter writer) throws IOException {
        String obj = context.getParameterName(1);
        writer.append("var copy").ws().append("=").ws().append("new ").append(obj).append(".constructor();")
                .softNewLine();
        writer.append("for").ws().append("(var field in " + obj + ")").ws().append("{").softNewLine().indent();
        writer.append("if").ws().append("(!" + obj + ".hasOwnProperty(field))").ws().append("{").softNewLine().indent();
        writer.append("continue;").softNewLine().outdent().append("}").softNewLine();
        writer.append("copy[field]").ws().append("=").ws().append(obj).append("[field];")
                .softNewLine().outdent().append("}").softNewLine();
        writer.append("return copy;").softNewLine();
    }

    private void generateSchedule(GeneratorContext context, SourceWriter writer, boolean timeout) throws IOException {
        String runnable = context.getParameterName(1);
        writer.append("return window.setTimeout(function()").ws().append("{").indent().softNewLine();
        String methodName = writer.getNaming().getFullNameFor(new MethodReference(Platform.class, "launchThread",
                PlatformRunnable.class, void.class));
        writer.append("$rt_rootInvocationAdapter(").append(methodName).append(")(").append(runnable).append(");")
                .softNewLine();
        writer.outdent().append("},").ws().append(timeout ? context.getParameterName(2) : "0")
                .append(");").softNewLine();
    }
}
