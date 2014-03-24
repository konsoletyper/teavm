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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.teavm.common.ConcurrentCachedMapper;
import org.teavm.common.Mapper;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.ClassHolderTransformer;

/**
 *
 * @author Alexey Andreev
 */
class JavascriptProcessedClassSource implements ClassHolderSource {
    private ClassHolderSource innerSource;
    private List<ClassHolderTransformer> transformers = new ArrayList<>();
    private ConcurrentCachedMapper<String, ClassHolder> mapper = new ConcurrentCachedMapper<>(
            new Mapper<String, ClassHolder>() {
        @Override public ClassHolder map(String preimage) {
            return getTransformed(preimage);
        }
    });

    public JavascriptProcessedClassSource(ClassHolderSource innerSource) {
        this.innerSource = innerSource;
    }

    public void addTransformer(ClassHolderTransformer transformer) {
        transformers.add(transformer);
    }

    @Override
    public ClassHolder get(String name) {
        return mapper.map(name);
    }

    private ClassHolder getTransformed(String name) {
        ClassHolder cls = innerSource.get(name);
        if (cls != null) {
            transformClass(cls);
        }
        return cls;
    }

    private void transformClass(ClassHolder cls) {
        for (ClassHolderTransformer transformer : transformers) {
            transformer.transformClass(cls, innerSource);
        }
    }
}
