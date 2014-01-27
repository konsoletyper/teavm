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

import org.teavm.common.FiniteExecutor;
import org.teavm.common.SimpleFiniteExecutor;
import org.teavm.model.ClassHolderSource;

/**
 *
 * @author Alexey Andreev
 */
public class JavascriptBuilderFactory {
    ClassHolderSource classSource;
    ClassLoader classLoader;
    FiniteExecutor executor = new SimpleFiniteExecutor();

    public ClassHolderSource getClassSource() {
        return classSource;
    }

    public void setClassSource(ClassHolderSource classSource) {
        this.classSource = classSource;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public FiniteExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(FiniteExecutor executor) {
        this.executor = executor;
    }

    public JavascriptBuilder create() {
        return new JavascriptBuilder(classSource, classLoader, executor);
    }
}
