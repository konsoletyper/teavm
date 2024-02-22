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
import java.util.Map;
import java.util.function.Predicate;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.rendering.RenderingManager;
import org.teavm.backend.javascript.spi.VirtualMethodContributor;
import org.teavm.backend.javascript.spi.VirtualMethodContributorContext;
import org.teavm.jso.JSClass;
import org.teavm.model.AnnotationReader;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
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
    private RenderingManager context;

    @Override
    public void begin(RenderingManager context, BuildTarget buildTarget) {
        writer = context.getWriter();
        classSource = context.getClassSource();
        typeHelper = new JSTypeHelper(context.getClassSource());
        this.context = context;
    }

    @Override
    public void complete() {
        exportClasses();
        exportModule();
    }

    private void exportClasses() {
        if (!hasClassesToExpose()) {
            return;
        }

        writer.startVariableDeclaration().appendFunction("$rt_jso_marker")
                .appendGlobal("Symbol").append("('jsoClass')").endDeclaration();
        writer.append("(()").ws().append("=>").ws().append("{").softNewLine().indent();
        writer.append("let c;").softNewLine();
        for (var className : classSource.getClassNames()) {
            var classReader = classSource.get(className);
            var hasExportedMembers = false;
            hasExportedMembers |= exportClassInstanceMembers(classReader);
            if (!className.equals(context.getEntryPoint())) {
                hasExportedMembers |= exportClassStaticMembers(classReader);
                if (hasExportedMembers && !typeHelper.isJavaScriptClass(className)
                        && !typeHelper.isJavaScriptImplementation(className)) {
                    exportClassFromModule(classReader);
                }
            }
        }
        writer.outdent().append("})();").newLine();
    }

    private boolean exportClassInstanceMembers(ClassReader classReader) {
        var members = collectMembers(classReader, method -> !method.hasModifier(ElementModifier.STATIC));

        var isJsClassImpl = typeHelper.isJavaScriptImplementation(classReader.getName());
        if (members.methods.isEmpty() && members.properties.isEmpty() && !isJsClassImpl) {
            return false;
        }

        writer.append("c").ws().append("=").ws().appendClass(classReader.getName()).append(".prototype;")
                .softNewLine();
        if (isJsClassImpl) {
            writer.append("c[").appendFunction("$rt_jso_marker").append("]").ws().append("=").ws().append("true;")
                    .softNewLine();
        }

        for (var aliasEntry : members.methods.entrySet()) {
            if (classReader.getMethod(aliasEntry.getValue()) == null) {
                continue;
            }
            appendMethodAlias(aliasEntry.getKey());
            writer.ws().append("=").ws().append("c.").appendVirtualMethod(aliasEntry.getValue())
                    .append(";").softNewLine();
        }
        for (var aliasEntry : members.properties.entrySet()) {
            var propInfo = aliasEntry.getValue();
            if (propInfo.getter == null || classReader.getMethod(propInfo.getter) == null) {
                continue;
            }
            appendPropertyAlias(aliasEntry.getKey());
            writer.append("get:").ws().append("c.").appendVirtualMethod(propInfo.getter);
            if (propInfo.setter != null && classReader.getMethod(propInfo.setter) != null) {
                writer.append(",").softNewLine();
                writer.append("set:").ws().append("c.").appendVirtualMethod(propInfo.setter);
            }
            writer.softNewLine().outdent().append("});").softNewLine();
        }

        var functorField = getFunctorField(classReader);
        if (functorField != null) {
            writeFunctor(classReader, functorField.getReference());
        }

        return true;
    }

    private boolean exportClassStaticMembers(ClassReader classReader) {
        var members = collectMembers(classReader, c -> c.hasModifier(ElementModifier.STATIC));

        if (members.methods.isEmpty() && members.properties.isEmpty()) {
            return false;
        }

        writer.append("c").ws().append("=").ws().appendClass(classReader.getName()).append(";").softNewLine();

        for (var aliasEntry : members.methods.entrySet()) {
            appendMethodAlias(aliasEntry.getKey());
            var fullRef = new MethodReference(classReader.getName(), aliasEntry.getValue());
            writer.ws().append("=").ws().appendMethod(fullRef).append(";").softNewLine();
        }
        for (var aliasEntry : members.properties.entrySet()) {
            var propInfo = aliasEntry.getValue();
            if (propInfo.getter == null) {
                continue;
            }
            appendPropertyAlias(aliasEntry.getKey());
            var fullGetter = new MethodReference(classReader.getName(), propInfo.getter);
            writer.append("get:").ws().appendMethod(fullGetter);
            if (propInfo.setter != null) {
                writer.append(",").softNewLine();
                var fullSetter = new MethodReference(classReader.getName(), propInfo.setter);
                writer.append("set:").ws().appendMethod(fullSetter);
            }
            writer.softNewLine().outdent().append("});").softNewLine();
        }

        return true;
    }

    private void appendMethodAlias(String name) {
        if (isKeyword(name)) {
            writer.append("c[\"").append(name).append("\"]");
        } else {
            writer.append("c.").append(name);
        }
    }

    private void appendPropertyAlias(String name) {
        writer.append("Object.defineProperty(c,")
                .ws().append("\"").append(name).append("\",")
                .ws().append("{").indent().softNewLine();
    }

    private Members collectMembers(ClassReader classReader, Predicate<MethodReader> filter) {
        var methods = new HashMap<String, MethodDescriptor>();
        var properties = new HashMap<String, PropertyInfo>();
        for (var method : classReader.getMethods()) {
            if (!filter.test(method)) {
                continue;
            }
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
        return new Members(methods, properties);
    }

    private void exportModule() {
        var cls = classSource.get(context.getEntryPoint());
        for (var method : cls.getMethods()) {
            if (!method.hasModifier(ElementModifier.STATIC)) {
                continue;
            }
            var methodAlias = getPublicAlias(method);
            if (methodAlias != null && methodAlias.kind == AliasKind.METHOD) {
                context.exportMethod(method.getReference(), methodAlias.name);
            }
        }
    }

    private void exportClassFromModule(ClassReader cls) {
        var name = cls.getSimpleName();
        if (name == null) {
            name = cls.getName().substring(cls.getName().lastIndexOf('.') + 1);
        }
        var jsExport = cls.getAnnotations().get(JSClass.class.getName());
        if (jsExport != null) {
            var nameValue = jsExport.getValue("name");
            if (nameValue != null) {
                var nameValueString = nameValue.getString();
                if (!nameValueString.isEmpty()) {
                    name = nameValueString;
                }
            }
        }
        context.exportClass(cls.getName(), name);
    }

    private boolean hasClassesToExpose() {
        for (String className : classSource.getClassNames()) {
            ClassReader cls = classSource.get(className);
            if (typeHelper.isJavaScriptImplementation(className)) {
                return true;
            }
            for (var method : cls.getMethods()) {
                if (getPublicAlias(method) != null) {
                    return true;
                }
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
        writer.append("return self.").appendVirtualMethod(functorMethod).append('(');
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

    private static class Members {
        final Map<String, MethodDescriptor> methods;
        final Map<String, PropertyInfo> properties;

        Members(Map<String, MethodDescriptor> methods, Map<String, PropertyInfo> properties) {
            this.methods = methods;
            this.properties = properties;
        }
    }

    private static class PropertyInfo {
        MethodDescriptor getter;
        MethodDescriptor setter;
    }

    private static class Alias {
        final String name;
        final AliasKind kind;

        Alias(String name, AliasKind kind) {
            this.name = name;
            this.kind = kind;
        }
    }

    private enum AliasKind {
        METHOD,
        GETTER,
        SETTER
    }
}
