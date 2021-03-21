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
package org.teavm.dependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.teavm.cache.IncrementalDependencyRegistration;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodHolder;
import org.teavm.model.optimization.UnreachableBasicBlockEliminator;
import org.teavm.model.util.ModelUtils;

class DependencyClassSource implements ClassHolderSource {
    private ClassReaderSource innerSource;
    ClassHierarchy innerHierarchy;
    private Diagnostics diagnostics;
    private IncrementalDependencyRegistration dependencyRegistration;
    private Map<String, ClassHolder> generatedClasses = new LinkedHashMap<>();
    private List<ClassHolderTransformer> transformers = new ArrayList<>();
    boolean obfuscated;
    boolean strict;
    Map<String, Optional<ClassHolder>> cache = new LinkedHashMap<>(1000, 0.5f);

    DependencyClassSource(ClassReaderSource innerSource, Diagnostics diagnostics,
            IncrementalDependencyRegistration dependencyRegistration) {
        this.innerSource = innerSource;
        this.diagnostics = diagnostics;
        innerHierarchy = new ClassHierarchy(innerSource);
        this.dependencyRegistration = dependencyRegistration;
    }

    @Override
    public ClassHolder get(String name) {
        return cache.computeIfAbsent(name, n -> Optional.ofNullable(findAndTransformClass(n))).orElse(null);
    }

    public void submit(ClassHolder cls) {
        if (innerSource.get(cls.getName()) != null || generatedClasses.containsKey(cls.getName())) {
            throw new IllegalArgumentException("Class " + cls.getName() + " is already defined");
        }
        if (!transformers.isEmpty()) {
            for (ClassHolderTransformer transformer : transformers) {
                transformer.transformClass(cls, transformContext);
            }
            cls = ModelUtils.copyClass(cls);
        }
        generatedClasses.put(cls.getName(), cls);
        for (MethodHolder method : cls.getMethods()) {
            if (method.getProgram() != null && method.getProgram().basicBlockCount() > 0) {
                new UnreachableBasicBlockEliminator().optimize(method.getProgram());
            }
        }
        cache.remove(cls.getName());
    }

    private ClassHolder findAndTransformClass(String name) {
        ClassHolder cls = findClass(name);
        if (cls != null && !transformers.isEmpty()) {
            for (ClassHolderTransformer transformer : transformers) {
                transformer.transformClass(cls, transformContext);
            }
        }
        return cls;
    }

    private ClassHolder findClass(String name) {
        ClassReader cls = innerSource.get(name);
        if (cls != null) {
            return ModelUtils.copyClass(cls);
        }
        return generatedClasses.get(name);
    }

    Collection<String> getGeneratedClassNames() {
        return generatedClasses.keySet();
    }

    public Collection<ClassHolder> getGeneratedClasses() {
        return generatedClasses.values();
    }

    public boolean isGeneratedClass(String className) {
        return generatedClasses.containsKey(className);
    }

    public void addTransformer(ClassHolderTransformer transformer) {
        transformers.add(transformer);
    }

    public void cleanup() {
        transformers.clear();
    }

    final ClassHolderTransformerContext transformContext = new ClassHolderTransformerContext() {
        @Override
        public ClassHierarchy getHierarchy() {
            return innerHierarchy;
        }

        @Override
        public Diagnostics getDiagnostics() {
            return diagnostics;
        }

        @Override
        public IncrementalDependencyRegistration getIncrementalCache() {
            return dependencyRegistration;
        }

        @Override
        public boolean isObfuscated() {
            return obfuscated;
        }

        @Override
        public boolean isStrict() {
            return strict;
        }

        @Override
        public void submit(ClassHolder cls) {
            DependencyClassSource.this.submit(cls);
        }
    };
}
