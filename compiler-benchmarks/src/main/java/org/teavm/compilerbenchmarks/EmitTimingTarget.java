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
package org.teavm.compilerbenchmarks;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.teavm.dependency.DependencyAnalyzer;
import org.teavm.dependency.DependencyListener;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ListableClassHolderSource;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.optimization.InliningFilterFactory;
import org.teavm.model.util.VariableCategoryProvider;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.TeaVMTarget;
import org.teavm.vm.TeaVMTargetController;
import org.teavm.vm.spi.TeaVMHostExtension;

/**
 * Delegates everything to the wrapped target, measuring time spent in {@link #emit}.
 */
class EmitTimingTarget implements TeaVMTarget {
    private final TeaVMTarget delegate;
    long emitStart;
    long emitEnd;

    EmitTimingTarget(TeaVMTarget delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<ClassHolderTransformer> getTransformers() {
        return delegate.getTransformers();
    }

    @Override
    public List<DependencyListener> getDependencyListeners() {
        return delegate.getDependencyListeners();
    }

    @Override
    public void setEntryPoint(String entryPoint, String name) {
        delegate.setEntryPoint(entryPoint, name);
    }

    @Override
    public void setController(TeaVMTargetController controller) {
        delegate.setController(controller);
    }

    @Override
    public List<TeaVMHostExtension> getHostExtensions() {
        return delegate.getHostExtensions();
    }

    @Override
    public VariableCategoryProvider variableCategoryProvider() {
        return delegate.variableCategoryProvider();
    }

    @Override
    public void contributeDependencies(DependencyAnalyzer dependencyAnalyzer) {
        delegate.contributeDependencies(dependencyAnalyzer);
    }

    @Override
    public void beforeInlining(Program program, MethodReader method) {
        delegate.beforeInlining(program, method);
    }

    @Override
    public void analyzeBeforeOptimizations(ListableClassReaderSource classSource) {
        delegate.analyzeBeforeOptimizations(classSource);
    }

    @Override
    public void beforeOptimizations(Program program, MethodReader method) {
        delegate.beforeOptimizations(program, method);
    }

    @Override
    public void afterOptimizations(Program program, MethodReader method) {
        delegate.afterOptimizations(program, method);
    }

    @Override
    public void emit(ListableClassHolderSource classes, BuildTarget buildTarget, String outputName)
            throws IOException {
        emitStart = System.nanoTime();
        try {
            delegate.emit(classes, buildTarget, outputName);
        } finally {
            emitEnd = System.nanoTime();
        }
    }

    @Override
    public String[] getPlatformTags() {
        return delegate.getPlatformTags();
    }

    @Override
    public boolean isAsyncSupported() {
        return delegate.isAsyncSupported();
    }

    @Override
    public InliningFilterFactory getInliningFilter() {
        return delegate.getInliningFilter();
    }

    @Override
    public Collection<? extends MethodReference> getInitializerMethods() {
        return delegate.getInitializerMethods();
    }

    @Override
    public boolean needsSystemArrayCopyOptimization() {
        return delegate.needsSystemArrayCopyOptimization();
    }

    @Override
    public boolean filterClassInitializer(String initializer) {
        return delegate.filterClassInitializer(initializer);
    }
}
