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
package org.teavm.incremental;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.typedarrays.NativeUint16Array;
import org.teavm.ast.AsyncMethodNode;
import org.teavm.backend.javascript.JavaScriptTarget;
import org.teavm.cache.AlwaysStaleCacheStatus;
import org.teavm.cache.AstCacheEntry;
import org.teavm.cache.CacheStatus;
import org.teavm.cache.InMemoryMethodNodeCache;
import org.teavm.cache.InMemoryProgramCache;
import org.teavm.cache.InMemorySymbolTable;
import org.teavm.callgraph.CallGraph;
import org.teavm.dependency.FastDependencyAnalyzer;
import org.teavm.diagnostics.DefaultProblemTextConsumer;
import org.teavm.diagnostics.Problem;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.ClassReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ReferenceCache;
import org.teavm.model.util.ModelUtils;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;
import org.teavm.vm.TeaVMOptimizationLevel;

public class IncrementalTest {
    private static final String OLD_FILE = "classes-old.js";
    private static final String NEW_FILE = "classes-new.js";
    private static final String REFRESHED_FILE = "classes-refreshed.js";
    private static ClassHolderSource oldClassSource = new ClasspathClassHolderSource(
            IncrementalTest.class.getClassLoader(), new ReferenceCache());
    private static Context rhinoContext;
    private static ScriptableObject rhinoRootScope;
    private String[] updatedMethods;
    private String oldResult;
    private String newResult;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void initClass() {
        rhinoContext = Context.enter();
        rhinoContext.setOptimizationLevel(-1);
        rhinoContext.setLanguageVersion(Context.VERSION_ES6);
        rhinoRootScope = rhinoContext.initStandardObjects();
    }

    @AfterClass
    public static void closeClass() {
        Context.exit();
        rhinoRootScope = null;
        rhinoContext = null;
    }

    @Test
    public void simple() {
        run();
        checkUpdatedMethods("Foo.get", "Main.callFoo");
        assertEquals("old", oldResult);
        assertEquals("new", newResult);
    }

    @Test
    public void lambda() {
        run();
        checkUpdatedMethods("Bar.call", "Bar$call$lambda$_1_0.get", "Bar$call$lambda$_1_0.<init>",
                "BarNew.lambda$call$0", "Main.run");
        assertEquals("Foo: bar-old", oldResult);
        assertEquals("Foo: bar-new", newResult);
    }

    @Test
    public void lambdaUnchanged() {
        run();
        checkUpdatedMethods();
        assertEquals("Foo: main", oldResult);
        assertEquals("Foo: main", newResult);
    }

    @Test
    public void meta() {
        run();
        checkUpdatedMethods("Main$PROXY$2_0.meta", "Main$StringSupplier$proxy$2_0_0.<init>", "Main.meta",
                "Main$StringSupplier$proxy$2_0_0.get");
        assertEquals("meta: ok", oldResult);
        assertEquals("meta: ok", newResult);
    }

    private void checkUpdatedMethods(String... methods) {
        assertEquals("Unexpected set of updated methods", new HashSet<>(Arrays.asList(methods)),
                new HashSet<>(Arrays.asList(updatedMethods)));
    }

    private void run() {
        String entryPoint = "org.teavm.incremental.data." + name.getMethodName().toLowerCase() + ".Main";
        Builder builder = new Builder(entryPoint);

        ClassHolderSourceImpl newClassSource = new ClassHolderSourceImpl(oldClassSource, true);
        ClassHolderSourceImpl refreshedClassSource = new ClassHolderSourceImpl(oldClassSource, false);

        builder.build(oldClassSource, AlwaysStaleCacheStatus.INSTANCE, OLD_FILE);
        builder.build(refreshedClassSource, refreshedClassSource, REFRESHED_FILE);
        builder.enableCapturing();
        builder.build(newClassSource, newClassSource, NEW_FILE);

        assertEquals("Script must be the same after refreshing", builder.buildTarget.get(OLD_FILE),
                builder.buildTarget.get(REFRESHED_FILE));

        updatedMethods = builder.programCache.updatedMethods
                .stream()
                .map(m -> getSimpleName(m.getClassName()) + "." + m.getName())
                .sorted()
                .toArray(String[]::new);

        oldResult = runScript(builder.buildTarget.get(OLD_FILE), OLD_FILE);
        newResult = runScript(builder.buildTarget.get(NEW_FILE), NEW_FILE);
    }

    private String runScript(String script, String fileName) {
        Scriptable scope = new NativeObject();
        scope.setParentScope(rhinoRootScope);
        rhinoContext.evaluateString(scope, script, fileName, 1, null);
        Function main = (Function) scope.get("main", scope);
        ScriptRuntime.doTopCall(main, rhinoContext, scope, scope,
                new Object[] { new NativeArray(0), Undefined.instance });
        NativeUint16Array jsChars = (NativeUint16Array) main.get("result", main);
        char[] chars = new char[jsChars.getArrayLength()];
        for (int i = 0; i < chars.length; ++i) {
            chars[i] = (char) jsChars.get(i).intValue();
        }
        return new String(chars);
    }

