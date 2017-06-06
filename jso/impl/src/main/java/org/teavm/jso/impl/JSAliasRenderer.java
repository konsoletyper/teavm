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
package org.teavm.jso.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.rendering.RenderingManager;
import org.teavm.model.AnnotationReader;
import org.teavm.model.ClassReader;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.spi.RendererListener;

class JSAliasRenderer implements RendererListener {
    private static String variableChars = "abcdefghijklmnopqrstuvwxyz";
    private SourceWriter writer;
    private ListableClassReaderSource classSource;

    @Override
    public void begin(RenderingManager context, BuildTarget buildTarget) throws IOException {
        writer = context.getWriter();
        classSource = context.getClassSource();
    }

    @Override
    public void complete() throws IOException {
        if (!hasClassesToExpose()) {
            return;
        }

        writer.append("(function()").ws().append("{").softNewLine().indent();
        writer.append("var c;").softNewLine();
        for (String className : classSource.getClassNames()) {
            ClassReader classReader = classSource.get(className);
            Map<MethodDescriptor, String> methods = new HashMap<>();
            for (MethodReader method : classReader.getMethods()) {
                String methodAlias = getPublicAlias(method);
                if (methodAlias != null) {
                    methods.put(method.getDescriptor(), methodAlias);
                }
            }
            if (methods.isEmpty()) {
                continue;
            }

            boolean first = true;
            for (Map.Entry<MethodDescriptor, String> aliasEntry : methods.entrySet()) {
                if (classReader.getMethod(aliasEntry.getKey()) == null) {
                    continue;
                }
                if (first) {
                    writer.append("c").ws().append("=").ws().appendClass(className).append(".prototype;")
                            .softNewLine();
                    first = false;
                }
                if (isKeyword(aliasEntry.getValue())) {
                    writer.append("c[\"").append(aliasEntry.getValue()).append("\"]");
                } else {
                    writer.append("c.").append(aliasEntry.getValue());
                }
                writer.ws().append("=").ws().append("c.").appendMethod(aliasEntry.getKey()).append(";").softNewLine();
            }

            FieldReader functorField = getFunctorField(classReader);
            if (functorField != null) {
                writeFunctor(classReader, functorField.getReference());
            }
        }
        writer.outdent().append("})();").newLine();
    }

    private boolean hasClassesToExpose() {
        for (String className : classSource.getClassNames()) {
            ClassReader cls = classSource.get(className);
            if (cls.getMethods().stream().anyMatch(method -> getPublicAlias(method) != null)) {
                return true;
            }
        }
        return false;
    }

    private String getPublicAlias(MethodReader method) {
        AnnotationReader annot = method.getAnnotations().get(JSMethodToExpose.class.getName());
        return annot != null ? annot.getValue("name").getString() : null;
    }

    private FieldReader getFunctorField(ClassReader cls) {
        return cls.getField("$$jso_functor$$");
    }

    private boolean isKeyword(String id) {
        switch (id) {
            case "with":
            case "delete":
            case "in":
            case "undefined":
            case "debugger":
            case "export":
            case "function":
            case "let":
            case "var":
            case "typeof":
            case "yield":
                return true;
            default:
                return false;
        }
    }

    private void writeFunctor(ClassReader cls, FieldReference functorField) throws IOException {
        AnnotationReader implAnnot = cls.getAnnotations().get(FunctorImpl.class.getName());
        MethodDescriptor functorMethod = MethodDescriptor.parse(implAnnot.getValue("value").getString());
        String alias = cls.getMethod(functorMethod).getAnnotations()
                .get(JSMethodToExpose.class.getName()).getValue("name").getString();
        if (alias == null) {
            return;
        }

        writer.append("c.jso$functor$").append(alias).ws().append("=").ws().append("function()").ws().append("{")
                .indent().softNewLine();
        writer.append("if").ws().append("(!this.").appendField(functorField).append(")").ws().append("{")
                .indent().softNewLine();
        writer.append("var self").ws().append('=').ws().append("this;").softNewLine();

        writer.append("this.").appendField(functorField).ws().append('=').ws().append("function(");
        appendArguments(functorMethod.parameterCount());
        writer.append(")").ws().append('{').indent().softNewLine();
        writer.append("return self.").appendMethod(functorMethod).append('(');
        appendArguments(functorMethod.parameterCount());
        writer.append(");").softNewLine();
        writer.outdent().append("};").softNewLine();

        writer.outdent().append("}").softNewLine();
        writer.append("return this.").appendField(functorField).append(';').softNewLine();
        writer.outdent().append("};").softNewLine();
    }

    private void appendArguments(int count) throws IOException {
        for (int i = 0; i < count; ++i) {
            if (i > 0) {
                writer.append(',').ws();
            }
            writer.append(variableName(i));
        }
    }

    private String variableName(int index) {
        StringBuilder sb = new StringBuilder();
        sb.append(variableChars.charAt(index % variableChars.length()));
        index /= variableChars.length();
        if (index > 0) {
            sb.append(index);
        }
        return sb.toString();
    }
}
