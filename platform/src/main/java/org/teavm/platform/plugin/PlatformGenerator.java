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
import java.lang.annotation.Annotation;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.*;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformClass;
import org.teavm.platform.PlatformRunnable;

public class PlatformGenerator implements Generator, Injector, DependencyPlugin {
    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method, CallLocation location) {
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
            case "getCurrentThread":
                method.getResult().propagate(agent.getType("java.lang.Thread"));
                break;
        }
    }

    @Override
    public void generate(InjectorContext context, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "asJavaClass":
            case "classFromResource":
            case "objectFromResource":
            case "marshall":
            case "getPlatformObject":
                context.writeExpr(context.getArgument(0));
                break;
            case "initClass":
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(".$clinit()");
                break;
        }
    }

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "newInstanceImpl":
                generateNewInstance(context, writer);
                break;
            case "prepareNewInstance":
                generatePrepareNewInstance(context, writer);
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
            case "getEnumConstants":
                generateEnumConstants(context, writer);
                break;
            case "getAnnotations":
                generateAnnotations(context, writer);
                break;
        }
    }

    private void generatePrepareNewInstance(GeneratorContext context, SourceWriter writer)
            throws IOException {
        writer.append("var c").ws().append("=").ws().append("'$$constructor$$';").softNewLine();
        for (String clsName : context.getClassSource().getClassNames()) {
            ClassReader cls = context.getClassSource().get(clsName);
            MethodReader method = cls.getMethod(new MethodDescriptor("<init>", void.class));
            if (method != null) {
                writer.appendClass(clsName).append("[c]").ws().append("=").ws()
                        .appendMethodBody(method.getReference()).append(";").softNewLine();
            }
        }
    }

    private void generateNewInstance(GeneratorContext context, SourceWriter writer) throws IOException {
        String cls = context.getParameterName(1);

        writer.append("if").ws().append("($rt_resuming())").ws().append("{").indent().softNewLine();
        writer.append("var $r = $rt_nativeThread().pop();").softNewLine();
        writer.append(cls + ".$$constructor$$($r);").softNewLine();
        writer.append("if").ws().append("($rt_suspending())").ws().append("{").indent().softNewLine();
        writer.append("return $rt_nativeThread().push($r);").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return $r;").softNewLine();
        writer.outdent().append("}").softNewLine();

        writer.append("if").ws().append("(!").append(cls).append(".hasOwnProperty('$$constructor$$'))")
                .ws().append("{").indent().softNewLine();
        writer.append("return null;").softNewLine();
        writer.outdent().append("}").softNewLine();

        writer.append("var $r").ws().append('=').ws().append("new ").append(cls).append("();").softNewLine();
        writer.append(cls).append(".$$constructor$$($r);").softNewLine();
        writer.append("if").ws().append("($rt_suspending())").ws().append("{").indent().softNewLine();
        writer.append("return $rt_nativeThread().push($r);").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return $r;").softNewLine();
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
        MethodReference launchRef = new MethodReference(Platform.class, "launchThread",
                PlatformRunnable.class, void.class);
        String runnable = context.getParameterName(1);
        writer.append("return setTimeout(function()").ws().append("{").indent().softNewLine();
        if (timeout) {
            writer.appendMethodBody(launchRef);
        } else {
            writer.append("$rt_threadStarter(").appendMethodBody(launchRef).append(")");
        }
        writer.append("(").append(runnable).append(");").softNewLine();
        writer.outdent().append("},").ws().append(timeout ? context.getParameterName(2) : "0")
                .append(");").softNewLine();
    }

    private void generateEnumConstants(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append("var c").ws().append("=").ws().append("'$$enumConstants$$';").softNewLine();
        for (String clsName : context.getClassSource().getClassNames()) {
            ClassReader cls = context.getClassSource().get(clsName);
            MethodReader method = cls.getMethod(new MethodDescriptor("values",
                    ValueType.arrayOf(ValueType.object(clsName))));
            if (method != null) {
                writer.appendClass(clsName).append("[c]").ws().append("=").ws();
                writer.appendMethodBody(method.getReference());
                writer.append(";").softNewLine();
            }
        }

        String selfName = writer.getNaming().getFullNameFor(new MethodReference(Platform.class, "getEnumConstants",
                PlatformClass.class, Enum[].class));
        writer.append(selfName).ws().append("=").ws().append("function(cls)").ws().append("{").softNewLine().indent();
        writer.append("if").ws().append("(!cls.hasOwnProperty(c))").ws().append("{").indent().softNewLine();
        writer.append("return null;").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("if").ws().append("(typeof cls[c]").ws().append("===").ws().append("\"function\")").ws()
                .append("{").indent().softNewLine();
        writer.append("cls[c]").ws().append("=").ws().append("cls[c]();").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return cls[c];").softNewLine();
        writer.outdent().append("};").softNewLine();

        writer.append("return ").append(selfName).append("(").append(context.getParameterName(1))
                .append(");").softNewLine();
    }

    private void generateAnnotations(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append("var c").ws().append("=").ws().append("'$$annotations$$';").softNewLine();
        for (String clsName : context.getClassSource().getClassNames()) {
            ClassReader annotCls = context.getClassSource().get(clsName + "$$__annotations__$$");
            if (annotCls != null) {
                writer.appendClass(clsName).append("[c]").ws().append("=").ws();
                MethodReference ctor = new MethodReference(annotCls.getName(), "<init>", ValueType.VOID);
                writer.append(writer.getNaming().getNameForInit(ctor));
                writer.append("();").softNewLine();
            }
        }

        String selfName = writer.getNaming().getFullNameFor(new MethodReference(Platform.class, "getAnnotations",
                PlatformClass.class, Annotation[].class));
        writer.append(selfName).ws().append("=").ws().append("function(cls)").ws().append("{").softNewLine().indent();
        writer.append("if").ws().append("(!cls.hasOwnProperty(c))").ws().append("{").indent().softNewLine();
        writer.append("return null;").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return cls[c].").appendMethod("getAnnotations", Annotation[].class).append("();").softNewLine();
        writer.outdent().append("};").softNewLine();

        writer.append("return ").append(selfName).append("(").append(context.getParameterName(1))
                .append(");").softNewLine();
    }
}
