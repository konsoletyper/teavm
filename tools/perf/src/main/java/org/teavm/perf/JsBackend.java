/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.perf;

import java.io.File;
import org.teavm.backend.javascript.JavaScriptTarget;
import org.teavm.perf.runtime.BenchmarkJsEntryPoint;

public class JsBackend extends BrowserBackend {
    public JsBackend(BenchmarkEnvironment environment, String browser) {
        super(environment, browser);
    }

    @Override
    public String getName() {
        return "js";
    }

    @Override
    public String getDescription() {
        return "TeaVM JavaScript";
    }

    @Override
    protected String getRunnerType() {
        return "JAVASCRIPT";
    }

    @Override
    public CompiledBenchmark compile(BenchmarkInfo benchmark, File directory) throws BenchmarkException {
        var target = new JavaScriptTarget();
        target.setObfuscated(true);
        build(target, BenchmarkJsEntryPoint.class.getName(), benchmark, directory, "benchmark.js");
        return new CompiledBenchmark(benchmark, directory, new File(directory, "benchmark.js"));
    }
}
