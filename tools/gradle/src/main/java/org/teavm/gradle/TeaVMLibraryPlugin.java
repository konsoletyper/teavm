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

import javax.inject.Inject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.teavm.gradle.api.TeaVMBaseExtension;
import org.teavm.gradle.config.ArtifactCoordinates;

public class TeaVMLibraryPlugin implements Plugin<Project> {
    private ObjectFactory objectFactory;

    @Inject
    public TeaVMLibraryPlugin(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }

    @Override
    public void apply(Project project) {
        var extension = new TeaVMBaseExtensionImpl(project, objectFactory);
        project.getExtensions().add(TeaVMBaseExtension.class, TeaVMPlugin.EXTENSION_NAME, extension);
        project.getDependencies().add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, ArtifactCoordinates.JUNIT);
        project.getDependencies().add(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME, ArtifactCoordinates.CLASSLIB);
        TeaVMTestConfigurator.configure(project, extension.getTests());
    }
}
