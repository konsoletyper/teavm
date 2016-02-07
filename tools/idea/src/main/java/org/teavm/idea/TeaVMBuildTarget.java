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
package org.teavm.idea;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.builders.BuildRootIndex;
import org.jetbrains.jps.builders.BuildTarget;
import org.jetbrains.jps.builders.BuildTargetRegistry;
import org.jetbrains.jps.builders.ModuleBasedTarget;
import org.jetbrains.jps.builders.TargetOutputIndex;
import org.jetbrains.jps.builders.storage.BuildDataPaths;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.artifacts.instructions.ArtifactRootDescriptor;
import org.jetbrains.jps.indices.IgnoredFileIndex;
import org.jetbrains.jps.indices.ModuleExcludeIndex;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.module.JpsModule;

public class TeaVMBuildTarget extends ModuleBasedTarget<ArtifactRootDescriptor> {
    TeaVMBuildTarget(TeaVMBuildTargetType targetType, @NotNull JpsModule module) {
        super(targetType, module);
    }

    @Override
    public boolean isTests() {
        return false;
    }

    @Override
    public String getId() {
        return getModule().getName() + "_teavm";
    }

    @Override
    public Collection<BuildTarget<?>> computeDependencies(BuildTargetRegistry targetRegistry,
            TargetOutputIndex outputIndex) {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<ArtifactRootDescriptor> computeRootDescriptors(JpsModel model, ModuleExcludeIndex index,
            IgnoredFileIndex ignoredFileIndex, BuildDataPaths dataPaths) {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public ArtifactRootDescriptor findRootDescriptor(String rootId, BuildRootIndex rootIndex) {
        return null;
    }

    @NotNull
    @Override
    public String getPresentableName() {
        return getModule().getName() + " (TeaVM)";
    }

    @NotNull
    @Override
    public Collection<File> getOutputRoots(CompileContext context) {
        TeaVMModuleExtension extension = getModule().getContainer().getChild(TeaVMModuleExtension.ROLE);
        if (extension == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(new File(extension.getTargetDirectory()));
    }
}
