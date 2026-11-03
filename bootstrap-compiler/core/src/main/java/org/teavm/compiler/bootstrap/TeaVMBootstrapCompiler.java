/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.compiler.bootstrap;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.teavm.backend.wasm.WasmDebugInfoLevel;
import org.teavm.backend.wasm.WasmDebugInfoLocation;
import org.teavm.backend.wasm.WasmGCTarget;
import org.teavm.backend.wasm.debug.sourcemap.SourceMapBuilder;
import org.teavm.classlib.impl.JCLPlugin;
import org.teavm.compiler.bootstrap.extension.GuestPolicyProvider;
import org.teavm.diagnostics.ProblemProvider;
import org.teavm.extension.bridge.BridgeGuestModule;
import org.teavm.extension.bridge.guest.ExtensionGuest;
import org.teavm.extension.spi.reflection.ReflectionPolicy;
import org.teavm.extension.spi.resources.ResourcesPolicy;
import org.teavm.jso.impl.JSOPlugin;
import org.teavm.metaprogramming.impl.MetaprogrammingPlugin;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.PreOptimizingClassHolderSource;
import org.teavm.model.ReferenceCache;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.parsing.CompositeClassHolderSource;
import org.teavm.parsing.resource.CompositeResourceProvider;
import org.teavm.parsing.resource.ResourceProvider;
import org.teavm.parsing.substitution.DefaultSubstituteClassNameMapping;
import org.teavm.platform.plugin.PlatformPlugin;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.TeaVMBuilder;
import org.teavm.vm.TeaVMOptimizationLevel;
import org.teavm.vm.spi.TeaVMPlugin;

public class TeaVMBootstrapCompiler {
    public static final String CLASSLIB_RESOURCE = "org/teavm/compiler/bootstrap/runtime-classlib-teavm.bin";
    private static final String EXTENSIONS_MODULE_NAME = "extensions.wasm";

    private String mainClass;
    private String outputName = "app";
    private boolean strictMode;
    private boolean obfuscated;
    private boolean debugInfo;
    private boolean sourceMap;
    private ResourceProvider classlibResourceProvider;
    private ResourceProvider classesResourceProvider;
    private BuildTarget buildTarget;
    private TeaVMOptimizationLevel optimizationLevel = TeaVMOptimizationLevel.ADVANCED;
    private WasmDebugInfoLevel debugInfoLevel;
    private Properties properties = new Properties();
    private List<TeaVMPlugin> plugins = new ArrayList<>();
    private ExtensionModuleLoader extensionModuleLoader;

    private ReferenceCache refCache = new ReferenceCache();
    private ResourceProvider resourceProvider;
    private ClassHolderSource classlibSource;
    private ProblemProvider problemProvider;
    private boolean extensionsResolved;
    private BridgeGuestModule extensionModule;

