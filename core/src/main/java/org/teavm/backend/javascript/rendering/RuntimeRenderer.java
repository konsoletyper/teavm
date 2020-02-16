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
package org.teavm.backend.javascript.rendering;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ast.AstRoot;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.vm.RenderingException;

public class RuntimeRenderer {
    private static final String STRING_CLASS = String.class.getName();
    private static final String THREAD_CLASS = Thread.class.getName();

    private static final MethodReference NPE_INIT_METHOD = new MethodReference(NullPointerException.class,
            "<init>", void.class);
    private static final MethodDescriptor STRING_INTERN_METHOD = new MethodDescriptor("intern", String.class);
    private static final MethodDescriptor CURRENT_THREAD_METHOD = new MethodDescriptor("currentThread",
            Thread.class);
    private static final MethodReference STACK_TRACE_ELEM_INIT = new MethodReference(StackTraceElement.class,
            "<init>", String.class, String.class, String.class, int.class, void.class);
    private static final MethodReference SET_STACK_TRACE_METHOD = new MethodReference(Throwable.class,
            "setStackTrace", StackTraceElement[].class, void.class);
    private static final MethodReference AIOOBE_INIT_METHOD = new MethodReference(ArrayIndexOutOfBoundsException.class,
            "<init>", void.class);

    private final ClassReaderSource classSource;
    private final SourceWriter writer;

    public RuntimeRenderer(ClassReaderSource classSource, SourceWriter writer) {
        this.classSource = classSource;
        this.writer = writer;
    }

    public void renderRuntime() throws RenderingException {
        try {
            renderHandWrittenRuntime("runtime.js");
            renderSetCloneMethod();
            renderRuntimeCls();
            renderRuntimeString();
            renderRuntimeUnwrapString();
            renderRuntimeObjcls();
            renderRuntimeNullCheck();
            renderRuntimeIntern();
            renderRuntimeThreads();
            renderRuntimeCreateException();
            renderCreateStackTraceElement();
            renderSetStackTrace();
            renderThrowAIOOBE();
        } catch (IOException e) {
            throw new RenderingException("IO error", e);
        }
    }

    public void renderHandWrittenRuntime(String name) throws IOException {
        AstRoot ast = parseRuntime(name);
        ast.visit(new StringConstantElimination());
        new RuntimeAstTransformer(writer.getNaming()).accept(ast);
        AstWriter astWriter = new AstWriter(writer);
        astWriter.hoist(ast);
        astWriter.print(ast);
    }

    private AstRoot parseRuntime(String name) throws IOException {
        CompilerEnvirons env = new CompilerEnvirons();
        env.setRecoverFromErrors(true);
        env.setLanguageVersion(Context.VERSION_1_8);
        JSParser factory = new JSParser(env);

        ClassLoader loader = RuntimeRenderer.class.getClassLoader();
        try (InputStream input = loader.getResourceAsStream("org/teavm/backend/javascript/" + name);
                Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return factory.parse(reader, null, 0);
        }
    }

    private void renderSetCloneMethod() throws IOException {
        writer.append("function $rt_setCloneMethod(target, f)").ws().append("{").softNewLine().indent();
        writer.append("target.").appendMethod("clone", Object.class).ws().append('=').ws().append("f;").
                softNewLine();
        writer.outdent().append("}").newLine();
    }

    private void renderRuntimeCls() throws IOException {
        writer.append("function $rt_cls(cls)").ws().append("{").softNewLine().indent();
        writer.append("return ").appendMethodBody("java.lang.Class", "getClass",
                ValueType.object("org.teavm.platform.PlatformClass"),
                ValueType.object("java.lang.Class")).append("(cls);")
                .softNewLine();
        writer.outdent().append("}").newLine();
    }

    private void renderRuntimeString() throws IOException {
        MethodReference stringCons = new MethodReference(String.class, "<init>", char[].class, void.class);
        writer.append("function $rt_str(str) {").indent().softNewLine();
        writer.append("if (str === null) {").indent().softNewLine();
        writer.append("return null;").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("var characters = $rt_createCharArray(str.length);").softNewLine();
        writer.append("var charsBuffer = characters.data;").softNewLine();
        writer.append("for (var i = 0; i < str.length; i = (i + 1) | 0) {").indent().softNewLine();
        writer.append("charsBuffer[i] = str.charCodeAt(i) & 0xFFFF;").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return ").appendInit(stringCons).append("(characters);").softNewLine();
        writer.outdent().append("}").newLine();
    }

    private void renderRuntimeUnwrapString() throws IOException {
        FieldReference stringChars = new FieldReference(STRING_CLASS, "characters");
        writer.append("function $rt_ustr(str) {").indent().softNewLine();
        writer.append("if (str === null) {").indent().softNewLine();
        writer.append("return null;").softNewLine();
        writer.outdent().append("}").softNewLine();

        writer.append("var data = str.").appendField(stringChars).append(".data;").softNewLine();
        writer.append("var result = \"\";").softNewLine();
        writer.append("for (var i = 0; i < data.length; i = (i + 1) | 0) {").indent().softNewLine();
        writer.append("result += String.fromCharCode(data[i]);").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return result;").softNewLine();
        writer.outdent().append("}").newLine();
    }

