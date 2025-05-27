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
import org.jreleaser.gradle.plugin.JReleaserExtension;
import org.jreleaser.gradle.plugin.JReleaserPlugin;
import org.jreleaser.model.Active;
import org.jreleaser.model.Signing;
import org.jreleaser.model.api.deploy.maven.MavenCentralMavenDeployer;

public abstract class PublishTeaVMPlugin implements Plugin<Project> {
    private static final String EXTENSION_NAME = "teavmPublish";

    @Override
    public void apply(Project target) {
        target.getPlugins().apply(PublishingPlugin.class);
        target.getPlugins().apply(MavenPublishPlugin.class);

        var publish = Boolean.parseBoolean(target.getProviders().gradleProperty("teavm.mavenCentral.publish")
                .getOrElse("false"));
        if (publish) {
            target.getPlugins().apply(JReleaserPlugin.class);
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
                var jreleaser = target.getExtensions().getByType(JReleaserExtension.class);
                jreleaser.getGitRootSearch().set(true);
                jreleaser.signing(signing -> {
                    var providers = target.getProviders();
                    signing.getActive().set(Active.ALWAYS);
                    signing.getArmored().set(true);
                    signing.getMode().set(Signing.Mode.COMMAND);
                    signing.command(command -> {
                        command.getKeyName().set(providers.gradleProperty("teavm.publish.gpg.keyName"));
                        command.getPublicKeyring().set(providers.gradleProperty(
                                "teavm.publish.gpg.secretKeyRingFile"));
                        command.getDefaultKeyring().set(true);
                        command.getArgs().add("--no-random-seed-file");
                    });
                    signing.getPassphrase().set(providers.gradleProperty("teavm.publish.gpg.password"));
                });
                jreleaser.deploy(deploy -> {
                    deploy.maven(maven -> {
                        maven.pomchecker(pomchecker -> {
                            pomchecker.getFailOnError().set(false);
                            pomchecker.getFailOnWarning().set(false);
                            pomchecker.getStrict().set(false);
                        });
                        maven.mavenCentral(mavenCentral -> {
                            var sonatype = maven.getMavenCentral().maybeCreate("sonatype");
                            sonatype.getActive().set(Active.ALWAYS);
                            sonatype.getUrl().set("https://central.sonatype.com/api/v1/publisher");
                            sonatype.getStagingRepositories().add("build/staging-deploy");
                            sonatype.getUsername().set(target.getProviders().gradleProperty("ossrhUsername"));
                            sonatype.getPassword().set(target.getProviders().gradleProperty("ossrhPassword"));
                            sonatype.getStage().set(MavenCentralMavenDeployer.Stage.FULL);
                            sonatype.getSign().set(true);
                            sonatype.getApplyMavenCentralRules().set(false);
                        });
                    });
                });
                jreleaser.release(release -> {
                    release.github(github -> {
                        github.getSkipRelease().set(true);
                        github.getToken().set("123");
                    });
                });
            }
            publishing.repositories(repositories -> {
                var url = target.getProviders().gradleProperty("teavm.publish.url");
                if (url.isPresent()) {
                    repositories.maven(repository -> {
                        repository.setName("teavm");
                        repository.setUrl(url.get());
                        repository.getCredentials().setUsername(target.getProviders().gradleProperty(
                                "ossrhUsername").get());
                        repository.getCredentials().setPassword(target.getProviders().gradleProperty(
                                "ossrhPassword").get());
                    });
                } else {
                    repositories.maven(repository -> {
                        repository.setName("teavm");
                        repository.setUrl(target.getLayout().getBuildDirectory().dir("staging-deploy"));
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
