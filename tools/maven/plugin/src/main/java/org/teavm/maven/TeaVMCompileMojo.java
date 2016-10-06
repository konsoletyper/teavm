/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.maven;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.tooling.ClassAlias;
import org.teavm.tooling.MethodAlias;
import org.teavm.tooling.RuntimeCopyOperation;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMToolException;
import org.teavm.tooling.sources.DirectorySourceFileProvider;
import org.teavm.tooling.sources.SourceFileProvider;
import org.teavm.vm.TeaVMOptimizationLevel;

@Mojo(name = "compile", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME,
        defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class TeaVMCompileMojo extends AbstractTeaVMMojo {
    @Parameter(defaultValue = "${project.build.directory}/javascript")
    private File targetDirectory;

    @Parameter(defaultValue = "${project.build.sourceDirectory}")
    private File sourceDirectory;

    @Parameter
    private String targetFileName = "";

    @Parameter
    private String mainClass;

    @Parameter
    private boolean mainPageIncluded;

    @Parameter
    private ClassAlias[] classAliases;

    @Parameter
    private MethodAlias[] methodAliases;

    @Parameter
    private boolean stopOnErrors = true;

    @Parameter
    protected RuntimeCopyOperation runtime = RuntimeCopyOperation.SEPARATE;

    @Parameter
    private TeaVMOptimizationLevel optimizationLevel = TeaVMOptimizationLevel.SIMPLE;

    @Parameter
    private TeaVMTargetType targetType = TeaVMTargetType.JAVASCRIPT;

    @Parameter(defaultValue = "${project.build.directory}/teavm-cache")
    protected File cacheDirectory;

    private TeaVMTool tool = new TeaVMTool();

    @Parameter
    private WasmBinaryVersion wasmVersion;

    @Override
    protected File getTargetDirectory() {
        return targetDirectory;
    }

    @Override
    public void execute() throws MojoExecutionException {
        Log log = getLog();
        setupTool(tool);
        tool.setLog(new MavenTeaVMToolLog(log));
        try {
            tool.setMainClass(mainClass);
            tool.setMainPageIncluded(mainPageIncluded);
            tool.setRuntime(runtime);
            if (!targetFileName.isEmpty()) {
                tool.setTargetFileName(targetFileName);
            }
            tool.setOptimizationLevel(optimizationLevel);
            if (classAliases != null) {
                tool.getClassAliases().addAll(Arrays.asList(classAliases));
            }
            if (methodAliases != null) {
                tool.getMethodAliases().addAll(Arrays.asList(methodAliases));
            }
            tool.setCacheDirectory(cacheDirectory);
            tool.setTargetType(targetType);
            tool.setWasmVersion(wasmVersion);
            tool.generate();
            if (stopOnErrors && !tool.getProblemProvider().getSevereProblems().isEmpty()) {
                throw new MojoExecutionException("Build error");
            }
        } catch (RuntimeException e) {
            throw new MojoExecutionException("Unexpected error occured", e);
        } catch (TeaVMToolException e) {
            throw new MojoExecutionException("IO error occured", e);
        }
    }

    @Override
    protected void addSourceProviders(List<SourceFileProvider> providers) {
        providers.add(new DirectorySourceFileProvider(sourceDirectory));
    }
}
