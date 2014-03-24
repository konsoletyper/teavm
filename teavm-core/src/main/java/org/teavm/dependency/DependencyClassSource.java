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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.teavm.common.ConcurrentCachedMapper;
import org.teavm.common.Mapper;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;

/**
 *
 * @author Alexey Andreev
 */
class DependencyClassSource implements ClassReaderSource {
    private ClassReaderSource innerSource;
    private ConcurrentMap<String, ClassHolder> generatedClasses = new ConcurrentHashMap<>();
    private ConcurrentCachedMapper<String, ClassReader> cache = new ConcurrentCachedMapper<>(
            new Mapper<String, ClassReader>() {
        @Override public ClassReader map(String preimage) {
            return findClass(preimage);
        }
    });

    public DependencyClassSource(ClassReaderSource innerSource) {
        this.innerSource = innerSource;
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

    private ClassReader findClass(String name) {
        ClassReader cls = innerSource.get(name);
        if (cls == null) {
            cls = generatedClasses.get(name);
        }
        return cls;
    }

    public Collection<ClassHolder> getGeneratedClasses() {
        return generatedClasses.values();
    }
}
