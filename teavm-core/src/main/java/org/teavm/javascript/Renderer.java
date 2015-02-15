/*
 *  Copyright 2012 Alexey Andreev.
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
package org.teavm.javascript;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.teavm.codegen.NamingException;
import org.teavm.codegen.NamingStrategy;
import org.teavm.codegen.SourceWriter;
import org.teavm.common.ServiceRepository;
import org.teavm.debugging.information.DebugInformationEmitter;
import org.teavm.debugging.information.DeferredCallSite;
import org.teavm.debugging.information.DummyDebugInformationEmitter;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.javascript.ast.*;
import org.teavm.javascript.spi.GeneratorContext;
import org.teavm.javascript.spi.InjectedBy;
import org.teavm.javascript.spi.Injector;
import org.teavm.javascript.spi.InjectorContext;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
public class Renderer implements ExprVisitor, StatementVisitor, RenderingContext {
    private static final String variableNames = "abcdefghijkmnopqrstuvwxyz";
    private static final String variablePartNames = "abcdefghijkmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private NamingStrategy naming;
    private SourceWriter writer;
    private ListableClassHolderSource classSource;
    private ClassLoader classLoader;
    private boolean minifying;
    private Map<MethodReference, InjectorHolder> injectorMap = new HashMap<>();
    private Map<String, Integer> stringPoolMap = new HashMap<>();
    private List<String> stringPool = new ArrayList<>();
    private Properties properties = new Properties();
    private ServiceRepository services;
    private DebugInformationEmitter debugEmitter = new DummyDebugInformationEmitter();
    private Deque<LocationStackEntry> locationStack = new ArrayDeque<>();
    private DeferredCallSite lastCallSite;
    private DeferredCallSite prevCallSite;
    private Set<MethodReference> asyncMethods;
    private Set<MethodReference> asyncFamilyMethods;
    private Diagnostics diagnostics;
    private boolean async;
    private Priority priority;
    private Associativity associativity;
    private boolean wasGrouped;
    private Deque<OperatorPrecedence> precedenceStack = new ArrayDeque<>();
    private Map<String, String> blockIdMap = new HashMap<>();

    private static class OperatorPrecedence {
        Priority priority;
        Associativity associativity;
        boolean wasGrouped;
    }

    @Override
    public void visit(MonitorEnterStatement statement) {
        try {
            MethodReference monitorEnterRef = new MethodReference(
                    Object.class, "monitorEnter", Object.class, void.class);
            writer.appendMethodBody(monitorEnterRef).append("(");
            statement.getObjectRef().acceptVisitor(this);
            writer.append(",").ws();
            writer.append("$rt_continue($part_").append(statement.getAsyncTarget()).append(')');
            writer.append(");").softNewLine();
        } catch (IOException ex){
            throw new RenderingException("IO error occured", ex);
        }
    }

    @Override
    public void visit(MonitorExitStatement statement) {
        try {
            MethodReference monitorExitRef = new MethodReference(
                    Object.class, "monitorExit", Object.class, void.class);
            writer.appendMethodBody(monitorExitRef).append("(");
            statement.getObjectRef().acceptVisitor(this);
            writer.append(");").softNewLine();
        } catch (IOException ex){
            throw new RenderingException("IO error occured", ex);
        }
    }

    private static class InjectorHolder {
        public final Injector injector;

        public InjectorHolder(Injector injector) {
            this.injector = injector;
        }
    }

    private static class LocationStackEntry {
        NodeLocation location;

        public LocationStackEntry(NodeLocation location) {
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
        return new Properties(properties);
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

    public void render(List<ClassNode> classes) throws RenderingException {
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
                if (field.getModifiers().contains(NodeModifier.STATIC)) {
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
            writer.outdent().append("}").newLine();

            for (FieldNode field : staticFields) {
                Object value = field.getInitialValue();
                if (value == null) {
                    value = getDefaultValue(field.getType());
                }
                FieldReference fieldRef = new FieldReference(cls.getName(), field.getName());
                writer.appendClass(cls.getName()).append('.').appendField(fieldRef).ws().append("=").ws()
                        .append(constantToString(value)).append(";").softNewLine();
            }
        } catch (NamingException e) {
            throw new RenderingException("Error rendering class " + cls.getName() + ". See a cause for details", e);
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    private void renderMethodBodies(ClassNode cls) throws RenderingException {
        debugEmitter.emitClass(cls.getName());
        try {
            List<MethodNode> nonInitMethods = new ArrayList<>();
            List<MethodNode> virtualMethods = new ArrayList<>();
            MethodHolder clinit = classSource.get(cls.getName()).getMethod(
                    new MethodDescriptor("<clinit>", ValueType.VOID));
            List<MethodNode> clinitMethods = new ArrayList<>();
            for (MethodNode method : cls.getMethods()) {
                if (clinit == null || (!method.getModifiers().contains(NodeModifier.STATIC) &&
                        !method.getReference().getName().equals("<init>"))) {
                    nonInitMethods.add(method);
                } else {
                    clinitMethods.add(method);
                }
            }
            boolean needsClinit = clinit != null;

            if (needsClinit) {
                writer.append("function ").appendClass(cls.getName()).append("_$clinit()").ws()
                        .append("{").softNewLine().indent();
                writer.appendClass(cls.getName()).append("_$clinit").ws().append("=").ws()
                        .append("function(){};").newLine();
                for (MethodNode method : clinitMethods) {
                    renderBody(method, true);
                }
                if (clinit != null) {
                    writer.appendMethodBody(new MethodReference(cls.getName(), clinit.getDescriptor()))
                            .append("();").softNewLine();
                }
                writer.outdent().append("}").newLine();
            }
            if (!cls.getModifiers().contains(NodeModifier.INTERFACE)) {
                for (MethodNode method : cls.getMethods()) {
                    cls.getMethods();
                    if (!method.getModifiers().contains(NodeModifier.STATIC)) {
                        if (method.getReference().getName().equals("<init>")) {
                            renderInitializer(method);
                        } else {
                            virtualMethods.add(method);
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
            throw new RenderingException("IO error occured", e);
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
                if (cls.getModifiers().contains(NodeModifier.ENUM)) {
                    flags &= 1;
                }
                writer.append(flags).append(',').ws();
                MethodHolder clinit = classSource.get(cls.getName()).getMethod(
                        new MethodDescriptor("<clinit>", ValueType.VOID));
                if (clinit != null) {
                    writer.appendClass(cls.getName()).append("_$clinit");
                } else {
                    writer.append('0');
                }
                writer.append(',').ws();

                List<String> stubNames = new ArrayList<>();
                List<MethodNode> virtualMethods = new ArrayList<>();
                for (MethodNode method : cls.getMethods()) {
                    if (clinit != null && (method.getModifiers().contains(NodeModifier.STATIC) ||
                            method.getReference().getName().equals("<init>"))) {
                        stubNames.add(naming.getFullNameFor(method.getReference()));
                    }
                    if (!method.getModifiers().contains(NodeModifier.STATIC)) {
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
            throw new RenderingException("IO error occured", e);
        }
    }

    private static Object getDefaultValue(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            ValueType.Primitive primitive = (ValueType.Primitive)type;
            switch (primitive.getKind()) {
                case BOOLEAN:
                    return false;
                case BYTE:
                    return (byte)0;
                case SHORT:
                    return (short)0;
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
            String methodName = method.isAsync() ? naming.getNameForAsync(ref) : naming.getNameFor(ref);
            writer.append("\"").append(methodName).append("\"");
            writer.append(",").ws().append("function(");
            List<String> args = new ArrayList<>();
            for (int i = 1; i <= ref.parameterCount(); ++i) {
                args.add(variableName(i));
            }
            if (method.isAsync()) {
                args.add("$return");
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
            for (int i = 0; i < args.size(); ++i) {
                writer.append(",").ws().append(args.get(i));
            }
            writer.append(");").ws().append("}");
            debugEmitter.emitMethod(null);

            if (!method.isAsync() && asyncFamilyMethods.contains(method.getReference())) {
                writer.append(",").newLine();
                writer.append("\"").append(naming.getNameForAsync(ref)).append("\",").ws();
                writer.append("$rt_asyncAdapter(").appendMethodBody(ref).append(')');
            }
        }
        writer.append("]");
    }

    public void renderBody(MethodNode method, boolean inner) throws IOException {
        blockIdMap.clear();
        MethodReference ref = method.getReference();
        debugEmitter.emitMethod(ref.getDescriptor());
        if (inner) {
            writer.appendMethodBody(ref).ws().append("=").ws().append("function(");
        } else {
            writer.append("function ").appendMethodBody(ref).append("(");
        }
        int startParam = 0;
        if (method.getModifiers().contains(NodeModifier.STATIC)) {
            startParam = 1;
        }
        for (int i = startParam; i <= ref.parameterCount(); ++i) {
            if (i > startParam) {
                writer.append(",").ws();
            }
            writer.append(variableName(i));
        }
        if (method.isAsync()) {
            if (startParam < ref.parameterCount() + 1) {
                writer.append(',').ws();
            }
            writer.append("$return");
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
                throw new RenderingException("IO error occured", e);
            }
        }

        @Override
        public void visit(RegularMethodNode method) {
            try {
                Renderer.this.async = false;
                this.async = false;
                MethodReference ref = method.getReference();
                for (int i = 0; i < method.getParameterDebugNames().size(); ++i) {
                    debugEmitter.emitVariable(method.getParameterDebugNames().get(i).toArray(new String[0]),
                            variableName(i));
                }
                int variableCount = 0;
                for (int var : method.getVariables()) {
                    variableCount = Math.max(variableCount, var + 1);
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
                method.getBody().acceptVisitor(Renderer.this);
            } catch (IOException e) {
                throw new RenderingException("IO error occured", e);
            }
        }

        @Override
        public void visit(AsyncMethodNode methodNode) {
            try {
                Renderer.this.async = true;
                this.async = true;
                MethodReference ref = methodNode.getReference();
                for (int i = 0; i < methodNode.getParameterDebugNames().size(); ++i) {
                    debugEmitter.emitVariable(methodNode.getParameterDebugNames().get(i).toArray(new String[0]),
                            variableName(i));
                }
                int variableCount = 0;
                for (int var : methodNode.getVariables()) {
                    variableCount = Math.max(variableCount, var + 1);
                }
                List<String> variableNames = new ArrayList<>();
                for (int i = ref.parameterCount() + 1; i < variableCount; ++i) {
                    variableNames.add(variableName(i));
                }
                TryCatchFinder tryCatchFinder = new TryCatchFinder();
                for (AsyncMethodPart part :  methodNode.getBody()) {
                    if (!tryCatchFinder.tryCatchFound) {
                        part.getStatement().acceptVisitor(tryCatchFinder);
                    }
                }
                boolean hasTryCatch = tryCatchFinder.tryCatchFound;
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
                for (int i = 0; i < methodNode.getBody().size(); ++i) {
                    writer.append("var $part_").append(i).ws().append("=").ws().append("$rt_guardAsync(function(");
                    if (i > 0) {
                        writer.append("$restore");
                    }
                    writer.append(")").ws().append("{").indent().softNewLine();
                    AsyncMethodPart part = methodNode.getBody().get(i);
                    part.getStatement().acceptVisitor(Renderer.this);
                    writer.outdent().append("},").ws().append("$return);").softNewLine();
                }
                writer.append("return $part_0();").softNewLine();
            } catch (IOException e) {
                throw new RenderingException("IO error occured", e);
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
        public String getCompleteContinuation() {
            return "$return";
        }

        @Override
        public Diagnostics getDiagnostics() {
            return diagnostics;
        }
    }

    private void pushLocation(NodeLocation location) {
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
                priority = Priority.COMMA;
                associativity = Associativity.NONE;
                statement.getLeftValue().acceptVisitor(this);
                writer.ws().append("=").ws();
            }
            priority = Priority.COMMA;
            associativity = Associativity.NONE;
            statement.getRightValue().acceptVisitor(this);
            debugEmitter.emitCallSite();
            writer.append(";").softNewLine();
            if (statement.getLocation() != null) {
                popLocation();
            }
            if (statement.getLeftValue() instanceof VariableExpr) {
                VariableExpr receiver = (VariableExpr)statement.getLeftValue();
                debugEmitter.emitVariable(statement.getDebugNames().toArray(new String[0]),
                        variableName(receiver.getIndex()));
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(SequentialStatement statement) {
        for (Statement part : statement.getSequence()) {
            part.acceptVisitor(this);
        }
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
                priority = Priority.COMMA;
                associativity = Associativity.NONE;
                statement.getCondition().acceptVisitor(this);
                if (statement.getCondition().getLocation() != null) {
                    popLocation();
                }
                debugEmitter.emitCallSite();
                writer.append(")").ws().append("{").softNewLine().indent();
                for (Statement part : statement.getConsequent()) {
                    part.acceptVisitor(this);
                }
                if (!statement.getAlternative().isEmpty()) {
                    writer.outdent().append("}").ws();
                    if (statement.getAlternative().size() == 1 &&
                            statement.getAlternative().get(0) instanceof ConditionalStatement) {
                        statement = (ConditionalStatement)statement.getAlternative().get(0);
                        writer.append("else ");
                        continue;
                    }
                    writer.append("else").ws().append("{").indent().softNewLine();
                    for (Statement part : statement.getAlternative()) {
                        part.acceptVisitor(this);
                    }
                }
                break;
            }
            writer.outdent().append("}").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
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
            priority = Priority.COMMA;
            associativity = Associativity.NONE;
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
                for (Statement part : clause.getBody()) {
                    part.acceptVisitor(this);
                }
                writer.outdent();
            }
            if (statement.getDefaultClause() != null) {
                writer.append("default:").softNewLine().indent();
                for (Statement part : statement.getDefaultClause()) {
                    part.acceptVisitor(this);
                }
                writer.outdent();
            }
            writer.outdent().append("}").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
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
                priority = Priority.COMMA;
                associativity = Associativity.NONE;
                statement.getCondition().acceptVisitor(this);
                debugEmitter.emitCallSite();
                if (statement.getCondition().getLocation() != null) {
                    popLocation();
                }
            } else {
                writer.append("true");
            }
            writer.append(")").ws().append("{").softNewLine().indent();
            for (Statement part : statement.getBody()) {
                part.acceptVisitor(this);
            }
            writer.outdent().append("}").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    private String mapBlockId(String id) {
        String name = blockIdMap.get(id);
        if (name == null) {
            StringBuilder sb = new StringBuilder();
            int index = blockIdMap.size();
            do {
                sb.append(variablePartNames.charAt(index % variablePartNames.length()));
                index /= variablePartNames.length();
            } while (index > 0);
            name = "$b" + sb;
            blockIdMap.put(id, name);
        }
        return name;
    }

    @Override
    public void visit(BlockStatement statement) {
        try {
            writer.append(mapBlockId(statement.getId())).append(":").ws().append("{").softNewLine().indent();
            for (Statement part : statement.getBody()) {
                part.acceptVisitor(this);
            }
            writer.outdent().append("}").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
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
            throw new RenderingException("IO error occured", e);
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
            throw new RenderingException("IO error occured", e);
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
            if (async) {
                writer.append(" $return($rt_asyncResult(");
            }
            if (statement.getResult() != null) {
                if (!async) {
                    writer.append(' ');
                }
                prevCallSite = debugEmitter.emitCallSite();
                priority = Priority.COMMA;
                associativity = Associativity.NONE;
                statement.getResult().acceptVisitor(this);
                debugEmitter.emitCallSite();
            }
            if (async) {
                writer.append("))");
            }
            writer.append(";").softNewLine();
            if (statement.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(ThrowStatement statement) {
        try {
            debugEmitter.emitStatementStart();
            if (statement.getLocation() != null) {
                pushLocation(statement.getLocation());
            }
            writer.append("$rt_throw(");
            prevCallSite = debugEmitter.emitCallSite();
            priority = Priority.COMMA;
            associativity = Associativity.NONE;
            statement.getException().acceptVisitor(this);
            writer.append(");").softNewLine();
            debugEmitter.emitCallSite();
            if (statement.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(InitClassStatement statement) {
        try {
            debugEmitter.emitStatementStart();
            if (statement.getLocation() != null) {
                pushLocation(statement.getLocation());
            }
            writer.appendClass(statement.getClassName()).append("_$clinit();").softNewLine();
            if (statement.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    public String variableName(int index) {
        if (index == 0) {
            return minifying ? "$t" : "$this";
        }
        --index;
        if (index < variableNames.length()) {
            return Character.toString(variableNames.charAt(index));
        } else {
            return Character.toString(variableNames.charAt(index % variableNames.length())) +
                    index / variableNames.length();
        }
    }

    private void visitBinary(BinaryExpr expr, String op, Priority priority, Associativity associativity) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            enterPriority(priority, associativity == Associativity.LEFT ? associativity : Associativity.NONE, true);
            expr.getFirstOperand().acceptVisitor(this);
            writer.ws().append(op).ws();
            this.associativity = associativity == Associativity.RIGHT ? associativity : Associativity.NONE;
            expr.getSecondOperand().acceptVisitor(this);
            exitPriority();
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    private void enterPriority(Priority priority, Associativity associativity, boolean autoGroup) throws IOException {
        OperatorPrecedence precedence = new OperatorPrecedence();
        precedence.wasGrouped = this.wasGrouped;
        precedence.priority = this.priority;
        precedence.associativity = this.associativity;
        precedenceStack.push(precedence);
        wasGrouped = false;
        if (autoGroup && (priority.ordinal() < this.priority.ordinal() ||
                priority.ordinal() == this.priority.ordinal() &&
                (associativity != this.associativity || associativity == Associativity.NONE))) {
            wasGrouped = true;
            writer.append('(');
        }
        this.priority = priority;
        this.associativity = associativity;
    }

    private void exitPriority() throws IOException {
        if (wasGrouped) {
            writer.append(')');
        }
        OperatorPrecedence precedence = precedenceStack.pop();
        this.priority = precedence.priority;
        this.associativity = precedence.associativity;
        this.wasGrouped = precedence.wasGrouped;
    }

    private void visitBinaryFunction(BinaryExpr expr, String function) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            enterPriority(Priority.COMMA, Associativity.NONE, false);
            writer.append(function);
            writer.append('(');
            expr.getFirstOperand().acceptVisitor(this);
            writer.append(",").ws();
            expr.getSecondOperand().acceptVisitor(this);
            writer.append(')');
            exitPriority();
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(BinaryExpr expr) {
        switch (expr.getOperation()) {
            case ADD:
                visitBinary(expr, "+", Priority.ADDITION, Associativity.LEFT);
                break;
            case ADD_LONG:
                visitBinaryFunction(expr, "Long_add");
                break;
            case SUBTRACT:
                visitBinary(expr, "-", Priority.ADDITION, Associativity.LEFT);
                break;
            case SUBTRACT_LONG:
                visitBinaryFunction(expr, "Long_sub");
                break;
            case MULTIPLY:
                visitBinary(expr, "*", Priority.MULTIPLICATION, Associativity.LEFT);
                break;
            case MULTIPLY_LONG:
                visitBinaryFunction(expr, "Long_mul");
                break;
            case DIVIDE:
                visitBinary(expr, "/", Priority.MULTIPLICATION, Associativity.LEFT);
                break;
            case DIVIDE_LONG:
                visitBinaryFunction(expr, "Long_div");
                break;
            case MODULO:
                visitBinary(expr, "%", Priority.MULTIPLICATION, Associativity.LEFT);
                break;
            case MODULO_LONG:
                visitBinaryFunction(expr, "Long_rem");
                break;
            case EQUALS:
                visitBinary(expr, "==", Priority.EQUALITY, Associativity.LEFT);
                break;
            case NOT_EQUALS:
                visitBinary(expr, "!=", Priority.EQUALITY, Associativity.LEFT);
                break;
            case GREATER:
                visitBinary(expr, ">", Priority.COMPARISON, Associativity.LEFT);
                break;
            case GREATER_OR_EQUALS:
                visitBinary(expr, ">=", Priority.COMPARISON, Associativity.LEFT);
                break;
            case LESS:
                visitBinary(expr, "<", Priority.COMPARISON, Associativity.LEFT);
                break;
            case LESS_OR_EQUALS:
                visitBinary(expr, "<=", Priority.COMPARISON, Associativity.LEFT);
                break;
            case STRICT_EQUALS:
                visitBinary(expr, "===", Priority.COMPARISON, Associativity.LEFT);
                break;
            case STRICT_NOT_EQUALS:
                visitBinary(expr, "!==", Priority.COMPARISON, Associativity.LEFT);
                break;
            case COMPARE:
                visitBinaryFunction(expr, "$rt_compare");
                break;
            case COMPARE_LONG:
                visitBinaryFunction(expr, "Long_compare");
                break;
            case OR:
                visitBinary(expr, "||", Priority.LOGICAL_OR, Associativity.LEFT);
                break;
            case AND:
                visitBinary(expr, "&&", Priority.LOGICAL_AND, Associativity.LEFT);
                break;
            case BITWISE_OR:
                visitBinary(expr, "|", Priority.BITWISE_OR, Associativity.LEFT);
                break;
            case BITWISE_OR_LONG:
                visitBinaryFunction(expr, "Long_or");
                break;
            case BITWISE_AND:
                visitBinary(expr, "&", Priority.BITWISE_AND, Associativity.LEFT);
                break;
            case BITWISE_AND_LONG:
                visitBinaryFunction(expr, "Long_and");
                break;
            case BITWISE_XOR:
                visitBinary(expr, "^", Priority.BITWISE_XOR, Associativity.LEFT);
                break;
            case BITWISE_XOR_LONG:
                visitBinaryFunction(expr, "Long_xor");
                break;
            case LEFT_SHIFT:
                visitBinary(expr, "<<", Priority.BITWISE_SHIFT, Associativity.LEFT);
                break;
            case LEFT_SHIFT_LONG:
                visitBinaryFunction(expr, "Long_shl");
                break;
            case RIGHT_SHIFT:
                visitBinary(expr, ">>", Priority.BITWISE_SHIFT, Associativity.LEFT);
                break;
            case RIGHT_SHIFT_LONG:
                visitBinaryFunction(expr, "Long_shr");
                break;
            case UNSIGNED_RIGHT_SHIFT:
                visitBinary(expr, ">>>", Priority.BITWISE_SHIFT, Associativity.LEFT);
                break;
            case UNSIGNED_RIGHT_SHIFT_LONG:
                visitBinaryFunction(expr, "Long_shru");
                break;
        }
    }

    @Override
    public void visit(UnaryExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            switch (expr.getOperation()) {
                case NOT:
                    enterPriority(Priority.UNARY, Associativity.RIGHT, true);
                    writer.append("!");
                    expr.getOperand().acceptVisitor(this);
                    exitPriority();
                    break;
                case NEGATE:
                    enterPriority(Priority.UNARY, Associativity.RIGHT, true);
                    writer.append("-");
                    expr.getOperand().acceptVisitor(this);
                    exitPriority();
                    break;
                case LENGTH:
                    enterPriority(Priority.MEMBER_ACCESS, Associativity.LEFT, true);
                    expr.getOperand().acceptVisitor(this);
                    exitPriority();
                    writer.append(".length");
                    break;
                case INT_TO_LONG:
                    enterPriority(Priority.COMMA, Associativity.NONE, false);
                    writer.append("Long_fromInt(");
                    expr.getOperand().acceptVisitor(this);
                    writer.append(')');
                    exitPriority();
                    break;
                case NUM_TO_LONG:
                    enterPriority(Priority.COMMA, Associativity.NONE, false);
                    writer.append("Long_fromNumber(");
                    expr.getOperand().acceptVisitor(this);
                    writer.append(')');
                    exitPriority();
                    break;
                case LONG_TO_NUM:
                    enterPriority(Priority.COMMA, Associativity.NONE, false);
                    writer.append("Long_toNumber(");
                    expr.getOperand().acceptVisitor(this);
                    writer.append(')');
                    exitPriority();
                    break;
                case LONG_TO_INT:
                    enterPriority(Priority.MEMBER_ACCESS, Associativity.LEFT, false);
                    expr.getOperand().acceptVisitor(this);
                    exitPriority();
                    writer.append(".lo");
                    break;
                case NEGATE_LONG:
                    enterPriority(Priority.COMMA, Associativity.NONE, false);
                    writer.append("Long_neg(");
                    expr.getOperand().acceptVisitor(this);
                    writer.append(')');
                    exitPriority();
                    break;
                case NOT_LONG:
                    enterPriority(Priority.COMMA, Associativity.NONE, false);
                    writer.append("Long_not(");
                    expr.getOperand().acceptVisitor(this);
                    writer.append(')');
                    exitPriority();
                    break;
                case BYTE_TO_INT:
                    enterPriority(Priority.BITWISE_SHIFT, Associativity.LEFT, true);
                    writer.append("(");
                    expr.getOperand().acceptVisitor(this);
                    writer.ws().append("<<").ws().append("24)").ws().append(">>").ws().append("24");
                    exitPriority();
                    break;
                case SHORT_TO_INT:
                    enterPriority(Priority.BITWISE_SHIFT, Associativity.LEFT, true);
                    writer.append("(");
                    expr.getOperand().acceptVisitor(this);
                    writer.ws().append("<<").ws().append("16)").ws().append(">>").ws().append("16");
                    exitPriority();
                    break;
                case NULL_CHECK:
                    enterPriority(Priority.COMMA, Associativity.NONE, false);
                    writer.append("$rt_nullCheck(");
                    expr.getOperand().acceptVisitor(this);
                    writer.append(')');
                    exitPriority();
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
    public void visit(ConditionalExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            enterPriority(priority, Associativity.RIGHT, async);
            expr.getCondition().acceptVisitor(this);
            writer.ws().append("?").ws();
            expr.getConsequent().acceptVisitor(this);
            writer.ws().append(":").ws();
            expr.getAlternative().acceptVisitor(this);
            exitPriority();
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
            writer.append(constantToString(expr.getValue()));
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    public String constantToString(Object cst) {
        if (cst == null) {
            return "null";
        }
        if (cst instanceof ValueType) {
            ValueType type = (ValueType)cst;
            return "$rt_cls(" + typeToClsString(naming, type) + ")";
        } else if (cst instanceof String) {
            String string = (String)cst;
            Integer index = stringPoolMap.get(string);
            if (index == null) {
                index = stringPool.size();
                stringPool.add(string);
                stringPoolMap.put(string, index);
            }
            return "$rt_s(" + index + ")";
        } else if (cst instanceof Long) {
            long value = (Long)cst;
            if (value == 0) {
                return "Long_ZERO";
            } else if ((int)value == value) {
                return "Long_fromInt(" + value + ")";
            } else {
                return "new Long(" + (value & 0xFFFFFFFFL) + ", " + (value >>> 32) + ")";
            }
        } else if (cst instanceof Character) {
            return Integer.toString((Character)cst);
        } else {
            return cst.toString();
        }
    }

    public static String typeToClsString(NamingStrategy naming, ValueType type) {
        int arrayCount = 0;
        while (type instanceof ValueType.Array) {
            arrayCount++;
            type = ((ValueType.Array)type).getItemType();
        }
        String value;
        if (type instanceof ValueType.Object) {
            ValueType.Object objType = (ValueType.Object)type;
            value = naming.getNameFor(objType.getClassName());
        } else if (type instanceof ValueType.Void) {
            value = "$rt_voidcls()";
        } else if (type instanceof ValueType.Primitive) {
            ValueType.Primitive primitiveType = (ValueType.Primitive)type;
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
            enterPriority(Priority.MEMBER_ACCESS, Associativity.LEFT, true);
            expr.getArray().acceptVisitor(this);
            writer.append('[');
            enterPriority(Priority.COMMA, Associativity.NONE, false);
            expr.getIndex().acceptVisitor(this);
            exitPriority();
            writer.append(']');
            exitPriority();
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(UnwrapArrayExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            enterPriority(Priority.MEMBER_ACCESS, Associativity.LEFT, true);
            expr.getArray().acceptVisitor(this);
            writer.append(".data");
            if (expr.getLocation() != null) {
                popLocation();
            }
            exitPriority();
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
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
                if (expr.getAsyncTarget() != null) {
                    writer.append("return ");
                }
                if (expr.getType() == InvocationType.DYNAMIC) {
                    expr.getArguments().get(0).acceptVisitor(this);
                }
                String name = expr.getAsyncTarget() == null ? naming.getNameFor(expr.getMethod()) :
                        naming.getNameForAsync(expr.getMethod());
                String fullName = naming.getFullNameFor(expr.getMethod());
                DeferredCallSite callSite = prevCallSite;
                boolean shouldEraseCallSite = lastCallSite == null;
                if (lastCallSite == null) {
                    lastCallSite = callSite;
                }
                boolean virtual = false;
                boolean hasParams = false;
                enterPriority(Priority.COMMA, Associativity.NONE, false);
                switch (expr.getType()) {
                    case STATIC:
                        writer.append(fullName).append("(");
                        prevCallSite = debugEmitter.emitCallSite();
                        for (int i = 0; i < expr.getArguments().size(); ++i) {
                            if (i > 0) {
                                writer.append(",").ws();
                            }
                            expr.getArguments().get(i).acceptVisitor(this);
                            hasParams = true;
                        }
                        break;
                    case SPECIAL:
                        writer.append(fullName).append("(");
                        prevCallSite = debugEmitter.emitCallSite();
                        expr.getArguments().get(0).acceptVisitor(this);
                        hasParams = true;
                        for (int i = 1; i < expr.getArguments().size(); ++i) {
                            writer.append(",").ws();
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
                            hasParams = true;
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
                            hasParams = true;
                            expr.getArguments().get(i).acceptVisitor(this);
                        }
                        break;
                }
                if (expr.getAsyncTarget() != null) {
                    if (hasParams) {
                        writer.append(',').ws();
                    }
                    writer.append("$rt_continue($part_").append(expr.getAsyncTarget()).append(')');
                }
                writer.append(')');
                exitPriority();
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
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(QualificationExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            enterPriority(Priority.MEMBER_ACCESS, Associativity.LEFT, true);
            expr.getQualified().acceptVisitor(this);
            writer.append('.').appendField(expr.getField());
            if (expr.getLocation() != null) {
                popLocation();
            }
            exitPriority();
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(NewExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            enterPriority(Priority.FUNCTION_CALL, Associativity.RIGHT, true);
            writer.append("new ").append(naming.getNameFor(expr.getConstructedClass()));
            exitPriority();
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(NewArrayExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            ValueType type = expr.getType();
            enterPriority(Priority.COMMA, Associativity.NONE, false);
            if (type instanceof ValueType.Primitive) {
                switch (((ValueType.Primitive)type).getKind()) {
                    case BOOLEAN:
                        writer.append("$rt_createBooleanArray(");
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case BYTE:
                        writer.append("$rt_createByteArray(");
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case SHORT:
                        writer.append("$rt_createShortArray(");
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case INTEGER:
                        writer.append("$rt_createIntArray(");
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case LONG:
                        writer.append("$rt_createLongArray(");
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case FLOAT:
                        writer.append("$rt_createFloatArray(");
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case DOUBLE:
                        writer.append("$rt_createDoubleArray(");
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case CHARACTER:
                        writer.append("$rt_createCharArray(");
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                }
            } else {
                writer.append("$rt_createArray(").append(typeToClsString(naming, expr.getType())).append(",").ws();
                expr.getLength().acceptVisitor(this);
                writer.append(")");
            }
            exitPriority();
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
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
                type = ((ValueType.Array)type).getItemType();
            }
            enterPriority(Priority.COMMA, Associativity.NONE, false);
            if (type instanceof ValueType.Primitive) {
                switch (((ValueType.Primitive)type).getKind()) {
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
                dimension.acceptVisitor(this);
            }
            writer.append("])");
            exitPriority();
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(InstanceOfExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            if (expr.getType() instanceof ValueType.Object) {
                String clsName = ((ValueType.Object)expr.getType()).getClassName();
                ClassHolder cls = classSource.get(clsName);
                if (cls != null && !cls.getModifiers().contains(ElementModifier.INTERFACE)) {
                    enterPriority(Priority.COMPARISON, Associativity.LEFT, true);
                    expr.getExpr().acceptVisitor(this);
                    writer.append(" instanceof ").appendClass(clsName);
                    exitPriority();
                    if (expr.getLocation() != null) {
                        popLocation();
                    }
                    return;
                }
            }
            enterPriority(Priority.COMMA, Associativity.NONE, false);
            writer.append("$rt_isInstance(");
            expr.getExpr().acceptVisitor(this);
            writer.append(",").ws().append(typeToClsString(naming, expr.getType())).append(")");
            exitPriority();
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(StaticClassExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            writer.append(typeToClsString(naming, expr.getType()));
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(TryCatchStatement statement) {
        try {
            writer.append("try").ws().append("{").softNewLine().indent();
            List<TryCatchStatement> sequence = new ArrayList<>();
            sequence.add(statement);
            List<Statement> protectedBody = statement.getProtectedBody();
            while (protectedBody.size() == 1 && protectedBody.get(0) instanceof TryCatchStatement) {
                TryCatchStatement nextStatement = (TryCatchStatement)protectedBody.get(0);
                sequence.add(nextStatement);
                protectedBody = nextStatement.getProtectedBody();
            }
            for (Statement part : protectedBody) {
                part.acceptVisitor(this);
            }
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
                for (Statement part : catchClause.getHandler()) {
                    part.acceptVisitor(this);
                }
                writer.outdent().append("}").ws().append("else ");
            }
            writer.append("{").indent().softNewLine();
            writer.append("throw $e;").softNewLine();
            writer.outdent().append("}").softNewLine();
            writer.outdent().append("}").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(RestoreAsyncStatement statement) {
        try {
            if (statement.getReceiver() != null) {
                writer.append(variableName(statement.getReceiver())).ws().append('=').ws();
            }
            writer.append("$restore();").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    private Injector getInjector(MethodReference ref) {
        InjectorHolder holder = injectorMap.get(ref);
        if (holder == null) {
            MethodHolder method = classSource.get(ref.getClassName()).getMethod(ref.getDescriptor());
            holder = new InjectorHolder(null);
            if (method != null) {
                AnnotationHolder injectedByAnnot = method.getAnnotations().get(InjectedBy.class.getName());
                if (injectedByAnnot != null) {
                    ValueType type = injectedByAnnot.getValues().get("value").getJavaClass();
                    holder = new InjectorHolder(instantiateInjector(((ValueType.Object)type).getClassName()));
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
        private List<Expr> arguments;

        public InjectorContextImpl(List<Expr> arguments) {
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
    }

    @Override
    public <T> T getService(Class<T> type) {
        return services.getService(type);
    }
}
