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
package org.teavm.idea.maven;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import java.util.List;
import java.util.Map;
import org.jdom.Element;
import org.jetbrains.idea.maven.importing.MavenImporter;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
import org.jetbrains.idea.maven.model.MavenPlugin;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.project.MavenProjectsTree;
import org.teavm.idea.TeaVMConfigurationStorage;
import org.teavm.idea.jps.model.TeaVMJpsConfiguration;

public class TeaVMMavenImporter extends MavenImporter {
    private static final Logger logger = Logger.getInstance(TeaVMMavenImporter.class);

    public TeaVMMavenImporter() {
        super("org.teavm", "teavm-maven-plugin");
    }

    @Override
    public void preProcess(Module module, MavenProject mavenProject, MavenProjectChanges changes,
            IdeModifiableModelsProvider modifiableModelsProvider) {
    }

    @Override
    public void process(IdeModifiableModelsProvider modifiableModelsProvider, Module module,
            MavenRootModelAdapter rootModel, MavenProjectsTree mavenModel, MavenProject mavenProject,
            MavenProjectChanges changes, Map<MavenProject, String> mavenProjectToModuleName,
            List<MavenProjectsProcessorTask> postTasks) {
        TeaVMConfigurationStorage configurationStorage = ModuleServiceManager.getService(module,
                TeaVMConfigurationStorage.class);
        if (configurationStorage == null) {
            logger.warn("Could not load component to retrieve TeaVM build configuration");
            return;
        }

        TeaVMJpsConfiguration configuration = configurationStorage.getState();

        for (MavenPlugin mavenPlugin : mavenProject.getPlugins()) {
            if (mavenPlugin.getGroupId().equals(myPluginGroupID)
                    && mavenPlugin.getArtifactId().equals(myPluginArtifactID)) {
                updateConfiguration(mavenPlugin, configuration);
            }
        }

        configurationStorage.loadState(configuration);
    }

    private void updateConfiguration(MavenPlugin plugin, TeaVMJpsConfiguration configuration) {
        if (plugin.getConfigurationElement() != null) {
            updateConfiguration(plugin.getConfigurationElement(), configuration);
        }
        for (MavenPlugin.Execution execution : plugin.getExecutions()) {
            if (execution.getGoals().contains("compile")) {
                if (execution.getConfigurationElement() != null) {
                    updateConfiguration(execution.getConfigurationElement(), configuration);
                }
                break;
            }
        }
    }

    private void updateConfiguration(Element source, TeaVMJpsConfiguration configuration) {
        configuration.setEnabled(true);
        for (Element child : source.getChildren()) {
            switch (child.getName()) {
                case "sourceFilesCopied":
                    configuration.setSourceFilesCopied(Boolean.parseBoolean(child.getTextTrim()));
                    break;
                case "sourceMapsGenerated":
                    configuration.setSourceMapsFileGenerated(Boolean.parseBoolean(child.getTextTrim()));
                    break;
                case "minifying":
                    configuration.setMinifying(Boolean.parseBoolean(child.getTextTrim()));
                    break;
                case "targetDirectory":
                    configuration.setTargetDirectory(child.getTextTrim());
                    break;
                case "mainClass":
                    configuration.setMainClass(child.getTextTrim());
                    break;
            }
        }
    }
}
