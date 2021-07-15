/*
 *  Copyright 2016 MJ.
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

import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.DependencyResolutionListener;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.teavm.gradle.extension.TeaVMExtension;
import org.teavm.gradle.task.CompileTask;

/** Gradle plugin for TeaVM applications.
 * <p>
 * Adds "teavm" dependency configuration for TeaVM sources to copy. Adds "teavm" extension with plugin's settings. Adds
 * "compileTeaVM" task that compiles the sources.
 *
 * @author MJ */
public class TeaVMPlugin implements Plugin<Project> {
    /** Name of the configuration added by the compilation task to the application's sources. Should reference only
     * source dependencies. */
    public static final String TEAVM_CONFIGURATION = "teavm";
    /** Name of the extension which contains plugin's settings.
     *
     * @see TeaVMExtension */
    public static final String TEAVM_EXTENSION = "teavm";

    @Override
    public void apply(final Project project) {
        applyPlugin(project, "java");
        applyPlugin(project, "application");
        project.getConfigurations().create(TEAVM_CONFIGURATION);
        project.getExtensions().create(TEAVM_EXTENSION, TeaVMExtension.class);
        addDependencies(project);
        addTasks(project);
    }

    private static void applyPlugin(final Project project, final String pluginName) {
        final Map<String, String> plugin = new HashMap<String, String>();
        plugin.put("plugin", pluginName);
        project.apply(plugin);
    }

    private static void addDependencies(final Project project) {
        final DependencySet compileDependencies = project.getConfigurations().getByName("compile").getDependencies();
        final DependencySet sourceDependencies = project.getConfigurations().getByName(TEAVM_CONFIGURATION)
                .getDependencies();
        project.getGradle().addListener(new DependencyResolutionListener() {
            @Override
            public void beforeResolve(final ResolvableDependencies resolvableDependencies) {
                final DependencyHandler handler = project.getDependencies();
                final TeaVMExtension extension = project.getExtensions().getByType(TeaVMExtension.class);
                if (!extension.isIncludeDependencies()) {
                    return;
                }
                final String version = extension.getVersion();
                compileDependencies.add(handler.create("org.teavm:teavm-classlib:" + version));
                compileDependencies.add(handler.create("org.teavm:teavm-jso:" + version));
                compileDependencies.add(handler.create("org.teavm:teavm-jso-apis:" + version));
                sourceDependencies.add(handler.create("org.teavm:teavm-platform:" + version + ":sources"));
                sourceDependencies.add(handler.create("org.teavm:teavm-classlib:" + version + ":sources"));
                sourceDependencies.add(handler.create("org.teavm:teavm-jso:" + version + ":sources"));
                sourceDependencies.add(handler.create("org.teavm:teavm-jso-apis:" + version + ":sources"));
                project.getGradle().removeListener(this);
            }

            @Override
            public void afterResolve(final ResolvableDependencies resolvableDependencies) {
            }
        });
    }

    private static void addTasks(final Project project) {
        final Map<String, Object> taskData = new HashMap<String, Object>();
        taskData.put(Task.TASK_TYPE, CompileTask.class);
        taskData.put(Task.TASK_DEPENDS_ON, "build");
        taskData.put(Task.TASK_DESCRIPTION, "Compiles TeaVM application.");
        taskData.put(Task.TASK_GROUP, "build");
        project.task(taskData, "compileTeaVM");
    }
}
