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
package org.teavm.perf;

import java.io.File;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.PreOptimizingClassHolderSource;
import org.teavm.model.ReferenceCache;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.parsing.ClasspathResourceProvider;
import org.teavm.vm.TeaVMOptimizationLevel;

/**
 * State shared by all compilations: class model, class loader, compiler settings, etc.
 */
public class BenchmarkEnvironment {
    private final ClassLoader classLoader;
    private final ReferenceCache referenceCache = new ReferenceCache();
    private final ClasspathResourceProvider resourceProvider;
    private final ClassHolderSource classSource;
    private final File outputDir;
    private TeaVMOptimizationLevel optimizationLevel = TeaVMOptimizationLevel.ADVANCED;

    public BenchmarkEnvironment(ClassLoader classLoader, File outputDir) {
        this.classLoader = classLoader;
        this.outputDir = outputDir;
        resourceProvider = new ClasspathResourceProvider(classLoader);
        classSource = new PreOptimizingClassHolderSource(new ClasspathClassHolderSource(resourceProvider,
                referenceCache, classLoader));
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public ReferenceCache getReferenceCache() {
        return referenceCache;
    }

    public ClasspathResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    public ClassHolderSource getClassSource() {
        return classSource;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public TeaVMOptimizationLevel getOptimizationLevel() {
        return optimizationLevel;
    }

    public void setOptimizationLevel(TeaVMOptimizationLevel optimizationLevel) {
        this.optimizationLevel = optimizationLevel;
    }
}
