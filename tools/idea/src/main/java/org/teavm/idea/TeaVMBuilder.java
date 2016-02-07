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

import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.ModuleChunk;
import org.jetbrains.jps.builders.DirtyFilesHolder;
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor;
import org.jetbrains.jps.incremental.BuilderCategory;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.ModuleBuildTarget;
import org.jetbrains.jps.incremental.ModuleLevelBuilder;
import org.jetbrains.jps.incremental.ProjectBuildException;

public class TeaVMBuilder extends ModuleLevelBuilder {
    public TeaVMBuilder(BuilderCategory category) {
        super(category);
    }

    @Override
    public ExitCode build(CompileContext context, ModuleChunk chunk,
            DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget> dirtyFilesHolder,
            OutputConsumer outputConsumer) throws ProjectBuildException, IOException {
        return ExitCode.OK;
    }

    @NotNull
    @Override
    public String getPresentableName() {
        return "TeaVM builder";
    }
}
