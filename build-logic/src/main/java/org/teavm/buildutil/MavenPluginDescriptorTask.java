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
import javax.inject.Inject;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.generator.GeneratorException;
import org.apache.maven.tools.plugin.generator.PluginDescriptorFilesGenerator;
import org.apache.maven.tools.plugin.scanner.MojoScanner;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

public abstract class MavenPluginDescriptorTask extends DefaultTask {
    @Input
    public abstract Property<String> getGroupId();

    @Input
    public abstract Property<String> getArtifactId();

    @Input
    public abstract Property<String> getVersion();

    @Input
    public abstract Property<String> getPluginName();

    @Input
    @Optional
    public abstract Property<String> getPluginDescription();

    @InputDirectory
    public abstract Property<File> getClassesDirectory();

    @OutputDirectory
    public abstract Property<File> getOutputDirectory();

    @Inject
    public MavenPluginDescriptorTask() {
        var project = getProject();
        getPluginName().convention(project.provider(project::getName));
        getPluginDescription().convention(project.provider(() -> project.getDescription() != null
                ? project.getDescription() : ""));
        getVersion().convention(project.provider(() -> project.getVersion().toString()));
        getGroupId().convention(project.provider(() -> project.getGroup().toString()));
        getArtifactId().convention(project.provider(project::getName));
    }

    @TaskAction
    public void generate() throws ExtractionException, InvalidPluginDescriptorException, GeneratorException,
            PlexusContainerException, ComponentLookupException {
        var cc = new DefaultContainerConfiguration()
                .setClassPathScanning(PlexusConstants.SCANNING_INDEX)
                .setAutoWiring(true)
                .setJSR250Lifecycle(true)
                .setName("maven");
        var container = new DefaultPlexusContainer(cc);
        var scanner = container.lookup(MojoScanner.class);
        var descriptor = new PluginDescriptor();
        descriptor.setGroupId(getGroupId().get());
        descriptor.setArtifactId(getArtifactId().get());
        descriptor.setVersion(getVersion().get());
        descriptor.setName(getPluginName().get());
        descriptor.setDescription(getPluginDescription().get());

        var project = new MavenProject();
        project.getBuild().setOutputDirectory(getClassesDirectory().get().getAbsolutePath());
        project.getBuild().setDirectory(getOutputDirectory().get().getAbsolutePath());
        project.setGroupId(getGroupId().get());
        project.setArtifactId(getArtifactId().get());
        project.setVersion(getVersion().get());
        project.setArtifact(new DefaultArtifact(getGroupId().get(), getArtifactId().get(),
                getVersion().get(), "compile", "jar", "", new DefaultArtifactHandler()));

        var request = new DefaultPluginToolsRequest(project, descriptor);
        request.setEncoding("UTF-8");
        request.setSkipErrorNoDescriptorsFound(false);

        scanner.populatePluginDescriptor(request);

        var out = getOutputDirectory().get();
        out.getParentFile().mkdirs();

        var pluginDescriptorGenerator = new PluginDescriptorFilesGenerator();
        pluginDescriptorGenerator.execute(out, request);
    }
}
