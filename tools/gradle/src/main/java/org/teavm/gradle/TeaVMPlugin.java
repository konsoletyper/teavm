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
package org.teavm.gradle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.bundling.War;
import org.teavm.gradle.api.TeaVMConfiguration;
import org.teavm.gradle.api.TeaVMExtension;
import org.teavm.gradle.config.ArtifactCoordinates;
import org.teavm.gradle.tasks.GenerateCTask;
import org.teavm.gradle.tasks.GenerateJavaScriptTask;
import org.teavm.gradle.tasks.GenerateWasiTask;
import org.teavm.gradle.tasks.GenerateWasmTask;
import org.teavm.gradle.tasks.TeaVMTask;

public class TeaVMPlugin implements Plugin<Project> {
    public static final String EXTENSION_NAME = "teavm";
    public static final String SOURCE_SET_NAME = "teavm";
    public static final String JS_TASK_NAME = "generateJavaScript";
    public static final String WASM_TASK_NAME = "generateWasm";
    public static final String WASI_TASK_NAME = "generateWasi";
    public static final String C_TASK_NAME = "generateC";
    public static final String CONFIGURATION_NAME = "teavm";
    public static final String CLASSPATH_CONFIGURATION_NAME = "teavmClasspath";
    private ObjectFactory objectFactory;

    @Inject
    public TeaVMPlugin(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }

    @Override
    public void apply(Project project) {
        registerExtension(project);
        registerConfiguration(project);
        registerSourceSet(project);
        registerTasks(project);
        addDependencies(project);
        setupWarTask(project);
        TeaVMTestConfigurator.configure(project, project.getExtensions().getByType(TeaVMExtension.class).getTests());
    }

    private void registerExtension(Project project) {
        var extension = new TeaVMExtensionImpl(project, objectFactory);
        project.getExtensions().add(TeaVMExtension.class, EXTENSION_NAME, extension);
    }

    private void registerConfiguration(Project project) {
        var teavm = project.getConfigurations().create(CONFIGURATION_NAME);
        var teavmClasspath = project.getConfigurations().create(CLASSPATH_CONFIGURATION_NAME);
        project.getConfigurations().configureEach(config -> {
            if (config.getName().equals(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)) {
                teavmClasspath.extendsFrom(config);
            }
            if (config.getName().equals(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME)) {
                config.extendsFrom(teavm);
            }
        });
        teavmClasspath.extendsFrom(teavm);
    }

