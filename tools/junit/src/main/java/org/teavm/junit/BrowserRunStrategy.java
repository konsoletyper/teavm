/*
 *  Copyright 2021 Alexey Andreev.
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

import static org.teavm.junit.PropertyNames.JS_DECODE_STACK;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.teavm.browserrunner.BrowserRunDescriptor;
import org.teavm.browserrunner.BrowserRunner;

class BrowserRunStrategy implements TestRunStrategy {
    private File baseDir;
    private BrowserRunner runner;

    BrowserRunStrategy(File baseDir, String type, Function<String, Process> browserRunner) {
        this.baseDir = baseDir;
        runner = new BrowserRunner(baseDir, type, browserRunner,
                Boolean.parseBoolean(System.getProperty(JS_DECODE_STACK, "true")));
    }

    @Override
    public void beforeAll() {
        runner.start();
    }

    @Override
    public void afterAll() {
        runner.stop();
    }

    @Override
    public void runTest(TestRun run) throws IOException {
        var testFile = new File(run.getGroup().getBaseDirectory(), run.getGroup().getFileName());
        var testPath = baseDir.getAbsoluteFile().toPath().relativize(testFile.toPath()).toString();
        var descriptor = new BrowserRunDescriptor(
                run.getGroup().getFileName(),
                "tests/" + testPath,
                run.getGroup().isModule(),
                additionalJs(run).stream().map(p -> "resources/" + p).collect(Collectors.toList()),
                run.getArgument(),
                true
        );

        runner.runTest(descriptor);
    }

    @Override
    public void cleanup() {
        runner.cleanup();
    }

    private Collection<String> additionalJs(TestRun run) {
        var result = new LinkedHashSet<String>();

        var method = run.getMethod();
        var attachAnnot = method.getAnnotation(AttachJavaScript.class);
        if (attachAnnot != null) {
            result.addAll(List.of(attachAnnot.value()));
        }

        var cls = method.getDeclaringClass();
        while (cls != null) {
            var classAttachAnnot = cls.getAnnotation(AttachJavaScript.class);
            if (classAttachAnnot != null) {
                result.addAll(List.of(attachAnnot.value()));
            }
            cls = cls.getSuperclass();
        }

        return result;
    }
}
