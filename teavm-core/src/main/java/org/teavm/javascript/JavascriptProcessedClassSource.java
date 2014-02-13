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
package org.teavm.javascript;

import java.util.ArrayList;
import java.util.List;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.MethodHolder;

/**
 *
 * @author Alexey Andreev
 */
class JavascriptProcessedClassSource implements ClassHolderSource {
    private ThreadLocal<JavascriptNativeProcessor> processor = new ThreadLocal<>();
    private ClassHolderSource innerSource;
    private List<ClassHolderTransformer> transformers = new ArrayList<>();

    public JavascriptProcessedClassSource(ClassHolderSource innerSource) {
        this.innerSource = innerSource;
    }

    public void addTransformer(ClassHolderTransformer transformer) {
        transformers.add(transformer);
    }

    @Override
    public ClassHolder get(String name) {
        ClassHolder cls = innerSource.get(name);
        if (cls != null) {
            transformClass(cls);
        }
        return cls;
    }

    private void transformClass(ClassHolder cls) {
        JavascriptNativeProcessor processor = getProcessor();
        processor.processClass(cls);
        for (MethodHolder method : cls.getMethods()) {
            if (method.getProgram() != null) {
                processor.processProgram(method.getProgram());
            }
        }
        for (ClassHolderTransformer transformer : transformers) {
            transformer.transformClass(cls);
        }
    }

    private JavascriptNativeProcessor getProcessor() {
        if (processor.get() == null) {
            processor.set(new JavascriptNativeProcessor(innerSource));
        }
        return processor.get();
    }
}
