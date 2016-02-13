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
package org.teavm.idea.jps;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.ModuleChunk;
import org.jetbrains.jps.builders.DirtyFilesHolder;
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor;
import org.jetbrains.jps.incremental.BuilderCategory;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.ModuleBuildTarget;
import org.jetbrains.jps.incremental.ModuleLevelBuilder;
import org.jetbrains.jps.incremental.ProjectBuildException;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsOrderRootType;
import org.jetbrains.jps.model.module.JpsDependencyElement;
import org.jetbrains.jps.model.module.JpsLibraryDependency;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleDependency;
import org.teavm.idea.jps.model.TeaVMJpsConfiguration;

public class TeaVMBuilder extends ModuleLevelBuilder {
    public TeaVMBuilder() {
        super(BuilderCategory.CLASS_POST_PROCESSOR);
    }

    @Override
    public ExitCode build(CompileContext context, ModuleChunk chunk,
            DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget> dirtyFilesHolder,
            OutputConsumer outputConsumer) throws ProjectBuildException, IOException {
        for (JpsModule module : chunk.getModules()) {
            buildModule(module);
        }
        return ExitCode.OK;
    }

    private void buildModule(JpsModule module) {
        TeaVMJpsConfiguration config = TeaVMJpsConfiguration.get(module);
        if (config == null || !config.isEnabled()) {
            return;
        }
        Set<String> classPathEntries = new HashSet<>();
        buildClassPath(module, new HashSet<>(), classPathEntries);
        System.out.println(classPathEntries.stream().collect(Collectors.joining(":")));
    }

    private void buildClassPath(JpsModule module, Set<JpsModule> visited, Set<String> classPathEntries) {
        if (!visited.add(module)) {
            return;
        }
        File output = JpsJavaExtensionService.getInstance().getOutputDirectory(module, false);
        if (output != null) {
            classPathEntries.add(output.getPath());
        }
        for (JpsDependencyElement dependency : module.getDependenciesList().getDependencies()) {
            if (dependency instanceof JpsModuleDependency) {
                buildClassPath(((JpsModuleDependency) dependency).getModule(), visited, classPathEntries);
            } else if (dependency instanceof JpsLibraryDependency) {
                JpsLibrary library = ((JpsLibraryDependency) dependency).getLibrary();
                if (library == null) {
                    continue;
                }
                classPathEntries.addAll(library.getFiles(JpsOrderRootType.COMPILED).stream().map(File::getPath)
                        .collect(Collectors.toList()));
            }
        }
    }

    @NotNull
    @Override
    public String getPresentableName() {
        return "TeaVM builder";
    }
}
