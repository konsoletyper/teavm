/*
 *  Copyright 2016 Alexey Andreev.
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
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.runner.Description;
import org.teavm.model.MethodReference;

class TestRun {
    private File baseDirectory;
    private Method method;
    private MethodReference reference;
    private Description description;
    private TestRunCallback callback;
    private Set<Class<?>> expectedExceptions;

    TestRun(File baseDirectory, Method method, MethodReference reference, Description description,
            TestRunCallback callback, Set<Class<?>> expectedExceptions) {
        this.baseDirectory = baseDirectory;
        this.method = method;
        this.reference = reference;
        this.description = description;
        this.callback = callback;
        this.expectedExceptions = Collections.unmodifiableSet(new HashSet<>(expectedExceptions));
    }

    public File getBaseDirectory() {
        return baseDirectory;
    }

    public Method getMethod() {
        return method;
    }

    public MethodReference getReference() {
        return reference;
    }

    public Description getDescription() {
        return description;
    }

    public TestRunCallback getCallback() {
        return callback;
    }

    public Set<Class<?>> getExpectedExceptions() {
        return expectedExceptions;
    }
}
