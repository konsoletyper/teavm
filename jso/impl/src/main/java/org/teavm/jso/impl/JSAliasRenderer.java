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

import static org.teavm.jso.impl.AliasCollector.collectMembers;
import static org.teavm.jso.impl.AliasCollector.getPublicAlias;
import java.util.HashMap;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.rendering.RenderingManager;
import org.teavm.backend.javascript.spi.MethodContributor;
import org.teavm.backend.javascript.spi.MethodContributorContext;
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

class JSAliasRenderer implements RendererListener, MethodContributor {
    private static String variableChars = "abcdefghijklmnopqrstuvwxyz";
    private SourceWriter writer;
    private ListableClassReaderSource classSource;
    private JSTypeHelper typeHelper;
    private RenderingManager context;
    private int lastExportIndex;

    @Override
    public void begin(RenderingManager context, BuildTarget buildTarget) {
        writer = context.getWriter();
        classSource = context.getClassSource();
        typeHelper = new JSTypeHelper(context.getOriginalClassSource());
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
        writer.append("(()").sameLineWs().append("=>").ws().append("{").softNewLine().indent();
        writer.append("let c;").softNewLine();
        var exportedNamesByClass = new HashMap<String, String>();
        for (var className : classSource.getClassNames()) {
            var classReader = classSource.get(className);
            var hasExportedMembers = false;
            hasExportedMembers |= exportClassInstanceMembers(classReader);
            if (!className.equals(context.getEntryPoint())) {
                var name = "$rt_export_class_ " + getClassAliasName(classReader) + "_" + lastExportIndex++;
                hasExportedMembers |= exportClassStaticMembers(classReader, name);
                if (hasExportedMembers) {
                    exportedNamesByClass.put(className, name);
                }
            }
        }
        writer.outdent().append("})();").newLine();
        for (var className : classSource.getClassNames()) {
            var classReader = classSource.get(className);
            var name = exportedNamesByClass.get(className);
            if (name != null && !typeHelper.isJavaScriptClass(className)
                    && !typeHelper.isJavaScriptImplementation(className)) {
                exportClassFromModule(classReader, name);
            }
        }
    }

    private boolean exportClassInstanceMembers(ClassReader classReader) {
        var members = collectMembers(classReader, AliasCollector::isInstanceMember);

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
            if (classReader.getMethod(aliasEntry.getValue().getDescriptor()) == null) {
                continue;
            }
            appendMethodAlias(aliasEntry.getKey());
            writer.ws().append("=").ws().appendFunction("$rt_callWithReceiver").append("(")
                    .appendMethod(aliasEntry.getValue()).append(");").softNewLine();
        }
        for (var aliasEntry : members.properties.entrySet()) {
            var propInfo = aliasEntry.getValue();
            if (propInfo.getter == null || classReader.getMethod(propInfo.getter.getDescriptor()) == null) {
                continue;
            }
            appendPropertyAlias(aliasEntry.getKey());
            writer.append("get:").ws().appendFunction("$rt_callWithReceiver").append("(")
                    .appendMethod(propInfo.getter).append(")");
            if (propInfo.setter != null && classReader.getMethod(propInfo.setter.getDescriptor()) != null) {
                writer.append(",").softNewLine();
                writer.append("set:").ws().appendFunction("$rt_callWithReceiver").append("(")
                        .appendMethod(propInfo.setter).append(")");
            }
            writer.softNewLine().outdent().append("});").softNewLine();
        }

        var functorField = getFunctorField(classReader);
        if (functorField != null) {
            writeFunctor(classReader, functorField.getReference());
        }

        return true;
    }

    private boolean exportClassStaticMembers(ClassReader classReader, String name) {
        var members = collectMembers(classReader, AliasCollector::isStaticMember);

        if (members.methods.isEmpty() && members.properties.isEmpty()) {
            return false;
        }

        writer.append("c").ws().append("=").ws().appendFunction(name).append(";").softNewLine();

        for (var aliasEntry : members.methods.entrySet()) {
            appendMethodAlias(aliasEntry.getKey());
            var fullRef = aliasEntry.getValue();
            writer.ws().append("=").ws().appendMethod(fullRef).append(";").softNewLine();
        }
        for (var aliasEntry : members.properties.entrySet()) {
            var propInfo = aliasEntry.getValue();
            if (propInfo.getter == null) {
                continue;
            }
            appendPropertyAlias(aliasEntry.getKey());
            var fullGetter = propInfo.getter;
            writer.append("get:").ws().appendMethod(fullGetter);
            if (propInfo.setter != null) {
                writer.append(",").softNewLine();
                var fullSetter = propInfo.setter;
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

    private void exportModule() {
        var cls = classSource.get(context.getEntryPoint());
        for (var method : cls.getMethods()) {
            if (!method.hasModifier(ElementModifier.STATIC)) {
                continue;
            }
            var methodAlias = getPublicAlias(method);
            if (methodAlias != null && methodAlias.kind == AliasCollector.AliasKind.METHOD) {
                context.exportMethod(method.getReference(), methodAlias.name);
            }
        }
    }

    private void exportClassFromModule(ClassReader cls, String functionName) {
        var name = getClassAliasName(cls);
        var constructors = collectMembers(cls, AliasCollector::isInstanceMember);

        var method = constructors.constructor;
        writer.append("function ").appendFunction(functionName).append("(");
        if (method != null) {
            for (var i = 0; i < method.parameterCount(); ++i) {
                if (i > 0) {
                    writer.append(",").ws();
                }
                writer.append("p" + i);
            }
        }
        writer.append(")").ws().appendBlockStart();
        if (method != null) {
            writer.appendClass(cls.getName()).append(".call(this);").softNewLine();
            writer.appendMethod(method).append("(this");
            for (var i = 0; i < method.parameterCount(); ++i) {
                writer.append(",").ws().append("p" + i);
            }
            writer.append(");").softNewLine();
        } else {
            writer.append("throw new Error(\"Can't instantiate this class directly\");").softNewLine();
        }

        writer.outdent().append("}").append(";").softNewLine();

        writer.appendFunction(functionName).append(".prototype").ws().append("=").ws()
                .appendClass(cls.getName()).append(".prototype;").softNewLine();
        context.exportFunction(functionName, name);
    }

    private String getClassAliasName(ClassReader cls) {
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
        return name;
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
    public boolean isContributing(MethodContributorContext context, MethodReference methodRef) {
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


}
