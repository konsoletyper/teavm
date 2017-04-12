/*
 *  Copyright 2017 Alexey Andreev.
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.builders.BuildRootDescriptor;
import org.jetbrains.jps.builders.BuildRootIndex;
import org.jetbrains.jps.builders.BuildTarget;
import org.jetbrains.jps.builders.BuildTargetRegistry;
import org.jetbrains.jps.builders.ModuleBasedTarget;
import org.jetbrains.jps.builders.TargetOutputIndex;
import org.jetbrains.jps.builders.java.JavaModuleBuildTargetType;
import org.jetbrains.jps.builders.storage.BuildDataPaths;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.ModuleBuildTarget;
import org.jetbrains.jps.indices.IgnoredFileIndex;
import org.jetbrains.jps.indices.ModuleExcludeIndex;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.module.JpsModule;
import org.teavm.idea.jps.model.TeaVMJpsConfiguration;

public class TeaVMBuildTarget extends ModuleBasedTarget<BuildRootDescriptor> {
    private TeaVMJpsConfiguration configuration;

    public TeaVMBuildTarget(TeaVMBuildTargetType targetType, @NotNull JpsModule module,
            @NotNull TeaVMJpsConfiguration configuration) {
        super(targetType, module);
        this.configuration = configuration;
    }

    @Override
    public boolean isTests() {
        return false;
    }

    @Override
    public String getId() {
        return getModule().getName() + "-" + configuration.getTargetType().name();
    }

    @Override
    public Collection<BuildTarget<?>> computeDependencies(BuildTargetRegistry targetRegistry,
            TargetOutputIndex outputIndex) {
        return Collections.singleton(new ModuleBuildTarget(myModule, JavaModuleBuildTargetType.PRODUCTION));
    }

    @NotNull
    @Override
    public List<BuildRootDescriptor> computeRootDescriptors(JpsModel model, ModuleExcludeIndex index,
            IgnoredFileIndex ignoredFileIndex, BuildDataPaths dataPaths) {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public BuildRootDescriptor findRootDescriptor(String rootId, BuildRootIndex rootIndex) {
        return null;
    }

    @NotNull
    @Override
    public String getPresentableName() {
        return "TeaVM configuration '" + getModule().getName() + "' - " + configuration.getTargetType();
    }

    @NotNull
    @Override
    public Collection<File> getOutputRoots(CompileContext context) {
        return Collections.singleton(new File(configuration.getTargetDirectory()));
    }

    public TeaVMJpsConfiguration getConfiguration() {
        return configuration;
    }
}