    private static String getSimpleName(String name) {
        return name.substring(name.lastIndexOf('.') + 1);
    }

    static class Builder {
        String entryPoint;
        CapturingMethodNodeCache astCache = new CapturingMethodNodeCache();
        CapturingProgramCache programCache = new CapturingProgramCache();
        BuildTargetImpl buildTarget = new BuildTargetImpl();

        Builder(String entryPoint) {
            this.entryPoint = entryPoint;
        }

        void enableCapturing() {
            programCache.capturing = true;
            astCache.capturing = true;
        }

        void build(ClassHolderSource classSource, CacheStatus cacheStatus, String name) {
            JavaScriptTarget target = new JavaScriptTarget();
            TeaVM vm = new TeaVMBuilder(target)
                    .setClassLoader(IncrementalTest.class.getClassLoader())
                    .setClassSource(classSource)
                    .setDependencyAnalyzerFactory(FastDependencyAnalyzer::new)
                    .build();
            vm.setCacheStatus(cacheStatus);
            vm.setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE);
            vm.setProgramCache(programCache);
            target.setAstCache(astCache);
            target.setObfuscated(false);
            target.setStrict(true);
            vm.add(new EntryPointTransformer(entryPoint));
            vm.entryPoint(EntryPoint.class.getName());
            vm.installPlugins();
            vm.build(buildTarget, name);
            List<Problem> problems = vm.getProblemProvider().getSevereProblems();
            if (!problems.isEmpty()) {
                fail("Compiler error generating file '" + name + "'\n" + buildErrorMessage(vm));
            }
            astCache.commit();
            programCache.commit();
        }

        private String buildErrorMessage(TeaVM vm) {
            CallGraph cg = vm.getDependencyInfo().getCallGraph();
            DefaultProblemTextConsumer consumer = new DefaultProblemTextConsumer();
            StringBuilder sb = new StringBuilder();
            for (Problem problem : vm.getProblemProvider().getProblems()) {
                consumer.clear();
                problem.render(consumer);
                sb.append(consumer.getText());
                TeaVMProblemRenderer.renderCallStack(cg, problem.getLocation(), sb);
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    static class CapturingMethodNodeCache extends InMemoryMethodNodeCache {
        final Set<MethodReference> updatedMethods = new HashSet<>();
        boolean capturing;

        CapturingMethodNodeCache() {
            super(new ReferenceCache(), new InMemorySymbolTable(), new InMemorySymbolTable(),
                    new InMemorySymbolTable());
        }

        @Override
        public void store(MethodReference methodReference, AstCacheEntry node, Supplier<String[]> dependencies) {
            super.store(methodReference, node, dependencies);
            if (capturing) {
                updatedMethods.add(methodReference);
            }
        }

        @Override
        public void storeAsync(MethodReference methodReference, AsyncMethodNode node, Supplier<String[]> dependencies) {
            super.storeAsync(methodReference, node, dependencies);
            if (capturing) {
                updatedMethods.add(methodReference);
            }
        }
    }

    static class CapturingProgramCache extends InMemoryProgramCache {
        final Set<MethodReference> updatedMethods = new HashSet<>();
        boolean capturing;

        CapturingProgramCache() {
            super(new ReferenceCache(), new InMemorySymbolTable(), new InMemorySymbolTable(),
                    new InMemorySymbolTable());
        }

        @Override
        public void store(MethodReference method, Program program, Supplier<String[]> dependencies) {
            super.store(method, program, dependencies);
            if (capturing) {
                updatedMethods.add(method);
            }
        }
    }

    static class ClassHolderSourceImpl implements ClassHolderSource, CacheStatus {
        private ClassHolderSource underlying;
        private Map<String, ClassHolder> cache = new HashMap<>();
        private boolean replace;

        ClassHolderSourceImpl(ClassHolderSource underlying, boolean replace) {
            this.underlying = underlying;
            this.replace = replace;
        }

        @Override
        public boolean isStaleClass(String className) {
            ClassReader cls = underlying.get(className);
            if (cls == null) {
                return false;
            }

            return cls.getAnnotations().get(Update.class.getName()) != null;
        }

        @Override
        public boolean isStaleMethod(MethodReference method) {
            return isStaleClass(method.getClassName());
        }

        @Override
        public ClassHolder get(String name) {
            if (!replace) {
                return underlying.get(name);
            }
            return cache.computeIfAbsent(name, key -> {
                ClassHolder cls = underlying.get(key);
                if (cls == null) {
                    return cls;
                }
                if (cls.getAnnotations().get(Update.class.getName()) != null) {
                    ClassHolder newClass = underlying.get(key + "New");
                    if (newClass != null) {
                        cls = ModelUtils.copyClass(newClass, new ClassHolder(key));
                    }
                }
                return cls;
            });
        }
    }

    static class BuildTargetImpl implements BuildTarget {
        private Map<String, ByteArrayOutputStream> fs = new HashMap<>();

        public String get(String name) {
            return new String(fs.get(name).toByteArray(), StandardCharsets.UTF_8);
        }

        @Override
        public OutputStream createResource(String fileName) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            fs.put(fileName, out);
            return out;
        }
    }
}
