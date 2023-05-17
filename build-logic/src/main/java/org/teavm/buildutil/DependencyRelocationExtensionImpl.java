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
package org.teavm.buildutil;

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.artifacts.component.ProjectComponentSelector;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.attributes.Usage;
import org.gradle.api.initialization.Settings;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.catalog.VersionCatalogPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.jvm.tasks.Jar;

class DependencyRelocationExtensionImpl implements DependencyRelocationExtension {
    private static final String RELOCATE_JAR = "relocateJar";
    private Settings target;
    private Map<String, RelocatedDependency> depsByProjectPath = new LinkedHashMap<>();
    private String artifactIdPrefix;
    private Set<String> skippedProjects = new HashSet<>();

    DependencyRelocationExtensionImpl(Settings target) {
        this.target = target;
        target.getGradle().afterProject(project -> {
            var dependency = depsByProjectPath.get(project.getPath());
            if (dependency != null) {
                setupSynthesizedProject(project, dependency);
            } else {
                setupConsumingProject(project);
            }
        });
    }

    private void setupSynthesizedProject(Project project, RelocatedDependency dependency) {
        project.getPlugins().apply(VersionCatalogPlugin.class);
        project.getPlugins().apply(JavaLibraryPlugin.class);
        project.getPlugins().apply(PublishTeaVMPlugin.class);
        project.setDescription("Relocated " + dependency.alias + " artifact to avoid JAR hell");

        var synthesizedProjects = new HashMap<ModuleIdentifier, String>();
        for (var dep : depsByProjectPath.values()) {
            var lib = project.getExtensions().getByType(VersionCatalogsExtension.class).named(dep.libs)
                    .findLibrary(dep.alias).get().get();
            synthesizedProjects.put(lib.getModule(), dep.projectPath);
        }

        var library = project.getExtensions().getByType(VersionCatalogsExtension.class).named(dependency.libs)
                .findLibrary(dependency.alias).get().get();
        var libraryDep = project.getDependencies().create(library);

        project.getDependencies().add(JavaPlugin.COMPILE_ONLY_API_CONFIGURATION_NAME, library);

        copyLibraryDeps(project, libraryDep, Usage.JAVA_RUNTIME, JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME,
                synthesizedProjects);

        project.setGroup(project.getRootProject().getGroup());
        var prefix = artifactIdPrefix != null ? artifactIdPrefix : project.getRootProject().getName() + "-relocated";
        var fullArtifactId = prefix + "-" + dependency.libs + "-" + dependency.alias;
        project.getExtensions().configure(PublishTeaVMExtension.class, extension -> {
            extension.setArtifactId(fullArtifactId);
        });

        var config = project.getConfigurations().detachedConfiguration(project.getDependencies().create(library));
        config.setTransitive(false);
        setupRelocation(project, config, dependency.relocations);
    }

    private void replaceArtifacts(Project project, String configurationName, Task task) {
        var configuration = project.getConfigurations().getByName(configurationName);
        configuration.getArtifacts().clear();
        project.getArtifacts().add(configurationName, task);
    }

    private void copyLibraryDeps(Project project, Dependency dependency, String usage, String targetConfig,
            Map<ModuleIdentifier, String> synthesizedProjects) {
        var detachedConfig = project.getConfigurations().detachedConfiguration(
                project.getDependencies().create(dependency));
        detachedConfig.setTransitive(true);
        detachedConfig.getAttributes().attribute(Usage.USAGE_ATTRIBUTE,
                project.getObjects().named(Usage.class, usage));
        var root = detachedConfig.getIncoming().getResolutionResult().getRoot().getDependencies().iterator().next();
        if (root instanceof ResolvedDependencyResult) {
            var rootComponent = ((ResolvedDependencyResult) root).getSelected();
            for (var dep : rootComponent.getDependencies()) {
                var requested = dep.getRequested();
                if (requested instanceof ModuleComponentSelector) {
                    var moduleDep = (ModuleComponentSelector) requested;
                    var replacement = synthesizedProjects.get(moduleDep.getModuleIdentifier());
                    if (replacement != null) {
                        var notation = new HashMap<String, String>();
                        notation.put("path", replacement);
                        project.getDependencies().add(targetConfig,
                                project.getDependencies().project(notation));
                    } else {
                        project.getDependencies().add(targetConfig,
                                moduleDep.getGroup() + ":" + moduleDep.getModule() + ":" + moduleDep.getVersion());
                    }
                } else if (requested instanceof ProjectComponentSelector) {
                    var projectDep = (ProjectComponentSelector) requested;
                    var notation = new HashMap<String, String>();
                    notation.put("path", projectDep.getProjectPath());
                    project.getDependencies().add(targetConfig, project.getDependencies().project(notation));
                }
            }
        }
    }

