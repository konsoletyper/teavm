/*
 *  Copyright 2014 Alexey Andreev.
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

import org.teavm.dependency.DependencyAnalyzerFactory;
import org.teavm.dependency.PreciseDependencyAnalyzer;
import org.teavm.interop.PlatformMarker;
import org.teavm.model.ClassReaderSource;
import org.teavm.parsing.ClasspathClassHolderSource;

public class TeaVMBuilder {
    TeaVMTarget target;
    ClassReaderSource classSource;
    ClassLoader classLoader;
    DependencyAnalyzerFactory dependencyAnalyzerFactory = PreciseDependencyAnalyzer::new;

    public TeaVMBuilder(TeaVMTarget target) {
        this.target = target;
        classLoader = TeaVMBuilder.class.getClassLoader();
        classSource = !isBootstrap() ? new ClasspathClassHolderSource(classLoader) : name -> null;
    }

    public ClassReaderSource getClassSource() {
        return classSource;
    }

    public TeaVMBuilder setClassSource(ClassReaderSource classSource) {
        this.classSource = classSource;
        return this;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public TeaVMBuilder setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    public DependencyAnalyzerFactory getDependencyAnalyzerFactory() {
        return dependencyAnalyzerFactory;
    }

    public TeaVMBuilder setDependencyAnalyzerFactory(DependencyAnalyzerFactory dependencyAnalyzerFactory) {
        this.dependencyAnalyzerFactory = dependencyAnalyzerFactory;
        return this;
    }

    public TeaVM build() {
        return new TeaVM(this);
    }

    @PlatformMarker
    private static boolean isBootstrap() {
        return false;
    }
}
