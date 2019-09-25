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
import org.junit.runner.Description;

class TestRun {
    private File baseDirectory;
    private Method method;
    private Description description;
    private String fileName;
    private RunKind kind;
    private TestRunCallback callback;

    TestRun(File baseDirectory, Method method, Description description, String fileName, RunKind kind,
            TestRunCallback callback) {
        this.baseDirectory = baseDirectory;
        this.method = method;
        this.description = description;
        this.fileName = fileName;
        this.kind = kind;
        this.callback = callback;
    }

    public File getBaseDirectory() {
        return baseDirectory;
    }

    public Method getMethod() {
        return method;
    }

    public Description getDescription() {
        return description;
    }

    public String getFileName() {
        return fileName;
    }

    public RunKind getKind() {
        return kind;
    }

    public TestRunCallback getCallback() {
        return callback;
    }
}
