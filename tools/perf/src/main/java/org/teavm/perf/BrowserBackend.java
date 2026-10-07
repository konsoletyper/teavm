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
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.teavm.browserrunner.BrowserRunDescriptor;
import org.teavm.browserrunner.BrowserRunner;

/**
 * Runs benchmarks in a browser using {@link BrowserRunner}. Every fork runs in a fresh iframe.
 */
public abstract class BrowserBackend extends BenchmarkBackend {
    private final String browser;
    private BrowserRunner runner;

    protected BrowserBackend(BenchmarkEnvironment environment, String browser) {
        super(environment);
        this.browser = browser;
    }

    protected abstract String getRunnerType();

    @Override
    public void start() throws BenchmarkException {
        var browserFactory = BrowserRunner.pickBrowser(browser);
        if (browserFactory == null) {
            throw new BenchmarkException("Browser must be specified to run benchmarks for " + getName());
        }
        runner = new BrowserRunner(environment.getOutputDir(), getRunnerType(), browserFactory, false);
        if (environment.isCpuProfiling() && supportsCpuProfiling()) {
            runner.enableDevTools();
        }
        try {
            runner.start();
        } catch (RuntimeException e) {
            runner.stop();
            runner = null;
            throw new BenchmarkException("Could not start browser: " + e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        if (runner != null) {
            runner.stop();
            runner = null;
        }
    }

    @Override
    public boolean supportsCpuProfiling() {
        // Profiles are collected via Chrome DevTools protocol
        return browser.equals("browser-chrome");
    }

    @Override
    public String takeCpuProfile(String title) throws BenchmarkException {
        try {
            return runner.takeConsoleProfile(title, 60, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new BenchmarkException("CPU profile was not received from " + getName());
        } catch (RuntimeException e) {
            throw new BenchmarkException("Error receiving CPU profile from " + getName() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void run(CompiledBenchmark benchmark, String argument, Consumer<String> outputConsumer)
            throws BenchmarkException {
        var baseDir = environment.getOutputDir().getAbsoluteFile().toPath();
        var path = baseDir.relativize(benchmark.getFile().getAbsoluteFile().toPath()).toString()
                .replace(File.separatorChar, '/');
        var descriptor = new BrowserRunDescriptor(benchmark.getBenchmark().getName(), "tests/" + path, false,
                List.of(), argument, false);
        try {
            runner.runTest(descriptor, (stderr, line) -> {
                if (stderr) {
                    System.err.println(line);
                } else {
                    outputConsumer.accept(line);
                }
            });
        } catch (IOException | RuntimeException e) {
            throw new BenchmarkException("Error running " + benchmark.getBenchmark().getName() + " on "
                    + getName() + ": " + e.getMessage(), e);
        }
    }
}
