/*
 *  Copyright 2016 Alexey Andreev.
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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.teavm.ast.AssignmentStatement;
import org.teavm.ast.AsyncMethodNode;
import org.teavm.ast.AsyncMethodPart;
import org.teavm.ast.BinaryExpr;
import org.teavm.ast.BinaryOperation;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.BreakStatement;
import org.teavm.ast.CastExpr;
import org.teavm.ast.ClassNode;
import org.teavm.ast.ConditionalExpr;
import org.teavm.ast.ConditionalStatement;
import org.teavm.ast.ConstantExpr;
import org.teavm.ast.ContinueStatement;
import org.teavm.ast.Expr;
import org.teavm.ast.ExprVisitor;
import org.teavm.ast.FieldNode;
import org.teavm.ast.GotoPartStatement;
import org.teavm.ast.InitClassStatement;
import org.teavm.ast.InstanceOfExpr;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.InvocationType;
import org.teavm.ast.MethodNode;
import org.teavm.ast.MethodNodeVisitor;
import org.teavm.ast.MonitorEnterStatement;
import org.teavm.ast.MonitorExitStatement;
import org.teavm.ast.NativeMethodNode;
import org.teavm.ast.NewArrayExpr;
import org.teavm.ast.NewExpr;
import org.teavm.ast.NewMultiArrayExpr;
import org.teavm.ast.OperationType;
import org.teavm.ast.PrimitiveCastExpr;
import org.teavm.ast.QualificationExpr;
import org.teavm.ast.RegularMethodNode;
import org.teavm.ast.ReturnStatement;
import org.teavm.ast.SequentialStatement;
import org.teavm.ast.Statement;
import org.teavm.ast.StatementVisitor;
import org.teavm.ast.SubscriptExpr;
import org.teavm.ast.SwitchClause;
import org.teavm.ast.SwitchStatement;
import org.teavm.ast.ThrowStatement;
import org.teavm.ast.TryCatchStatement;
import org.teavm.ast.UnaryExpr;
import org.teavm.ast.UnwrapArrayExpr;
import org.teavm.ast.VariableExpr;
import org.teavm.ast.VariableNode;
import org.teavm.ast.WhileStatement;
import org.teavm.backend.javascript.codegen.NamingException;
import org.teavm.backend.javascript.codegen.NamingOrderer;
import org.teavm.backend.javascript.codegen.NamingStrategy;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.common.ServiceRepository;
import org.teavm.debugging.information.DebugInformationEmitter;
import org.teavm.debugging.information.DeferredCallSite;
import org.teavm.debugging.information.DummyDebugInformationEmitter;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReference;
import org.teavm.model.ListableClassHolderSource;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.vm.RenderingException;

public class Renderer implements ExprVisitor, StatementVisitor, RenderingContext {
    private static final String variableNames = "abcdefghijkmnopqrstuvwxyz";
    private static final String variablePartNames = "abcdefghijkmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Set<String> keywords = new HashSet<>(Arrays.asList("break", "case", "catch",
            "class", "const", "continue", "debugger", "default", "delete", "do", "else", "export",
            "extends", "finally", "for", "function", "if", "import", "in", "instanceof", "new", "return",
            "super", "switch", "this", "throw", "try", "typeof", "var", "void", "while", "with", "yield"));
    private final NamingStrategy naming;
    private final SourceWriter writer;
    private final ListableClassHolderSource classSource;
    private final ClassLoader classLoader;
    private boolean minifying;
    private final Map<MethodReference, InjectorHolder> injectorMap = new HashMap<>();
    private final Map<String, Integer> stringPoolMap = new HashMap<>();
    private final List<String> stringPool = new ArrayList<>();
    private final Properties properties = new Properties();
    private final ServiceRepository services;
    private DebugInformationEmitter debugEmitter = new DummyDebugInformationEmitter();
    private final Deque<LocationStackEntry> locationStack = new ArrayDeque<>();
    private DeferredCallSite lastCallSite;
    private DeferredCallSite prevCallSite;
    private final Set<MethodReference> asyncMethods;
    private final Set<MethodReference> asyncFamilyMethods;
    private final Diagnostics diagnostics;
    private boolean async;
    private Precedence precedence;
    private final Map<String, String> blockIdMap = new HashMap<>();
    private final List<String> cachedVariableNames = new ArrayList<>();
    private final Set<String> usedVariableNames = new HashSet<>();
    private MethodNode currentMethod;
    private boolean end;
    private int currentPart;
    private List<PostponedFieldInitializer> postponedFieldInitializers = new ArrayList<>();

    private static class InjectorHolder {
        public final Injector injector;

        private InjectorHolder(Injector injector) {
            this.injector = injector;
        }
    }

    private static class LocationStackEntry {
        final TextLocation location;

        LocationStackEntry(TextLocation location) {
            this.location = location;
        }
    }

    public void addInjector(MethodReference method, Injector injector) {
        injectorMap.put(method, new InjectorHolder(injector));
    }

    public Renderer(SourceWriter writer, ListableClassHolderSource classSource, ClassLoader classLoader,
            ServiceRepository services, Set<MethodReference> asyncMethods, Set<MethodReference> asyncFamilyMethods,
            Diagnostics diagnostics) {
        this.naming = writer.getNaming();
        this.writer = writer;
        this.classSource = classSource;
        this.classLoader = classLoader;
        this.services = services;
        this.asyncMethods = new HashSet<>(asyncMethods);
        this.asyncFamilyMethods = new HashSet<>(asyncFamilyMethods);
        this.diagnostics = diagnostics;
    }

    @Override
    public SourceWriter getWriter() {
        return writer;
    }

    @Override
    public NamingStrategy getNaming() {
        return naming;
    }

    @Override
    public boolean isMinifying() {
        return minifying;
    }

    public void setMinifying(boolean minifying) {
        this.minifying = minifying;
    }

    @Override
    public ListableClassHolderSource getClassSource() {
        return classSource;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public Properties getProperties() {
        Properties properties = new Properties();
        properties.putAll(this.properties);
        return properties;
    }

    public DebugInformationEmitter getDebugEmitter() {
        return debugEmitter;
    }

    public void setDebugEmitter(DebugInformationEmitter debugEmitter) {
        this.debugEmitter = debugEmitter;
    }

    public void setProperties(Properties properties) {
        this.properties.clear();
        this.properties.putAll(properties);
    }

    public void renderStringPool() throws RenderingException {
        if (stringPool.isEmpty()) {
            return;
        }
        try {
            writer.append("$rt_stringPool([");
            for (int i = 0; i < stringPool.size(); ++i) {
                if (i > 0) {
                    writer.append(',').ws();
                }
                writer.append('"').append(escapeString(stringPool.get(i))).append('"');
            }
            writer.append("]);").newLine();
        } catch (IOException e) {
            throw new RenderingException("IO error", e);
        }
    }

    public void renderStringConstants() throws RenderingException {
        try {
            for (PostponedFieldInitializer initializer : postponedFieldInitializers) {
                writer.appendStaticField(initializer.field).ws().append("=").ws()
                        .append(constantToString(initializer.value)).append(";").softNewLine();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error", e);
        }
    }

    public void renderRuntime() throws RenderingException {
        try {
            renderRuntimeCls();
            renderRuntimeString();
            renderRuntimeUnwrapString();
            renderRuntimeObjcls();
            renderRuntimeNullCheck();
            renderRuntimeIntern();
            renderRuntimeThreads();
        } catch (NamingException e) {
            throw new RenderingException("Error rendering runtime methods. See a cause for details", e);
        } catch (IOException e) {
            throw new RenderingException("IO error", e);
        }
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
        writer.append("if (str===null){").indent().softNewLine();
        writer.append("return null;").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("var characters = $rt_createCharArray(str.length);").softNewLine();
        writer.append("var charsBuffer = characters.data;").softNewLine();
        writer.append("for (var i = 0; i < str.length; i = (i + 1) | 0) {").indent().softNewLine();
        writer.append("charsBuffer[i] = str.charCodeAt(i) & 0xFFFF;").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return ").append(naming.getNameForInit(stringCons)).append("(characters);").softNewLine();
        writer.outdent().append("}").newLine();
    }

    private void renderRuntimeUnwrapString() throws IOException {
        MethodReference stringLen = new MethodReference(String.class, "length", int.class);
        MethodReference getChars = new MethodReference(String.class, "getChars", int.class, int.class,
                char[].class, int.class, void.class);
        writer.append("function $rt_ustr(str) {").indent().softNewLine();
        writer.append("var result = \"\";").softNewLine();
        writer.append("var sz = ").appendMethodBody(stringLen).append("(str);").softNewLine();
        writer.append("var array = $rt_createCharArray(sz);").softNewLine();
        writer.appendMethodBody(getChars).append("(str, 0, sz, array, 0);").softNewLine();
        writer.append("for (var i = 0; i < sz; i = (i + 1) | 0) {").indent().softNewLine();
        writer.append("result += String.fromCharCode(array.data[i]);").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return result;").softNewLine();
        writer.outdent().append("}").newLine();
    }

    private void renderRuntimeNullCheck() throws IOException {
        writer.append("function $rt_nullCheck(val) {").indent().softNewLine();
        writer.append("if (val === null) {").indent().softNewLine();
        writer.append("$rt_throw(").append(naming.getNameForInit(new MethodReference(NullPointerException.class,
                "<init>", void.class))).append("());").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return val;").softNewLine();
        writer.outdent().append("}").newLine();
    }

    private void renderRuntimeIntern() throws IOException {
        writer.append("function $rt_intern(str) {").indent().softNewLine();
        writer.append("return ").appendMethodBody(new MethodReference(String.class, "intern", String.class))
            .append("(str);").softNewLine();
        writer.outdent().append("}").newLine();
    }

    private void renderRuntimeObjcls() throws IOException {
        writer.append("function $rt_objcls() { return ").appendClass("java.lang.Object").append("; }").newLine();
    }

    private void renderRuntimeThreads() throws IOException {
        writer.append("function $rt_getThread()").ws().append("{").indent().softNewLine();
        writer.append("return ").appendMethodBody(Thread.class, "currentThread", Thread.class).append("();")
                .softNewLine();
        writer.outdent().append("}").newLine();

        writer.append("function $rt_setThread(t)").ws().append("{").indent().softNewLine();
        writer.append("return ").appendMethodBody(Thread.class, "setCurrentThread", Thread.class, void.class)
                .append("(t);").softNewLine();
        writer.outdent().append("}").newLine();
    }

    private void renderRuntimeAliases() throws IOException {
        String[] names = { "$rt_throw", "$rt_compare", "$rt_nullCheck", "$rt_cls", "$rt_createArray",
                "$rt_isInstance", "$rt_nativeThread", "$rt_suspending", "$rt_resuming", "$rt_invalidPointer" };
        boolean first = true;
        for (String name : names) {
            if (!first) {
                writer.softNewLine();
            }
            first = false;
            writer.append("var ").appendFunction(name).ws().append('=').ws().append(name).append(";").softNewLine();
        }
        writer.newLine();
    }

    public void render(List<ClassNode> classes) throws RenderingException {
        if (minifying) {
            NamingOrderer orderer = new NamingOrderer();
            NameFrequencyEstimator estimator = new NameFrequencyEstimator(orderer, classSource, asyncMethods,
                    asyncFamilyMethods);
            for (ClassNode cls : classes) {
                estimator.estimate(cls);
            }
            orderer.apply(naming);
        }

        if (minifying) {
            try {
                renderRuntimeAliases();
            } catch (IOException e) {
                throw new RenderingException(e);
            }
        }
        for (ClassNode cls : classes) {
            renderDeclaration(cls);
        }
        for (ClassNode cls : classes) {
            renderMethodBodies(cls);
        }
        renderClassMetadata(classes);
    }

    private void renderDeclaration(ClassNode cls) throws RenderingException {
        debugEmitter.addClass(cls.getName(), cls.getParentName());
        try {
            writer.append("function ").appendClass(cls.getName()).append("()").ws().append("{")
                    .indent().softNewLine();
            boolean thisAliased = false;
            List<FieldNode> nonStaticFields = new ArrayList<>();
            List<FieldNode> staticFields = new ArrayList<>();
            for (FieldNode field : cls.getFields()) {
                if (field.getModifiers().contains(ElementModifier.STATIC)) {
                    staticFields.add(field);
                } else {
                    nonStaticFields.add(field);
                }
            }
            if (nonStaticFields.size() > 1) {
                thisAliased = true;
                writer.append("var a").ws().append("=").ws().append("this;").ws();
            }
            if (cls.getParentName() != null) {
                writer.appendClass(cls.getParentName()).append(".call(").append(thisAliased ? "a" : "this")
                        .append(");").softNewLine();
            }
            for (FieldNode field : nonStaticFields) {
                Object value = field.getInitialValue();
                if (value == null) {
                    value = getDefaultValue(field.getType());
                }
                FieldReference fieldRef = new FieldReference(cls.getName(), field.getName());
                writer.append(thisAliased ? "a" : "this").append(".").appendField(fieldRef).ws()
                        .append("=").ws().append(constantToString(value)).append(";").softNewLine();
                debugEmitter.addField(field.getName(), naming.getNameFor(fieldRef));
            }

            if (cls.getName().equals("java.lang.Object")) {
                writer.append("this.$id").ws().append('=').ws().append("0;").softNewLine();
            }

            writer.outdent().append("}").newLine();

            for (FieldNode field : staticFields) {
                Object value = field.getInitialValue();
                if (value == null) {
                    value = getDefaultValue(field.getType());
                }
                FieldReference fieldRef = new FieldReference(cls.getName(), field.getName());
                if (value instanceof String) {
                    constantToString(value);
                    postponedFieldInitializers.add(new PostponedFieldInitializer(fieldRef, (String) value));
                    value = null;
                }
                writer.append("var ").appendStaticField(fieldRef).ws().append("=").ws()
                        .append(constantToString(value)).append(";").softNewLine();
            }
        } catch (NamingException e) {
            throw new RenderingException("Error rendering class " + cls.getName() + ". See cause for details", e);
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    private void renderMethodBodies(ClassNode cls) throws RenderingException {
        debugEmitter.emitClass(cls.getName());
        try {
            List<MethodNode> nonInitMethods = new ArrayList<>();
            MethodHolder clinit = classSource.get(cls.getName()).getMethod(
                    new MethodDescriptor("<clinit>", ValueType.VOID));
            boolean needsClinit = clinit != null;
            List<MethodNode> clinitMethods = new ArrayList<>();
            for (MethodNode method : cls.getMethods()) {
                if (needsClinit && (method.getModifiers().contains(ElementModifier.STATIC)
                        || method.getReference().getName().equals("<init>"))) {
                    clinitMethods.add(method);
                } else {
                    nonInitMethods.add(method);
                }
            }

            if (needsClinit) {
                writer.append("function ").appendClass(cls.getName()).append("_$callClinit()").ws()
                        .append("{").softNewLine().indent();
                writer.appendClass(cls.getName()).append("_$callClinit").ws().append("=").ws()
                        .append("function(){};").newLine();
                for (MethodNode method : clinitMethods) {
                    renderBody(method, true);
                }
                writer.appendMethodBody(new MethodReference(cls.getName(), clinit.getDescriptor()))
                        .append("();").softNewLine();
                writer.outdent().append("}").newLine();
            }
            if (!cls.getModifiers().contains(ElementModifier.INTERFACE)) {
                for (MethodNode method : cls.getMethods()) {
                    cls.getMethods();
                    if (!method.getModifiers().contains(ElementModifier.STATIC)) {
                        if (method.getReference().getName().equals("<init>")) {
                            renderInitializer(method);
                        }
                    }
                }
            }

            for (MethodNode method : nonInitMethods) {
                renderBody(method, false);
            }
        } catch (NamingException e) {
            throw new RenderingException("Error rendering class " + cls.getName() + ". See a cause for details", e);
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
        debugEmitter.emitClass(null);
    }

    private void renderClassMetadata(List<ClassNode> classes) {
        try {
            writer.append("$rt_metadata([");
            boolean first = true;
            for (ClassNode cls : classes) {
                if (!first) {
                    writer.append(',').softNewLine();
                }
                first = false;
                writer.appendClass(cls.getName()).append(",").ws();
                writer.append("\"").append(escapeString(cls.getName())).append("\",").ws();
                if (cls.getParentName() != null) {
                    writer.appendClass(cls.getParentName());
                } else {
                    writer.append("0");
                }
                writer.append(',').ws();
                writer.append("[");
                for (int i = 0; i < cls.getInterfaces().size(); ++i) {
                    String iface = cls.getInterfaces().get(i);
                    if (i > 0) {
                        writer.append(",").ws();
                    }
                    writer.appendClass(iface);
                }
                writer.append("],").ws();
                int flags = 0;
                if (cls.getModifiers().contains(ElementModifier.ENUM)) {
                    flags |= 1;
                }
                writer.append(flags).append(',').ws();
                MethodHolder clinit = classSource.get(cls.getName()).getMethod(
                        new MethodDescriptor("<clinit>", ValueType.VOID));
                if (clinit != null) {
                    writer.appendClass(cls.getName()).append("_$callClinit");
                } else {
                    writer.append('0');
                }
                writer.append(',').ws();

                List<String> stubNames = new ArrayList<>();
                List<MethodNode> virtualMethods = new ArrayList<>();
                for (MethodNode method : cls.getMethods()) {
                    if (clinit != null && (method.getModifiers().contains(ElementModifier.STATIC)
                            || method.getReference().getName().equals("<init>"))) {
                        stubNames.add(naming.getFullNameFor(method.getReference()));
                    }
                    if (!method.getModifiers().contains(ElementModifier.STATIC)) {
                        virtualMethods.add(method);
                    }
                }
                if (stubNames.size() == 1) {
                    writer.append("'").append(stubNames.get(0)).append("'");
                } else {
                    writer.append('[');
                    for (int j = 0; j < stubNames.size(); ++j) {
                        if (j > 0) {
                            writer.append(",").ws();
                        }
                        writer.append("'").append(stubNames.get(j)).append("'");
                    }
                    writer.append(']');
                }
                writer.append(',').ws();

                renderVirtualDeclarations(virtualMethods);
            }
            writer.append("]);").newLine();
        } catch (NamingException e) {
            throw new RenderingException("Error rendering class metadata. See a cause for details", e);
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    private static Object getDefaultValue(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            ValueType.Primitive primitive = (ValueType.Primitive) type;
            switch (primitive.getKind()) {
                case BOOLEAN:
                    return false;
                case BYTE:
                    return (byte) 0;
                case SHORT:
                    return (short) 0;
                case INTEGER:
                    return 0;
                case CHARACTER:
                    return '\0';
                case LONG:
                    return 0L;
                case FLOAT:
                    return 0F;
                case DOUBLE:
                    return 0.0;
            }
        }
        return null;
    }

    private void renderInitializer(MethodNode method) throws IOException {
        MethodReference ref = method.getReference();
        debugEmitter.emitMethod(ref.getDescriptor());
        writer.append("function ").append(naming.getNameForInit(ref)).append("(");
        for (int i = 1; i <= ref.parameterCount(); ++i) {
            if (i > 1) {
                writer.append(",").ws();
            }
            writer.append(variableName(i));
        }
        writer.append(")").ws().append("{").softNewLine().indent();
        writer.append("var $r").ws().append("=").ws().append("new ").appendClass(
                ref.getClassName()).append("();").softNewLine();
        writer.append(naming.getFullNameFor(ref)).append("($r");
        for (int i = 1; i <= ref.parameterCount(); ++i) {
            writer.append(",").ws();
            writer.append(variableName(i));
        }
        writer.append(");").softNewLine();
        writer.append("return $r;").softNewLine();
        writer.outdent().append("}").newLine();
        debugEmitter.emitMethod(null);
    }

    private void renderVirtualDeclarations(List<MethodNode> methods) throws NamingException, IOException {
        writer.append("[");
        boolean first = true;
        for (MethodNode method : methods) {
            debugEmitter.emitMethod(method.getReference().getDescriptor());
            MethodReference ref = method.getReference();
            if (!first) {
                writer.append(",").ws();
            }
            first = false;
            emitVirtualDeclaration(ref);
            debugEmitter.emitMethod(null);
        }
        writer.append("]");
    }

    private void emitVirtualDeclaration(MethodReference ref) throws IOException {
        String methodName = naming.getNameFor(ref.getDescriptor());
        writer.append("\"").append(methodName).append("\"");
        writer.append(",").ws().append("function(");
        List<String> args = new ArrayList<>();
        for (int i = 1; i <= ref.parameterCount(); ++i) {
            args.add(variableName(i));
        }
        for (int i = 0; i < args.size(); ++i) {
            if (i > 0) {
                writer.append(",").ws();
            }
            writer.append(args.get(i));
        }
        writer.append(")").ws().append("{").ws();
        if (ref.getDescriptor().getResultType() != ValueType.VOID) {
            writer.append("return ");
        }
        writer.appendMethodBody(ref).append("(");
        writer.append("this");
        for (String arg : args) {
            writer.append(",").ws().append(arg);
        }
        writer.append(");").ws().append("}");
    }

    private void renderBody(MethodNode method, boolean inner) throws IOException {
        currentMethod = method;
        cachedVariableNames.clear();
        usedVariableNames.clear();
        blockIdMap.clear();
        MethodReference ref = method.getReference();
        debugEmitter.emitMethod(ref.getDescriptor());
        String name = naming.getFullNameFor(ref);
        if (inner) {
            writer.append(name).ws().append("=").ws().append("function(");
        } else {
            writer.append("function ").append(name).append("(");
        }
        int startParam = 0;
        if (method.getModifiers().contains(ElementModifier.STATIC)) {
            startParam = 1;
        }
        for (int i = startParam; i <= ref.parameterCount(); ++i) {
            if (i > startParam) {
                writer.append(",").ws();
            }
            writer.append(variableName(i));
        }
        writer.append(")").ws().append("{").softNewLine().indent();
        method.acceptVisitor(new MethodBodyRenderer());
        writer.outdent().append("}");
        if (inner) {
            writer.append(';');
        }
        writer.newLine();
        debugEmitter.emitMethod(null);
    }

    private class MethodBodyRenderer implements MethodNodeVisitor, GeneratorContext {
        private boolean async;

        @Override
        public void visit(NativeMethodNode methodNode) {
            try {
                this.async = methodNode.isAsync();
                Renderer.this.async = methodNode.isAsync();
                methodNode.getGenerator().generate(this, writer, methodNode.getReference());
            } catch (IOException e) {
                throw new RenderingException("IO error occurred", e);
            }
        }

        @Override
        public void visit(RegularMethodNode method) {
            try {
                Renderer.this.async = false;
                this.async = false;
                MethodReference ref = method.getReference();
                for (int i = 0; i < method.getVariables().size(); ++i) {
                    debugEmitter.emitVariable(new String[] { method.getVariables().get(i).getName() },
                            variableName(i));
                }

                int variableCount = 0;
                for (VariableNode var : method.getVariables()) {
                    variableCount = Math.max(variableCount, var.getIndex() + 1);
                }
                TryCatchFinder tryCatchFinder = new TryCatchFinder();
                method.getBody().acceptVisitor(tryCatchFinder);
                boolean hasTryCatch = tryCatchFinder.tryCatchFound;
                List<String> variableNames = new ArrayList<>();
                for (int i = ref.parameterCount() + 1; i < variableCount; ++i) {
                    variableNames.add(variableName(i));
                }
                if (hasTryCatch) {
                    variableNames.add("$je");
                }
                if (!variableNames.isEmpty()) {
                    writer.append("var ");
                    for (int i = 0; i < variableNames.size(); ++i) {
                        if (i > 0) {
                            writer.append(",").ws();
                        }
                        writer.append(variableNames.get(i));
                    }
                    writer.append(";").softNewLine();
                }

                end = true;
                currentPart = 0;
                method.getBody().acceptVisitor(Renderer.this);
            } catch (IOException e) {
                throw new RenderingException("IO error occurred", e);
            }
        }

        @Override
        public void visit(AsyncMethodNode methodNode) {
            try {
                Renderer.this.async = true;
                this.async = true;
                MethodReference ref = methodNode.getReference();
                for (int i = 0; i < methodNode.getVariables().size(); ++i) {
                    debugEmitter.emitVariable(new String[] { methodNode.getVariables().get(i).getName() },
                            variableName(i));
                }
                int variableCount = 0;
                for (VariableNode var : methodNode.getVariables()) {
                    variableCount = Math.max(variableCount, var.getIndex() + 1);
                }
                List<String> variableNames = new ArrayList<>();
                for (int i = ref.parameterCount() + 1; i < variableCount; ++i) {
                    variableNames.add(variableName(i));
                }
                TryCatchFinder tryCatchFinder = new TryCatchFinder();
                for (AsyncMethodPart part : methodNode.getBody()) {
                    if (!tryCatchFinder.tryCatchFound) {
                        part.getStatement().acceptVisitor(tryCatchFinder);
                    }
                }
                boolean hasTryCatch = tryCatchFinder.tryCatchFound;
                if (hasTryCatch) {
                    variableNames.add("$je");
                }
                variableNames.add(pointerName());
                variableNames.add(tempVarName());
                if (!variableNames.isEmpty()) {
                    writer.append("var ");
                    for (int i = 0; i < variableNames.size(); ++i) {
                        if (i > 0) {
                            writer.append(",").ws();
                        }
                        writer.append(variableNames.get(i));
                    }
                    writer.append(";").softNewLine();
                }

                int firstToSave = 0;
                if (methodNode.getModifiers().contains(ElementModifier.STATIC)) {
                    firstToSave = 1;
                }

                String popName = minifying ? "l" : "pop";
                String pushName = minifying ? "s" : "push";
                writer.append(pointerName()).ws().append('=').ws().append("0;").softNewLine();
                writer.append("if").ws().append("(").appendFunction("$rt_resuming").append("())").ws()
                        .append("{").indent().softNewLine();
                writer.append("var ").append(threadName()).ws().append('=').ws()
                        .appendFunction("$rt_nativeThread").append("();").softNewLine();
                writer.append(pointerName()).ws().append('=').ws().append(threadName()).append(".")
                        .append(popName).append("();");
                for (int i = variableCount - 1; i >= firstToSave; --i) {
                    writer.append(variableName(i)).ws().append('=').ws().append(threadName()).append(".")
                            .append(popName).append("();");
                }
                writer.softNewLine();
                writer.outdent().append("}").softNewLine();

                if (methodNode.getModifiers().contains(ElementModifier.SYNCHRONIZED)) {
                    writer.append("try").ws().append('{').indent().softNewLine();
                }
                writer.append(mainLoopName()).append(":").ws().append("while").ws().append("(true)")
                        .ws().append("{").ws();
                writer.append("switch").ws().append("(").append(pointerName()).append(")").ws()
                        .append('{').softNewLine();
                for (int i = 0; i < methodNode.getBody().size(); ++i) {
                    writer.append("case ").append(i).append(":").indent().softNewLine();
                    if (i == 0 && methodNode.getModifiers().contains(ElementModifier.SYNCHRONIZED)) {
                        writer.appendMethodBody(new MethodReference(Object.class, "monitorEnter",
                                Object.class, void.class));
                        writer.append("(");
                        appendMonitor(methodNode);
                        writer.append(");").softNewLine();
                        emitSuspendChecker();
                    }
                    AsyncMethodPart part = methodNode.getBody().get(i);
                    end = true;
                    currentPart = i;
                    part.getStatement().acceptVisitor(Renderer.this);
                    writer.outdent();
                }
                writer.append("default:").ws().appendFunction("$rt_invalidPointer").append("();").softNewLine();
                writer.append("}}").softNewLine();

                if (methodNode.getModifiers().contains(ElementModifier.SYNCHRONIZED)) {
                    writer.outdent().append("}").ws().append("finally").ws().append('{').indent().softNewLine();
                    writer.append("if").ws().append("(!").appendFunction("$rt_suspending").append("())")
                            .ws().append("{").indent().softNewLine();
                    writer.appendMethodBody(new MethodReference(Object.class, "monitorExit",
                            Object.class, void.class));
                    writer.append("(");
                    appendMonitor(methodNode);
                    writer.append(");").softNewLine();
                    writer.outdent().append('}').softNewLine();
                    writer.outdent().append('}').softNewLine();
                }

                writer.appendFunction("$rt_nativeThread").append("().").append(pushName).append("(");
                for (int i = firstToSave; i < variableCount; ++i) {
                    writer.append(variableName(i)).append(',').ws();
                }
                writer.append(pointerName()).append(");");
                writer.softNewLine();
            } catch (IOException e) {
                throw new RenderingException("IO error occurred", e);
            }
        }

        @Override
        public String getParameterName(int index) {
            return variableName(index);
        }

        @Override
        public ListableClassReaderSource getClassSource() {
            return classSource;
        }

        @Override
        public ClassLoader getClassLoader() {
            return classLoader;
        }

        @Override
        public Properties getProperties() {
            return new Properties(properties);
        }

        @Override
        public <T> T getService(Class<T> type) {
            return services.getService(type);
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
        public boolean isAsyncFamily(MethodReference method) {
            return asyncFamilyMethods.contains(method);
        }

        @Override
        public Diagnostics getDiagnostics() {
            return diagnostics;
        }
    }

    private void appendMonitor(MethodNode methodNode) throws IOException {
        if (methodNode.getModifiers().contains(ElementModifier.STATIC)) {
            writer.appendFunction("$rt_cls").append("(")
                    .appendClass(methodNode.getReference().getClassName()).append(")");
        } else {
            writer.append(variableName(0));
        }
    }

    private void pushLocation(TextLocation location) {
        LocationStackEntry prevEntry = locationStack.peek();
        if (location != null) {
            if (prevEntry == null || !location.equals(prevEntry.location)) {
                debugEmitter.emitLocation(location.getFileName(), location.getLine());
            }
        } else {
            if (prevEntry != null) {
                debugEmitter.emitLocation(null, -1);
            }
        }
        locationStack.push(new LocationStackEntry(location));
    }

    private void popLocation() {
        LocationStackEntry prevEntry = locationStack.pop();
        LocationStackEntry entry = locationStack.peek();
        if (entry != null) {
            if (!entry.location.equals(prevEntry.location)) {
                debugEmitter.emitLocation(entry.location.getFileName(), entry.location.getLine());
            }
        } else {
            debugEmitter.emitLocation(null, -1);
        }
    }

    @Override
    public void visit(AssignmentStatement statement) throws RenderingException {
        try {
            debugEmitter.emitStatementStart();
            if (statement.getLocation() != null) {
                pushLocation(statement.getLocation());
            }
            prevCallSite = debugEmitter.emitCallSite();
            if (statement.getLeftValue() != null) {
                if (statement.isAsync()) {
                    writer.append(tempVarName());
                } else {
                    precedence = Precedence.COMMA;
                    statement.getLeftValue().acceptVisitor(this);
                }
                writer.ws().append("=").ws();
            }
            precedence = Precedence.COMMA;
            statement.getRightValue().acceptVisitor(this);
            debugEmitter.emitCallSite();
            writer.append(";").softNewLine();
            if (statement.isAsync()) {
                emitSuspendChecker();
                if (statement.getLeftValue() != null) {
                    precedence = Precedence.COMMA;
                    statement.getLeftValue().acceptVisitor(this);
                    writer.ws().append("=").ws().append(tempVarName()).append(";").softNewLine();
                }
            }
            if (statement.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(SequentialStatement statement) {
        visitStatements(statement.getSequence());
    }

    @Override
    public void visit(ConditionalStatement statement) {
        try {
            while (true) {
                debugEmitter.emitStatementStart();
                if (statement.getCondition().getLocation() != null) {
                    pushLocation(statement.getCondition().getLocation());
                }
                prevCallSite = debugEmitter.emitCallSite();
                writer.append("if").ws().append("(");
                precedence = Precedence.COMMA;
                statement.getCondition().acceptVisitor(this);
                if (statement.getCondition().getLocation() != null) {
                    popLocation();
                }
                debugEmitter.emitCallSite();
                writer.append(")").ws().append("{").softNewLine().indent();
                visitStatements(statement.getConsequent());
                if (!statement.getAlternative().isEmpty()) {
                    writer.outdent().append("}").ws();
                    if (statement.getAlternative().size() == 1
                            && statement.getAlternative().get(0) instanceof ConditionalStatement) {
                        statement = (ConditionalStatement) statement.getAlternative().get(0);
                        writer.append("else ");
                        continue;
                    }
                    writer.append("else").ws().append("{").indent().softNewLine();
                    visitStatements(statement.getAlternative());
                }
                break;
            }
            writer.outdent().append("}").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(SwitchStatement statement) {
        try {
            debugEmitter.emitStatementStart();
            if (statement.getValue().getLocation() != null) {
                pushLocation(statement.getValue().getLocation());
            }
            if (statement.getId() != null) {
                writer.append(mapBlockId(statement.getId())).append(":").ws();
            }
            prevCallSite = debugEmitter.emitCallSite();
            writer.append("switch").ws().append("(");
            precedence = Precedence.min();
            statement.getValue().acceptVisitor(this);
            if (statement.getValue().getLocation() != null) {
                popLocation();
            }
            debugEmitter.emitCallSite();
            writer.append(")").ws().append("{").softNewLine().indent();
            for (SwitchClause clause : statement.getClauses()) {
                for (int condition : clause.getConditions()) {
                    writer.append("case ").append(condition).append(":").softNewLine();
                }
                writer.indent();
                boolean oldEnd = end;
                for (Statement part : clause.getBody()) {
                    end = false;
                    part.acceptVisitor(this);
                }
                end = oldEnd;
                writer.outdent();
            }
            if (statement.getDefaultClause() != null) {
                writer.append("default:").softNewLine().indent();
                boolean oldEnd = end;
                for (Statement part : statement.getDefaultClause()) {
                    end = false;
                    part.acceptVisitor(this);
                }
                end = oldEnd;
                writer.outdent();
            }
            writer.outdent().append("}").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(WhileStatement statement) {
        try {
            debugEmitter.emitStatementStart();
            if (statement.getCondition() != null && statement.getCondition().getLocation() != null) {
                pushLocation(statement.getCondition().getLocation());
            }
            if (statement.getId() != null) {
                writer.append(mapBlockId(statement.getId())).append(":").ws();
            }
            writer.append("while").ws().append("(");
            if (statement.getCondition() != null) {
                prevCallSite = debugEmitter.emitCallSite();
                precedence = Precedence.min();
                statement.getCondition().acceptVisitor(this);
                debugEmitter.emitCallSite();
                if (statement.getCondition().getLocation() != null) {
                    popLocation();
                }
            } else {
                writer.append("true");
            }
            writer.append(")").ws().append("{").softNewLine().indent();
            boolean oldEnd = end;
            for (Statement part : statement.getBody()) {
                end = false;
                part.acceptVisitor(this);
            }
            end = oldEnd;
            writer.outdent().append("}").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    private String mapBlockId(String id) {
        String name = blockIdMap.get(id);
        if (name == null) {
            int index = blockIdMap.size();
            name = "$b" + indexToId(index);
            blockIdMap.put(id, name);
        }
        return name;
    }

    private String indexToId(int index) {
        StringBuilder sb = new StringBuilder();
        do {
            sb.append(variablePartNames.charAt(index % variablePartNames.length()));
            index /= variablePartNames.length();
        } while (index > 0);
        return sb.toString();
    }

    @Override
    public void visit(BlockStatement statement) {
        try {
            writer.append(mapBlockId(statement.getId())).append(":").ws().append("{").softNewLine().indent();
            visitStatements(statement.getBody());
            writer.outdent().append("}").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(BreakStatement statement) {
        try {
            debugEmitter.emitStatementStart();
            if (statement.getLocation() != null) {
                pushLocation(statement.getLocation());
            }
            writer.append("break");
            if (statement.getTarget() != null) {
                writer.append(' ').append(mapBlockId(statement.getTarget().getId()));
            }
            writer.append(";").softNewLine();
            if (statement.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(ContinueStatement statement) {
        try {
            debugEmitter.emitStatementStart();
            if (statement.getLocation() != null) {
                pushLocation(statement.getLocation());
            }
            writer.append("continue");
            if (statement.getTarget() != null) {
                writer.append(' ').append(mapBlockId(statement.getTarget().getId()));
            }
            writer.append(";").softNewLine();
            if (statement.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(ReturnStatement statement) {
        try {
            debugEmitter.emitStatementStart();
            if (statement.getLocation() != null) {
                pushLocation(statement.getLocation());
            }
            writer.append("return");
            if (statement.getResult() != null) {
                writer.append(' ');
                prevCallSite = debugEmitter.emitCallSite();
                precedence = Precedence.min();
                statement.getResult().acceptVisitor(this);
                debugEmitter.emitCallSite();
            }
            writer.append(";").softNewLine();
            if (statement.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(ThrowStatement statement) {
        try {
            debugEmitter.emitStatementStart();
            if (statement.getLocation() != null) {
                pushLocation(statement.getLocation());
            }
            writer.appendFunction("$rt_throw").append("(");
            prevCallSite = debugEmitter.emitCallSite();
            precedence = Precedence.min();
            statement.getException().acceptVisitor(this);
            writer.append(");").softNewLine();
            debugEmitter.emitCallSite();
            if (statement.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(InitClassStatement statement) {
        ClassReader cls = classSource.get(statement.getClassName());
        if (cls == null) {
            return;
        }
        MethodReader method = cls.getMethod(new MethodDescriptor("<clinit>", void.class));
        if (method == null) {
            return;
        }
        try {
            debugEmitter.emitStatementStart();
            if (statement.getLocation() != null) {
                pushLocation(statement.getLocation());
            }
            writer.appendClass(statement.getClassName()).append("_$callClinit();").softNewLine();
            if (statement.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    private String variableName(int index) {
        while (index >= cachedVariableNames.size()) {
            cachedVariableNames.add(null);
        }
        String name = cachedVariableNames.get(index);
        if (name == null) {
            name = generateVariableName(index);
            cachedVariableNames.set(index, name);
        }
        return name;
    }

    private String generateVariableName(int index) {
        if (index == 0) {
            return minifying ? "$t" : "$this";
        }

        if (!minifying) {
            VariableNode variable = index < currentMethod.getVariables().size()
                    ? currentMethod.getVariables().get(index)
                    : null;
            if (variable != null && variable.getName() != null) {
                String result = escapeName(variable.getName());
                if (keywords.contains(result) || !usedVariableNames.add(result)) {
                    String base = result;
                    int suffix = 0;
                    do {
                        result = base + "_" + suffix++;
                    } while (!usedVariableNames.add(result));
                }
                return result;
            } else {
                return "var$" + index;
            }
        } else {
            --index;
            if (index < variableNames.length()) {
                return Character.toString(variableNames.charAt(index));
            } else {
                return Character.toString(variableNames.charAt(index % variableNames.length()))
                        + index / variableNames.length();
            }
        }
    }

    private static String escapeName(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); ++i) {
            char c = name.charAt(i);
            sb.append(Character.isJavaIdentifierPart(c) ? c : '_');
        }
        return sb.toString();
    }

    private String pointerName() {
        return minifying ? "$p" : "$ptr";
    }

    private String mainLoopName() {
        return minifying ? "$m" : "$main";
    }

    private String tempVarName() {
        return minifying ? "$z" : "$tmp";
    }

    private String threadName() {
        return minifying ? "$T" : "$thread";
    }

    private void visitBinary(BinaryExpr expr, String op, boolean guarded) {
        if (expr.getLocation() != null) {
            pushLocation(expr.getLocation());
        }
        if (guarded) {
            visitBinary(BinaryOperation.OR, "|", () -> visitBinary(expr, op, false),
                    () -> {
                        try {
                            writer.append("0");
                        } catch (IOException e) {
                            throw new RenderingException("IO error occurred", e);
                        }
                    });
        } else {
            visitBinary(expr.getOperation(), op, () -> expr.getFirstOperand().acceptVisitor(this),
                    () -> expr.getSecondOperand().acceptVisitor(this));
        }
        if (expr.getLocation() != null) {
            popLocation();
        }
    }

    private void visitBinary(BinaryOperation operation, String infixText, Runnable a, Runnable b) {
        try {
            Precedence outerPrecedence = precedence;
            Precedence innerPrecedence = getPrecedence(operation);
            if (innerPrecedence.ordinal() < outerPrecedence.ordinal()) {
                writer.append('(');
            }

            switch (operation) {
                case ADD:
                case SUBTRACT:
                case MULTIPLY:
                case DIVIDE:
                case MODULO:
                case AND:
                case OR:
                case BITWISE_AND:
                case BITWISE_OR:
                case BITWISE_XOR:
                case LEFT_SHIFT:
                case RIGHT_SHIFT:
                case UNSIGNED_RIGHT_SHIFT:
                    precedence = innerPrecedence;
                    break;
                default:
                    precedence = innerPrecedence.next();
            }
            a.run();

            writer.ws().append(infixText).ws();

            switch (operation) {
                case ADD:
                case MULTIPLY:
                case AND:
                case OR:
                case BITWISE_AND:
                case BITWISE_OR:
                case BITWISE_XOR:
                    precedence = innerPrecedence;
                    break;
                default:
                    precedence = innerPrecedence.next();
                    break;
            }
            b.run();

            if (innerPrecedence.ordinal() < outerPrecedence.ordinal()) {
                writer.append(')');
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    private static Precedence getPrecedence(BinaryOperation op) {
        switch (op) {
            case ADD:
            case SUBTRACT:
                return Precedence.ADDITION;
            case MULTIPLY:
            case DIVIDE:
            case MODULO:
                return Precedence.MULTIPLICATION;
            case AND:
                return Precedence.LOGICAL_AND;
            case OR:
                return Precedence.LOGICAL_OR;
            case EQUALS:
            case NOT_EQUALS:
                return Precedence.EQUALITY;
            case GREATER:
            case GREATER_OR_EQUALS:
            case LESS:
            case LESS_OR_EQUALS:
                return Precedence.COMPARISON;
            case BITWISE_AND:
                return Precedence.BITWISE_AND;
            case BITWISE_OR:
                return Precedence.BITWISE_OR;
            case BITWISE_XOR:
                return Precedence.BITWISE_XOR;
            case LEFT_SHIFT:
            case RIGHT_SHIFT:
            case UNSIGNED_RIGHT_SHIFT:
                return Precedence.BITWISE_SHIFT;
            default:
                return Precedence.GROUPING;
        }
    }

    private void visitBinaryFunction(BinaryExpr expr, String function) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            writer.append(function);
            writer.append('(');
            precedence = Precedence.min();
            expr.getFirstOperand().acceptVisitor(this);
            writer.append(",").ws();
            precedence = Precedence.min();
            expr.getSecondOperand().acceptVisitor(this);
            writer.append(')');
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(BinaryExpr expr) {
        if (expr.getType() == OperationType.LONG) {
            switch (expr.getOperation()) {
                case ADD:
                    visitBinaryFunction(expr, "Long_add");
                    break;
                case SUBTRACT:
                    visitBinaryFunction(expr, "Long_sub");
                    break;
                case MULTIPLY:
                    visitBinaryFunction(expr, "Long_mul");
                    break;
                case DIVIDE:
                    visitBinaryFunction(expr, "Long_div");
                    break;
                case MODULO:
                    visitBinaryFunction(expr, "Long_rem");
                    break;
                case BITWISE_OR:
                    visitBinaryFunction(expr, "Long_or");
                    break;
                case BITWISE_AND:
                    visitBinaryFunction(expr, "Long_and");
                    break;
                case BITWISE_XOR:
                    visitBinaryFunction(expr, "Long_xor");
                    break;
                case LEFT_SHIFT:
                    visitBinaryFunction(expr, "Long_shl");
                    break;
                case RIGHT_SHIFT:
                    visitBinaryFunction(expr, "Long_shr");
                    break;
                case UNSIGNED_RIGHT_SHIFT:
                    visitBinaryFunction(expr, "Long_shru");
                    break;
                case COMPARE:
                    visitBinaryFunction(expr, "Long_compare");
                    break;
                case EQUALS:
                    visitBinaryFunction(expr, "Long_eq");
                    break;
                case NOT_EQUALS:
                    visitBinaryFunction(expr, "Long_ne");
                    break;
                case LESS:
                    visitBinaryFunction(expr, "Long_lt");
                    break;
                case LESS_OR_EQUALS:
                    visitBinaryFunction(expr, "Long_le");
                    break;
                case GREATER:
                    visitBinaryFunction(expr, "Long_gt");
                    break;
                case GREATER_OR_EQUALS:
                    visitBinaryFunction(expr, "Long_ge");
                    break;
                default:
                    break;
            }
        } else {
            switch (expr.getOperation()) {
                case ADD:
                    visitBinary(expr, "+", expr.getType() == OperationType.INT);
                    break;
                case SUBTRACT:
                    visitBinary(expr, "-", expr.getType() == OperationType.INT);
                    break;
                case MULTIPLY:
                    visitBinary(expr, "*", expr.getType() == OperationType.INT);
                    break;
                case DIVIDE:
                    visitBinary(expr, "/", expr.getType() == OperationType.INT);
                    break;
                case MODULO:
                    visitBinary(expr, "%", expr.getType() == OperationType.INT);
                    break;
                case EQUALS:
                    if (expr.getType() == OperationType.INT) {
                        visitBinary(expr, "==", false);
                    } else {
                        visitBinary(expr, "===", false);
                    }
                    break;
                case NOT_EQUALS:
                    if (expr.getType() == OperationType.INT) {
                        visitBinary(expr, "!=", false);
                    } else {
                        visitBinary(expr, "!==", false);
                    }
                    break;
                case GREATER:
                    visitBinary(expr, ">", false);
                    break;
                case GREATER_OR_EQUALS:
                    visitBinary(expr, ">=", false);
                    break;
                case LESS:
                    visitBinary(expr, "<", false);
                    break;
                case LESS_OR_EQUALS:
                    visitBinary(expr, "<=", false);
                    break;
                case COMPARE:
                    visitBinaryFunction(expr, naming.getNameForFunction("$rt_compare"));
                    break;
                case OR:
                    visitBinary(expr, "||", false);
                    break;
                case AND:
                    visitBinary(expr, "&&", false);
                    break;
                case BITWISE_OR:
                    visitBinary(expr, "|", false);
                    break;
                case BITWISE_AND:
                    visitBinary(expr, "&", false);
                    break;
                case BITWISE_XOR:
                    visitBinary(expr, "^", false);
                    break;
                case LEFT_SHIFT:
                    visitBinary(expr, "<<", false);
                    break;
                case RIGHT_SHIFT:
                    visitBinary(expr, ">>", false);
                    break;
                case UNSIGNED_RIGHT_SHIFT:
                    visitBinary(expr, ">>>", false);
                    break;
            }
        }
    }

    @Override
    public void visit(UnaryExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            Precedence outerPrecedence = precedence;
            switch (expr.getOperation()) {
                case NOT: {
                    if (expr.getType() == OperationType.LONG) {
                        writer.append("Long_not(");
                        precedence = Precedence.min();
                        expr.getOperand().acceptVisitor(this);
                        writer.append(')');
                    } else {
                        if (outerPrecedence.ordinal() > Precedence.UNARY.ordinal()) {
                            writer.append('(');
                        }
                        writer.append(expr.getType() == null ? "!" : "~");
                        precedence = Precedence.UNARY;
                        expr.getOperand().acceptVisitor(this);
                        if (outerPrecedence.ordinal() > Precedence.UNARY.ordinal()) {
                            writer.append(')');
                        }
                    }
                    break;
                }
                case NEGATE:
                    if (expr.getType() == OperationType.LONG) {
                        writer.append("Long_neg(");
                        precedence = Precedence.min();
                        expr.getOperand().acceptVisitor(this);
                        writer.append(')');
                    } else {
                        if (outerPrecedence.ordinal() > Precedence.UNARY.ordinal()) {
                            writer.append('(');
                        }
                        writer.append(" -");
                        precedence = Precedence.UNARY;
                        expr.getOperand().acceptVisitor(this);
                        if (outerPrecedence.ordinal() > Precedence.UNARY.ordinal()) {
                            writer.append(')');
                        }
                    }
                    break;
                case LENGTH:
                    precedence = Precedence.MEMBER_ACCESS;
                    expr.getOperand().acceptVisitor(this);
                    writer.append(".length");
                    break;
                case INT_TO_BYTE:
                    if (outerPrecedence.ordinal() > Precedence.BITWISE_SHIFT.ordinal()) {
                        writer.append('(');
                    }
                    precedence = Precedence.BITWISE_SHIFT;
                    expr.getOperand().acceptVisitor(this);
                    writer.ws().append("<<").ws().append("24").ws().append(">>").ws().append("24");
                    if (outerPrecedence.ordinal() > Precedence.BITWISE_SHIFT.ordinal()) {
                        writer.append(')');
                    }
                    break;
                case INT_TO_SHORT:
                    if (outerPrecedence.ordinal() > Precedence.BITWISE_SHIFT.ordinal()) {
                        writer.append('(');
                    }
                    precedence = Precedence.BITWISE_SHIFT;
                    expr.getOperand().acceptVisitor(this);
                    writer.ws().append("<<").ws().append("16").ws().append(">>").ws().append("16");
                    if (outerPrecedence.ordinal() > Precedence.BITWISE_SHIFT.ordinal()) {
                        writer.append(')');
                    }
                    break;
                case INT_TO_CHAR:
                    if (outerPrecedence.ordinal() > Precedence.BITWISE_AND.ordinal()) {
                        writer.append('(');
                    }
                    precedence = Precedence.BITWISE_AND;
                    expr.getOperand().acceptVisitor(this);
                    writer.ws().append("&").ws().append("65535");
                    if (outerPrecedence.ordinal() > Precedence.BITWISE_AND.ordinal()) {
                        writer.append(')');
                    }
                    break;
                case NULL_CHECK:
                    writer.appendFunction("$rt_nullCheck").append("(");
                    precedence = Precedence.min();
                    expr.getOperand().acceptVisitor(this);
                    writer.append(')');
                    break;
            }
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(CastExpr expr) {
        expr.getValue().acceptVisitor(this);
    }

    @Override
    public void visit(PrimitiveCastExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            switch (expr.getSource()) {
                case INT:
                    if (expr.getTarget() == OperationType.LONG) {
                        writer.append("Long_fromInt(");
                        precedence = Precedence.min();
                        expr.getValue().acceptVisitor(this);
                        writer.append(')');
                    } else {
                        expr.getValue().acceptVisitor(this);
                    }
                    break;
                case LONG:
                    switch (expr.getTarget()) {
                        case INT:
                            precedence = Precedence.MEMBER_ACCESS;
                            expr.getValue().acceptVisitor(this);
                            writer.append(".lo");
                            break;
                        case FLOAT:
                        case DOUBLE:
                            writer.append("Long_toNumber(");
                            precedence = Precedence.min();
                            expr.getValue().acceptVisitor(this);
                            writer.append(')');
                            break;
                        default:
                            expr.getValue().acceptVisitor(this);
                    }
                    break;
                case FLOAT:
                case DOUBLE:
                    switch (expr.getTarget()) {
                        case LONG:
                            writer.append("Long_fromNumber(");
                            precedence = Precedence.min();
                            expr.getValue().acceptVisitor(this);
                            writer.append(')');
                            break;
                        case INT:
                            visitBinary(BinaryOperation.BITWISE_OR, "|", () -> expr.getValue().acceptVisitor(this),
                                    () -> {
                                        try {
                                            writer.append("0");
                                        } catch (IOException e) {
                                            throw new RenderingException("IO error occurred", e);
                                        }
                                    });
                            break;
                        default:
                            expr.getValue().acceptVisitor(this);
                    }
                    break;
            }
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(ConditionalExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }

            Precedence outerPrecedence = precedence;
            if (outerPrecedence.ordinal() > Precedence.CONDITIONAL.ordinal()) {
                writer.append('(');
            }

            precedence = Precedence.CONDITIONAL.next();
            expr.getCondition().acceptVisitor(this);
            writer.ws().append("?").ws();
            precedence = Precedence.CONDITIONAL.next();
            expr.getConsequent().acceptVisitor(this);
            writer.ws().append(":").ws();
            precedence = Precedence.CONDITIONAL;
            expr.getAlternative().acceptVisitor(this);

            if (outerPrecedence.ordinal() > Precedence.CONDITIONAL.ordinal()) {
                writer.append('(');
            }

            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(ConstantExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            String str = constantToString(expr.getValue());
            if (str.startsWith("-")) {
                writer.append(' ');
            }
            writer.append(str);
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    private String constantToString(Object cst) {
        if (cst == null) {
            return "null";
        }
        if (cst instanceof ValueType) {
            ValueType type = (ValueType) cst;
            return naming.getNameForFunction("$rt_cls") + "(" + typeToClsString(naming, type) + ")";
        } else if (cst instanceof String) {
            String string = (String) cst;
            Integer index = stringPoolMap.get(string);
            if (index == null) {
                index = stringPool.size();
                stringPool.add(string);
                stringPoolMap.put(string, index);
            }
            return "$rt_s(" + index + ")";
        } else if (cst instanceof Long) {
            long value = (Long) cst;
            if (value == 0) {
                return "Long_ZERO";
            } else if ((int) value == value) {
                return "Long_fromInt(" + value + ")";
            } else {
                return "new Long(" + (value & 0xFFFFFFFFL) + ", " + (value >>> 32) + ")";
            }
        } else if (cst instanceof Character) {
            return Integer.toString((Character) cst);
        } else {
            return cst.toString();
        }
    }

    public static String typeToClsString(NamingStrategy naming, ValueType type) {
        int arrayCount = 0;
        while (type instanceof ValueType.Array) {
            arrayCount++;
            type = ((ValueType.Array) type).getItemType();
        }
        String value;
        if (type instanceof ValueType.Object) {
            ValueType.Object objType = (ValueType.Object) type;
            value = naming.getNameFor(objType.getClassName());
        } else if (type instanceof ValueType.Void) {
            value = "$rt_voidcls()";
        } else if (type instanceof ValueType.Primitive) {
            ValueType.Primitive primitiveType = (ValueType.Primitive) type;
            switch (primitiveType.getKind()) {
                case BOOLEAN:
                    value = "$rt_booleancls()";
                    break;
                case CHARACTER:
                    value = "$rt_charcls()";
                    break;
                case BYTE:
                    value = "$rt_bytecls()";
                    break;
                case SHORT:
                    value = "$rt_shortcls()";
                    break;
                case INTEGER:
                    value = "$rt_intcls()";
                    break;
                case LONG:
                    value = "$rt_longcls()";
                    break;
                case FLOAT:
                    value = "$rt_floatcls()";
                    break;
                case DOUBLE:
                    value = "$rt_doublecls()";
                    break;
                default:
                    throw new IllegalArgumentException("The type is not renderable");
            }
        } else {
            throw new IllegalArgumentException("The type is not renderable");
        }

        for (int i = 0; i < arrayCount; ++i) {
            value = "$rt_arraycls(" + value + ")";
        }
        return value;
    }

    public static String escapeString(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            switch (c) {
                case '\r':
                    sb.append("\\r");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\'':
                    sb.append("\\'");
                    break;
                case '\"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    if (c < ' ') {
                        sb.append("\\u00").append(Character.forDigit(c / 16, 16))
                                .append(Character.forDigit(c % 16, 16));
                    } else if (Character.isLowSurrogate(c) || Character.isHighSurrogate(c)) {
                        sb.append("\\u")
                                .append(Character.forDigit(c / 0x1000, 0x10))
                                .append(Character.forDigit((c / 0x100) % 0x10, 0x10))
                                .append(Character.forDigit((c / 0x10) % 0x10, 0x10))
                                .append(Character.forDigit(c % 0x10, 0x10));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    @Override
    public void visit(VariableExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            writer.append(variableName(expr.getIndex()));
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(SubscriptExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            precedence = Precedence.MEMBER_ACCESS;
            expr.getArray().acceptVisitor(this);
            writer.append('[');
            precedence = Precedence.min();
            expr.getIndex().acceptVisitor(this);
            writer.append(']');
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(UnwrapArrayExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            precedence = Precedence.MEMBER_ACCESS;
            expr.getArray().acceptVisitor(this);
            writer.append(".data");
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(InvocationExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            Injector injector = getInjector(expr.getMethod());
            if (injector != null) {
                injector.generate(new InjectorContextImpl(expr.getArguments()), expr.getMethod());
            } else {
                if (expr.getType() == InvocationType.DYNAMIC) {
                    precedence = Precedence.MEMBER_ACCESS;
                    expr.getArguments().get(0).acceptVisitor(this);
                }
                MethodReference method = expr.getMethod();
                String name = naming.getNameFor(method.getDescriptor());
                DeferredCallSite callSite = prevCallSite;
                boolean shouldEraseCallSite = lastCallSite == null;
                if (lastCallSite == null) {
                    lastCallSite = callSite;
                }
                boolean virtual = false;
                switch (expr.getType()) {
                    case STATIC:
                        writer.append(naming.getFullNameFor(method)).append("(");
                        prevCallSite = debugEmitter.emitCallSite();
                        for (int i = 0; i < expr.getArguments().size(); ++i) {
                            if (i > 0) {
                                writer.append(",").ws();
                            }
                            precedence = Precedence.min();
                            expr.getArguments().get(i).acceptVisitor(this);
                        }
                        break;
                    case SPECIAL:
                        writer.append(naming.getFullNameFor(method)).append("(");
                        prevCallSite = debugEmitter.emitCallSite();
                        precedence = Precedence.min();
                        expr.getArguments().get(0).acceptVisitor(this);
                        for (int i = 1; i < expr.getArguments().size(); ++i) {
                            writer.append(",").ws();
                            precedence = Precedence.min();
                            expr.getArguments().get(i).acceptVisitor(this);
                        }
                        break;
                    case DYNAMIC:
                        writer.append(".").append(name).append("(");
                        prevCallSite = debugEmitter.emitCallSite();
                        for (int i = 1; i < expr.getArguments().size(); ++i) {
                            if (i > 1) {
                                writer.append(",").ws();
                            }
                            precedence = Precedence.min();
                            expr.getArguments().get(i).acceptVisitor(this);
                        }
                        virtual = true;
                        break;
                    case CONSTRUCTOR:
                        writer.append(naming.getNameForInit(expr.getMethod())).append("(");
                        prevCallSite = debugEmitter.emitCallSite();
                        for (int i = 0; i < expr.getArguments().size(); ++i) {
                            if (i > 0) {
                                writer.append(",").ws();
                            }
                            precedence = Precedence.min();
                            expr.getArguments().get(i).acceptVisitor(this);
                        }
                        break;
                }
                writer.append(')');
                if (lastCallSite != null) {
                    if (virtual) {
                        lastCallSite.setVirtualMethod(expr.getMethod());
                    } else {
                        lastCallSite.setStaticMethod(expr.getMethod());
                    }
                    lastCallSite = callSite;
                }
                if (shouldEraseCallSite) {
                    lastCallSite = null;
                }
            }
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(QualificationExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            precedence = Precedence.MEMBER_ACCESS;

            if (expr.getQualified() != null) {
                expr.getQualified().acceptVisitor(this);
                writer.append('.').appendField(expr.getField());
            } else {
                writer.appendStaticField(expr.getField());
            }

            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(NewExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            precedence = Precedence.FUNCTION_CALL;
            writer.append("new ").append(naming.getNameFor(expr.getConstructedClass()));
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(NewArrayExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            ValueType type = expr.getType();
            if (type instanceof ValueType.Primitive) {
                switch (((ValueType.Primitive) type).getKind()) {
                    case BOOLEAN:
                        writer.append("$rt_createBooleanArray(");
                        precedence = Precedence.min();
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case BYTE:
                        writer.append("$rt_createByteArray(");
                        precedence = Precedence.min();
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case SHORT:
                        writer.append("$rt_createShortArray(");
                        precedence = Precedence.min();
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case INTEGER:
                        writer.append("$rt_createIntArray(");
                        precedence = Precedence.min();
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case LONG:
                        writer.append("$rt_createLongArray(");
                        precedence = Precedence.min();
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case FLOAT:
                        writer.append("$rt_createFloatArray(");
                        precedence = Precedence.min();
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case DOUBLE:
                        writer.append("$rt_createDoubleArray(");
                        precedence = Precedence.min();
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case CHARACTER:
                        writer.append("$rt_createCharArray(");
                        precedence = Precedence.min();
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                }
            } else {
                writer.appendFunction("$rt_createArray").append("(").append(typeToClsString(naming, expr.getType()))
                        .append(",").ws();
                precedence = Precedence.min();
                expr.getLength().acceptVisitor(this);
                writer.append(")");
            }
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(NewMultiArrayExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            ValueType type = expr.getType();
            for (int i = 0; i < expr.getDimensions().size(); ++i) {
                type = ((ValueType.Array) type).getItemType();
            }
            if (type instanceof ValueType.Primitive) {
                switch (((ValueType.Primitive) type).getKind()) {
                    case BOOLEAN:
                        writer.append("$rt_createBooleanMultiArray(");
                        break;
                    case BYTE:
                        writer.append("$rt_createByteMultiArray(");
                        break;
                    case SHORT:
                        writer.append("$rt_createShortMultiArray(");
                        break;
                    case INTEGER:
                        writer.append("$rt_createIntMultiArray(");
                        break;
                    case LONG:
                        writer.append("$rt_createLongMultiArray(");
                        break;
                    case FLOAT:
                        writer.append("$rt_createFloatMultiArray(");
                        break;
                    case DOUBLE:
                        writer.append("$rt_createDoubleMultiArray(");
                        break;
                    case CHARACTER:
                        writer.append("$rt_createCharMultiArray(");
                        break;
                }
            } else {
                writer.append("$rt_createMultiArray(").append(typeToClsString(naming, expr.getType()))
                        .append(",").ws();
            }
            writer.append("[");
            boolean first = true;
            List<Expr> dimensions = new ArrayList<>(expr.getDimensions());
            Collections.reverse(dimensions);
            for (Expr dimension : dimensions) {
                if (!first) {
                    writer.append(",").ws();
                }
                first = false;
                precedence = Precedence.min();
                dimension.acceptVisitor(this);
            }
            writer.append("])");
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(InstanceOfExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            if (expr.getType() instanceof ValueType.Object) {
                String clsName = ((ValueType.Object) expr.getType()).getClassName();
                ClassHolder cls = classSource.get(clsName);
                if (cls != null && !cls.getModifiers().contains(ElementModifier.INTERFACE)) {
                    boolean needsParentheses = Precedence.COMPARISON.ordinal() < precedence.ordinal();
                    if (needsParentheses) {
                        writer.append('(');
                    }
                    precedence = Precedence.CONDITIONAL.next();
                    expr.getExpr().acceptVisitor(this);
                    writer.append(" instanceof ").appendClass(clsName);
                    if (expr.getLocation() != null) {
                        popLocation();
                    }
                    return;
                }
            }
            writer.appendFunction("$rt_isInstance").append("(");
            precedence = Precedence.min();
            expr.getExpr().acceptVisitor(this);
            writer.append(",").ws().append(typeToClsString(naming, expr.getType())).append(")");
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    private void visitStatements(List<Statement> statements) {
        if (statements.isEmpty()) {
            return;
        }
        boolean oldEnd = end;
        for (int i = 0; i < statements.size() - 1; ++i) {
            end = false;
            statements.get(i).acceptVisitor(this);
        }
        end = oldEnd;
        statements.get(statements.size() - 1).acceptVisitor(this);
        end = oldEnd;
    }

    @Override
    public void visit(TryCatchStatement statement) {
        try {
            writer.append("try").ws().append("{").softNewLine().indent();
            List<TryCatchStatement> sequence = new ArrayList<>();
            sequence.add(statement);
            List<Statement> protectedBody = statement.getProtectedBody();
            while (protectedBody.size() == 1 && protectedBody.get(0) instanceof TryCatchStatement) {
                TryCatchStatement nextStatement = (TryCatchStatement) protectedBody.get(0);
                sequence.add(nextStatement);
                protectedBody = nextStatement.getProtectedBody();
            }
            visitStatements(protectedBody);
            writer.outdent().append("}").ws().append("catch").ws().append("($e)")
                    .ws().append("{").indent().softNewLine();
            writer.append("$je").ws().append("=").ws().append("$e.$javaException;").softNewLine();
            for (TryCatchStatement catchClause : sequence) {
                writer.append("if").ws().append("($je");
                if (catchClause.getExceptionType() != null) {
                    writer.ws().append("&&").ws().append("$je instanceof ")
                            .appendClass(catchClause.getExceptionType());
                }
                writer.append(")").ws().append("{").indent().softNewLine();
                if (catchClause.getExceptionVariable() != null) {
                    writer.append(variableName(catchClause.getExceptionVariable())).ws().append("=").ws()
                            .append("$je;").softNewLine();
                }
                visitStatements(catchClause.getHandler());
                writer.outdent().append("}").ws().append("else ");
            }
            writer.append("{").indent().softNewLine();
            writer.append("throw $e;").softNewLine();
            writer.outdent().append("}").softNewLine();
            writer.outdent().append("}").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(GotoPartStatement statement) {
        try {
            if (statement.getPart() != currentPart) {
                writer.append(pointerName()).ws().append("=").ws().append(statement.getPart()).append(";")
                        .softNewLine();
            }
            if (!end || statement.getPart() != currentPart + 1) {
                writer.append("continue ").append(mainLoopName()).append(";").softNewLine();
            }
        } catch (IOException ex) {
            throw new RenderingException("IO error occurred", ex);
        }
    }

    @Override
    public void visit(MonitorEnterStatement statement) {
        try {
            if (async) {
                MethodReference monitorEnterRef = new MethodReference(
                        Object.class, "monitorEnter", Object.class, void.class);
                writer.appendMethodBody(monitorEnterRef).append("(");
                precedence = Precedence.min();
                statement.getObjectRef().acceptVisitor(this);
                writer.append(");").softNewLine();
                emitSuspendChecker();
            } else {
                MethodReference monitorEnterRef = new MethodReference(
                        Object.class, "monitorEnterSync", Object.class, void.class);
                writer.appendMethodBody(monitorEnterRef).append('(');
                precedence = Precedence.min();
                statement.getObjectRef().acceptVisitor(this);
                writer.append(");").softNewLine();
            }
        } catch (IOException ex) {
            throw new RenderingException("IO error occurred", ex);
        }
    }

    private void emitSuspendChecker() throws IOException {
        writer.append("if").ws().append("(").appendFunction("$rt_suspending").append("())").ws()
                .append("{").indent().softNewLine();
        writer.append("break ").append(mainLoopName()).append(";").softNewLine();
        writer.outdent().append("}").softNewLine();
    }

    @Override
    public void visit(MonitorExitStatement statement) {
        try {
            if (async) {
                MethodReference monitorExitRef = new MethodReference(
                        Object.class, "monitorExit", Object.class, void.class);
                writer.appendMethodBody(monitorExitRef).append("(");
                precedence = Precedence.min();
                statement.getObjectRef().acceptVisitor(this);
                writer.append(");").softNewLine();
            } else {
                MethodReference monitorEnterRef = new MethodReference(
                        Object.class, "monitorExitSync", Object.class, void.class);
                writer.appendMethodBody(monitorEnterRef).append('(');
                precedence = Precedence.min();
                statement.getObjectRef().acceptVisitor(this);
                writer.append(");").softNewLine();
            }
        } catch (IOException ex) {
            throw new RenderingException("IO error occurred", ex);
        }
    }


    private Injector getInjector(MethodReference ref) {
        InjectorHolder holder = injectorMap.get(ref);
        if (holder == null) {
            holder = new InjectorHolder(null);
            ClassHolder cls = classSource.get(ref.getClassName());
            if (cls != null) {
                MethodHolder method = cls.getMethod(ref.getDescriptor());
                if (method != null) {
                    AnnotationHolder injectedByAnnot = method.getAnnotations().get(InjectedBy.class.getName());
                    if (injectedByAnnot != null) {
                        ValueType type = injectedByAnnot.getValues().get("value").getJavaClass();
                        holder = new InjectorHolder(instantiateInjector(((ValueType.Object) type).getClassName()));
                    }
                }
            }
            injectorMap.put(ref, holder);
        }
        return holder.injector;
    }

    private Injector instantiateInjector(String type) {
        try {
            Class<? extends Injector> cls = Class.forName(type, true, classLoader).asSubclass(Injector.class);
            Constructor<? extends Injector> cons = cls.getConstructor();
            return cons.newInstance();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Illegal injector: " + type, e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Default constructor was not found in the " + type + " injector", e);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException("Error instantiating injector " + type, e);
        }
    }

    private class InjectorContextImpl implements InjectorContext {
        private final List<Expr> arguments;
        private final Precedence precedence = Renderer.this.precedence;

        InjectorContextImpl(List<Expr> arguments) {
            this.arguments = arguments;
        }

        @Override
        public Expr getArgument(int index) {
            return arguments.get(index);
        }

        @Override
        public boolean isMinifying() {
            return minifying;
        }

        @Override
        public SourceWriter getWriter() {
            return writer;
        }

        @Override
        public void writeEscaped(String str) throws IOException {
            writer.append(escapeString(str));
        }

        @Override
        public void writeType(ValueType type) throws IOException {
            writer.append(typeToClsString(naming, type));
        }

        @Override
        public void writeExpr(Expr expr) throws IOException {
            writeExpr(expr, Precedence.GROUPING);
        }

        @Override
        public void writeExpr(Expr expr, Precedence precedence) throws IOException {
            Renderer.this.precedence = precedence;
            expr.acceptVisitor(Renderer.this);
        }

        @Override
        public int argumentCount() {
            return arguments.size();
        }

        @Override
        public <T> T getService(Class<T> type) {
            return services.getService(type);
        }

        @Override
        public Properties getProperties() {
            return new Properties(properties);
        }

        @Override
        public Precedence getPrecedence() {
            return precedence;
        }

        @Override
        public ClassLoader getClassLoader() {
            return classLoader;
        }

        @Override
        public ListableClassReaderSource getClassSource() {
            return classSource;
        }
    }

    @Override
    public <T> T getService(Class<T> type) {
        return services.getService(type);
    }

    private static class PostponedFieldInitializer {
        FieldReference field;
        String value;

        public PostponedFieldInitializer(FieldReference field, String value) {
            this.field = field;
            this.value = value;
        }
    }
}
