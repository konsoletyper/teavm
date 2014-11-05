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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.teavm.common.CachedMapper;
import org.teavm.common.Mapper;
import org.teavm.model.*;
import org.teavm.model.util.ModelUtils;

/**
 *
 * @author Alexey Andreev
 */
class DependencyClassSource implements ClassReaderSource {
    private ClassReaderSource innerSource;
    private Diagnostics diagnostics;
    private ConcurrentMap<String, ClassHolder> generatedClasses = new ConcurrentHashMap<>();
    private List<ClassHolderTransformer> transformers = new ArrayList<>();
    private CachedMapper<String, ClassReader> cache = new CachedMapper<>(
            new Mapper<String, ClassReader>() {
        @Override public ClassReader map(String preimage) {
            return findAndTransformClass(preimage);
        }
    });

    public DependencyClassSource(ClassReaderSource innerSource, Diagnostics diagnostics) {
        this.innerSource = innerSource;
        this.diagnostics = diagnostics;
    }

    @Override
    public ClassReader get(String name) {
        return cache.map(name);
    }

    public void submit(ClassHolder cls) {
        if (innerSource.get(cls.getName()) != null) {
            throw new IllegalArgumentException("Class " + cls.getName() + " is already defined");
        }
        if (generatedClasses.putIfAbsent(cls.getName(), cls) != null) {
            throw new IllegalArgumentException("Class " + cls.getName() + " is already defined");
        }
    }

    private ClassReader findAndTransformClass(String name) {
        ClassHolder cls = findClass(name);
        if (cls != null && !transformers.isEmpty()) {
            for (ClassHolderTransformer transformer : transformers) {
                transformer.transformClass(cls, innerSource, diagnostics);
            }
            cls = ModelUtils.copyClass(cls);
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

    public Collection<ClassHolder> getGeneratedClasses() {
        return generatedClasses.values();
    }

    public void addTransformer(ClassHolderTransformer transformer) {
        transformers.add(transformer);
    }
}
