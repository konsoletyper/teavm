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

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.BuildTargetLoader;
import org.jetbrains.jps.builders.ModuleBasedBuildTargetType;
import org.jetbrains.jps.incremental.ModuleBuildTarget;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.module.JpsModule;

public class TeaVMBuildTargetType extends ModuleBasedBuildTargetType<TeaVMBuildTarget> {
    public static final TeaVMBuildTargetType INSTANCE = new TeaVMBuildTargetType("teavm");

    public TeaVMBuildTargetType(String typeId) {
        super(typeId);
    }

    @NotNull
    @Override
    public List<TeaVMBuildTarget> computeAllTargets(@NotNull JpsModel model) {
        List<JpsModule> modules = model.getProject().getModules();
        List<TeaVMBuildTarget> targets = new ArrayList<>(modules.size());
        for (JpsModule module : modules) {
            if (module.getContainer().getChild(TeaVMModuleExtension.ROLE) != null) {
                targets.add(new TeaVMBuildTarget(this, module));
            }
        }
        return targets;
    }

    @NotNull
    @Override
    public BuildTargetLoader<TeaVMBuildTarget> createLoader(@NotNull JpsModel model) {
        return null;
    }
}