    private void registerSourceSet(Project project) {
        var sourceSets = project.getExtensions().findByType(SourceSetContainer.class);
        if (sourceSets != null) {
            var sourceSet = sourceSets.create(SOURCE_SET_NAME);
            var main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput();
            sourceSet.setRuntimeClasspath(sourceSet.getRuntimeClasspath().plus(main)
                    .plus(project.getConfigurations().getByName(CLASSPATH_CONFIGURATION_NAME)));
            sourceSet.setCompileClasspath(sourceSet.getCompileClasspath().plus(main)
                    .plus(project.getConfigurations().getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)));
            sourceSet.java(java -> { });
            project.getDependencies().add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, sourceSet.getOutput());
        }
    }

    private void registerTasks(Project project) {
        var compilerConfig = project.getConfigurations().detachedConfiguration(
                project.getDependencies().create(ArtifactCoordinates.TOOLS));
        registerJsTask(project, compilerConfig);
        registerWasmTask(project, compilerConfig);
        registerWasiTask(project, compilerConfig);
        registerCTask(project, compilerConfig);
    }

    private void registerJsTask(Project project, Configuration configuration) {
        var extension = project.getExtensions().getByType(TeaVMExtension.class);
        project.getTasks().create(JS_TASK_NAME, GenerateJavaScriptTask.class, task -> {
            var js = extension.getJs();
            applyToTask(js, task, configuration);
            task.getObfuscated().convention(js.getObfuscated());
            task.getSourceMap().convention(js.getSourceMap());
            task.getTargetFileName().convention(js.getTargetFileName());
            task.getStrict().convention(js.getStrict());
            task.getEntryPointName().convention(js.getEntryPointName());
            task.getSourceFilePolicy().convention(js.getSourceFilePolicy());

            task.getSourceFiles().from(project.provider(() -> {
                var result = new ArrayList<File>();
                addSourceDirs(project, result);
                return result;
            }));
            task.getSourceFiles().from(project.provider(() -> {
                var dependencies = project.getConfigurations()
                        .getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)
                        .getIncoming()
                        .getResolutionResult()
                        .getAllDependencies();

                var result = new ArrayList<File>();
                for (var dependencyResult : dependencies) {
                    if (!(dependencyResult instanceof ResolvedDependencyResult)) {
                        continue;
                    }
                    var id = ((ResolvedDependencyResult) dependencyResult).getSelected().getId();
                    if (id instanceof ProjectComponentIdentifier) {
                        var path = ((ProjectComponentIdentifier) id).getProjectPath();
                        var refProject = project.getRootProject().findProject(path);
                        if (refProject != null) {
                            addSourceDirs(refProject, result);
                        }
                    } else if (id instanceof ModuleComponentIdentifier) {
                        var moduleId = (ModuleComponentIdentifier) id;
                        var sourcesDep = project.getDependencies().create(Map.of(
                                "group", moduleId.getGroup(),
                                "name", moduleId.getModuleIdentifier().getName(),
                                "version", moduleId.getVersion(),
                                "classifier", "sources"
                        ));
                        var tmpConfig = project.getConfigurations().detachedConfiguration(sourcesDep);
                        tmpConfig.setTransitive(false);
                        if (!tmpConfig.getResolvedConfiguration().hasError()) {
                            result.addAll(tmpConfig.getResolvedConfiguration().getLenientConfiguration().getFiles());
                        }
                    }
                }
                return result;
            }));
        });
    }

    private void addSourceDirs(Project project, List<File> result) {
        var sourceSets = project.getExtensions().findByType(SourceSetContainer.class);
        if (sourceSets != null) {
            for (var sourceSet : sourceSets) {
                result.addAll(sourceSet.getAllJava().getSourceDirectories().getFiles());
            }
        }
    }

    private void registerWasmTask(Project project, Configuration configuration) {
        var extension = project.getExtensions().getByType(TeaVMExtension.class);
        project.getTasks().create(WASM_TASK_NAME, GenerateWasmTask.class, task -> {
            var wasm = extension.getWasm();
            applyToTask(wasm, task, configuration);
            task.getTargetFileName().convention(wasm.getTargetFileName());
            task.getMinHeapSize().convention(wasm.getMinHeapSize());
            task.getMaxHeapSize().convention(wasm.getMaxHeapSize());
        });
    }

    private void registerWasiTask(Project project, Configuration configuration) {
        var extension = project.getExtensions().getByType(TeaVMExtension.class);
        project.getTasks().create(WASI_TASK_NAME, GenerateWasiTask.class, task -> {
            var wasi = extension.getWasi();
            applyToTask(wasi, task, configuration);
            task.getTargetFileName().convention(wasi.getTargetFileName());
            task.getMinHeapSize().convention(wasi.getMinHeapSize());
            task.getMaxHeapSize().convention(wasi.getMaxHeapSize());
        });
    }

    private void registerCTask(Project project, Configuration configuration) {
        var extension = project.getExtensions().getByType(TeaVMExtension.class);
        project.getTasks().create(C_TASK_NAME, GenerateCTask.class, task -> {
            var c = extension.getC();
            applyToTask(c, task, configuration);
            task.getMinHeapSize().convention(c.getMinHeapSize());
            task.getMaxHeapSize().convention(c.getMaxHeapSize());
            task.getHeapDump().convention(c.getHeapDump());
            task.getShortFileNames().convention(c.getShortFileNames());
            task.getObfuscated().convention(c.getObfuscated());
        });
    }

    private void addDependencies(Project project) {
        project.getDependencies().add(CONFIGURATION_NAME, ArtifactCoordinates.CLASSLIB);
        project.getDependencies().add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, ArtifactCoordinates.JUNIT);
        project.getDependencies().add(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME, ArtifactCoordinates.CLASSLIB);
    }

    private void setupWarTask(Project project) {
        var extension = project.getExtensions().getByType(TeaVMExtension.class);

        project.getTasks().withType(War.class).configureEach(task -> {
            if (task.getName().equals(WarPlugin.WAR_TASK_NAME)) {
                var jsAddedToWebApp = extension.getJs().getAddedToWebApp().get();
                var wasmAddedToWebApp = extension.getWasm().getAddedToWebApp().get();
                if (jsAddedToWebApp) {
                    task.dependsOn(project.getTasks().named(JS_TASK_NAME));
                    var outDir = extension.getJs().getOutputDir();
                    var relPath = extension.getJs().getRelativePathInOutputDir();
                    task.with(project.copySpec(spec -> {
                        spec.into(relPath);
                        spec.from(project.files(outDir.map(dir -> new File(dir, relPath.get()))));
                        spec.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
                    }));
                }
                if (wasmAddedToWebApp) {
                    task.dependsOn(project.getTasks().named(WASM_TASK_NAME));
                    var outDir = extension.getWasm().getOutputDir();
                    var relPath = extension.getWasm().getRelativePathInOutputDir();
                    task.with(project.copySpec(spec -> {
                        spec.into(relPath);
                        spec.from(project.files(outDir.map(dir -> new File(dir, relPath.get()))));
                    }));
                }
            }
        });
    }

    private void applyToTask(TeaVMConfiguration configuration, TeaVMTask task, Configuration toolsConfiguration) {
        task.getMainClass().convention(configuration.getMainClass());
        task.getClasspath().from(task.getProject().getConfigurations().getByName(CLASSPATH_CONFIGURATION_NAME));
        task.getPreservedClasses().addAll(configuration.getPreservedClasses());
        task.getDebugInformation().convention(configuration.getDebugInformation());
        task.getFastGlobalAnalysis().convention(configuration.getFastGlobalAnalysis());
        task.getOptimization().convention(configuration.getOptimization());
        task.getOutOfProcess().convention(configuration.getOutOfProcess());
        task.getProcessMemory().convention(configuration.getProcessMemory());
        task.getProperties().putAll(configuration.getProperties());
        task.getDaemonClasspath().from(toolsConfiguration);
        task.getOutputDir().convention(configuration.getOutputDir().map(
                d -> new File(d, configuration.getRelativePathInOutputDir().get())));

        var project = task.getProject();

        var sourceSets = project.getExtensions().findByType(SourceSetContainer.class);
        if (sourceSets != null) {
            task.getClasspath().from(sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput());
            task.getClasspath().from(sourceSets.getByName(SOURCE_SET_NAME).getOutput());
        }
    }
}
