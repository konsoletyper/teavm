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
package org.teavm.backend.javascript.rendering;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.teavm.ast.AsyncMethodNode;
import org.teavm.ast.AsyncMethodPart;
import org.teavm.ast.MethodNode;
import org.teavm.ast.MethodNodeVisitor;
import org.teavm.ast.RegularMethodNode;
import org.teavm.ast.VariableNode;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.dependency.DependencyInfo;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class MethodBodyRenderer implements MethodNodeVisitor, GeneratorContext {
    private RenderingContext context;
    private Diagnostics diagnostics;
    private boolean minifying;
    private boolean async;
    private Set<MethodReference> asyncMethods;
    private SourceWriter writer;
    private StatementRenderer statementRenderer;
    private boolean threadLibraryUsed;

    public MethodBodyRenderer(RenderingContext context, Diagnostics diagnostics, boolean minifying,
            Set<MethodReference> asyncMethods, SourceWriter writer) {
        this.context = context;
        this.diagnostics = diagnostics;
        this.minifying = minifying;
        this.asyncMethods = asyncMethods;
        this.writer = writer;
        statementRenderer = new StatementRenderer(context, writer);
    }

    public void setCurrentMethod(MethodNode node) {
        statementRenderer.setCurrentMethod(node);
    }

    public boolean isThreadLibraryUsed() {
        return threadLibraryUsed;
    }

    @Override
    public DependencyInfo getDependency() {
        return context.getDependencyInfo();
    }

    public void renderNative(Generator generator, boolean async, MethodReference reference) {
        threadLibraryUsed = false;
        this.async = async;
        statementRenderer.setAsync(async);
        generator.generate(this, writer, reference);
    }

    public void render(MethodNode node, boolean async) {
        threadLibraryUsed = false;
        this.async = async;
        statementRenderer.setAsync(async);
        prepareVariables(node);
        node.acceptVisitor(this);
        statementRenderer.clear();
    }

    private void prepareVariables(MethodNode method) {
        for (int i = 0; i < method.getVariables().size(); ++i) {
            writer.emitVariables(new String[] { method.getVariables().get(i).getName() },
                    statementRenderer.variableName(i));
        }
    }

    public void renderParameters(MethodReference reference, Set<ElementModifier> modifiers) {
        int startParam = 0;
        if (modifiers.contains(ElementModifier.STATIC)) {
            startParam = 1;
        }
        var count = reference.parameterCount() - startParam + 1;
        if (count != 1) {
            writer.append("(");
        }
        for (int i = startParam; i <= reference.parameterCount(); ++i) {
            if (i > startParam) {
                writer.append(",").ws();
            }
            writer.append(statementRenderer.variableName(i));
        }
        if (count != 1) {
            writer.append(")");
        }
    }

    private void appendMonitor(StatementRenderer statementRenderer, MethodNode methodNode) {
        if (methodNode.getModifiers().contains(ElementModifier.STATIC)) {
            writer.appendFunction("$rt_cls").append("(")
                    .appendClass(methodNode.getReference().getClassName()).append(")");
        } else {
            writer.append(statementRenderer.variableName(0));
        }
    }

    @Override
    public void visit(RegularMethodNode method) {
        statementRenderer.setAsync(false);
        this.async = false;

        int variableCount = 0;
        for (VariableNode var : method.getVariables()) {
            variableCount = Math.max(variableCount, var.getIndex() + 1);
        }
        TryCatchFinder tryCatchFinder = new TryCatchFinder();
        method.getBody().acceptVisitor(tryCatchFinder);
        boolean hasTryCatch = tryCatchFinder.tryCatchFound;
        List<String> variableNames = new ArrayList<>();
        for (int i = method.getReference().parameterCount() + 1; i < variableCount; ++i) {
            variableNames.add(statementRenderer.variableName(i));
        }
        if (hasTryCatch) {
            variableNames.add("$$je");
        }
        if (!variableNames.isEmpty()) {
            writer.append("let ");
            for (int i = 0; i < variableNames.size(); ++i) {
                if (i > 0) {
                    writer.append(",").ws();
                }
                writer.append(variableNames.get(i));
            }
            writer.append(";").softNewLine();
        }

        statementRenderer.setEnd(true);
        statementRenderer.setCurrentPart(0);

        if (method.getModifiers().contains(ElementModifier.SYNCHRONIZED)) {
            writer.appendMethodBody(NameFrequencyEstimator.MONITOR_ENTER_SYNC_METHOD);
            writer.append("(");
            appendMonitor(statementRenderer, method);
            writer.append(");").softNewLine();

            writer.append("try").ws().append("{").softNewLine().indent();
        }

        method.getBody().acceptVisitor(statementRenderer);

        if (method.getModifiers().contains(ElementModifier.SYNCHRONIZED)) {
            writer.outdent().append("}").ws().append("finally").ws().append("{").indent().softNewLine();

            writer.appendMethodBody(NameFrequencyEstimator.MONITOR_EXIT_SYNC_METHOD);
            writer.append("(");
            appendMonitor(statementRenderer, method);
            writer.append(");").softNewLine();

            writer.outdent().append("}").softNewLine();
        }
    }

    @Override
    public void visit(AsyncMethodNode methodNode) {
        threadLibraryUsed = true;
        statementRenderer.setAsync(true);
        this.async = true;
        MethodReference ref = methodNode.getReference();
        int variableCount = 0;
        for (VariableNode var : methodNode.getVariables()) {
            variableCount = Math.max(variableCount, var.getIndex() + 1);
        }
        List<String> variableNames = new ArrayList<>();
        for (int i = ref.parameterCount() + 1; i < variableCount; ++i) {
            variableNames.add(statementRenderer.variableName(i));
        }
        TryCatchFinder tryCatchFinder = new TryCatchFinder();
        for (AsyncMethodPart part : methodNode.getBody()) {
            if (!tryCatchFinder.tryCatchFound) {
                part.getStatement().acceptVisitor(tryCatchFinder);
            }
        }
        boolean hasTryCatch = tryCatchFinder.tryCatchFound;
        if (hasTryCatch) {
            variableNames.add("$$je");
        }
        variableNames.add(context.pointerName());
        variableNames.add(context.tempVarName());
        writer.append("let ");
        for (int i = 0; i < variableNames.size(); ++i) {
            if (i > 0) {
                writer.append(",").ws();
            }
            writer.append(variableNames.get(i));
        }
        writer.append(";").softNewLine();

        int firstToSave = 0;
        if (methodNode.getModifiers().contains(ElementModifier.STATIC)) {
            firstToSave = 1;
        }

        String popName = minifying ? "l" : "pop";
        String pushName = minifying ? "s" : "push";
        writer.append(context.pointerName()).ws().append('=').ws().append("0;").softNewLine();
        writer.append("if").ws().append("(").appendFunction("$rt_resuming").append("())").ws()
                .append("{").indent().softNewLine();
        writer.append("let ").append(context.threadName()).ws().append('=').ws()
                .appendFunction("$rt_nativeThread").append("();").softNewLine();
        writer.append(context.pointerName()).ws().append('=').ws().append(context.threadName()).append(".")
                .append(popName).append("();");
        for (int i = variableCount - 1; i >= firstToSave; --i) {
            writer.append(statementRenderer.variableName(i)).ws().append('=').ws()
                    .append(context.threadName())
                    .append(".").append(popName).append("();");
        }
        writer.softNewLine();
        writer.outdent().append("}").softNewLine();

        if (methodNode.getModifiers().contains(ElementModifier.SYNCHRONIZED)) {
            writer.append("try").ws().append('{').indent().softNewLine();
        }

        Renderer.renderAsyncPrologue(writer, context);
        for (int i = 0; i < methodNode.getBody().size(); ++i) {
            writer.append("case ").append(i).append(":").indent().softNewLine();
            if (i == 0 && methodNode.getModifiers().contains(ElementModifier.SYNCHRONIZED)) {
                writer.appendMethodBody(NameFrequencyEstimator.MONITOR_ENTER_METHOD);
                writer.append("(");
                appendMonitor(statementRenderer, methodNode);
                writer.append(");").softNewLine();
                statementRenderer.emitSuspendChecker();
            }
            AsyncMethodPart part = methodNode.getBody().get(i);
            statementRenderer.setEnd(true);
            statementRenderer.setCurrentPart(i);
            part.getStatement().acceptVisitor(statementRenderer);
            writer.outdent();
        }
        Renderer.renderAsyncEpilogue(writer);

        if (methodNode.getModifiers().contains(ElementModifier.SYNCHRONIZED)) {
            writer.outdent().append("}").ws().append("finally").ws().append('{').indent().softNewLine();
            writer.append("if").ws().append("(!").appendFunction("$rt_suspending").append("())")
                    .ws().append("{").indent().softNewLine();
            writer.appendMethodBody(NameFrequencyEstimator.MONITOR_EXIT_METHOD).append("(");
            appendMonitor(statementRenderer, methodNode);
            writer.append(");").softNewLine();
            writer.outdent().append('}').softNewLine();
            writer.outdent().append('}').softNewLine();
        }

        writer.appendFunction("$rt_nativeThread").append("().").append(pushName).append("(");
        for (int i = firstToSave; i < variableCount; ++i) {
            writer.append(statementRenderer.variableName(i)).append(',').ws();
        }
        writer.append(context.pointerName()).append(");");
        writer.softNewLine();
    }

    @Override
    public String getParameterName(int index) {
        return statementRenderer.variableName(index);
    }

    @Override
    public ListableClassReaderSource getClassSource() {
        return context.getClassSource();
    }

    @Override
    public ClassReaderSource getInitialClassSource() {
        return context.getInitialClassSource();
    }

    @Override
    public ClassLoader getClassLoader() {
        return context.getClassLoader();
    }

    @Override
    public Properties getProperties() {
        return new Properties(context.getProperties());
    }

    @Override
    public <T> T getService(Class<T> type) {
        return context.getServices().getService(type);
    }

    @Override
    public boolean isAsync() {
        return async;
    }

    @Override
    public boolean isAsync(MethodReference method) {
        return asyncMethods.contains(method);
    }

    @Override
    public Diagnostics getDiagnostics() {
        return diagnostics;
    }

    @Override
    public void typeToClassString(SourceWriter writer, ValueType type) {
        context.typeToClsString(writer, type);
    }

    @Override
    public boolean isDynamicInitializer(String className) {
        return context.isDynamicInitializer(className);
    }

    @Override
    public String importModule(String name) {
        return context.importModule(name);
    }
}
