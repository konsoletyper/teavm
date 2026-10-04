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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import org.teavm.diagnostics.ProblemSeverity;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.vm.DirectoryBuildTarget;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;
import org.teavm.vm.TeaVMTarget;

/**
 * Compiles benchmarks for a particular TeaVM backend and runs them.
 */
public abstract class BenchmarkBackend {
    protected final BenchmarkEnvironment environment;

    protected BenchmarkBackend(BenchmarkEnvironment environment) {
        this.environment = environment;
    }

    /**
     * Short identifier of the backend, also used in command line.
     */
    public abstract String getName();

    /**
     * Human-readable description, used in reports. Mapped to {@code vmName} field of JMH JSON output.
     */
    public abstract String getDescription();

    public abstract CompiledBenchmark compile(BenchmarkInfo benchmark, File directory) throws BenchmarkException;

    public void start() throws BenchmarkException {
    }

    public void stop() {
    }

    /**
     * Runs compiled benchmark once, i.e. performs a single JMH fork.
     *
     * @param outputConsumer receives every line written by benchmark to stdout.
     */
    public abstract void run(CompiledBenchmark benchmark, String argument, Consumer<String> outputConsumer)
            throws BenchmarkException;

    protected final void build(TeaVMTarget target, String entryPoint, BenchmarkInfo benchmark, File directory,
            String fileName) throws BenchmarkException {
        directory.mkdirs();
        var vm = new TeaVMBuilder(target)
                .setClassLoader(environment.getClassLoader())
                .setClassSource(environment.getClassSource())
                .setResourceProvider(environment.getResourceProvider())
                .setReferenceCache(environment.getReferenceCache())
                .build();
        vm.setOptimizationLevel(environment.getOptimizationLevel());
        new BenchmarkEntryPointTransformer(benchmark).install(vm);
        vm.installPlugins();
        vm.setEntryPoint(entryPoint);
        try {
            vm.build(new DirectoryBuildTarget(directory), fileName);
        } catch (RuntimeException e) {
            throw new BenchmarkException("Error compiling " + benchmark.getName() + " for " + getName(), e);
        }
        checkProblems(vm, benchmark);
    }

    private void checkProblems(TeaVM vm, BenchmarkInfo benchmark) throws BenchmarkException {
        var problems = TeaVMProblemRenderer.render(vm.getDependencyInfo().getCallGraph(), vm.getProblemProvider());
        var sb = new StringBuilder();
        for (var problem : problems) {
            if (problem.getSeverity() == ProblemSeverity.ERROR) {
                sb.append(problem.getText()).append(problem.getStackTrace()).append('\n');
            }
        }
        if (sb.length() > 0) {
            throw new BenchmarkException("Error compiling " + benchmark.getName() + " for " + getName() + ":\n"
                    + sb);
        }
    }

    protected final void copyResource(String resource, File target) throws BenchmarkException {
        try (InputStream input = BenchmarkBackend.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new BenchmarkException("Resource not found on classpath: " + resource);
            }
            Files.copy(input, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BenchmarkException("Error copying resource " + resource, e);
        }
    }
}