    public static ByteArrayResourceProvider loadClasslib() throws IOException {
        var input = TeaVMBootstrapCompiler.class.getClassLoader().getResourceAsStream(CLASSLIB_RESOURCE);
        if (input == null) {
            throw new IOException("TeaVM class library not found: " + CLASSLIB_RESOURCE);
        }
        try (input) {
            return ByteArrayResourceProvider.fromArchive(input);
        }
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public void setOutputName(String outputName) {
        this.outputName = outputName;
    }

    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    public void setObfuscated(boolean obfuscated) {
        this.obfuscated = obfuscated;
    }

    public void setDebugInfo(boolean debugInfo) {
        this.debugInfo = debugInfo;
    }

    public void setSourceMap(boolean sourceMap) {
        this.sourceMap = sourceMap;
    }

    public void setClasslibResourceProvider(ResourceProvider classlibResourceProvider) {
        this.classlibResourceProvider = classlibResourceProvider;
    }

    public void setClassesResourceProvider(ResourceProvider classesResourceProvider) {
        this.classesResourceProvider = classesResourceProvider;
    }

    public void setBuildTarget(BuildTarget buildTarget) {
        this.buildTarget = buildTarget;
    }

    public void setOptimizationLevel(TeaVMOptimizationLevel optimizationLevel) {
        this.optimizationLevel = optimizationLevel;
    }

    public void setDebugInfoLevel(WasmDebugInfoLevel debugInfoLevel) {
        this.debugInfoLevel = debugInfoLevel;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void setPlugins(List<TeaVMPlugin> plugins) {
        this.plugins = new ArrayList<>(plugins);
    }

    /**
     * Compiles application. Compilation is performed in two phases:
     *
     * <ol>
     *   <li>Extensions (like {@link ReflectionPolicy}), registered in the class path, are compiled into a separate
     *     module, which is then loaded via {@link ExtensionModuleLoader}. If class path does not contain
     *     extensions, this phase is skipped. The result of this phase is cached, so subsequent compilations
     *     reuse already loaded module.</li>
     *   <li>Application itself is compiled, with extensions running in the module, loaded in previous phase.</li>
     * </ol>
     *
     * <p>Since loading of module is asynchronous, the result is reported via callback.
     */
    public void compile(CompileCallback callback) {
        try {
            if (!extensionsResolved) {
                if (extensionModuleLoader == null || !hasExtensionProviders()) {
                    extensionsResolved = true;
                } else {
                    compileWithExtensions(callback);
                    return;
                }
            }
            compileApplication();
        } catch (Throwable e) {
            callback.error(e);
            return;
        }
        callback.complete();
    }

    private void compileWithExtensions(CompileCallback callback) {
        var moduleFiles = new LinkedHashMap<String, ByteArrayOutputStream>();
        doCompile(ExtensionGuest.class.getName(), EXTENSIONS_MODULE_NAME, new MemoryBuildTarget(moduleFiles),
                List.of(), null, false);
        if (!problemProvider.getSevereProblems().isEmpty()) {
            callback.complete();
            return;
        }
        extensionModuleLoader.load(moduleFiles.get(EXTENSIONS_MODULE_NAME).toByteArray(),
                new ExtensionModuleLoader.Callback() {
                    @Override
                    public void loaded(BridgeGuestModule module) {
                        extensionModule = module;
                        extensionsResolved = true;
                        try {
                            compileApplication();
                        } catch (Throwable e) {
                            callback.error(e);
                            return;
                        }
                        callback.complete();
                    }

                    @Override
                    public void failed(String message) {
                        callback.error(new RuntimeException("Could not load module with extensions: " + message));
                    }
                });
    }

    private void compileApplication() {
        var policyProvider = extensionModule != null
                ? new GuestPolicyProvider(extensionModule)
                : null;
        doCompile(mainClass, outputName, buildTarget, plugins, policyProvider, sourceMap);
    }

    /**
     * Checks whether class path registers any extensions. If not, the phase that compiles extensions into
     * a separate module can be skipped.
     */
    private boolean hasExtensionProviders() throws IOException {
        return hasProviders(ReflectionPolicy.class) || hasProviders(ResourcesPolicy.class);
    }

    private boolean hasProviders(Class<?> type) throws IOException {
        var resources = classesResourceProvider.getResources("META-INF/services/" + type.getName());
        while (resources.hasNext()) {
            try (var input = resources.next().open();
                    var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                while (true) {
                    var line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    var commentIndex = line.indexOf('#');
                    if (commentIndex >= 0) {
                        line = line.substring(0, commentIndex);
                    }
                    if (!line.isBlank()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void doCompile(String mainClass, String outputName, BuildTarget buildTarget, List<TeaVMPlugin> plugins,
            GuestPolicyProvider policyProvider, boolean sourceMap) {
        var target = new WasmGCTarget();
        var substitute = DefaultSubstituteClassNameMapping.createWithPolicies(List.of(
                new ClasslibSubstitutionPolicy()));
        if (classlibSource == null) {
            resourceProvider = new CompositeResourceProvider(classlibResourceProvider, classesResourceProvider);
            classlibSource = new PreOptimizingClassHolderSource(
                    new ClasspathClassHolderSource(classlibResourceProvider, refCache, substitute));
        }
        var currentClassSource = new CompositeClassHolderSource(List.of(classlibSource,
                new PreOptimizingClassHolderSource(
                        new ClasspathClassHolderSource(classesResourceProvider, refCache, substitute))));
        var teavm = new TeaVMBuilder(target)
                .setClassSource(currentClassSource)
                .setResourceProvider(resourceProvider)
                .setReferenceCache(refCache)
                .setObfuscated(obfuscated)
                .setStrict(strictMode)
                .build();
        teavm.setOptimizationLevel(optimizationLevel);
        teavm.setProperties(properties);
        if (policyProvider != null) {
            teavm.addExtensionPolicyProvider(policyProvider);
        }

        new MetaprogrammingPlugin().install(teavm);
        new PlatformPlugin().install(teavm);
        new JCLPlugin().install(teavm);
        new JSOPlugin().install(teavm);
        for (var plugin : plugins) {
            plugin.install(teavm);
        }

        teavm.setEntryPoint(mainClass);
        target.setObfuscated(obfuscated);
        target.setDebugInfoLocation(WasmDebugInfoLocation.EMBEDDED);
        target.setDebugInfo(debugInfo);
        if (debugInfoLevel != null) {
            target.setDebugInfoLevel(debugInfoLevel);
        }
        var wasmSourceMapWriter = new SourceMapBuilder();
        if (sourceMap) {
            target.setSourceMapBuilder(wasmSourceMapWriter);
            target.setSourceMapLocation(outputName + ".map");
        }
        target.setStrict(strictMode);
        teavm.build(buildTarget, outputName);
        if (sourceMap) {
            try (var out = buildTarget.createResource(outputName + ".map");
                    var writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                wasmSourceMapWriter.writeSourceMap(writer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        problemProvider = teavm.getProblemProvider();
    }

    /**
     * Sets loader of module with extensions. If not set, extensions found in class path are ignored.
     */
    public void setExtensionModuleLoader(ExtensionModuleLoader extensionModuleLoader) {
        this.extensionModuleLoader = extensionModuleLoader;
    }

    public ProblemProvider getProblemProvider() {
        return problemProvider;
    }

    private static class MemoryBuildTarget implements BuildTarget {
        private final Map<String, ByteArrayOutputStream> files;

        MemoryBuildTarget(Map<String, ByteArrayOutputStream> files) {
            this.files = files;
        }

        @Override
        public OutputStream createResource(String fileName) {
            var output = new ByteArrayOutputStream();
            files.put(fileName, output);
            return output;
        }
    }
}
