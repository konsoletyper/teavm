/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.vm;

import java.io.*;
import java.util.*;
import org.teavm.codegen.*;
import org.teavm.common.FiniteExecutor;
import org.teavm.common.ServiceRepository;
import org.teavm.debugging.DebugInformationEmitter;
import org.teavm.debugging.SourceLocation;
import org.teavm.dependency.*;
import org.teavm.javascript.Decompiler;
import org.teavm.javascript.Renderer;
import org.teavm.javascript.RenderingException;
import org.teavm.javascript.ast.ClassNode;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.Injector;
import org.teavm.model.*;
import org.teavm.model.util.ListingBuilder;
import org.teavm.model.util.ProgramUtils;
import org.teavm.model.util.RegisterAllocator;
import org.teavm.optimization.ClassSetOptimizer;
import org.teavm.optimization.Devirtualization;
import org.teavm.vm.spi.RendererListener;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

/**
 * <p>TeaVM itself. This class builds a JavaScript VM that runs a certain code.
 * Here you can specify entry points into your code (such like {@code main} method).
 * TeaVM guarantees that all required classes and methods will be provided by
 * built VM.</p>
 *
 * <p>Here is a typical code snippet:</p>
 *
 * <pre>{@code
 *ClassLoader classLoader = ...; // obtain ClassLoader somewhere
 *ClassHolderSource classSource = new ClasspathClassHolderSource(classLoader);
 *TeaVM vm = new TeaVMBuilder()
 *        .setClassLoader(classLoader)
 *        .setClassSource(classSource)
 *        .build();
 *vm.setMinifying(false); // optionally disable obfuscation
 *vm.installPlugins();    // install all default plugins
 *                        // that are found in a classpath
 *vm.addEntryPoint("main", new MethodReference(
 *        "fully.qualified.ClassName",  "main",
 *         ValueType.array(ValueType.object("java.lang.String")),
 *         ValueType.VOID));
 *StringBuilder sb = new StringBuilder();
 *vm.build(sb, null);
 *vm.checkForMissingItems();
 *}</pre>
 *
 * @author Alexey Andreev
 */
public class TeaVM implements TeaVMHost, ServiceRepository {
    private ClassReaderSource classSource;
    private DependencyChecker dependencyChecker;
    private FiniteExecutor executor;
    private ClassLoader classLoader;
    private boolean minifying = true;
    private boolean bytecodeLogging;
    private OutputStream logStream = System.out;
    private Map<String, TeaVMEntryPoint> entryPoints = new HashMap<>();
    private Map<String, String> exportedClasses = new HashMap<>();
    private Map<MethodReference, Generator> methodGenerators = new HashMap<>();
    private Map<MethodReference, Injector> methodInjectors = new HashMap<>();
    private List<RendererListener> rendererListeners = new ArrayList<>();
    private Map<Class<?>, Object> services = new HashMap<>();
    private Properties properties = new Properties();
    private DebugInformationEmitter debugEmitter;

    TeaVM(ClassReaderSource classSource, ClassLoader classLoader, FiniteExecutor executor) {
        this.classSource = classSource;
        this.classLoader = classLoader;
        dependencyChecker = new DependencyChecker(this.classSource, classLoader, this, executor);
        this.executor = executor;
    }

    @Override
    public void add(DependencyListener listener) {
        dependencyChecker.addDependencyListener(listener);
    }

    @Override
    public void add(ClassHolderTransformer transformer) {
        dependencyChecker.addClassTransformer(transformer);
    }

    @Override
    public void add(MethodReference methodRef, Generator generator) {
        methodGenerators.put(methodRef, generator);
    }

    @Override
    public void add(MethodReference methodRef, Injector injector) {
        methodInjectors.put(methodRef, injector);
    }

