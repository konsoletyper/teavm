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
package org.teavm.junit;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.PreOptimizingClassHolderSource;
import org.teavm.model.ReferenceCache;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.parsing.ClasspathResourceProvider;

/**
 * Holds the state that's shared between every test-framework front end (JUnit 4/TestNG's
 * {@link TeaVMTestRunner}, JUnit 5's {@link TeaVMJupiterExtension}): the TeaVM class model used to
 * compile tests, the set of enabled backend platforms, and the per-platform run strategies. It's
 * initialized once per JVM, on whichever front end's classes get loaded first.
 */
final class TeaVMTestInfrastructure {
    static final ReferenceCache referenceCache = new ReferenceCache();
    static final ClassLoader classLoader;
    static final ClassHolderSource classSource;
    static final File outputDir;
    static final List<TestPlatformSupport<?>> platforms = new ArrayList<>();
    static final Map<TestPlatform, TestRunStrategy> runners = new HashMap<>();

    static {
        classLoader = TeaVMTestInfrastructure.class.getClassLoader();
        classSource = getClassSource(classLoader);

        String outputPath = System.getProperty(PropertyNames.PATH_PARAM);
        outputDir = outputPath != null ? new File(outputPath) : null;

        platforms.add(new JSPlatformSupport(classSource, referenceCache));
        platforms.add(new WebAssemblyGCPlatformSupport(classSource, referenceCache,
                Boolean.parseBoolean(System.getProperty(PropertyNames.WASM_GC_DISASM))));
        platforms.add(new CPlatformSupport(classSource, referenceCache));

        for (var platform : platforms) {
            if (platform.isEnabled() && !platform.getConfigurations().isEmpty()) {
                var runStrategy = platform.createRunStrategy(outputDir);
                if (runStrategy != null) {
                    runners.put(platform.getPlatform(), runStrategy);
                }
            }
        }

        for (var strategy : runners.values()) {
            strategy.beforeAll();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (var strategy : runners.values()) {
                strategy.afterAll();
            }
        }));
    }

    private TeaVMTestInfrastructure() {
    }

    private static ClassHolderSource getClassSource(ClassLoader classLoader) {
        var resourceProvider = new ClasspathResourceProvider(classLoader);
        return new PreOptimizingClassHolderSource(new ClasspathClassHolderSource(resourceProvider, referenceCache,
                classLoader));
    }
}
