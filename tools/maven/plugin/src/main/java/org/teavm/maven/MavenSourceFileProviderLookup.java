/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.teavm.tooling.builder.BuildStrategy;

public class MavenSourceFileProviderLookup {
    private MavenProject mavenProject;
    private RepositorySystem repositorySystem;
    private ArtifactRepository localRepository;
    private List<? extends ArtifactRepository> remoteRepositories;
    private List<Artifact> pluginDependencies;

    public void setMavenProject(MavenProject mavenProject) {
        this.mavenProject = mavenProject;
    }

    public void setRepositorySystem(RepositorySystem repositorySystem) {
        this.repositorySystem = repositorySystem;
    }

    public void setLocalRepository(ArtifactRepository localRepository) {
        this.localRepository = localRepository;
    }

    public void setRemoteRepositories(List<? extends ArtifactRepository> remoteRepositories) {
        this.remoteRepositories = remoteRepositories;
    }

    public void setPluginDependencies(List<Artifact> pluginDependencies) {
        this.pluginDependencies = pluginDependencies;
    }

    public void resolve(BuildStrategy builder) {
        List<Artifact> initialArtifacts = new ArrayList<>();
        initialArtifacts.addAll(mavenProject.getArtifacts());
        if (pluginDependencies != null) {
            initialArtifacts.addAll(pluginDependencies);
        }
        Set<Artifact> artifacts = new HashSet<>();
        for (Artifact artifact : initialArtifacts) {
            if (artifact.getClassifier() != null && artifact.getClassifier().equals("sources")) {
                artifacts.add(artifact);
            } else {
                artifacts.add(repositorySystem.createArtifactWithClassifier(artifact.getGroupId(),
                        artifact.getArtifactId(), artifact.getVersion(), artifact.getType(), "sources"));
            }
        }

        artifacts.addAll(initialArtifacts);
        for (Artifact artifact : artifacts) {
            ArtifactResolutionRequest request = new ArtifactResolutionRequest()
                    .setLocalRepository(localRepository)
                    .setRemoteRepositories(new ArrayList<>(remoteRepositories))
                    .setArtifact(artifact);
            ArtifactResolutionResult result = repositorySystem.resolve(request);
            for (Artifact resolvedArtifact : result.getArtifacts()) {
                if (resolvedArtifact.getFile() != null) {
                    File file = resolvedArtifact.getFile();
                    if (!file.isDirectory()) {
                        builder.addSourcesJar(file.getAbsolutePath());
                    } else {
                        builder.addSourcesDirectory(file.getAbsolutePath());
                    }
                }
            }
        }
        for (String sourceRoot : mavenProject.getCompileSourceRoots()) {
            builder.addSourcesDirectory(new File(sourceRoot).getAbsolutePath());
        }
    }
}
