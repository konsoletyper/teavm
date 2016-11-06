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

import com.intellij.facet.FacetManager;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.module.Module;
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
import org.teavm.idea.TeaVMFacet;
import org.teavm.idea.TeaVMFacetType;
import org.teavm.idea.jps.model.TeaVMJpsConfiguration;

public class TeaVMMavenImporter extends MavenImporter {
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
        FacetManager facetManager = FacetManager.getInstance(module);

        for (MavenPlugin mavenPlugin : mavenProject.getPlugins()) {
            if (mavenPlugin.getGroupId().equals(myPluginGroupID)
                    && mavenPlugin.getArtifactId().equals(myPluginArtifactID)) {
                updateConfiguration(mavenPlugin, facetManager, module);
            }
        }
    }

    private void updateConfiguration(MavenPlugin plugin, FacetManager facetManager, Module module) {
        if (plugin.getConfigurationElement() != null) {
            updateConfiguration(plugin.getConfigurationElement(), facetManager, module);
        }

        for (MavenPlugin.Execution execution : plugin.getExecutions()) {
            if (execution.getGoals().contains("compile")) {
                if (execution.getConfigurationElement() != null) {
                    updateConfiguration(execution.getConfigurationElement(), facetManager, module);
                }
                break;
            }
        }
    }

    private void updateConfiguration(Element source, FacetManager facetManager, Module module) {
        TeaVMFacet facet = facetManager.getFacetByType(TeaVMFacetType.TYPE_ID);
        if (facet == null) {
            TeaVMFacetType type = TeaVMFacetType.getInstance();
            facet = new TeaVMFacet(module, "TeaVM (JS)", type.createDefaultConfiguration());
            facetManager.addFacet(type, facet.getName(), facet);
        }

        TeaVMJpsConfiguration configuration = facet.getConfiguration().getState();

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

        facet.getConfiguration().loadState(configuration);
    }
}
