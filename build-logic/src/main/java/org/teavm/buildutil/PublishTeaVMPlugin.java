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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.publish.plugins.PublishingPlugin;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.CoreJavadocOptions;
import org.gradle.plugins.signing.SigningExtension;
import org.gradle.plugins.signing.SigningPlugin;

public abstract class PublishTeaVMPlugin implements Plugin<Project> {
    private static final String EXTENSION_NAME = "teavmPublish";

    @Override
    public void apply(Project target) {
        target.getPlugins().apply(PublishingPlugin.class);
        target.getPlugins().apply(MavenPublishPlugin.class);

        var publish = Boolean.parseBoolean(target.getProviders().gradleProperty("teavm.mavenCentral.publish")
                .getOrElse("false"));
        if (publish) {
            target.getPlugins().apply(SigningPlugin.class);
        }

        var extension = new ExtensionImpl();
        target.getExtensions().add(PublishTeaVMExtension.class, EXTENSION_NAME, extension);

        target.afterEvaluate(p -> target.getExtensions().configure(PublishingExtension.class, publishing -> {
            var pluginMavenPublication = publishing.getPublications().findByName("pluginMaven");
            if (pluginMavenPublication == null) {
                publishing.publications(publications -> {
                    publications.create("java", MavenPublication.class, publication -> {
                        customizePublication(target, publication, extension, true);
                    });
                });
            } else {
                customizePublication(target, (MavenPublication) pluginMavenPublication, extension, false);
            }
            if (publish) {
                var signing = target.getExtensions().getByType(SigningExtension.class);
                publishing.getPublications().configureEach(signing::sign);
            }
            publishing.repositories(repositories -> {
                var url = target.getProviders().gradleProperty("teavm.publish.url");
                if (url.isPresent()) {
                    repositories.maven(repository -> {
                        repository.setName("teavm");
                        repository.setUrl(url.get());
                        repository.getCredentials().setUsername(target.getProviders().gradleProperty(
                                "teavm.publish.username").get());
                        repository.getCredentials().setPassword(target.getProviders().gradleProperty(
                                "teavm.publish.password").get());
                    });
                }
                if (publish) {
                    repositories.maven(repository -> {
                        repository.setName("OSSRH");
                        repository.setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2");
                        repository.getCredentials().setUsername(target.getProviders().gradleProperty(
                                "ossrhUsername").get());
                        repository.getCredentials().setPassword(target.getProviders().gradleProperty(
                                "ossrhPassword").get());
                    });
                }
            });
        }));

        target.getExtensions().configure(JavaPluginExtension.class, ext -> {
            ext.withSourcesJar();
            ext.withJavadocJar();
        });

        target.getTasks().withType(Javadoc.class, task -> {
            var options = (CoreJavadocOptions) task.getOptions();
            options.addStringOption("Xdoclint:none", "-quiet");
        });
    }

    private void customizePublication(Project project, MavenPublication publication, ExtensionImpl extension,
            boolean includeComponent) {
        publication.setGroupId("org.teavm");
        if (extension.getArtifactId() != null) {
            publication.setArtifactId(extension.getArtifactId());
        }
        if (includeComponent) {
            publication.from(project.getComponents().getByName("java"));
        }
        if (extension.packaging != null) {
            publication.getPom().setPackaging(extension.packaging);
        }
    }

    private static class ExtensionImpl implements PublishTeaVMExtension {
        private String artifactId;
        private String packaging;

        @Override
        public String getArtifactId() {
            return artifactId;
        }

        @Override
        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        @Override
        public String getPackaging() {
            return packaging;
        }

        @Override
        public void setPackaging(String packaging) {
            this.packaging = packaging;
        }
    }
}