    private void renderRuntimeNullCheck() throws IOException {
        writer.append("function $rt_nullCheck(val) {").indent().softNewLine();
        writer.append("if (val === null) {").indent().softNewLine();
        writer.append("$rt_throw(").appendInit(NPE_INIT_METHOD).append("());").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return val;").softNewLine();
        writer.outdent().append("}").newLine();
    }

    private void renderRuntimeIntern() throws IOException {
        if (!needInternMethod()) {
            writer.append("function $rt_intern(str) {").indent().softNewLine();
            writer.append("return str;").softNewLine();
            writer.outdent().append("}").softNewLine();
        } else {
            renderHandWrittenRuntime("intern.js");
            writer.append("function $rt_stringHash(s)").ws().append("{").indent().softNewLine();
            writer.append("return ").appendMethodBody(String.class, "hashCode", int.class)
                    .append("(s);").softNewLine();
            writer.outdent().append("}").softNewLine();
            writer.append("function $rt_stringEquals(a,").ws().append("b)").ws().append("{").indent().softNewLine();
            writer.append("return ").appendMethodBody(String.class, "equals", Object.class, boolean.class)
                    .append("(a").ws().append(",b);").softNewLine();
            writer.outdent().append("}").softNewLine();
        }
    }

    private boolean needInternMethod() {
        ClassReader cls = classSource.get(STRING_CLASS);
        if (cls == null) {
            return false;
        }
        MethodReader method = cls.getMethod(STRING_INTERN_METHOD);
        return method != null && method.hasModifier(ElementModifier.NATIVE);
    }

    private void renderRuntimeObjcls() throws IOException {
        writer.append("function $rt_objcls() { return ").appendClass("java.lang.Object").append("; }").newLine();
    }

    private void renderRuntimeThreads() throws IOException {
        ClassReader threadCls = classSource.get(THREAD_CLASS);
        MethodReader currentThreadMethod = threadCls != null ? threadCls.getMethod(CURRENT_THREAD_METHOD) : null;
        boolean threadUsed = currentThreadMethod != null && currentThreadMethod.getProgram() != null;

        writer.append("function $rt_getThread()").ws().append("{").indent().softNewLine();
        if (threadUsed) {
            writer.append("return ").appendMethodBody(Thread.class, "currentThread", Thread.class).append("();")
                    .softNewLine();
        } else {
            writer.append("return null;").softNewLine();
        }
        writer.outdent().append("}").newLine();

        writer.append("function $rt_setThread(t)").ws().append("{").indent().softNewLine();
        if (threadUsed) {
            writer.append("return ").appendMethodBody(Thread.class, "setCurrentThread", Thread.class, void.class)
                    .append("(t);").softNewLine();
        }
        writer.outdent().append("}").newLine();
    }

    private void renderRuntimeCreateException() throws IOException {
        writer.append("function $rt_createException(message)").ws().append("{").indent().softNewLine();
        writer.append("return ");
        writer.appendInit(new MethodReference(RuntimeException.class, "<init>", String.class, void.class));
        writer.append("(message);").softNewLine();
        writer.outdent().append("}").newLine();
    }

    private void renderCreateStackTraceElement() throws IOException {
        ClassReader cls = classSource.get(STACK_TRACE_ELEM_INIT.getClassName());
        MethodReader stackTraceElemInit = cls != null ? cls.getMethod(STACK_TRACE_ELEM_INIT.getDescriptor()) : null;
        boolean supported = stackTraceElemInit != null && stackTraceElemInit.getProgram() != null;

        writer.append("function $rt_createStackElement(")
                .append("className,").ws()
                .append("methodName,").ws()
                .append("fileName,").ws()
                .append("lineNumber)").ws().append("{").indent().softNewLine();
        writer.append("return ");
        if (supported) {
            writer.appendInit(STACK_TRACE_ELEM_INIT);
            writer.append("(className,").ws()
                    .append("methodName,").ws()
                    .append("fileName,").ws()
                    .append("lineNumber)");
        } else {
            writer.append("null");
        }
        writer.append(";").softNewLine();
        writer.outdent().append("}").newLine();
    }

    private void renderSetStackTrace() throws IOException {
        ClassReader cls = classSource.get(SET_STACK_TRACE_METHOD.getClassName());
        MethodReader setStackTrace = cls != null ? cls.getMethod(SET_STACK_TRACE_METHOD.getDescriptor()) : null;
        boolean supported = setStackTrace != null && setStackTrace.getProgram() != null;

        writer.append("function $rt_setStack(e,").ws().append("stack)").ws().append("{").indent().softNewLine();
        if (supported) {
            writer.appendMethodBody(SET_STACK_TRACE_METHOD);
            writer.append("(e,").ws().append("stack);").softNewLine();
        }
        writer.outdent().append("}").newLine();
    }

    private void renderThrowAIOOBE() throws IOException {
        writer.append("function $rt_throwAIOOBE()").ws().append("{").indent().softNewLine();

        ClassReader cls = classSource.get(AIOOBE_INIT_METHOD.getClassName());
        if (cls != null) {
            MethodReader method = cls.getMethod(AIOOBE_INIT_METHOD.getDescriptor());
            if (method != null && !method.hasModifier(ElementModifier.ABSTRACT)) {
                writer.append("$rt_throw(").appendInit(AIOOBE_INIT_METHOD).append("());").softNewLine();
            }
        }

        writer.outdent().append("}").newLine();
    }
}
