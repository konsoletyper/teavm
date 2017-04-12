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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.builders.BuildTargetLoader;
import org.jetbrains.jps.builders.ModuleBasedBuildTargetType;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.module.JpsModule;
import org.teavm.idea.jps.model.TeaVMJpsConfiguration;
import org.teavm.tooling.TeaVMTargetType;

public class TeaVMBuildTargetType extends ModuleBasedBuildTargetType<TeaVMBuildTarget> {
    public static final String ID = "teavm";
    public static final TeaVMBuildTargetType INSTANCE = new TeaVMBuildTargetType();

    private TeaVMBuildTargetType() {
        super(ID);
    }

    @NotNull
    @Override
    public List<TeaVMBuildTarget> computeAllTargets(@NotNull JpsModel model) {
        List<TeaVMBuildTarget> targets = new ArrayList<>();
        for (JpsModule module : model.getProject().getModules()) {
            for (TeaVMJpsConfiguration config : TeaVMJpsConfiguration.getAll(module)) {
                targets.add(new TeaVMBuildTarget(this, module, config));
            }
        }
        return targets;
    }

    @NotNull
    @Override
    public BuildTargetLoader<TeaVMBuildTarget> createLoader(@NotNull JpsModel model) {
        return new Loader(model);
    }

    class Loader extends BuildTargetLoader<TeaVMBuildTarget> {
        private final Map<String, Configuration> modules;

        public Loader(JpsModel model) {
            modules = new HashMap<>();
            for (JpsModule module : model.getProject().getModules()) {
                for (TeaVMJpsConfiguration config : TeaVMJpsConfiguration.getAll(module)) {
                    modules.put(module.getName() + "-" + config.getTargetType().name(),
                            new Configuration(module, config.getTargetType()));
                }
            }
        }

        @Nullable
        @Override
        public TeaVMBuildTarget createTarget(@NotNull String targetId) {
            Configuration configuration = modules.get(targetId);
            if (configuration == null) {
                return null;
            }

            for (TeaVMJpsConfiguration jpsConfig : TeaVMJpsConfiguration.getAll(configuration.module)) {
                if (jpsConfig.getTargetType() == configuration.type) {
                    return new TeaVMBuildTarget(TeaVMBuildTargetType.this, configuration.module, jpsConfig);
                }
            }

            return null;
        }
    }

    class Configuration {
        final JpsModule module;
        final TeaVMTargetType type;

        public Configuration(@NotNull JpsModule module, @NotNull TeaVMTargetType type) {
            this.module = module;
            this.type = type;
        }
    }
}
