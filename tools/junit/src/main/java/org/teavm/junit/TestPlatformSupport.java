/*
 *  Copyright 2023 Alexey Andreev.
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

import static org.teavm.junit.TestUtil.resourceToFile;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.teavm.callgraph.CallGraph;
import org.teavm.dependency.DependencyAnalyzerFactory;
import org.teavm.dependency.PreciseDependencyAnalyzer;
import org.teavm.diagnostics.DefaultProblemTextConsumer;
import org.teavm.diagnostics.Problem;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ReferenceCache;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.vm.DirectoryBuildTarget;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;
import org.teavm.vm.TeaVMTarget;

abstract class TestPlatformSupport<T extends TeaVMTarget> {
    private ClassHolderSource classSource;
    private ReferenceCache referenceCache;

    TestPlatformSupport(ClassHolderSource classSource, ReferenceCache referenceCache) {
        this.classSource = classSource;
        this.referenceCache = referenceCache;
    }

    abstract boolean isEnabled();

    abstract TestRunStrategy createRunStrategy(File outputDir);

    abstract TestPlatform getPlatform();

    abstract String getPath();

    abstract String getExtension();

    abstract List<TeaVMTestConfiguration<T>> getConfigurations();

    abstract CompileResult compile(Consumer<TeaVM> additionalProcessing, String baseName,
            TeaVMTestConfiguration<T> configuration, File path);

    abstract boolean usesFileName();

    CompileResult compile(TeaVMTestConfiguration<T> configuration,
            Supplier<T> targetSupplier, String entryPoint, File path, String extension,
            CompilePostProcessor postBuild, Consumer<TeaVM> additionalProcessing, String baseName) {
        CompileResult result = new CompileResult();

        File outputFile = getOutputFile(path, baseName, configuration.getSuffix(), extension);
        result.file = outputFile;

        ClassLoader classLoader = TeaVMTestRunner.class.getClassLoader();

        var target = targetSupplier.get();
        configuration.apply(target);

        DependencyAnalyzerFactory dependencyAnalyzerFactory = PreciseDependencyAnalyzer::new;

        try {
            TeaVM vm = new TeaVMBuilder(target)
                    .setClassLoader(classLoader)
                    .setClassSource(classSource)
                    .setReferenceCache(referenceCache)
                    .setDependencyAnalyzerFactory(dependencyAnalyzerFactory)
                    .build();

            configuration.apply(vm);
            additionalProcessing.accept(vm);
            vm.installPlugins();

            new TestExceptionPlugin().install(vm);

            vm.entryPoint(entryPoint);

            if (usesFileName()) {
                if (!outputFile.getParentFile().exists()) {
                    outputFile.getParentFile().mkdirs();
                }
                vm.build(new DirectoryBuildTarget(outputFile.getParentFile()), outputFile.getName());
            } else {
                outputFile.getParentFile().mkdirs();
                vm.build(new DirectoryBuildTarget(outputFile), "");
            }
            if (!vm.getProblemProvider().getProblems().isEmpty()) {
                result.success = false;
                result.errorMessage = buildErrorMessage(vm);
            } else {
                if (postBuild != null) {
                    postBuild.process(vm, outputFile);
                }
            }

            return result;
        } catch (Exception e) {
            result = new CompileResult();
            result.success = false;
            result.throwable = e;
            return result;
        }
    }

    private File getOutputFile(File path, String baseName, String suffix, String extension) {
        StringBuilder simpleName = new StringBuilder();
        simpleName.append(baseName);
        if (!suffix.isEmpty()) {
            simpleName.append('-').append(suffix);
        }
        File outputFile;
        simpleName.append(extension);
        outputFile = new File(path, simpleName.toString());

        return outputFile;
    }

    private String buildErrorMessage(TeaVM vm) {
        CallGraph cg = vm.getDependencyInfo().getCallGraph();
        DefaultProblemTextConsumer consumer = new DefaultProblemTextConsumer();
        StringBuilder sb = new StringBuilder();
        for (Problem problem : vm.getProblemProvider().getProblems()) {
            consumer.clear();
            problem.render(consumer);
            sb.append(consumer.getText());
            TeaVMProblemRenderer.renderCallStack(cg, problem.getLocation(), sb);
            sb.append("\n");
        }
        return sb.toString();
    }

    void additionalOutput(File outputPath, File outputPathForMethod, TeaVMTestConfiguration<?> configuration,
            MethodReference reference) {
    }

    void additionalSingleTestOutput(File outputPathForMethod, TeaVMTestConfiguration<?> configuration,
            MethodReference reference) {
    }

    protected final void htmlOutput(File outputPath, File outputPathForMethod, TeaVMTestConfiguration<?> configuration,
            MethodReference reference, String template) {
        var testPath = getOutputFile(outputPath, "classTest", configuration.getSuffix(), getExtension());
        var htmlPath = getOutputFile(outputPathForMethod, "test", configuration.getSuffix(), ".html");
        var properties = Map.of(
                "SCRIPT", "../" + testPath.getName(),
                "IDENTIFIER", reference.toString()
        );
        try {
            resourceToFile(template, htmlPath, properties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected final void htmlSingleTestOutput(File outputPathForMethod, TeaVMTestConfiguration<?> configuration,
            String template) {
        File testPath = getOutputFile(outputPathForMethod, "test", configuration.getSuffix(), getExtension());
        File htmlPath = getOutputFile(outputPathForMethod, "test", configuration.getSuffix(), ".html");
        var properties = Map.of(
                "SCRIPT", testPath.getName(),
                "IDENTIFIER", ""
        );

        try {
            resourceToFile(template, htmlPath, properties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
