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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.teavm.backend.wasm.WasmDebugInfoLevel;
import org.teavm.backend.wasm.WasmDebugInfoLocation;
import org.teavm.backend.wasm.WasmGCTarget;
import org.teavm.backend.wasm.debug.sourcemap.SourceMapBuilder;
import org.teavm.classlib.impl.JCLPlugin;
import org.teavm.diagnostics.ProblemProvider;
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

    private ReferenceCache refCache = new ReferenceCache();
    private ResourceProvider resourceProvider;
    private ClassHolderSource classlibSource;
    private ProblemProvider problemProvider;

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

    public void compile() {
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

    public ProblemProvider getProblemProvider() {
        return problemProvider;
    }
}