    @Override
    public void add(RendererListener listener) {
        rendererListeners.add(listener);
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Reports whether this TeaVM instance uses obfuscation when generating the JavaScript code.
     *
     * @see #setMinifying(boolean)
     */
    public boolean isMinifying() {
        return minifying;
    }

    /**
     * Specifies whether this TeaVM instance uses obfuscation when generating the JavaScript code.
     *
     * @see #isMinifying()
     */
    public void setMinifying(boolean minifying) {
        this.minifying = minifying;
    }

    public boolean isBytecodeLogging() {
        return bytecodeLogging;
    }

    public void setBytecodeLogging(boolean bytecodeLogging) {
        this.bytecodeLogging = bytecodeLogging;
    }

    /**
     * Specifies configuration properties for TeaVM and its plugins. You should call this method before
     * installing any plugins or interceptors.
     *
     * @param properties configuration properties to set. These properties will be copied into this VM instance,
     * so VM won't see any further changes in this object.
     */
    public void setProperties(Properties properties) {
        this.properties.clear();
        if (properties != null) {
            this.properties.putAll(properties);
        }
    }

    @Override
    public Properties getProperties() {
        return new Properties(properties);
    }

    /**
     * <p>Adds an entry point. TeaVM guarantees, that all methods that are required by the entry point
     * will be available at run-time in browser. Also you need to specify for each parameter of entry point
     * which actual types will be passed here by calling {@link TeaVMEntryPoint#withValue(int, String)}.
     * It is highly recommended to read explanation on {@link TeaVMEntryPoint} class documentation.</p>
     *
     * <p>You should call this method after installing all plugins and interceptors, but before
     * doing the actual build.</p>
     *
     * @param name the name under which this entry point will be available for JavaScript code.
     * @param ref a full reference to the method which is an entry point.
     * @return an entry point that you can additionally adjust.
     */
    public TeaVMEntryPoint entryPoint(String name, MethodReference ref) {
        if (name != null) {
            if (entryPoints.containsKey(name)) {
                throw new IllegalArgumentException("Entry point with public name `" + name + "' already defined " +
                        "for method " + ref);
            }
        }
        TeaVMEntryPoint entryPoint = new TeaVMEntryPoint(name, ref,
                dependencyChecker.linkMethod(ref, DependencyStack.ROOT));
        dependencyChecker.initClass(ref.getClassName(), DependencyStack.ROOT);
        if (name != null) {
            entryPoints.put(name, entryPoint);
        }
        return entryPoint;
    }

    /**
     * <p>Adds an entry point. TeaVM guarantees, that all methods that are required by the entry point
     * will be available at run-time in browser. Also you need to specify for each parameter of entry point
     * which actual types will be passed here by calling {@link TeaVMEntryPoint#withValue(int, String)}.
     * It is highly recommended to read explanation on {@link TeaVMEntryPoint} class documentation.</p>
     *
     * <p>You should call this method after installing all plugins and interceptors, but before
     * doing the actual build.</p>
     *
     * @param ref a full reference to the method which is an entry point.
     * @return an entry point that you can additionally adjust.
     */
    public TeaVMEntryPoint entryPoint(MethodReference ref) {
        return entryPoint(null, ref);
    }

    public TeaVMEntryPoint linkMethod(MethodReference ref) {
        TeaVMEntryPoint entryPoint = new TeaVMEntryPoint("", ref,
                dependencyChecker.linkMethod(ref, DependencyStack.ROOT));
        dependencyChecker.initClass(ref.getClassName(), DependencyStack.ROOT);
        return entryPoint;
    }

    public void exportType(String name, String className) {
        if (exportedClasses.containsKey(name)) {
            throw new IllegalArgumentException("Class with public name `" + name + "' already defined for class " +
                    className);
        }
        dependencyChecker.initClass(className, DependencyStack.ROOT);
        exportedClasses.put(name, className);
    }

    public void linkType(String className) {
        dependencyChecker.initClass(className, DependencyStack.ROOT);
    }

    /**
     * Gets a {@link ClassReaderSource} which is used by this TeaVM instance. It is exactly what was
     * passed to {@link TeaVMBuilder#setClassSource(ClassHolderSource)}.
     */
    public ClassReaderSource getClassSource() {
        return classSource;
    }

    /**
     * <p>After building indicates whether build has failed due to some missing items (classes, methods and fields)
     * in the classpath. This can happen when you forgot some items in class path or when your code uses unimplemented
     * Java class library methods. The behavior of this method before building is not specified.</p>
     */
    public boolean hasMissingItems() {
        return dependencyChecker.hasMissingItems();
    }

    /**
     * <p>After building allows to build report on all items (classes, methods, fields) that are missing.
     * This can happen when you forgot some items in class path or when your code uses unimplemented
     * Java class library methods. The behavior of this method before building is not specified.</p>
     *
     * @param target where to append all dependency diagnostics errors.
     */
    public void showMissingItems(Appendable target) throws IOException {
        dependencyChecker.showMissingItems(target);
    }

    /**
     * <p>After building checks whether the build has failed due to some missing items (classes, methods and fields).
     * If it has failed, throws exception, containing report on all missing items.
     * This can happen when you forgot some items in class path or when your code uses unimplemented
     * Java class library methods. The behavior of this method before building is not specified.</p>
     */
    public void checkForMissingItems() {
        dependencyChecker.checkForMissingItems();
    }

    public DebugInformationEmitter getDebugEmitter() {
        return debugEmitter;
    }

    public void setDebugEmitter(DebugInformationEmitter debugEmitter) {
        this.debugEmitter = debugEmitter;
    }

    /**
     * <p>Does actual build. Call this method after TeaVM is fully configured and all entry points
     * are specified. This method may fail if there are items (classes, methods and fields)
     * that are required by entry points, but weren't found in classpath. In this case no
     * actual generation happens and no exceptions thrown, but you can further call
     * {@link #checkForMissingItems()} or {@link #hasMissingItems()} to learn the build state.</p>
     *
     * @param writer where to generate JavaScript. Should not be null.
     * @param target where to generate additional resources. Can be null, but if there are
     * plugins or inteceptors that generate additional resources, the build process will fail.
     */
    public void build(Appendable writer, BuildTarget target) throws RenderingException {
        // Check dependencies
        AliasProvider aliasProvider = minifying ? new MinifyingAliasProvider() : new DefaultAliasProvider();
        dependencyChecker.linkMethod(new MethodReference("java.lang.Class", "createNew",
                ValueType.object("java.lang.Class")), DependencyStack.ROOT).use();
        dependencyChecker.linkMethod(new MethodReference("java.lang.String", "<init>",
                ValueType.arrayOf(ValueType.CHARACTER), ValueType.VOID), DependencyStack.ROOT).use();
        dependencyChecker.linkMethod(new MethodReference("java.lang.String", "getChars",
                ValueType.INTEGER, ValueType.INTEGER, ValueType.arrayOf(ValueType.CHARACTER), ValueType.INTEGER,
                ValueType.VOID), DependencyStack.ROOT).use();
        MethodDependency internDep = dependencyChecker.linkMethod(new MethodReference("java.lang.String", "intern",
                ValueType.object("java.lang.String")), DependencyStack.ROOT);
        internDep.getVariable(0).propagate("java.lang.String");
        internDep.use();
        dependencyChecker.linkMethod(new MethodReference("java.lang.String", "length", ValueType.INTEGER),
                DependencyStack.ROOT).use();
        dependencyChecker.linkMethod(new MethodReference("java.lang.Object", new MethodDescriptor("clone",
                ValueType.object("java.lang.Object"))), DependencyStack.ROOT).use();
        executor.complete();
        if (hasMissingItems()) {
            return;
        }

        // Link
        Linker linker = new Linker();
        ListableClassHolderSource classSet = linker.link(dependencyChecker);

        // Optimize and allocate registers
        devirtualize(classSet, dependencyChecker);
        executor.complete();
        ClassSetOptimizer optimizer = new ClassSetOptimizer(executor);
        optimizer.optimizeAll(classSet);
        executor.complete();
        allocateRegisters(classSet);
        executor.complete();
        if (bytecodeLogging) {
            try {
                logBytecode(new PrintWriter(new OutputStreamWriter(logStream, "UTF-8")), classSet);
            } catch (UnsupportedEncodingException e) {
                // Just don't do anything
            }
        }

        // Decompile
        Decompiler decompiler = new Decompiler(classSet, classLoader, executor);
        for (Map.Entry<MethodReference, Generator> entry : methodGenerators.entrySet()) {
            decompiler.addGenerator(entry.getKey(), entry.getValue());
        }
        for (MethodReference injectedMethod : methodInjectors.keySet()) {
            decompiler.addMethodToPass(injectedMethod);
        }
        List<ClassNode> clsNodes = decompiler.decompile(classSet.getClassNames());

        // Render
        DefaultNamingStrategy naming = new DefaultNamingStrategy(aliasProvider, dependencyChecker.getClassSource());
        naming.setMinifying(minifying);
        SourceWriterBuilder builder = new SourceWriterBuilder(naming);
        builder.setMinified(minifying);
        SourceWriter sourceWriter = builder.build(writer);
        Renderer renderer = new Renderer(sourceWriter, classSet, classLoader, this);
        if (debugEmitter != null) {
            for (String className : classSet.getClassNames()) {
                ClassHolder cls = classSet.get(className);
                for (MethodHolder method : cls.getMethods()) {
                    if (method.getProgram() != null) {
                        emitCFG(debugEmitter, method.getProgram());
                    }
                }
            }
            renderer.setDebugEmitter(debugEmitter);
        }
        renderer.getDebugEmitter().setLocationProvider(sourceWriter);
        for (Map.Entry<MethodReference, Injector> entry : methodInjectors.entrySet()) {
            renderer.addInjector(entry.getKey(), entry.getValue());
        }
        try {
            for (RendererListener listener : rendererListeners) {
                listener.begin(renderer, target);
            }
            renderer.renderRuntime();
            for (ClassNode clsNode : clsNodes) {
                ClassReader cls = classSet.get(clsNode.getName());
                for (RendererListener listener : rendererListeners) {
                    listener.beforeClass(cls);
                }
                renderer.render(clsNode);
                for (RendererListener listener : rendererListeners) {
                    listener.afterClass(cls);
                }
            }
            renderer.renderStringPool();
            for (Map.Entry<String, TeaVMEntryPoint> entry : entryPoints.entrySet()) {
                sourceWriter.append(entry.getKey()).ws().append("=").ws().appendMethodBody(entry.getValue().reference)
                        .append(";").softNewLine();
            }
            for (Map.Entry<String, String> entry : exportedClasses.entrySet()) {
                sourceWriter.append(entry.getKey()).ws().append("=").ws().appendClass(entry.getValue()).append(";")
                        .softNewLine();
            }
            for (RendererListener listener : rendererListeners) {
                listener.complete();
            }
        } catch (IOException e) {
            throw new RenderingException("IO Error occured", e);
        }
    }

    private void emitCFG(DebugInformationEmitter emitter, Program program) {
        Map<InstructionLocation, InstructionLocation[]> cfg = ProgramUtils.getLocationCFG(program);
        for (Map.Entry<InstructionLocation, InstructionLocation[]> entry : cfg.entrySet()) {
            SourceLocation location = map(entry.getKey());
            SourceLocation[] successors = new SourceLocation[entry.getValue().length];
            for (int i = 0; i < entry.getValue().length; ++i) {
                successors[i] = map(entry.getValue()[i]);
            }
            emitter.addSuccessors(location, successors);
        }
    }

    private static SourceLocation map(InstructionLocation location) {
        if (location == null) {
            return null;
        }
        return new SourceLocation(location.getFileName(), location.getLine());
    }

    private void devirtualize(ListableClassHolderSource classes, DependencyInfo dependency) {
        final Devirtualization devirtualization = new Devirtualization(dependency, classes);
        for (String className : classes.getClassNames()) {
            ClassHolder cls = classes.get(className);
            for (final MethodHolder method : cls.getMethods()) {
                if (method.getProgram() != null) {
                    executor.execute(new Runnable() {
                        @Override public void run() {
                            devirtualization.apply(method);
                        }
                    });
                }
            }
        }
    }

    private void allocateRegisters(ListableClassHolderSource classes) {
        for (String className : classes.getClassNames()) {
            ClassHolder cls = classes.get(className);
            for (final MethodHolder method : cls.getMethods()) {
                if (method.getProgram() != null && method.getProgram().basicBlockCount() > 0) {
                    executor.execute(new Runnable() {
                        @Override public void run() {
                            RegisterAllocator allocator = new RegisterAllocator();
                            Program program = ProgramUtils.copy(method.getProgram());
                            allocator.allocateRegisters(method, program);
                            method.setProgram(program);
                        }
                    });
                }
            }
        }
    }

    private void logBytecode(PrintWriter writer, ListableClassHolderSource classes) {
        for (String className : classes.getClassNames()) {
            ClassHolder classHolder = classes.get(className);
            printModifiers(writer, classHolder);
            writer.println("class " + className);
            for (MethodHolder method : classHolder.getMethods()) {
                logMethodBytecode(writer, method);
            }
        }
    }

    private void logMethodBytecode(PrintWriter writer, MethodHolder method) {
        writer.print("    ");
        printModifiers(writer, method);
        writer.print(method.getName() + "(");
        ValueType[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; ++i) {
            if (i > 0) {
                writer.print(", ");
            }
            printType(writer, parameterTypes[i]);
        }
        writer.println(")");
        Program program = method.getProgram();
        if (program != null && program.basicBlockCount() > 0) {
            ListingBuilder builder = new ListingBuilder();
            writer.print(builder.buildListing(program, "        "));
            writer.print("        Register allocation:");
            for (int i = 0; i < program.variableCount(); ++i) {
                writer.print(i + ":" + program.variableAt(i).getRegister() + " ");
            }
            writer.println();
            writer.println();
            writer.flush();
        } else {
            writer.println();
        }
    }

    private void printType(PrintWriter writer, ValueType type) {
        if (type instanceof ValueType.Object) {
            writer.print(((ValueType.Object)type).getClassName());
        } else if (type instanceof ValueType.Array) {
            printType(writer, ((ValueType.Array)type).getItemType());
            writer.print("[]");
        } else if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive)type).getKind()) {
                case BOOLEAN:
                    writer.print("boolean");
                    break;
                case SHORT:
                    writer.print("short");
                    break;
                case BYTE:
                    writer.print("byte");
                    break;
                case CHARACTER:
                    writer.print("char");
                    break;
                case DOUBLE:
                    writer.print("double");
                    break;
                case FLOAT:
                    writer.print("float");
                    break;
                case INTEGER:
                    writer.print("int");
                    break;
                case LONG:
                    writer.print("long");
                    break;
            }
        }
    }

    private void printModifiers(PrintWriter writer, ElementHolder element) {
        switch (element.getLevel()) {
            case PRIVATE:
                writer.print("private ");
                break;
            case PUBLIC:
                writer.print("public ");
                break;
            case PROTECTED:
                writer.print("protected ");
                break;
            default:
                break;
        }
        Set<ElementModifier> modifiers = element.getModifiers();
        if (modifiers.contains(ElementModifier.ABSTRACT)) {
            writer.print("abstract ");
        }
        if (modifiers.contains(ElementModifier.FINAL)) {
            writer.print("final ");
        }
        if (modifiers.contains(ElementModifier.STATIC)) {
            writer.print("static ");
        }
        if (modifiers.contains(ElementModifier.NATIVE)) {
            writer.print("native ");
        }
    }

    public void build(File dir, String fileName) throws RenderingException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(new File(dir, fileName)), "UTF-8")) {
            build(writer, new DirectoryBuildTarget(dir));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Platform does not support UTF-8", e);
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    /**
     * <p>Finds and install all plugins in the current class path. The standard {@link ServiceLoader}
     * approach is used to find plugins. So this method scans all
     * <code>META-INF/services/org.teavm.vm.spi.TeaVMPlugin</code> resources and
     * obtains all implementation classes that are enumerated there.</p>
     */
    public void installPlugins() {
        for (TeaVMPlugin plugin : ServiceLoader.load(TeaVMPlugin.class, classLoader)) {
            plugin.install(this);
        }
    }

    @Override
    public <T> T getService(Class<T> type) {
        Object service = services.get(type);
        if (service == null) {
            throw new IllegalArgumentException("Service not registered: " + type.getName());
        }
        return type.cast(service);
    }

    @Override
    public <T> void registerService(Class<T> type, T instance) {
        services.put(type, instance);
    }
}
