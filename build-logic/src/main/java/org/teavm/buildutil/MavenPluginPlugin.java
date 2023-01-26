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
package org.teavm.buildutil;

import java.io.File;
import java.util.Map;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.SourceSetContainer;

public class MavenPluginPlugin implements Plugin<Project> {
    private static final String GENERATED_DIR = "generated/resources/maven-plugin";
    private static final String PLUGIN_DIR = "META-INF/maven";

    @Override
    public void apply(Project project) {
        project.getPlugins().apply(JavaLibraryPlugin.class);

        installDependencies(project);

        var generatedDir = new File(project.getBuildDir(), GENERATED_DIR);
        var pluginDir = new File(generatedDir, PLUGIN_DIR);

        var task = project.getTasks().create("generateMavenPluginDescriptor", MavenPluginDescriptorTask.class);
        task.getOutputDirectory().convention(pluginDir);
        var thisArtifact = project.provider(() -> getProjectArtifact(project));
        task.getGroupId().convention(thisArtifact.map(a -> a.groupId));
        task.getArtifactId().convention(thisArtifact.map(a -> a.artifactId));
        task.getVersion().convention(thisArtifact.map(a -> a.version));

        var sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        var main = sourceSets.getByName("main");
        task.getClassesDirectory().convention(main.getOutput().getClassesDirs().getSingleFile());
        task.dependsOn(project.getTasks().getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME));
        main.getOutput().dir(Map.of("builtBy", task), generatedDir);
    }

    private void installDependencies(Project project) {
        var config = JavaPlugin.COMPILE_ONLY_API_CONFIGURATION_NAME;
        project.getDependencies().add(config, Config.MAVEN_PLUGIN_API);
        project.getDependencies().add(config, Config.MAVEN_PLUGIN_ANNOTATIONS);
        project.getDependencies().add(config, Config.MAVEN_ARTIFACT);
        project.getDependencies().add(config, Config.MAVEN_CORE);
    }

    private MavenArtifact getProjectArtifact(Project project) {
        var ext = project.getExtensions().getByType(PublishingExtension.class);
        for (var publication : ext.getPublications()) {
            if (publication instanceof MavenPublication) {
                var mavenPublication = (MavenPublication) publication;
                return new MavenArtifact(mavenPublication.getGroupId(), mavenPublication.getArtifactId(),
                        mavenPublication.getVersion());
            }
        }
        return new MavenArtifact(project.getGroup().toString(), project.getName(), project.getVersion().toString());
    }
}