    private void setupConsumingProject(Project project) {
        if (skippedProjects.contains(project.getPath())
                || project.getExtensions().findByType(JavaPluginExtension.class) == null) {
            return;
        }

        var relocations = new ArrayList<Relocation>();
        var depsByLibrary = new LinkedHashMap<Dependency, RelocatedDependency>();
        for (var dependency : depsByProjectPath.values()) {
            var library = project.getExtensions().getByType(VersionCatalogsExtension.class).named(dependency.libs)
                    .findLibrary(dependency.alias).get().get();
            if (library != null) {
                depsByLibrary.put(project.getDependencies().create(library), dependency);
            }
            relocations.addAll(dependency.relocations);
        }

        replaceDependencies(
                project,
                JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
                Arrays.asList(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME),
                Arrays.asList(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME,
                        JavaPlugin.TEST_COMPILE_ONLY_CONFIGURATION_NAME,
                        JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME),
                depsByLibrary
        );

        var sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
        var main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        var sourceDirs = main.getOutput();
        setupRelocation(project, sourceDirs, relocations);
    }

    private void replaceDependencies(Project project, String configurationName,
            List<String> runtimeConfigurations, List<String> compileConfigurations,
            Map<Dependency, RelocatedDependency> depsByLibrary) {
        var config = project.getConfigurations().findByName(configurationName);
        if (config == null) {
            return;
        }
        for (var iterator = config.getDependencies().iterator(); iterator.hasNext(); ) {
            var dependency = iterator.next();
            var relocatedDep = depsByLibrary.get(dependency);
            if (relocatedDep != null) {
                var projectNotation = new HashMap<String, String>();
                projectNotation.put("path", relocatedDep.projectPath);
                for (var runtimeConfiguration : runtimeConfigurations) {
                    if (project.getConfigurations().findByName(runtimeConfiguration) != null) {
                        project.getDependencies().add(runtimeConfiguration,
                                project.getDependencies().project(projectNotation));
                    }
                }
                for (var compileConfiguration : compileConfigurations) {
                    project.getDependencies().add(compileConfiguration, dependency);
                }
                iterator.remove();
            }
        }
    }

    private void setupRelocation(Project project, Object input, List<Relocation> relocations) {
        if (project.getTasks().findByPath(JavaPlugin.JAR_TASK_NAME) == null) {
            return;
        }
        project.getTasks().named(JavaPlugin.JAR_TASK_NAME, Jar.class, jar -> {
            jar.getArchiveClassifier().set("original");
        });
        var relocateTask = project.getTasks().create(RELOCATE_JAR, ShadowJar.class, task -> {
            task.getArchiveClassifier().set("");
            task.from(input);
            for (var relocation : relocations) {
                task.relocate(relocation.src, relocation.dest);
            }
            project.getTasks().getByName("assemble").dependsOn(task);
        });
        project.getTasks().withType(AbstractArchiveTask.class, archive -> {
            archive.setPreserveFileTimestamps(false);
            archive.setReproducibleFileOrder(true);
        });

        replaceArtifacts(project, JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME, relocateTask);
        replaceArtifacts(project, JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME, relocateTask);
    }

    @Override
    public void library(String libs, String alias, Action<DependencyRelocationLibrary> action) {
        var projectName = "relocated:" + libs + ":" + alias;
        var projectPath = ":" + projectName;
        target.include(projectName);
        target.project(projectPath).setProjectDir(new File(target.getRootDir(), "relocated/" + libs + "/" + alias));
        var dependency = new RelocatedDependency(projectPath, libs, alias);
        depsByProjectPath.put(projectPath, dependency);
        action.execute((src, dest) -> {
            dependency.relocations.add(new Relocation(src, dest));
        });
    }

    @Override
    public String getArtifactIdPrefix() {
        return artifactIdPrefix;
    }

    @Override
    public void setArtifactIdPrefix(String prefix) {
        artifactIdPrefix = prefix;
    }

    @Override
    public void skip(String project) {
        skippedProjects.add(project);
    }

    static class RelocatedDependency {
        String projectPath;
        String libs;
        String alias;
        List<Relocation> relocations = new ArrayList<>();

        RelocatedDependency(String projectPath, String libs, String alias) {
            this.projectPath = projectPath;
            this.libs = libs;
            this.alias = alias;
        }
    }

    static class Relocation {
        String src;
        String dest;

        Relocation(String src, String dest) {
            this.src = src;
            this.dest = dest;
        }
    }
}
