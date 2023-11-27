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

import java.util.HashMap;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.rendering.RenderingManager;
import org.teavm.backend.javascript.spi.VirtualMethodContributor;
import org.teavm.backend.javascript.spi.VirtualMethodContributorContext;
import org.teavm.model.AnnotationReader;
import org.teavm.model.ClassReader;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.spi.RendererListener;

class JSAliasRenderer implements RendererListener, VirtualMethodContributor {
    private static String variableChars = "abcdefghijklmnopqrstuvwxyz";
    private SourceWriter writer;
    private ListableClassReaderSource classSource;
    private JSTypeHelper typeHelper;

    @Override
    public void begin(RenderingManager context, BuildTarget buildTarget) {
        writer = context.getWriter();
        classSource = context.getClassSource();
        typeHelper = new JSTypeHelper(context.getClassSource());
    }

    @Override
    public void complete() {
        if (!hasClassesToExpose()) {
            return;
        }

        writer.append("let ").appendFunction("$rt_jso_marker").ws().append("=").ws()
                .appendGlobal("Symbol").append("('jsoClass');").newLine();
        writer.append("(function()").ws().append("{").softNewLine().indent();
        writer.append("var c;").softNewLine();
        for (String className : classSource.getClassNames()) {
            ClassReader classReader = classSource.get(className);
            var methods = new HashMap<String, MethodDescriptor>();
            var properties = new HashMap<String, PropertyInfo>();
            for (var method : classReader.getMethods()) {
                var methodAlias = getPublicAlias(method);
                if (methodAlias != null) {
                    switch (methodAlias.kind) {
                        case METHOD:
                            methods.put(methodAlias.name, method.getDescriptor());
                            break;
                        case GETTER: {
                            var propInfo = properties.computeIfAbsent(methodAlias.name, k -> new PropertyInfo());
                            propInfo.getter = method.getDescriptor();
                            break;
                        }
                        case SETTER: {
                            var propInfo = properties.computeIfAbsent(methodAlias.name, k -> new PropertyInfo());
                            propInfo.setter = method.getDescriptor();
                            break;
                        }
                    }
                }
            }

            var isJsClassImpl = typeHelper.isJavaScriptImplementation(className);
            if (methods.isEmpty() && properties.isEmpty() && !isJsClassImpl) {
                continue;
            }

            writer.append("c").ws().append("=").ws().appendClass(className).append(".prototype;")
                    .softNewLine();
            if (isJsClassImpl) {
                writer.append("c[").appendFunction("$rt_jso_marker").append("]").ws().append("=").ws().append("true;")
                        .softNewLine();
            }

            for (var aliasEntry : methods.entrySet()) {
                if (classReader.getMethod(aliasEntry.getValue()) == null) {
                    continue;
                }
                if (isKeyword(aliasEntry.getKey())) {
                    writer.append("c[\"").append(aliasEntry.getKey()).append("\"]");
                } else {
                    writer.append("c.").append(aliasEntry.getKey());
                }
                writer.ws().append("=").ws().append("c.").appendMethod(aliasEntry.getValue())
                        .append(";").softNewLine();
            }
            for (var aliasEntry : properties.entrySet()) {
                var propInfo = aliasEntry.getValue();
                if (propInfo.getter == null || classReader.getMethod(propInfo.getter) == null) {
                    continue;
                }
                writer.append("Object.defineProperty(c,")
                        .ws().append("\"").append(aliasEntry.getKey()).append("\",")
                        .ws().append("{").indent().softNewLine();
                writer.append("get:").ws().append("c.").appendMethod(propInfo.getter);
                if (propInfo.setter != null && classReader.getMethod(propInfo.setter) != null) {
                    writer.append(",").softNewLine();
                    writer.append("set:").ws().append("c.").appendMethod(propInfo.setter);
                }
                writer.softNewLine().outdent().append("});").softNewLine();
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
            if (cls.getMethods().stream().anyMatch(method -> getPublicAlias(method) != null)
                    || typeHelper.isJavaScriptImplementation(className)) {
                return true;
            }
        }
        return false;
    }

    private Alias getPublicAlias(MethodReader method) {
        var annot = method.getAnnotations().get(JSMethodToExpose.class.getName());
        if (annot != null) {
            return new Alias(annot.getValue("name").getString(), AliasKind.METHOD);
        }

        annot = method.getAnnotations().get(JSGetterToExpose.class.getName());
        if (annot != null) {
            return new Alias(annot.getValue("name").getString(), AliasKind.GETTER);
        }

        annot = method.getAnnotations().get(JSSetterToExpose.class.getName());
        if (annot != null) {
            return new Alias(annot.getValue("name").getString(), AliasKind.SETTER);
        }

        return null;
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

    private void writeFunctor(ClassReader cls, FieldReference functorField) {
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

    private void appendArguments(int count) {
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

    @Override
    public boolean isVirtual(VirtualMethodContributorContext context, MethodReference methodRef) {
        ClassReader classReader = context.getClassSource().get(methodRef.getClassName());
        if (classReader == null) {
            return false;
        }

        if (getFunctorField(classReader) != null) {
            return true;
        }

        MethodReader methodReader = classReader.getMethod(methodRef.getDescriptor());
        return methodReader != null && getPublicAlias(methodReader) != null;
    }

    static class PropertyInfo {
        MethodDescriptor getter;
        MethodDescriptor setter;
    }

    static class Alias {
        final String name;
        final AliasKind kind;

        Alias(String name, AliasKind kind) {
            this.name = name;
            this.kind = kind;
        }
    }

    enum AliasKind {
        METHOD,
        GETTER,
        SETTER
    }
}
